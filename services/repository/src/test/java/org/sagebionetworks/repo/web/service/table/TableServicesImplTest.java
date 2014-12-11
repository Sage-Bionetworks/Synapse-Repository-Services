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
import org.sagebionetworks.repo.manager.table.TableRowManagerImpl;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.Pair;
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
	QueryBundleRequest queryBundle;
	List<ColumnModel> models;
	List<String> headers;
	RowSet selectStar;
	QueryResult selectStarResult;
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
		selectStarResult = new QueryResult();
		selectStarResult.setNextPageToken(null);
		selectStarResult.setQueryResults(selectStar);

		sqlQuery = new SqlQuery("select * from "+tableId, models);
	}
}
