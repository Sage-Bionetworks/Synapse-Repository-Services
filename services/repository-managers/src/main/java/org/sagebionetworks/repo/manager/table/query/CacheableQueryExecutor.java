package org.sagebionetworks.repo.manager.table.query;

import org.sagebionetworks.repo.manager.table.QueryCacheManager;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.CachedQueryRequest;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.TableIndexDAO;

public class CacheableQueryExecutor implements QueryExecutor {

	private QueryCacheManager cacheManager;
	private int cacheExpiration;
	
	public CacheableQueryExecutor(QueryCacheManager cacheManager, int cacheExpiration) {
		this.cacheManager = cacheManager;
		this.cacheExpiration = cacheExpiration;
	}

	@Override
	public RowSet executeQuery(TableIndexDAO indexDao, QueryTranslator query) {
		if (query.getIndexDescription().supportQueryCache()) {
			CachedQueryRequest cacheRequest = CachedQueryRequest.clone(query).setExpiresInSec(cacheExpiration);
			return cacheManager.getQueryResults(indexDao, cacheRequest);
		}
		
		return indexDao.query(query);
	}
}
