package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.asynch.CacheableRequestBody;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;

import com.google.common.collect.Lists;

public class TableQueryUtilsTest {
	
	String tableId;
	String sql;
	DownloadFromTableRequest downloadRequest;
	QueryBundleRequest queryRequest;
	QueryNextPageToken nextPageToken;
	
	@Before
	public void before(){
		
		tableId = "syn123";
		sql = "select * from "+tableId;
		
		downloadRequest = new DownloadFromTableRequest();
		downloadRequest.setSql(sql);
		
		Query query = new Query();
		query.setSql(sql);
		queryRequest = new QueryBundleRequest();
		queryRequest.setQuery(query);
		
		nextPageToken = TableQueryUtils.createNextPageToken("select * from syn123", null, null, null, true, null);
	}

	@Test
	public void testCreateNextPageTokenEscapingSingle() throws Exception {
		String sql = "select \"i-0\" from syn123" ;
		SortItem sort = new SortItem();
		sort.setColumn("i0");
		sort.setDirection(SortDirection.DESC);
		List<SortItem> sortList= Lists.newArrayList(sort);
		FacetColumnRequest facet = new FacetColumnRangeRequest();
		facet.setColumnName("facetName");
		List<FacetColumnRequest> selectedFacets = Lists.newArrayList(facet);
		
		Long nextOffset = 10L;
		Long limit = 21L;
		boolean isConsistent = true;
		QueryNextPageToken token = TableQueryUtils.createNextPageToken(sql, sortList, nextOffset, limit, isConsistent, selectedFacets);
		Query query = TableQueryUtils.createQueryFromNextPageToken(token);
		assertEquals(sql, query.getSql());
		assertEquals(nextOffset, query.getOffset());
		assertEquals(limit, query.getLimit());
		assertEquals(isConsistent, query.getIsConsistent());
		assertEquals(sortList, query.getSort());
		assertEquals(selectedFacets, query.getSelectedFacets());
	}
	
	@Test
	public void testExtractTableIdFromSql(){
		String resultTableId = TableQueryUtils.extractTableIdFromSql(sql);
		assertEquals(tableId, resultTableId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testExtractTableIdFromSqlNull(){
		String sql = null;
		TableQueryUtils.extractTableIdFromSql(sql);
	}
	
	@Test
	public void testGetTableIdDownloadFromTableRequest(){
		String resultTableId = TableQueryUtils.getTableId(downloadRequest);
		assertEquals(tableId, resultTableId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetTableIdDownloadFromTableRequestNull(){
		DownloadFromTableRequest request = null;
		TableQueryUtils.getTableId(request);
	}
	
	@Test
	public void testGetTableIdQueryBundleRequest(){
		String resultTableId = TableQueryUtils.getTableId(queryRequest);
		assertEquals(tableId, resultTableId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetTableIdQueryBundleRequestNull(){
		QueryBundleRequest request = null;
		TableQueryUtils.getTableId(request);
	}
	
	@Test
	public void testGetTableIdQueryNextPageToken(){
		String requestTableId = TableQueryUtils.getTableId(nextPageToken);
		assertEquals(tableId, requestTableId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetTableIdQueryNextPageTokenNull(){
		QueryNextPageToken request = null;
		TableQueryUtils.getTableId(request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetTableIdFromRequestBodydUnknownType(){
		CacheableRequestBody unknownType = Mockito.mock(CacheableRequestBody.class);
		TableQueryUtils.getTableIdFromRequestBody(unknownType);
	}
	
	@Test
	public void testGetTableIdFromRequestBodyDownloadRequest(){
		String requestTableId = TableQueryUtils.getTableIdFromRequestBody(downloadRequest);
		assertEquals(tableId, requestTableId);
	}
	
	@Test
	public void testGetTableIdFromRequestBodyQueryBundleRequest(){
		String requestTableId = TableQueryUtils.getTableIdFromRequestBody(queryRequest);
		assertEquals(tableId, requestTableId);
	}
	
	@Test
	public void testGetTableIdFromRequestBodyQueryNextPageToken(){
		String requestTableId = TableQueryUtils.getTableIdFromRequestBody(nextPageToken);
		assertEquals(tableId, requestTableId);
	}
}
