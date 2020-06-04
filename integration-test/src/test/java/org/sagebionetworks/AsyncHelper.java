package org.sagebionetworks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.client.exceptions.SynapseTableUnavailableException;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;

public class AsyncHelper {
	
	private static final Logger LOG = LogManager.getLogger(AsyncHelper.class);
	private static final int MAX_QUERIES = 5;
	private static final long STATUS_CHECK_FREQUENCY = 1000L;
	
	/**
	 * Wait for the query to finish and return the results.
	 * 
	 * @param sql
	 * @return
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	public static QueryResultBundle waitForBundleQueryResults(SynapseClient client, long timeoutMs, String sql, Long offset, Long limit, int partsMask, final String tableId) throws Exception {
		return waitForBundleQueryResults(client, timeoutMs, sql, offset, limit, partsMask, tableId, null);
	}
	
	/**
	 * Wait for the query to finish and return the results. When supplied, tests the results against the given matcher. If the match fails keeps waiting
	 * @param sql
	 * @return
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	public static QueryResultBundle waitForBundleQueryResults(SynapseClient client, long timeoutMs, String sql, Long offset, Long limit, int partsMask, final String tableId, Predicate<QueryResultBundle> resultMatcher) throws Exception {
		Query query = new Query();
		
		query.setSql(sql);
		query.setIsConsistent(true);
		query.setOffset(offset);
		query.setLimit(limit);
		
		QueryOptions queryOptions = new QueryOptions().withMask((long) partsMask);
		
		return waitForBundleQueryResults(client, timeoutMs, query, queryOptions, tableId, resultMatcher);
	}
	
	public static QueryResultBundle waitForBundleQueryResults(SynapseClient client, long timeoutMs, Query query, QueryOptions queryOptions, final String tableId, Predicate<QueryResultBundle> resultMatcher) throws Exception {
		
		final AtomicInteger queryCount = new AtomicInteger();
		final AtomicReference<String> tokenContainer = new AtomicReference<>();
		
		return TimeUtils.waitFor(timeoutMs, STATUS_CHECK_FREQUENCY, () -> {

			String token = tokenContainer.get();
			int currentCount = queryCount.get();
			
			try {
				
				if (token == null) {
					token = client.queryTableEntityBundleAsyncStart(query, queryOptions, tableId);
					LOG.info("Query request submitted (Token: {}): {}", token, query.getSql());
					tokenContainer.set(token);
					currentCount = queryCount.incrementAndGet();
				}
				
				QueryResultBundle result = client.queryTableEntityBundleAsyncGet(token, tableId);
				
				// No need to test on the result, we are done
				if (resultMatcher == null) {
					return Pair.create(true, result);
				}
				
				// Test on the predicate to see if we are done or we still need to wait
				boolean matchPredicate = resultMatcher.test(result);
				
				// The predicate didn't match, since we might be stalling we reset the token
				// in order to submit another query (which might trigger an update on a view)
				if (!matchPredicate && currentCount <= MAX_QUERIES) {
					LOG.info("Waiting for results match...(Token: {})", token);
					tokenContainer.set(null);
				}
				
				return Pair.create(matchPredicate, result);

			} catch (SynapseResultNotReadyException | SynapseTableUnavailableException e) {
				LOG.info("Waiting for table...(Token: {})", token);
				// Result not ready yet
				return Pair.create(false, null);
			}
		});
	}

}
