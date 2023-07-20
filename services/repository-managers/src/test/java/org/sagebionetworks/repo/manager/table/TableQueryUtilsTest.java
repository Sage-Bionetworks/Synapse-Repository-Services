package org.sagebionetworks.repo.manager.table;


import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.asynch.CacheableRequestBody;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableConstants;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableQueryUtilsTest {

	String tableId;
	String sql;
	DownloadFromTableRequest downloadRequest;
	QueryBundleRequest queryRequest;
	QueryNextPageToken nextPageToken;
	
	@BeforeEach
	public void before(){
		
		tableId = "syn123";
		sql = "select * from "+tableId;
		
		downloadRequest = new DownloadFromTableRequest();
		downloadRequest.setSql(sql);
		
		Query query = new Query();
		query.setSql(sql);
		queryRequest = new QueryBundleRequest();
		queryRequest.setQuery(query);
		
		nextPageToken = TableQueryUtils.createNextPageToken("select * from syn123", null, null, null, null);
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
		QueryNextPageToken token = TableQueryUtils.createNextPageToken(sql, sortList, nextOffset, limit, selectedFacets);
		Query query = TableQueryUtils.createQueryFromNextPageToken(token);
		assertEquals(sql, query.getSql());
		assertEquals(nextOffset, query.getOffset());
		assertEquals(limit, query.getLimit());
		assertEquals(sortList, query.getSort());
		assertEquals(selectedFacets, query.getSelectedFacets());
	}
	
	@Test
	public void testExtractTableIdFromSql(){
		String resultTableId = TableQueryUtils.extractTableIdFromSql(sql);
		assertEquals(tableId, resultTableId);
	}
	
	@Test
	public void testExtractTableIdFromSqlWithJoin(){
		sql = "select * from syn123 join syn456";
		String message = assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.extractTableIdFromSql(sql);
		}).getMessage();
		assertEquals(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE, message);
	}
	
	@Test
	public void testExtractTableIdFromSqlNull(){
		String sql = null;
		assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.extractTableIdFromSql(sql);
		});
	}
	
	@Test
	public void testGetTableIdDownloadFromTableRequest(){
		String resultTableId = TableQueryUtils.getTableId(downloadRequest);
		assertEquals(tableId, resultTableId);
	}
	
	@Test
	public void testGetTableIdDownloadFromTableRequestNull(){
		DownloadFromTableRequest request = null;
		assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.getTableId(request);
		});
	}
	
	@Test
	public void testGetTableIdQueryBundleRequest(){
		String resultTableId = TableQueryUtils.getTableId(queryRequest);
		assertEquals(tableId, resultTableId);
	}
	
	@Test
	public void testGetTableIdQueryBundleRequestNull(){
		QueryBundleRequest request = null;
		assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.getTableId(request);
		});
	}
	
	@Test
	public void testGetTableIdQueryNextPageToken(){
		String requestTableId = TableQueryUtils.getTableId(nextPageToken);
		assertEquals(tableId, requestTableId);
	}
	
	@Test
	public void testGetTableIdQueryNextPageTokenNull(){
		QueryNextPageToken request = null;
		assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.getTableId(request);
		});
	}
	
	@Test
	public void testGetTableIdFromRequestBodydUnknownType(){
		CacheableRequestBody unknownType = Mockito.mock(CacheableRequestBody.class);
		assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.getTableIdFromRequestBody(unknownType);
		});
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
	
	/**
	 * For PLFM-6027, a user's query contained an unknown character ("\u2018") resulting in a 
	 * org.sagebionetworks.table.query.TokenMgrError exception.  Since this exception was
	 * not mapped in the controllers, we returned a 500.
	 * 
	 * We expanded the parser to ignore characters in this range.
	 */
	@Test
	public void testPLFM_6027() {
		char unknownChar = 0x2018;
		String sql = "select * from syn123 "+unknownChar;
		String result = TableQueryUtils.extractTableIdFromSql(sql);
		assertEquals("syn123", result);
	}

	@Test
	/**
	 * PLFM-6392 Add a more informative message to the user for cases where keywords
	 * must be escaped.
	 */
	public void testExtractTableIdFromSqlWithParserException() {
		String sql = "select year from syn123 where timestamp = 1";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.extractTableIdFromSql(sql);
		});
		assertTrue(exception.getMessage().startsWith("Encountered \" \"TIMESTAMP\" \"timestamp \"\" at line 1, column 31."));
		assertTrue(exception.getMessage().contains(TableExceptionTranslator.UNQUOTED_KEYWORDS_ERROR_MESSAGE));
	}
}
