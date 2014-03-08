package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.semaphore.ExclusiveOrSharedSemaphoreRunner;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class TableRowManagerImplTest {
	
	TableRowTruthDAO mockTruthDao;
	AuthorizationManager mockAuthManager;
	TableStatusDAO mockTableStatusDAO;
	TableRowManagerImpl manager;
	ColumnModelDAO mockColumnModelDAO;
	ConnectionFactory mockTableConnectionFactory;
	TableIndexDAO mockTableIndexDAO;
	ExclusiveOrSharedSemaphoreRunner mockExclusiveOrSharedSemaphoreRunner;
	List<ColumnModel> models;
	UserInfo user;
	String tableId;
	RowSet set;
	RowReferenceSet refSet;
	
	
	@Before
	public void before() throws Exception {
		mockTruthDao = Mockito.mock(TableRowTruthDAO.class);
		mockAuthManager = Mockito.mock(AuthorizationManager.class);
		mockTableStatusDAO = Mockito.mock(TableStatusDAO.class);
		mockColumnModelDAO = Mockito.mock(ColumnModelDAO.class);
		mockTableConnectionFactory = Mockito.mock(ConnectionFactory.class);
		mockTableIndexDAO = Mockito.mock(TableIndexDAO.class);
		mockExclusiveOrSharedSemaphoreRunner = Mockito.mock(ExclusiveOrSharedSemaphoreRunner.class);
		
		// Just call the caller.
		stub(mockExclusiveOrSharedSemaphoreRunner.tryRunWithSharedLock(anyString(), anyLong(), any(Callable.class))).toAnswer(new Answer<RowSet>() {
			@Override
			public RowSet answer(InvocationOnMock invocation) throws Throwable {
				if(invocation == null) return null;
				Callable<RowSet> callable = (Callable<RowSet>) invocation.getArguments()[2];
				if(callable != null){
					return callable.call();
				}else{
					return null;
				}
			}
		});
		
		manager = new TableRowManagerImpl();
		user = new UserInfo(false, 7L);
		models = TableModelUtils.createOneOfEachType();
		tableId = "syn123";
		List<Row> rows = TableModelUtils.createRows(models, 10);
		set = new RowSet();
		set.setTableId(tableId);
		set.setHeaders(TableModelUtils.getHeaders(models));
		set.setRows(rows);
		
		refSet = new RowReferenceSet();
		refSet.setTableId(tableId);
		refSet.setHeaders(TableModelUtils.getHeaders(models));
		
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(models);
		when(mockTableConnectionFactory.getConnection(tableId)).thenReturn(mockTableIndexDAO);
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		when(mockTableIndexDAO.query(any(SqlQuery.class))).thenReturn(set);
	
		
		ReflectionTestUtils.setField(manager, "tableRowTruthDao", mockTruthDao);
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthManager);
		ReflectionTestUtils.setField(manager, "tableStatusDAO", mockTableStatusDAO);
		ReflectionTestUtils.setField(manager, "columnModelDAO", mockColumnModelDAO);
		ReflectionTestUtils.setField(manager, "tableConnectionFactory", mockTableConnectionFactory);
		ReflectionTestUtils.setField(manager, "exclusiveOrSharedSemaphoreRunner", mockExclusiveOrSharedSemaphoreRunner);
		ReflectionTestUtils.setField(manager, "tableReadTimeoutMS", 5000L);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testAppendRowsUnauthroized() throws DatastoreException, NotFoundException, IOException{
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(false);
		manager.appendRows(user, tableId, models, set);
	}
	
	@Test
	public void testAppendRowsHappy() throws DatastoreException, NotFoundException, IOException{
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(true);
		when(mockTruthDao.appendRowSetToTable(user.getId().toString(), tableId, models, set)).thenReturn(refSet);
		RowReferenceSet results = manager.appendRows(user, tableId, models, set);
		assertEquals(refSet, results);
		// verify the table status was set
		verify(mockTableStatusDAO, times(1)).resetTableStatusToProcessing(tableId);
	}

	@Test (expected = UnauthorizedException.class)
	public void testQueryUnauthroized() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		manager.query(user, "select * from "+tableId, true);
	}
	
	@Test 
	public void testHappyCaseIsConsistentFalse() throws Exception {
		RowSet expected = new RowSet();
		expected.setTableId(tableId);
		when(mockTableIndexDAO.query(any(SqlQuery.class))).thenReturn(expected);
		RowSet results = manager.query(user, "select * from "+tableId, false);
		assertEquals(expected, results);
	}
	
	@Test 
	public void testHappyCaseIsConsistentTrueNotAvailable() throws Exception {
		RowSet results = manager.query(user, "select * from "+tableId, true);
		assertEquals(set, results);
	}
}
