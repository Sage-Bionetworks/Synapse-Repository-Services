package org.sagebionetworks.repo.web.service.table;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for TableServicesImpl.
 * 
 * @author John
 *
 */
public class TableServicesImplTest {
	
	UserManager mockUserManager;
	ColumnModelManager mockColumnModelManager;
	EntityManager mockEntityManager;
	TableRowManager mockTableRowManager;
	FileHandleManager mockFileHandleManager;
	TableServicesImpl tableService;
	Long userId;
	UserInfo userInfo;
	Query query;
	List<ColumnModel> models;
	List<String> headers;
	RowSet selectStar;
	RowSet selectCountStar;
	String tableId;
	SqlQuery sqlQuery;

	@Before
	public void before() throws Exception{
		mockUserManager = Mockito.mock(UserManager.class);
		mockColumnModelManager = Mockito.mock(ColumnModelManager.class);
		mockEntityManager = Mockito.mock(EntityManager.class);
		mockTableRowManager = Mockito.mock(TableRowManager.class);
		mockFileHandleManager = Mockito.mock(FileHandleManager.class);
		tableService = new TableServicesImpl();
		
		ReflectionTestUtils.setField(tableService, "userManager", mockUserManager);
		ReflectionTestUtils.setField(tableService, "columnModelManager", mockColumnModelManager);
		ReflectionTestUtils.setField(tableService, "entityManager", mockEntityManager);
		ReflectionTestUtils.setField(tableService, "tableRowManager", mockTableRowManager);
		ReflectionTestUtils.setField(tableService, "fileHandleManager", mockFileHandleManager);
		
		userId = 123L;
		userInfo = new UserInfo(false, userId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		models = TableModelTestUtils.createOneOfEachType();
		headers = TableModelUtils.getHeaders(models);
		tableId = "syn456";
		selectStar = new RowSet();
		selectStar.setEtag("etag");
		selectStar.setHeaders(headers);
		selectStar.setTableId(tableId);
		selectStar.setRows(TableModelTestUtils.createRows(models, 4));

		selectCountStar = new RowSet();
		selectCountStar.setEtag(null);
		selectCountStar.setTableId(tableId);
		selectCountStar.setHeaders(Arrays.asList("count(*)"));
		Row countRow = new Row();
		countRow.setValues(Arrays.asList("4"));
		selectCountStar.setRows(Arrays.asList(countRow));
		
		sqlQuery = new SqlQuery("select * from "+tableId, models);
	}
	
	@Test
	public void testQueryBundle() throws Exception {
		query = new Query();
		query.setSql("select * from myTable");
		when(mockTableRowManager.createQuery(query.getSql(), false)).thenReturn(sqlQuery);
		when(mockTableRowManager.query(userInfo, sqlQuery, true)).thenReturn(selectStar);
		when(mockTableRowManager.query(userInfo, query.getSql(), true, true)).thenReturn(selectCountStar);
		Long maxRowsPerPage = new Long(7);
		when(mockTableRowManager.getMaxRowsPerPage(sqlQuery.getSelectColumnModels())).thenReturn(maxRowsPerPage);
		
		// Request query only
		QueryResultBundle bundle = tableService.queryBundle(userId, query, true, TableServicesImpl.BUNDLE_MASK_QUERY_RESULTS);
		assertEquals(selectStar, bundle.getQueryResults());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(null, bundle.getMaxRowsPerPage());
		
		// Count only
		bundle = tableService.queryBundle(userId, query, true, TableServicesImpl.BUNDLE_MASK_QUERY_COUNT);
		assertEquals(null, bundle.getQueryResults());
		assertEquals(new Long(4), bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(null, bundle.getMaxRowsPerPage());
		
		// select columns
		bundle = tableService.queryBundle(userId, query, true, TableServicesImpl.BUNDLE_MASK_QUERY_SELECT_COLUMNS);
		assertEquals(null, bundle.getQueryResults());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(sqlQuery.getSelectColumnModels(), bundle.getSelectColumns());
		assertEquals(null, bundle.getMaxRowsPerPage());
		
		// max rows per page
		bundle = tableService.queryBundle(userId, query, true, TableServicesImpl.BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE);
		assertEquals(null, bundle.getQueryResults());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(maxRowsPerPage, bundle.getMaxRowsPerPage());
		
		// now combine them all
		bundle = tableService.queryBundle(userId, query, true, 
				(TableServicesImpl.BUNDLE_MASK_QUERY_RESULTS
				| TableServicesImpl.BUNDLE_MASK_QUERY_COUNT
				| TableServicesImpl.BUNDLE_MASK_QUERY_SELECT_COLUMNS
				|TableServicesImpl.BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE));
		assertEquals(selectStar, bundle.getQueryResults());
		assertEquals(new Long(4), bundle.getQueryCount());
		assertEquals(sqlQuery.getSelectColumnModels(), bundle.getSelectColumns());
		assertEquals(maxRowsPerPage, bundle.getMaxRowsPerPage());
		
	}
}
