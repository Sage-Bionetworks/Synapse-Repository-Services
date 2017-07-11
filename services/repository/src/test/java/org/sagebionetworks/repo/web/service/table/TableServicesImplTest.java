package org.sagebionetworks.repo.web.service.table;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

/**
 * Unit test for TableServicesImpl.
 * 
 * @author John
 *
 */
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
		MockitoAnnotations.initMocks(this);
		tableService = new TableServicesImpl();
		
		ReflectionTestUtils.setField(tableService, "userManager", mockUserManager);
		ReflectionTestUtils.setField(tableService, "columnModelManager", mockColumnModelManager);
		ReflectionTestUtils.setField(tableService, "entityManager", mockEntityManager);
		ReflectionTestUtils.setField(tableService, "tableEntityManager", mockTableEntityManager);
		ReflectionTestUtils.setField(tableService, "fileHandleManager", mockFileHandleManager);
		
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

		sqlQuery = new SqlQuery("select * from " + tableId, columns);
		
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
		String result = tableService.getFileHandleId(userId, tableId, rowRef, columnId);
		assertEquals(fileHandleId, result);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetFileHandleIdNonFileColumn() throws NotFoundException, IOException{
		fileColumn.setColumnType(ColumnType.INTEGER);
		// Call under test
		tableService.getFileHandleId(userId, tableId, rowRef, columnId);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetFileHandleIdNullRow() throws NotFoundException, IOException{
		// return null row.
		Row row = null;
		when(mockTableEntityManager.getCellValue(userInfo, tableId, rowRef, fileColumn)).thenReturn(row);
		// Call under test
		tableService.getFileHandleId(userId, tableId, rowRef, columnId);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetFileHandleIdRowNoValue() throws NotFoundException, IOException{
		Row row = new Row();
		// null values.
		row.setValues(null);
		when(mockTableEntityManager.getCellValue(userInfo, tableId, rowRef, fileColumn)).thenReturn(row);
		// Call under test
		tableService.getFileHandleId(userId, tableId, rowRef, columnId);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetFileHandleIdRowEmptyValue() throws NotFoundException, IOException{
		Row row = new Row();
		// null values.
		row.setValues(new LinkedList<String>());
		when(mockTableEntityManager.getCellValue(userInfo, tableId, rowRef, fileColumn)).thenReturn(row);
		// Call under test
		tableService.getFileHandleId(userId, tableId, rowRef, columnId);
	}
}
