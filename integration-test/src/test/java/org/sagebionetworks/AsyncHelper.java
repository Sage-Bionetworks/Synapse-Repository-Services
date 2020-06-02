package org.sagebionetworks;

import java.util.function.Predicate;

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
		
		final String asyncToken = client.queryTableEntityBundleAsyncStart(query, queryOptions, tableId);
		
		return TimeUtils.waitFor(timeoutMs, 500L, () -> {
			try {
				QueryResultBundle result = client.queryTableEntityBundleAsyncGet(asyncToken, tableId);
				
				boolean done = true;

				if (resultMatcher != null) {
					done = resultMatcher.test(result);
				}
				
				return Pair.create(done, result);
			} catch (SynapseResultNotReadyException | SynapseTableUnavailableException e) {
				return Pair.create(false, null);
			}
		});
	}

}
