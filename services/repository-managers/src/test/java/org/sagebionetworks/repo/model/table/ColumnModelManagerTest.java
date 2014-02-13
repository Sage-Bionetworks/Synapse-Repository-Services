package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManagerImpl;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for the ColumnManager.
 * @author John
 *
 */
public class ColumnModelManagerTest {
	
	ColumnModelDAO mockColumnModelDAO;
	AuthorizationManager mockauthorizationManager;
	ColumnModelManagerImpl columnModelManager;
	UserInfo user;
	
	@Before
	public void before(){
		mockColumnModelDAO = Mockito.mock(ColumnModelDAO.class);
		mockauthorizationManager = Mockito.mock(AuthorizationManager.class);
		columnModelManager = new ColumnModelManagerImpl();
		user = new UserInfo(false, 123L);
		ReflectionTestUtils.setField(columnModelManager, "columnModelDao", mockColumnModelDAO);
		ReflectionTestUtils.setField(columnModelManager, "authorizationManager", mockauthorizationManager);
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
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetColumnsNullUser() throws DatastoreException, NotFoundException{
		List<String> ids = new LinkedList<String>();
		columnModelManager.getColumnModel(null, ids);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetColumnsNullList() throws DatastoreException, NotFoundException{
		columnModelManager.getColumnModel(user, (List<String>)null);
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
		when(mockColumnModelDAO.getColumnModel(ids)).thenReturn(results);
		List<ColumnModel> out  = columnModelManager.getColumnModel(user, ids);
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
		when(mockauthorizationManager.canAccess(user, objectId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(true);
		List<String> ids = new LinkedList<String>();
		ids.add("123");
		when(mockColumnModelDAO.bindColumnToObject(ids, objectId)).thenReturn(1);
		assertTrue(columnModelManager.bindColumnToObject(user, ids, objectId));
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
		List<ColumnModel> expected = TableModelUtils.createOneOfEachType();
		when(mockauthorizationManager.canAccess(user, objectId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		when(mockColumnModelDAO.getColumnModelsForObject(objectId)).thenReturn(expected);
		List<ColumnModel> resutls = columnModelManager.getColumnModelsForTable(user, objectId);
		assertEquals(expected, resutls);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetColumnModelsForTableUnauthorized() throws DatastoreException, NotFoundException{
		String objectId = "syn123";
		List<ColumnModel> expected = TableModelUtils.createOneOfEachType();
		when(mockauthorizationManager.canAccess(user, objectId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		when(mockColumnModelDAO.getColumnModelsForObject(objectId)).thenReturn(expected);
		List<ColumnModel> resutls = columnModelManager.getColumnModelsForTable(user, objectId);
	}
}
