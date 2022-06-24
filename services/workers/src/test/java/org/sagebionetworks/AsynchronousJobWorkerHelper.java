package org.sagebionetworks;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.sagebionetworks.AsynchronousJobWorkerHelperImpl.AsyncJobResponse;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubmissionView;
import org.sagebionetworks.repo.model.table.TableEntity;

public interface AsynchronousJobWorkerHelper {

	int INFINITE_RETRIES = -1;

	/**
	 * See
	 * {@link #assertJobResponse(UserInfo, AsynchronousRequestBody, Consumer, int, int)},
	 * with only 1 retry
	 */
	<R extends AsynchronousRequestBody, T extends AsynchronousResponseBody> AsyncJobResponse<T> assertJobResponse(
			UserInfo user, R request, Consumer<T> responseConsumer, long maxWaitMs)
			throws AssertionError, AsynchJobFailedException;

	/**
	 * Starts and wait for a job completion with exponential back-off retry logic,
	 * the given consumer might check for assertions to wait for consistent results,
	 * if an {@link AssertionError} is thrown the method will restart a job until
	 * the max number of retries is exhausted.
	 * 
	 * @param <R>              The type of request
	 * @param <T>              The type of response
	 * @param user             The user that starts the job
	 * @param request          The job request body
	 * @param responseConsumer The consumer of the response, might perform asserts.
	 *                         If an {@link AssertionError} is thrown a new job will
	 *                         be started if the number of retries is not exhausted
	 * @param maxWaitMs        The timeout in milliseconds
	 * @param maxRetries       The maximum number of jobs to start
	 * @return The job response with additional metrics about the job
	 * @throws AssertionError If the job times out or if the max number of retries is exhausted
	 * @throws AsynchJobFailedException If the job failed for other reasons and an exception was not set for the job failure
	 */
	<R extends AsynchronousRequestBody, T extends AsynchronousResponseBody> AsyncJobResponse<T> assertJobResponse(
			UserInfo user, R request, Consumer<T> responseConsumer, long maxWaitMs, int maxRetries)
			throws AssertionError, AsynchJobFailedException;

	/**
	 * See {@link #assertQueryResult(UserInfo, Query, QueryOptions, Consumer, long)}. Will use as default query options
	 * the inclusion of the results and the column model as well as the count. The etag will be included in the results.
	 */
	QueryResultBundle assertQueryResult(UserInfo user, String sql, Consumer<QueryResultBundle> resultMatcher,
			long maxWaitMs) throws AssertionError, AsynchJobFailedException;

	/**
	 * Run the given query with the given options in a background job waiting for
	 * the results that will be passed to the given consumer. The job will retry and
	 * resubmit the query for a maximum of 10 times with exponential back-off if the
	 * consumer throws an {@link AssertionError}
	 * 
	 * @param user          The user that runs the query
	 * @param query         The query to run
	 * @param options       The query options
	 * @param resultMatcher A {@link Consumer} that will be supplied with the
	 *                      results of the query, might throw {@link AssertionError}
	 *                      if the results do not match, the query will be tried for
	 *                      a maximum of 10 times
	 * @param maxWaitTime   Timeout in milliseconds
	 * @return The query results
	 * @throws AssertionError If the job times out or if the max number of retries is exhausted (10)
	 * @throws AsynchJobFailedException If the job failed for other reasons and an exception was not set for the job failure
	 */
	QueryResultBundle assertQueryResult(UserInfo user, Query query, QueryOptions options,
			Consumer<QueryResultBundle> resultMatcher, long maxWaitMs)
			throws AssertionError, AsynchJobFailedException;

	/**
	 * Wait for the given entity to appear in the given view.
	 * 
	 * @param user
	 * @param tableId
	 * @param entityId
	 * @param maxWaitMS
	 * @return
	 * @throws InterruptedException
	 */
	ObjectDataDTO waitForEntityReplication(UserInfo user, String tableId, String entityId, long maxWaitMS)
			throws InterruptedException;

	/**
	 * Wait for the object with the given type and id to be replicated
	 * 
	 * @param objectType
	 * @param objectId
	 * @param etag
	 * @param maxWaitMS
	 * @return
	 * @throws InterruptedException
	 */
	ObjectDataDTO waitForObjectReplication(ReplicationType objectType, Long objectId, String etag, long maxWaitMS)
			throws InterruptedException;

	/**
	 * Create a view with the default columns for the type.
	 * 
	 * @param user
	 * @param name
	 * @param parentId
	 * @param scope
	 * @param viewTypeMask
	 * @return
	 */
	EntityView createEntityView(UserInfo user, String name, String parentId, List<String> scope, long viewTypeMask);
	
	/**
	 * Create a view with the given schema
	 * @param user
	 * @param name
	 * @param parentId
	 * @param schema
	 * @param scope
	 * @param viewTypeMask
	 * @return
	 */
	EntityView createEntityView(UserInfo user, String name, String parentId, List<String> schema, List<String> scope, long viewTypeMask);

	/**
	 * Creates a submission view with the default columns
	 * 
	 * @param user
	 * @param name
	 * @param parentId
	 * @param scope
	 * @return
	 */
	SubmissionView createSubmissionView(UserInfo user, String name, String parentId, List<String> scope);
	
	/**
	 * Updates the submission view with the given schema and/or scope
	 * @param viewId
	 * @param user
	 * @param schema
	 * @param scope
	 */
	void updateSubmissionView(String viewId, UserInfo user, List<String> schema, List<String> scope);
	
	/**
	 * Updates the schema and/or scope of the view
	 * @param viewId
	 * @param user
	 * @param schema
	 * @param scope
	 */
	void updateEntityView(String viewId, UserInfo user, List<String> schema, List<String> scope, long viewType);
	
	/**
	 * Create a dataset with the default columns.
	 * @param user
	 * @param dataset
	 * @return
	 */
	Dataset createDataset(UserInfo user, Dataset dataset);
	
	/**
	 * Creates a table with the given columns.
	 * 
	 * @param user
	 * @param name
	 * @param parentId
	 * @param columnIds
	 * @param searchEnabled
	 * @return
	 */
	TableEntity createTable(UserInfo user, String name, String parentId, List<String> columnIds, boolean searchEnabled);

	/**
	 * Updates the table schema and/or search state
	 * 
	 * @param tableId
	 * @param user
	 * @param newSchema
	 * @param searchEnabled
	 */
	void updateTable(String tableId, UserInfo user, List<String> newSchema, Boolean searchEnabled) throws InterruptedException;
	
	/**
	 * Creates a materialized view from the given query
	 *  
	 * @param user
	 * @param parentId
	 * @param sql
	 * @return
	 */
	MaterializedView createMaterializedView(UserInfo user, String parentId, String sql);


	/**
	 * Helper to download the contents of the given FileHandle ID to a string.
	 * 
	 * @param fileHandleId
	 * @return
	 * @throws IOException
	 */
	String downloadFileHandleFromS3(String fileHandleId) throws IOException;

	void emptyAllQueues();
	
	/**
	 * Wait for an email message to appear in S3.
	 * 
	 * Note: This will delete the message once it is read from S3.
	 * 
	 * @param recieverEmailsAddress
	 * @return
	 * @throws Exception 
	 */
	String waitForEmailMessgae(String recieverEmailsAddress, long maxWaitMs) throws Exception;


}
