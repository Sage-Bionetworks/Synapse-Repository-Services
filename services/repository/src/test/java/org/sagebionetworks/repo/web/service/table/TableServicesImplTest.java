package org.sagebionetworks.repo.web.service.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetType;
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
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;

import com.google.common.collect.Lists;

/**
 * Unit test for TableServicesImpl.
 *
 */
@ExtendWith(MockitoExtension.class)
public class TableServicesImplTest {
	
	@Mock
	private UserManager mockUserManager;
	@Mock
	private ColumnModelManager mockColumnModelManager;
	@Mock
	private EntityManager mockEntityManager;
	@Mock
	private TableEntityManager mockTableEntityManager;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private SchemaProvider mockSchemaProvider;
	@InjectMocks
	private TableServicesImpl tableService;
	
	private Long userId;
	private UserInfo userInfo;
	private List<ColumnModel> columns;
	private List<SelectColumn> headers;
	private RowSet selectStar;
	private QueryResult selectStarResult;
	private String tableId;
	private RowReferenceSet fileHandlesToFind;
	private RowReference rowRef;
	private String columnId;
	private ColumnModel fileColumn;
	private String fileHandleId;

	@BeforeEach
	public void beforeEach() throws Exception{
		userId = 123L;
		userInfo = new UserInfo(false, userId);
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

//		sqlQuery = QueryTranslator.builder("select * from " + tableId, mockSchemaProvider, userId).indexDescription(new TableIndexDescription(IdAndVersion.parse(tableId))).build();
		fileHandlesToFind = new RowReferenceSet();
		fileHandlesToFind.setTableId(tableId);
		fileHandlesToFind.setRows(new LinkedList<RowReference>());
		fileHandlesToFind.setHeaders(new LinkedList<SelectColumn>());

		rowRef = new RowReference();
		rowRef.setRowId(1L);
		columnId = "444";
		fileColumn = new ColumnModel();
		fileColumn.setColumnType(ColumnType.FILEHANDLEID);
		fileColumn.setId(columnId);
		fileColumn.setName("aFileColumn");

	}
	
	/**
	 * PLFM-4191 caused by NullPointerException thrown from tableService.getFileHandles().
	 * @throws Exception
	 */
	@Test
	public void testPLFM_4191() throws Exception {
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		List<ColumnModel> columns = new LinkedList<ColumnModel>();
		when(mockColumnModelManager.getCurrentColumns(userInfo, tableId, fileHandlesToFind.getHeaders())).thenReturn(columns);
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
		when(mockColumnModelManager.getColumnModel(userInfo, columnId)).thenReturn(fileColumn);
		fileHandleId = "555";
		Row row = new Row();
		row.setValues(Lists.newArrayList(fileHandleId));
		when(mockTableEntityManager.getCellValue(userInfo, tableId, rowRef, fileColumn)).thenReturn(row);
		// Call under test
		String result = tableService.getFileHandleId(userInfo, tableId, rowRef, columnId);
		assertEquals(fileHandleId, result);
	}
	
	@Test
	public void testGetFileHandleIdNonFileColumn() throws NotFoundException, IOException{
		fileColumn.setColumnType(ColumnType.INTEGER);
		when(mockColumnModelManager.getColumnModel(userInfo, columnId)).thenReturn(fileColumn);
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			tableService.getFileHandleId(userInfo, tableId, rowRef, columnId);
		});
	}
	
	@Test
	public void testGetFileHandleIdNullRow() throws NotFoundException, IOException{
		when(mockColumnModelManager.getColumnModel(userInfo, columnId)).thenReturn(fileColumn);
		NotFoundException thrown = Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			tableService.getFileHandleId(userInfo, tableId, rowRef, columnId);
		});
	}
	
	@Test
	public void testGetFileHandleIdRowNoValue() throws NotFoundException, IOException{
		when(mockColumnModelManager.getColumnModel(userInfo, columnId)).thenReturn(fileColumn);
		NotFoundException thrown = Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			tableService.getFileHandleId(userInfo, tableId, rowRef, columnId);
		});
	}
	
	@Test
	public void testGetFileHandleIdRowEmptyValue() throws NotFoundException, IOException{
		when(mockColumnModelManager.getColumnModel(userInfo, columnId)).thenReturn(fileColumn);
		NotFoundException thrown = Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			tableService.getFileHandleId(userInfo, tableId, rowRef, columnId);
		});
	}
	
	@Test
	public void testTransformSqlRequestFacet() throws IOException, ParseException {
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
	
	@Test
	public void testTransformSqlRequestFacetNull() throws ParseException {
		TransformSqlWithFacetsRequest request = null;
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			SqlTransformResponse response = tableService.transformSqlRequest(request);
			assertNotNull(response);
		});
	}
	
	@Test
	public void testGetFileRedirectURL() throws IOException {
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		when(mockColumnModelManager.getColumnModel(userInfo, columnId)).thenReturn(fileColumn);
		fileHandleId = "555";
		Row row = new Row();
		row.setValues(Lists.newArrayList(fileHandleId));
		when(mockTableEntityManager.getCellValue(userInfo, tableId, rowRef, fileColumn)).thenReturn(row);

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
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		fileHandleId = "555";
		String fileHandlePreviewId = "456";
		when(mockColumnModelManager.getColumnModel(userInfo, columnId)).thenReturn(fileColumn);
		Row row = new Row();
		row.setValues(Lists.newArrayList(fileHandleId));
		when(mockTableEntityManager.getCellValue(userInfo, tableId, rowRef, fileColumn)).thenReturn(row);
		
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandlePreviewId)
				.withAssociation(FileHandleAssociateType.TableEntity, tableId);
		
		String expectedUrl = "https://testurl.org";
		
		when(mockFileHandleManager.getPreviewFileHandleId(eq(fileHandleId))).thenReturn(fileHandlePreviewId);
		when(mockFileHandleManager.getRedirectURLForFileHandle(eq(urlRequest))).thenReturn(expectedUrl);
		
		// call under test
		String url = tableService.getFilePreviewRedirectURL(userId, tableId, rowRef, columnId);
		
		verify(mockFileHandleManager).getPreviewFileHandleId(eq(fileHandleId));
		verify(mockFileHandleManager).getRedirectURLForFileHandle(eq(urlRequest));
		
		assertEquals(expectedUrl, url);
		
		
	}

	@Test
	public void testGetFileHandlesNullHeadersPlfm7341() throws IOException {
		RowReferenceSet rowReferenceSet = new RowReferenceSet();
		rowReferenceSet.setTableId(tableId);
		rowReferenceSet.setHeaders(null);
		InvalidModelException thrown = Assertions.assertThrows(InvalidModelException.class, () -> {
			// Call under test
			TableFileHandleResults results = tableService.getFileHandles(userInfo.getId(), rowReferenceSet);
		});
	}
	
}
