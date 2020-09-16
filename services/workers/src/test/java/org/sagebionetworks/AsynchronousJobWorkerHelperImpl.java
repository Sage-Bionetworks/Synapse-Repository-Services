package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.SubmissionView;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class AsynchronousJobWorkerHelperImpl implements AsynchronousJobWorkerHelper {

	public static class AsyncJobResponse<T extends AsynchronousResponseBody> {
		private final T response;
		private final String token;
		private final long startTime;
		private final int tries;

		private AsyncJobResponse(T response, String token, long startTime, int tries) {
			this.response = response;
			this.token = token;
			this.startTime = startTime;
			this.tries = tries;
		}

		public String getJobToken() {
			return token;
		}

		public long getStartTime() {
			return startTime;
		}

		public int getTries() {
			return tries;
		}

		public T getResponse() {
			return response;
		}

	}
	
	private static class AsyncJobTimeoutException extends AsynchJobFailedException {
		public AsyncJobTimeoutException(AsynchronousJobStatus status) {
			super(status);
		}
	}
	
	private static class AsyncJobExecutor<R extends AsynchronousResponseBody> {
		
		private final AsynchJobStatusManager manager;
		private final AsynchronousRequestBody request;
		private final long maxWaitMs;
		private final int maxRetries;
		private final String jobName;
		
		private long statusCheckFrequency = STATUS_CHECK_FREQUENCY;
		private int tries = 0;
		
		
		private AsyncJobExecutor(AsynchJobStatusManager manager, AsynchronousRequestBody request, long maxWaitMs, int maxRetries) {
			this.manager = manager;
			this.request = request;
			this.maxWaitMs = maxWaitMs;
			this.maxRetries = maxRetries;
			this.jobName = request.getClass().getSimpleName();
		}

		public AsyncJobResponse<R> execute(UserInfo user, Consumer<R> responseConsumer) throws AsynchJobFailedException {
			long startTime = System.currentTimeMillis();

			AsynchronousJobStatus status = null;
			AssertionError lastException = null;
			
			while (maxRetries == INFINITE_RETRIES || tries < maxRetries) {
				
				try {
					status = waitForJobStatus(user, startTime);
				} catch (AsyncJobTimeoutException e) {
					status = e.getStatus();
					
					if (lastException == null) {
						return fail(logErrorMessage(e.getStatus(), "timed out"));
					}
					
					return fail(logErrorMessage(e.getStatus(), "timed out with assert failure"), lastException);
					
				}
				
				@SuppressWarnings("unchecked")
				R response = (R) status.getResponseBody();
				
				try {
					responseConsumer.accept(response);
				} catch (AssertionError e) {
					lastException = e;
					logMessage(status, "results invalid, retrying...");
					// Applies exponential back-off for retrying
					statusCheckFrequency *= 1.2;
					continue;
				}

				logMessage(status, "completed");
				
				// The consumer didn't throw, we are done
				return new AsyncJobResponse<R>(response, status.getJobId(), startTime, tries);
				
			}
			
			// We reached the max number of jobs, fail
			return fail(logErrorMessage(status, "failed, number of tries exhausted"), lastException);
		}
		
		private AsynchronousJobStatus waitForJobStatus(UserInfo user, long startTime) throws AsynchJobFailedException {
			AsynchronousJobStatus status = manager.startJob(user, request);
			tries++;
			logMessage(status, "submitted");
			
			for(;;) {
				long elapsedTime = System.currentTimeMillis() - startTime;

				if (elapsedTime >= maxWaitMs) {
					throw new AsyncJobTimeoutException(status);
				}
				
				try {
					Thread.sleep(statusCheckFrequency);
				} catch (InterruptedException e) {
					return fail(e.getMessage(), e);
				}
				
				status = manager.getJobStatus(user, status.getJobId());
				
				try {
					AsynchJobUtils.throwExceptionIfFailed(status);
				} catch (Throwable e) {
					if (e instanceof RuntimeException) {
						throw (RuntimeException) e;
					} else if (e instanceof AsynchJobFailedException) {
						throw (AsynchJobFailedException) e;
					} else {
						throw new RuntimeException(e);
					}
				}
				
				if (AsynchJobState.PROCESSING == status.getJobState()) {
					logMessage(status, "results not ready, waiting...");
					continue;
				}

				return status;	
			}
		}
		
		private String logErrorMessage(AsynchronousJobStatus status, String message) {
			String errorMessage = String.format(
					"%s Job %s (Token: %s, Status: %s, Tries: %d)",
					jobName, message, status.getJobId(), status.getJobState().name(), tries);
			
			LOG.error(errorMessage);
			
			return errorMessage;
		}
		
		private void logMessage(AsynchronousJobStatus status, String message) {
			LOG.info("{} Job {} (Token: {}, Status: {}, Tries: {})", jobName, message, status.getJobId(), status.getJobState(), tries);
		}
	}

	private static final Logger LOG = LogManager.getLogger(AsynchronousJobWorkerHelperImpl.class);
	private static final int STATUS_CHECK_FREQUENCY = 1000;
	private static final int MAX_QUERY_RETRY = 20;

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private ConnectionFactory tableConnectionFactory;
	@Autowired
	private TableManagerSupport tableMangerSupport;
	@Autowired
	private TableViewManager tableViewManager;
	@Autowired
	private TableEntityManager tableEntityManager;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private SynapseS3Client s3Client;

	@Override
	public <R extends AsynchronousRequestBody, T extends AsynchronousResponseBody> AsyncJobResponse<T> assertJobResponse(
			UserInfo user, R request, Consumer<T> responseConsumer, long maxWaitMs)
			throws AssertionError, AsynchJobFailedException {
		return assertJobResponse(user, request, responseConsumer, maxWaitMs, 1);
	}

	@Override
	public <R extends AsynchronousRequestBody, T extends AsynchronousResponseBody> AsyncJobResponse<T> assertJobResponse(
			UserInfo user, R request, Consumer<T> responseConsumer, long maxWaitMs, int maxRetries)
			throws AssertionError, AsynchJobFailedException {
		ValidateArgument.required(request, "The request body");
		ValidateArgument.requirement(maxWaitMs > 0, "The max wait time must be greater than 0");
		ValidateArgument.requirement(maxRetries == INFINITE_RETRIES || maxRetries > 0, "The number of maxRetries should be greater than 0 or equal to " + INFINITE_RETRIES);
		
		AsyncJobExecutor<T> executor = new AsyncJobExecutor<>(asynchJobStatusManager, request, maxWaitMs, maxRetries);
		
		return executor.execute(user, responseConsumer);

	}

	@Override
	public QueryResultBundle assertQueryResult(UserInfo user, String sql, Consumer<QueryResultBundle> resultMatcher,
			long maxWaitTime) throws AssertionError, AsynchJobFailedException {
		Query query = new Query();
		query.setSql(sql);
		query.setIncludeEntityEtag(true);
		
		QueryOptions options = new QueryOptions()
				.withRunQuery(true)
				.withRunCount(true)
				.withReturnFacets(false)
				.withReturnColumnModels(true);
		
		return assertQueryResult(user, query, options, resultMatcher, maxWaitTime);
	}

	@Override
	public QueryResultBundle assertQueryResult(UserInfo user, Query query, QueryOptions options,
			Consumer<QueryResultBundle> resultMatcher, long maxWaitTime) throws AssertionError, AsynchJobFailedException {
		QueryBundleRequest request = new QueryBundleRequest();

		request.setQuery(query);
		request.setPartMask(options.getPartMask());

		return assertJobResponse(user, request, resultMatcher, maxWaitTime, MAX_QUERY_RETRY).getResponse();
	}

	/**
	 * Wait for EntityReplication to show the given etag for the given entityId.
	 * 
	 * @param tableId
	 * @param entityId
	 * @param etag
	 * @return
	 * @throws InterruptedException
	 */
	@Override
	public ObjectDataDTO waitForEntityReplication(UserInfo user, String tableId, String entityId, long maxWaitMS)
			throws InterruptedException {
		Entity entity = entityManager.getEntity(user, entityId);
		return waitForObjectReplication(ViewObjectType.ENTITY, KeyFactory.stringToKey(entity.getId()), entity.getEtag(),
				maxWaitMS);
	}

	@Override
	public ObjectDataDTO waitForObjectReplication(ViewObjectType objectType, Long objectId, String etag, long maxWaitMS)
			throws InterruptedException {
		TableIndexDAO indexDao = tableConnectionFactory.getFirstConnection();
		long start = System.currentTimeMillis();
		while (true) {
			ObjectDataDTO dto = indexDao.getObjectData(objectType, objectId);
			if (dto == null || !dto.getEtag().equals(etag)) {
				assertTrue((System.currentTimeMillis() - start) < maxWaitMS, "Timed out waiting for object replication.");
				LOG.info("Waiting for object replication...(Type: {}, Id: {}, Etag: {})", objectType.name(), objectId, etag);
				Thread.sleep(STATUS_CHECK_FREQUENCY);
			} else {
				return dto;
			}
		}
	}

	/**
	 * Create a View with the default schema for its type.
	 * 
	 * @param user
	 * @param name
	 * @param parentId
	 * @param schema
	 * @param scope
	 * @param viewTypeMask
	 * @return
	 */
	@Override
	public EntityView createView(UserInfo user, String name, String parentId, List<String> scope, long viewTypeMask) {
		ViewEntityType entityType = ViewEntityType.entityview;
		List<ColumnModel> defaultColumns = tableMangerSupport.getDefaultTableViewColumns(entityType, viewTypeMask);
		EntityView view = new EntityView();
		view.setName(name);
		view.setViewTypeMask(viewTypeMask);
		view.setParentId(parentId);
		view.setColumnIds(TableModelUtils.getIds(defaultColumns));
		view.setScopeIds(scope);
		String viewId = entityManager.createEntity(user, view, null);
		view = entityManager.getEntity(user, viewId, EntityView.class);
		ViewScope viewScope = new ViewScope();
		viewScope.setViewEntityType(entityType);
		viewScope.setScope(view.getScopeIds());
		viewScope.setViewTypeMask(viewTypeMask);
		tableViewManager.setViewSchemaAndScope(user, view.getColumnIds(), viewScope, viewId);
		return view;
	}

	@Override
	public SubmissionView createSubmissionView(UserInfo user, String name, String parentId, List<String> scope) {
		ViewEntityType entityType = ViewEntityType.submissionview;
		Long typeMask = 0L;
		List<ColumnModel> defaultColumns = tableMangerSupport.getDefaultTableViewColumns(entityType, typeMask);
		SubmissionView view = new SubmissionView();
		view.setName(name);
		view.setParentId(parentId);
		view.setColumnIds(TableModelUtils.getIds(defaultColumns));
		view.setScopeIds(scope);
		String viewId = entityManager.createEntity(user, view, null);
		view = entityManager.getEntity(user, viewId, SubmissionView.class);

		ViewScope viewScope = new ViewScope();
		viewScope.setViewEntityType(entityType);
		viewScope.setScope(view.getScopeIds());
		viewScope.setViewTypeMask(typeMask);

		tableViewManager.setViewSchemaAndScope(user, view.getColumnIds(), viewScope, viewId);

		return view;
	}

	@Override
	public void setTableSchema(UserInfo userInfo, List<String> newSchema, String tableId, long maxWaitMS)
			throws InterruptedException {
		long start = System.currentTimeMillis();
		while (true) {
			try {
				tableEntityManager.setTableSchema(userInfo, newSchema, tableId);
				return;
			} catch (TemporarilyUnavailableException e) {
				LOG.info("Waiting for excluisve lock on {}...", tableId);
				Thread.sleep(1000);
			}
			assertTrue((System.currentTimeMillis() - start) < maxWaitMS, "Timed out Waiting for excluisve lock on " + tableId);
		}
	}

	/**
	 * Helper to download the contents of the given FileHandle ID to a string.
	 * 
	 * @param fileHandleId
	 * @return
	 * @throws IOException
	 */
	@Override
	public String downloadFileHandleFromS3(String fileHandleId) throws IOException {
		FileHandle fh = fileHandleDao.get(fileHandleId);
		if (!(fh instanceof S3FileHandle)) {
			throw new IllegalArgumentException("Not a S3 file handle: " + fh.getClass().getName());
		}
		S3FileHandle s3Handle = (S3FileHandle) fh;
		try (Reader reader = new InputStreamReader(
				s3Client.getObject(s3Handle.getBucketName(), s3Handle.getKey()).getObjectContent(),
				StandardCharsets.UTF_8)) {
			return IOUtils.toString(reader);
		}
	}
	
	@Override
	public void emptyAllQueues() {
		asynchJobStatusManager.emptyAllQueues();
	}

}
