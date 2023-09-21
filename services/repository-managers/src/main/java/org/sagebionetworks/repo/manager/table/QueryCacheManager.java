package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.CachedQueryRequest;
import org.sagebionetworks.table.cluster.TableIndexDAO;

public interface QueryCacheManager {

	/**
	 * Get the query results for the given query. This method will return cached
	 * results if available.
	 * 
	 * @param indexDao
	 * @param facetSqlQuery
	 * @return
	 */
	RowSet getQueryResults(TableIndexDAO indexDao, CachedQueryRequest request);

	/**
	 * Trigger the refresh of the query identified by the provided hash.
	 * 
	 * @param indexDao
	 * @param requestHash
	 */
	void refreshCachedQuery(TableIndexDAO indexDao, String requestHash);

}
