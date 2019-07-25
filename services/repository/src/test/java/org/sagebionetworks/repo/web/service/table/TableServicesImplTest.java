package org.sagebionetworks.repo.web.service.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SqlTransformResponse;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.model.table.TransformSqlWithFacetsRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.SqlQueryBuilder;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;

import com.google.common.collect.Lists;

/**
 * Unit test for TableServicesImpl.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TableServicesImplTest {
	
	@Mock
	UserManager mockUserManager;
	@Mock
	ColumnModelManager mockColumnModelManager;
	@Mock
	EntityManager mockEntityManager;
	@Mock
	TableEntityManager mockTableEntityManager;
	@Mock
	FileHandleManager mockFileHandleManager;
	@InjectMocks
	TableServicesImpl tableService;
	
	Long userId;
	UserInfo userInfo;
	QueryBundleRequest queryBundle;
	List<ColumnModel> columns;
	List<SelectColumn> headers;
	RowSet selectStar;
	QueryResult selectStarResult;
	String tableId;
	SqlQuery sqlQuery;
	RowReferenceSet fileHandlesToFind;
	RowReference rowRef;
	String columnId;
	ColumnModel fileColumn;
	String fileHandleId;

	@Before
	public void before() throws Exception{
		userId = 123L;
		userInfo = new UserInfo(false, userId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		columns = TableModelTestUtils.createOneOfEachType();
		headers = TableModelUtils.getSelectColumns(columns);
		tableId = "syn456";
		selectStar = new RowSet();
		selectStar.setEtag("etag");
		selectStar.setHeaders(headers);
		selectStar.setTableId(tableId);
		selectStar.setRows(TableModelTestUtils.createRows(columns, 4));
		selectStarResult = new QueryResult();
		selectStarResult.setNextPageToken(null);
		selectStarResult.setQueryResults(selectStar);

		sqlQuery = new SqlQueryBuilder("select * from " + tableId, columns).build();
		
		List<ColumnModel> columns = new LinkedList<ColumnModel>();
		
		fileHandlesToFind = new RowReferenceSet();
		fileHandlesToFind.setTableId(tableId);
		fileHandlesToFind.setRows(new LinkedList<RowReference>());
		fileHandlesToFind.setHeaders(new LinkedList<SelectColumn>());
		when(mockColumnModelManager.getCurrentColumns(userInfo, tableId, fileHandlesToFind.getHeaders())).thenReturn(columns);
		
		rowRef = new RowReference();
		rowRef.setRowId(1L);
		columnId = "444";
		fileColumn = new ColumnModel();
		fileColumn.setColumnType(ColumnType.FILEHANDLEID);
		fileColumn.setId(columnId);
		fileColumn.setName("aFileColumn");
		when(mockColumnModelManager.getColumnModel(userInfo, columnId)).thenReturn(fileColumn);
		
		fileHandleId = "555";
		Row row = new Row();
		row.setValues(Lists.newArrayList(fileHandleId));
		when(mockTableEntityManager.getCellValue(userInfo, tableId, rowRef, fileColumn)).thenReturn(row);
	}
	
	/**
	 * PLFM-4191 caused by NullPointerException thrown from tableService.getFileHandles().
	 * @throws Exception
	 */
	@Test
	public void testPLFM_4191() throws Exception {
		RowSet rowSet = new RowSet();
		Row row = new Row();
		rowSet.setRows(Lists.newArrayList(row));
		when(mockTableEntityManager.getCellValues(any(UserInfo.class), anyString(), anyListOf(RowReference.class), anyListOf(ColumnModel.class))).thenReturn(rowSet);
		// call under test
		TableFileHandleResults results = tableService.getFileHandles(userId, fileHandlesToFind);
		assertNotNull(results);
	}
	
	/**
	 * This is a test for PLFM-4454.
	 * @throws IOException 
	 * @throws NotFoundException 
	 * 
	 */
	@Test
	public void testGetFileHandleId() throws NotFoundException, IOException{
		// Call under test
		String result = tableService.getFileHandleId(userInfo, tableId, rowRef, columnId);
		assertEquals(fileHandleId, result);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetFileHandleIdNonFileColumn() throws NotFoundException, IOException{
		fileColumn.setColumnType(ColumnType.INTEGER);
		// Call under test
		tableService.getFileHandleId(userInfo, tableId, rowRef, columnId);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetFileHandleIdNullRow() throws NotFoundException, IOException{
		// return null row.
		Row row = null;
		when(mockTableEntityManager.getCellValue(userInfo, tableId, rowRef, fileColumn)).thenReturn(row);
		// Call under test
		tableService.getFileHandleId(userInfo, tableId, rowRef, columnId);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetFileHandleIdRowNoValue() throws NotFoundException, IOException{
		Row row = new Row();
		// null values.
		row.setValues(null);
		when(mockTableEntityManager.getCellValue(userInfo, tableId, rowRef, fileColumn)).thenReturn(row);
		// Call under test
		tableService.getFileHandleId(userInfo, tableId, rowRef, columnId);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetFileHandleIdRowEmptyValue() throws NotFoundException, IOException{
		Row row = new Row();
		// null values.
		row.setValues(new LinkedList<String>());
		when(mockTableEntityManager.getCellValue(userInfo, tableId, rowRef, fileColumn)).thenReturn(row);
		// Call under test
		tableService.getFileHandleId(userInfo, tableId, rowRef, columnId);
	}
	
	@Test
	public void testTransformSqlRequestFacet() throws ParseException {
		TransformSqlWithFacetsRequest request = new TransformSqlWithFacetsRequest();
		request.setSqlToTransform("select * from syn123");
		FacetColumnRangeRequest facet = new FacetColumnRangeRequest();
		facet.setColumnName("foo");
		facet.setMax("100");
		facet.setMin("0");
		request.setSelectedFacets(Lists.newArrayList(facet));
		ColumnModel column = new ColumnModel();
		column.setName("foo");
		column.setFacetType(FacetType.range);
		column.setColumnType(ColumnType.INTEGER);
		request.setSchema(Lists.newArrayList(column));
		// Call under test
		SqlTransformResponse response = tableService.transformSqlRequest(request);
		assertNotNull(response);
		assertEquals("SELECT * FROM syn123 WHERE ( ( \"foo\" BETWEEN '0' AND '100' ) )", response.getTransformedSql());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTransformSqlRequestFacetNull() throws ParseException {
		TransformSqlWithFacetsRequest request = null;
		// Call under test
		SqlTransformResponse response = tableService.transformSqlRequest(request);
		assertNotNull(response);
	}
	
	@Test
	public void testGetFileRedirectURL() throws IOException {
		
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandleId)
				.withAssociation(FileHandleAssociateType.TableEntity, tableId);
		
		String expectedUrl = "https://testurl.org";
		
		when(mockFileHandleManager.getRedirectURLForFileHandle(eq(urlRequest))).thenReturn(expectedUrl);
		
		String url = tableService.getFileRedirectURL(userId, tableId, rowRef, columnId);
		
		verify(mockFileHandleManager).getRedirectURLForFileHandle(eq(urlRequest));
		
		assertEquals(expectedUrl, url);
			
	}
	
	@Test
	public void testGetFilePreviewRedirectURL() throws IOException {
		
		String fileHandlePreviewId = "456";
		
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandlePreviewId)
				.withAssociation(FileHandleAssociateType.TableEntity, tableId);
		
		String expectedUrl = "https://testurl.org";
		
		when(mockFileHandleManager.getPreviewFileHandleId(eq(fileHandleId))).thenReturn(fileHandlePreviewId);
		when(mockFileHandleManager.getRedirectURLForFileHandle(eq(urlRequest))).thenReturn(expectedUrl);
		
		String url = tableService.getFilePreviewRedirectURL(userId, tableId, rowRef, columnId);
		
		verify(mockFileHandleManager).getPreviewFileHandleId(eq(fileHandleId));
		verify(mockFileHandleManager).getRedirectURLForFileHandle(eq(urlRequest));
		
		assertEquals(expectedUrl, url);
		
		
	}
	
}
