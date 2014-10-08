package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.table.ColumnModelManagerImpl;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Unit test for the ColumnManager.
 * @author John
 *
 */
public class ColumnModelManagerTest {
	
	ColumnModelDAO mockColumnModelDAO;
	AuthorizationManager mockauthorizationManager;
	ColumnModelManagerImpl columnModelManager;
	TableStatusDAO mockTableStatusDao;
	UserInfo user;
	
	@Before
	public void before(){
		mockColumnModelDAO = Mockito.mock(ColumnModelDAO.class);
		mockauthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockTableStatusDao = Mockito.mock(TableStatusDAO.class);
		columnModelManager = new ColumnModelManagerImpl();
		user = new UserInfo(false, 123L);
		ReflectionTestUtils.setField(columnModelManager, "columnModelDao", mockColumnModelDAO);
		ReflectionTestUtils.setField(columnModelManager, "authorizationManager", mockauthorizationManager);
		ReflectionTestUtils.setField(columnModelManager, "tableStatusDAO", mockTableStatusDao);
	}
	
	@Test
	public void testListColumnModels(){
		String prefix = "A";
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		ColumnModel cm = new ColumnModel();
		cm.setName("abb");
		results.add(cm);
		long limitMax = 100;
		when(mockColumnModelDAO.listColumnModels(prefix, limitMax, 0)).thenReturn(results);
		when(mockColumnModelDAO.listColumnModelsCount(prefix)).thenReturn(1l);
		
		// make the call
		PaginatedColumnModels page = columnModelManager.listColumnModels(user, prefix, limitMax, 0);
		assertNotNull(page);
		assertEquals(new Long(1), page.getTotalNumberOfResults());
		assertEquals(results, page.getResults());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testListColumnModelsLimitNegative(){
		PaginatedColumnModels page = columnModelManager.listColumnModels(user, "aa", -1, 0);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testListColumnModelsLimitTooLarge(){
		PaginatedColumnModels page = columnModelManager.listColumnModels(user, "aa", 101, 0);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testListColumnModelsOffsetNegative(){
		PaginatedColumnModels page = columnModelManager.listColumnModels(user, "aa", 1, -1);
	}
	
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateColumnModelAnonymous() throws UnauthorizedException, DatastoreException, NotFoundException{
		ColumnModel cm = new ColumnModel();
		cm.setName("abb");
		// Setup the anonymous users
		when(mockauthorizationManager.isAnonymousUser(user)).thenReturn(true);
		columnModelManager.createColumnModel(user, cm);
	}
	
	@Test
	public void testCreateColumnModelHappy() throws DatastoreException, NotFoundException{
		ColumnModel in = new ColumnModel();
		in.setName("abb");
		ColumnModel out = new ColumnModel();
		out.setName("abb");
		out.setId("21");
		// Setup the anonymous users
		when(mockauthorizationManager.isAnonymousUser(user)).thenReturn(false);
		when(mockColumnModelDAO.createColumnModel(in)).thenReturn(out);
		ColumnModel results = columnModelManager.createColumnModel(user, in);
		assertEquals(out, results);
	}
	
	@Test
	public void testCreateColumnModelsHappy() throws DatastoreException, NotFoundException {
		ColumnModel in1 = new ColumnModel();
		in1.setName("abb1");
		ColumnModel in2 = new ColumnModel();
		in2.setName("abb2");
		ColumnModel out1 = new ColumnModel();
		out1.setName("abb1");
		out1.setId("21");
		ColumnModel out2 = new ColumnModel();
		out2.setName("abb2");
		out2.setId("22");
		// Setup the anonymous users
		when(mockauthorizationManager.isAnonymousUser(user)).thenReturn(false);
		when(mockColumnModelDAO.createColumnModel(in1)).thenReturn(out1);
		when(mockColumnModelDAO.createColumnModel(in2)).thenReturn(out2);
		List<ColumnModel> results = columnModelManager.createColumnModels(user, Lists.newArrayList(in1, in2));
		assertEquals(Lists.newArrayList(out1, out2), results);
	}

	/**
	 * Should not be able to create a column with a name that is reserved.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test
	public void testCreateColumnModelReservedName() throws DatastoreException, NotFoundException{
		ColumnModel valid = new ColumnModel();
		valid.setName("abc");
		ColumnModel invalid = new ColumnModel();
		invalid.setName(TableConstants.ROW_ID.toLowerCase());
		// Setup the anonymous users
		when(mockauthorizationManager.isAnonymousUser(user)).thenReturn(false);
		when(mockColumnModelDAO.createColumnModel(invalid)).thenReturn(invalid);
		try{
			columnModelManager.createColumnModel(user, invalid);
			fail("should not be able to create a column model with a reserved column name");
		}catch(IllegalArgumentException e){
			// expected
			assertTrue(e.getMessage().contains(invalid.getName()));
		}
		try {
			columnModelManager.createColumnModels(user, Lists.newArrayList(valid, invalid));
			fail("should not be able to create a column model with a reserved column name");
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().contains(invalid.getName()));
		}
	}
	
	/**
	 * Should not be able to create a column with a name that is a SQL key word
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test
	public void testCreateColumnModelKeyWordsAsName() throws DatastoreException, NotFoundException{
		ColumnModel valid = new ColumnModel();
		valid.setName("abc");
		ColumnModel invalid = new ColumnModel();
		invalid.setName("max");
		// Setup the anonymous users
		when(mockauthorizationManager.isAnonymousUser(user)).thenReturn(false);
		when(mockColumnModelDAO.createColumnModel(invalid)).thenReturn(invalid);
		try{
			columnModelManager.createColumnModel(user, invalid);
			fail("should not be able to create a column model with a key word column name");
		}catch(IllegalArgumentException e){
			// expected
			assertTrue(e.getMessage().contains(invalid.getName()));
			assertTrue(e.getMessage().contains("SQL key word"));
		}
		try {
			columnModelManager.createColumnModels(user, Lists.newArrayList(valid, invalid));
			fail("should not be able to create a column model with a reserved column name");
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().contains(invalid.getName()));
			assertTrue(e.getMessage().contains("SQL key word"));
		}
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetColumnsNullUser() throws DatastoreException, NotFoundException{
		List<String> ids = new LinkedList<String>();
		columnModelManager.getColumnModel(null, ids, false);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetColumnsNullList() throws DatastoreException, NotFoundException{
		columnModelManager.getColumnModel(user, (List<String>) null, false);
	}
	
	@Test
	public void testGetColumnsHappy() throws DatastoreException, NotFoundException{
		List<String> ids = new LinkedList<String>();
		ids.add("123");
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		ColumnModel cm = new ColumnModel();
		cm.setName("abb");
		cm.setId("123");
		results.add(cm);
		when(mockColumnModelDAO.getColumnModel(ids, false)).thenReturn(results);
		List<ColumnModel> out = columnModelManager.getColumnModel(user, ids, false);
		assertNotNull(out);
		assertEquals(results, out);
	}
	
	@Test
	public void testGetColumnsSingleHappy() throws DatastoreException, NotFoundException{
		ColumnModel cm = new ColumnModel();
		String id = "123";
		cm.setName("abb");
		cm.setId(id);
		when(mockColumnModelDAO.getColumnModel(id)).thenReturn(cm);
		ColumnModel result  = columnModelManager.getColumnModel(user, id);
		assertNotNull(result);
		assertEquals(cm, result);
	}

	
	@Test
	public void testBindColumnToObjectHappy() throws DatastoreException, NotFoundException{
		String objectId = "syn123";
		when(mockauthorizationManager.canAccess(user, objectId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		List<String> ids = new LinkedList<String>();
		ids.add("123");
		when(mockColumnModelDAO.bindColumnToObject(ids, objectId)).thenReturn(1);
		assertTrue(columnModelManager.bindColumnToObject(user, ids, objectId, false));
		// Validate that the table status gets changed
		verify(mockTableStatusDao, times(1)).resetTableStatusToProcessing(objectId);
	}
	
	/**
	 * This is a test for PLFM-2636
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test
	public void testBindColumnNull() throws DatastoreException, NotFoundException{
		String objectId = "syn123";
		when(mockauthorizationManager.canAccess(user, objectId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		List<String> ids = new LinkedList<String>();
		ids.add("123");
		when(mockColumnModelDAO.bindColumnToObject(ids, objectId)).thenReturn(0);
		assertTrue("Binding null columns should trigger a rest for a new object",columnModelManager.bindColumnToObject(user, ids, objectId, true));
		// Validate that the table status gets changed
		verify(mockTableStatusDao, times(1)).resetTableStatusToProcessing(objectId);
	}
	
	
	@Test (expected =IllegalArgumentException.class)
	public void testListObjectsBoundToColumnNullUser(){
		Set<String> columnId = new HashSet<String>();
		columnModelManager.listObjectsBoundToColumn(null, columnId, true, Long.MAX_VALUE, 0);
	}
	
	@Test (expected =IllegalArgumentException.class)
	public void testListObjectsBoundToColumnNullSet(){
		Set<String> columnId = new HashSet<String>();
		columnModelManager.listObjectsBoundToColumn(user, null, true, Long.MAX_VALUE, 0);
	}
	
	@Test
	public void testListObjectsBoundToColumnHappy(){
		Set<String> columnIds = new HashSet<String>();
		columnIds.add("134");
		List<String> resultList = new LinkedList<String>();
		resultList.add("syn987");
		when(mockColumnModelDAO.listObjectsBoundToColumn(columnIds, false, 10, 0)).thenReturn(resultList);
		when(mockColumnModelDAO.listObjectsBoundToColumnCount(columnIds, false)).thenReturn(1l);
		PaginatedIds page = columnModelManager.listObjectsBoundToColumn(user, columnIds, false, 10, 0);
		assertNotNull(page);
		assertEquals(resultList, page.getResults());
		assertEquals(new Long(1), page.getTotalNumberOfResults());
	}
	
	@Test
	public void testUnbindColumnFromObject() throws DatastoreException, NotFoundException {
		String objectId = "syn123";
		columnModelManager.unbindAllColumnsAndOwnerFromObject(objectId);
		verify(mockColumnModelDAO).unbindAllColumnsFromObject(objectId);
		verify(mockColumnModelDAO).deleteOwner(objectId);
	}

	@Test (expected=UnauthorizedException.class)
	public void testTruncateAllDataUnauthroized(){
		UserInfo user = new UserInfo(false);
		columnModelManager.truncateAllColumnData(user);
	}
	
	@Test
	public void testTruncateAllDataHappy(){
		UserInfo user = new UserInfo(true);
		when(mockColumnModelDAO.truncateAllColumnData()).thenReturn(true);
		assertTrue(columnModelManager.truncateAllColumnData(user));
	}
	
	@Test
	public void testGetColumnModelsForTableAuthorized() throws DatastoreException, NotFoundException{
		String objectId = "syn123";
		List<ColumnModel> expected = TableModelTestUtils.createOneOfEachType();
		when(mockauthorizationManager.canAccess(user, objectId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockColumnModelDAO.getColumnModelsForObject(objectId)).thenReturn(expected);
		List<ColumnModel> resutls = columnModelManager.getColumnModelsForTable(user, objectId);
		assertEquals(expected, resutls);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetColumnModelsForTableUnauthorized() throws DatastoreException, NotFoundException{
		String objectId = "syn123";
		List<ColumnModel> expected = TableModelTestUtils.createOneOfEachType();
		when(mockauthorizationManager.canAccess(user, objectId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		when(mockColumnModelDAO.getColumnModelsForObject(objectId)).thenReturn(expected);
		List<ColumnModel> resutls = columnModelManager.getColumnModelsForTable(user, objectId);
	}
}
