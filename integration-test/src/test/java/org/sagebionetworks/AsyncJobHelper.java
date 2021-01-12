package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.client.exceptions.SynapseTableUnavailableException;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;

public class AsyncJobHelper {
	
	public static final int INFINITE_RETRIES = -1;
	
	private static final Logger LOG = LogManager.getLogger(AsyncJobHelper.class);
	private static final int MAX_QUERIES_RETRIES = 5;
	private static final long STATUS_CHECK_FREQUENCY = 1000L;
	
	
	public static AsyncJobResponse<QueryResultBundle> assertQueryBundleResults(SynapseClient client, String tableId, String sql, Long offset, Long limit, int partsMask, Consumer<QueryResultBundle> resultConsumer, long timeoutMs) throws Exception {
		Query query = new Query();
		
		query.setSql(sql);
		query.setOffset(offset);
		query.setLimit(limit);
		
		QueryOptions queryOptions = new QueryOptions().withMask((long) partsMask);
		
		return assertQueryBundleResults(client, tableId, query, queryOptions, resultConsumer, timeoutMs);
	}
	
	public static AsyncJobResponse<QueryResultBundle> assertQueryBundleResults(SynapseClient client, String tableId, Query query, QueryOptions queryOptions, Consumer<QueryResultBundle> resultConsumer, long timeoutMs) throws Exception {
		return assertQueryBundleResults(client, tableId, query, queryOptions, resultConsumer, timeoutMs, MAX_QUERIES_RETRIES);
	}
	
	public static AsyncJobResponse<QueryResultBundle> assertQueryBundleResults(SynapseClient client, String tableId, Query query, QueryOptions queryOptions, Consumer<QueryResultBundle> resultConsumer, long timeoutMs, int maxRetries) throws Exception {
		QueryBundleRequest bundleRequest = new QueryBundleRequest();
		
		bundleRequest.setEntityId(tableId);
		bundleRequest.setQuery(query);
		bundleRequest.setPartMask(queryOptions.getPartMask());
		
		return assertAysncJobResult(client, AsynchJobType.TableQuery, bundleRequest, resultConsumer, timeoutMs, maxRetries);
	}
	
	public static <T extends AsynchronousResponseBody> AsyncJobResponse<T> assertAysncJobResult(SynapseClient client, AsynchJobType jobType, AsynchronousRequestBody request, Consumer<T> resultConsumer, long timeout) throws AssertionError, SynapseException {
		return assertAysncJobResult(client, jobType, request, resultConsumer, timeout, 1);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends AsynchronousResponseBody> AsyncJobResponse<T> assertAysncJobResult(SynapseClient client, AsynchJobType jobType, AsynchronousRequestBody request, Consumer<T> resultConsumer, long timeout, int maxRetries) throws AssertionError, SynapseException {
		final long start = System.currentTimeMillis();
		
		AssertionError lastException = null;
		String jobToken = null;
		int tries = 0;
		long statusCheckFrequency = STATUS_CHECK_FREQUENCY;
		
		for (;;) {

			final long currentTime = System.currentTimeMillis();

			if (currentTime - start > timeout) {
				String message = String.format("%s Job timed out (Token: %s, Tries: %d)", jobType.name(), jobToken, tries);
				if (lastException == null) {
					// No exception was thrown, the job simply ran out of time
					fail(message);
				} else {
					LOG.error(message);
					// throw the last exception as is.
					// This allows IDE to parse the exception and provide a readable diff in cases such as assertEquals() failing
					throw lastException;
				}
			}
			
			// No token set, starting a new job
			if (jobToken == null) {
				jobToken = client.startAsynchJob(jobType, request);
				tries++;
				LOG.info("{} Job submitted (Token: {}, Try: {})", jobType.name(), jobToken, tries);
			}
			
			try {
				Thread.sleep(statusCheckFrequency);
			} catch (InterruptedException e) {
				fail(e.getMessage(), e);
			}
				
			T response;
			
			try {
				// Try and fetch the response
				response = (T) client.getAsyncResult(jobType, jobToken, request);
			} catch (SynapseResultNotReadyException | SynapseTableUnavailableException e) {
				LOG.info("{} Job results not ready, waiting...(Token: {}, Tries: {})", jobType.name(), jobToken, tries);
				continue;
			}
			
			try {
				resultConsumer.accept(response);
			} catch (AssertionError e) {
				lastException = e;
				if (maxRetries == INFINITE_RETRIES || tries < maxRetries) {
					LOG.info("{} Job results invalid, retrying...(Token: {}, Try: {})", jobType.name(), jobToken, tries);
					// Reset the token in order to start a new job
					jobToken = null;
					// Applies exponential back-off for retrying
					statusCheckFrequency *= 1.2;
					continue;
				} else {
					// We reached the max number of jobs, fail
					String message = String.format("%s Job results invalid, number of tries exhausted...(Token: %s, Try: %d)", jobType.name(), jobToken, tries);
					LOG.error(message);
					// throw the last exception as is.
					// This allows IDE to parse the exception and provide a readable diff in cases such as assertEquals() failing
					throw e;
				}
			}
			
			LOG.info("{} Job completed (Token: {}, Tries: {})", jobType.name(), jobToken, tries);
			
			// The consumer didn't throw, we are done
			return new AsyncJobResponse<T>(jobToken, response);
		}
		
	}
	
	public static class AsyncJobResponse<T extends AsynchronousResponseBody> {
		
		private String jobToken;
		private T response;
		
		private AsyncJobResponse(String jobToken, T response) {
			this.jobToken = jobToken;
			this.response = response;
		}
		
		public String getJobToken() {
			return jobToken;
		}
		
		public T getResponse() {
			return response;
		}
		
	}

}
