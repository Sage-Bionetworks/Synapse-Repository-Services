package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.query.ParseException;
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
	RowSet tooBigSet;
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
		int maxBytesPerRequest = 10000000;
		manager.setMaxBytesPerRequest(maxBytesPerRequest);
		user = new UserInfo(false, 7L);
		models = TableModelUtils.createOneOfEachType();
		tableId = "syn123";
		List<Row> rows = TableModelUtils.createRows(models, 10);
		set = new RowSet();
		set.setTableId(tableId);
		set.setHeaders(TableModelUtils.getHeaders(models));
		set.setRows(rows);
		
		// What is the row size for the model?
		int rowSizeBytes = TableModelUtils.calculateMaxRowSize(models);
		// Create a rowSet that is too big
		int tooManyRows = maxBytesPerRequest/rowSizeBytes+1;
		rows = TableModelUtils.createRows(models, tooManyRows);
		tooBigSet = new RowSet();
		tooBigSet.setTableId(tableId);
		tooBigSet.setHeaders(TableModelUtils.getHeaders(models));
		tooBigSet.setRows(rows);
		
		refSet = new RowReferenceSet();
		refSet.setTableId(tableId);
		refSet.setHeaders(TableModelUtils.getHeaders(models));
		
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(models);
		when(mockTableConnectionFactory.getConnection(tableId)).thenReturn(mockTableIndexDAO);
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
	
	@Test
	public void testAppendRowsTooLarge() throws DatastoreException, NotFoundException, IOException{
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(true);
		when(mockTruthDao.appendRowSetToTable(user.getId().toString(), tableId, models, set)).thenReturn(refSet);
		try {
			manager.appendRows(user, tableId, models, tooBigSet);
			fail("The passed RowSet should have been too large");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Request exceed the maximum number of bytes per request"));
		}
	}

	@Test (expected = UnauthorizedException.class)
	public void testQueryUnauthroized() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		manager.query(user, "select * from "+tableId, true, false);
	}
	
	@Test 
	public void testQueryHappyCaseIsConsistentFalse() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		RowSet expected = new RowSet();
		expected.setTableId(tableId);
		when(mockTableIndexDAO.query(any(SqlQuery.class))).thenReturn(expected);
		RowSet results = manager.query(user, "select * from "+tableId+" limit 1", false, false);
		// The etag should be null for this case
		assertEquals("The etag must be null for non-consistent query results.  These results cannot be used for a table update.", null, results.getEtag());
		assertEquals(expected, results);
		// The table status should not be checked for this case
		verify(mockTableStatusDAO, never()).getTableStatus(tableId);
	}
	
	@Test 
	public void testQueryHappyCaseIsConsistentTrue() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		TableStatus status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.AVAILABLE);
		status.setLastTableChangeEtag(UUID.randomUUID().toString());
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		RowSet results = manager.query(user, "select * from "+tableId+" limit 1", true, false);
		// The etag should be set
		assertEquals(status.getLastTableChangeEtag(), results.getEtag());
		assertEquals(set, results);
	}
	
	@Test 
	public void testQueryNoColumns() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		// Return no columns
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(new LinkedList<ColumnModel>());
		TableStatus status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.AVAILABLE);
		status.setLastTableChangeEtag(UUID.randomUUID().toString());
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		RowSet results = manager.query(user, "select * from "+tableId+" limit 1", true, false);
		assertNotNull(results);
		assertEquals(tableId, results.getTableId());
		assertNull(results.getEtag());
		assertNull(results.getHeaders());
		assertNull(results.getRows());
	}
	
	/**
	 * Test for a consistent query when the table index is not available.
	 * @throws Exception
	 */
	@Test
	public void testQueryIsConsistentTrueNotAvailable() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		TableStatus status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.PROCESSING);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		try{
			manager.query(user, "select * from "+tableId+" limit 1", true, false);
			fail("should have failed");
		}catch(TableUnavilableException e){
			// expected
			assertEquals(status, e.getStatus());
		}
	}
	
	/**
	 * Test for a consistent query when the table index worker is holding a write-lock-precursor
	 * on the index.  For this case the tryRunWithSharedLock() will throw a LockUnavilableException(),
	 * which should then be translated into a TableUnavilableException that contains the Table's status.
	 * @throws Exception
	 */
	@Test
	public void testQueryIsConsistentTrueLockUnavilableException() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		// First the status is checked show available.
		TableStatus status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// Throw a lock LockUnavilableException
		when(mockExclusiveOrSharedSemaphoreRunner.tryRunWithSharedLock(anyString(), anyLong(), any(Callable.class))).thenThrow(new LockUnavilableException());
		try{
			manager.query(user, "select * from "+tableId+" limit 1", true, false);
			fail("should have failed");
		}catch(TableUnavilableException e){
			// expected
			assertEquals(status, e.getStatus());
		}
	}
	
	@Test
	public void testValidateQuerySizeAggregation() throws ParseException{
		ColumnModel foo = new ColumnModel();
		foo.setColumnType(ColumnType.LONG);
		foo.setId("111");
		foo.setName("foo");
		ColumnModel bar = new ColumnModel();
		bar.setColumnType(ColumnType.STRING);
		bar.setId("222");
		bar.setName("bar");
		List<ColumnModel> models = Arrays.asList(foo, bar);
		Map<String, Long> nameToIdMap = TableModelUtils.createColumnNameToIdMap(models);
		SqlQuery query = new SqlQuery("select count(foo) from syn123", nameToIdMap);
		
		// Aggregate queries are always small enough to run. 
		TableRowManagerImpl.validateQuerySize(query, models, 1);
	}
	
	@Test
	public void testValidateQuerySizeMissingLimit() throws ParseException{
		ColumnModel foo = new ColumnModel();
		foo.setColumnType(ColumnType.LONG);
		foo.setId("111");
		foo.setName("foo");
		ColumnModel bar = new ColumnModel();
		bar.setColumnType(ColumnType.STRING);
		bar.setId("222");
		bar.setName("bar");
		bar.setMaximumSize(1L);
		List<ColumnModel> models = Arrays.asList(foo, bar);
		Map<String, Long> nameToIdMap = TableModelUtils.createColumnNameToIdMap(models);
		SqlQuery query = new SqlQuery("select foo, bar from syn123", nameToIdMap);
		
		int maxBytesPerRow = TableModelUtils.calculateMaxRowSize(models);
		try{
			TableRowManagerImpl.validateQuerySize(query, models, maxBytesPerRow*1000);
			fail("There is no limit on this query so it should have failed.");
		}catch (IllegalArgumentException e){
			// expected
			assertTrue(e.getMessage().contains("LIMIT"));
		}
	}
	
	@Test
	public void testValidateQuerySizeUnderLimit() throws ParseException{
		ColumnModel foo = new ColumnModel();
		foo.setColumnType(ColumnType.LONG);
		foo.setId("111");
		foo.setName("foo");
		ColumnModel bar = new ColumnModel();
		bar.setColumnType(ColumnType.STRING);
		bar.setId("222");
		bar.setName("bar");
		bar.setMaximumSize(2L);
		List<ColumnModel> models = Arrays.asList(foo, bar);
		Map<String, Long> nameToIdMap = TableModelUtils.createColumnNameToIdMap(models);
		SqlQuery query = new SqlQuery("select foo, bar from syn123 limit 2", nameToIdMap);
		
		int maxBytesPerRow = TableModelUtils.calculateMaxRowSize(models);
		// this is under the limit
		TableRowManagerImpl.validateQuerySize(query, models, maxBytesPerRow*2+1);
	}
	
	@Test 
	public void testValidateQuerySizeOverLimit() throws ParseException{
		ColumnModel foo = new ColumnModel();
		foo.setColumnType(ColumnType.LONG);
		foo.setId("111");
		foo.setName("foo");
		ColumnModel bar = new ColumnModel();
		bar.setColumnType(ColumnType.STRING);
		bar.setId("222");
		bar.setName("bar");
		bar.setMaximumSize(3L);
		List<ColumnModel> models = Arrays.asList(foo, bar);
		Map<String, Long> nameToIdMap = TableModelUtils.createColumnNameToIdMap(models);
		SqlQuery query = new SqlQuery("select foo, bar from syn123 limit 2", nameToIdMap);
		
		int maxBytesPerRow = TableModelUtils.calculateMaxRowSize(models);
		// Set too small for this query
		int testMaxBytesPerRow = maxBytesPerRow*2-1;
		// this is under the limit
		try{
			TableRowManagerImpl.validateQuerySize(query, models, testMaxBytesPerRow);
			fail("There is no limit on this query so it should have failed.");
		}catch (IllegalArgumentException e){
			// expected
			assertTrue(e.getMessage().contains(""+testMaxBytesPerRow));
		}
	}

}
