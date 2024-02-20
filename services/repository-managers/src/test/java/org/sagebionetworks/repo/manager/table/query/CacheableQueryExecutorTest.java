package org.sagebionetworks.repo.manager.table.query;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.QueryCacheManager;
import org.sagebionetworks.table.cluster.CachedQueryRequest;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.description.IndexDescription;

@ExtendWith(MockitoExtension.class)
public class CacheableQueryExecutorTest {

	@Mock
	private QueryCacheManager mockCacheManager;
	
	private int cacheExpiration = 10;
	
	@InjectMocks
	private CacheableQueryExecutor queryExecutor = new CacheableQueryExecutor(mockCacheManager, cacheExpiration);
	
	@Mock
	private TableIndexDAO mockIndexDao;

	@Mock
	private QueryTranslator mockQuery;
	
	@Mock
	private IndexDescription mockIndexDescription;
	
	@Test
	public void testExecuteQueryWithNoCache() {
		when(mockQuery.getIndexDescription()).thenReturn(mockIndexDescription);
		when(mockIndexDescription.supportQueryCache()).thenReturn(false);
		
		// Call under test
		queryExecutor.executeQuery(mockIndexDao, mockQuery);
		
		verify(mockIndexDao).query(mockQuery);
		verifyZeroInteractions(mockCacheManager);
		
	}
	
	@Test
	public void testExecuteQueryWithCache() {
		when(mockQuery.getIndexDescription()).thenReturn(mockIndexDescription);
		when(mockIndexDescription.supportQueryCache()).thenReturn(true);
		
		// Call under test
		queryExecutor.executeQuery(mockIndexDao, mockQuery);
		
		verify(mockCacheManager).getQueryResults(mockIndexDao, CachedQueryRequest.clone(mockQuery).setExpiresInSec(cacheExpiration));
		verifyZeroInteractions(mockIndexDao);
		
	}
	
}
