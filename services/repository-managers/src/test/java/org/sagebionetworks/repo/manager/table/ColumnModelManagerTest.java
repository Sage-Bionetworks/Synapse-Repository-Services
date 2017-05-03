package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnChangeDetails;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

/**
 * Unit test for the ColumnManager.
 * @author John
 *
 */
public class ColumnModelManagerTest {
	
	@Mock
	ColumnModelDAO mockColumnModelDAO;
	@Mock
	AuthorizationManager mockauthorizationManager;
	ColumnModelManagerImpl columnModelManager;
	UserInfo user;
	
	String tableId;
	List<ColumnModel> currentSchema;
	List<ColumnChange> changes;
	List<String> expectedNewSchemaIds;
	List<ColumnModel> newSchema;
	List<ColumnModel> underLimitSchema;
	List<String> underLimitSchemaIds;
	
	List<String> overLimitSchemaIds;
	List<ColumnModel> overLimitSchema;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		columnModelManager = new ColumnModelManagerImpl();
		user = new UserInfo(false, 123L);
		ReflectionTestUtils.setField(columnModelManager, "columnModelDao", mockColumnModelDAO);
		ReflectionTestUtils.setField(columnModelManager, "authorizationManager", mockauthorizationManager);
		
		tableId = "syn567";
		currentSchema = Lists.newArrayList(
				TableModelTestUtils.createColumn(111L),
				TableModelTestUtils.createColumn(222L),
				TableModelTestUtils.createColumn(333L)
				);
		
		
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(currentSchema);
		
		ColumnChange remove = new ColumnChange();
		remove.setOldColumnId("111");
		remove.setNewColumnId(null);
		
		ColumnChange update = new ColumnChange();
		update.setOldColumnId("222");
		update.setNewColumnId("444");
		
		ColumnChange add = new ColumnChange();
		add.setOldColumnId(null);
		add.setNewColumnId("555");
		
		changes = Lists.newArrayList(remove, update, add);
		
		expectedNewSchemaIds = Lists.newArrayList("444","333","555");
		
		List<ColumnModel> newSchema = Lists.newArrayList(
				TableModelTestUtils.createColumn(444L),
				TableModelTestUtils.createColumn(333L),
				TableModelTestUtils.createColumn(555L)
				);
		when(mockColumnModelDAO.getColumnModel(expectedNewSchemaIds, false)).thenReturn(newSchema);
		
		underLimitSchemaIds = Lists.newArrayList();
		underLimitSchema = Lists.newArrayList();
		// Currently the breaking point is 23 string columns of size 1000.
		for(int i=0; i<21; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.STRING);
			cm.setMaximumSize(1000L);
			underLimitSchema.add(cm);
			underLimitSchemaIds.add(""+cm.getId());
		}
		
		overLimitSchemaIds = Lists.newArrayList();
		overLimitSchema = Lists.newArrayList();
		// Currently the breaking point is 23 string columns of size 1000.
		for(int i=0; i<23; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.STRING);
			cm.setMaximumSize(1000L);
			overLimitSchema.add(cm);
			overLimitSchemaIds.add(""+cm.getId());
		}
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
		columnModelManager.listColumnModels(user, "aa", -1, 0);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testListColumnModelsLimitTooLarge(){
		columnModelManager.listColumnModels(user, "aa", 101, 0);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testListColumnModelsOffsetNegative(){
		columnModelManager.listColumnModels(user, "aa", 1, -1);
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
	
	@Test (expected = IllegalArgumentException.class)
	public void testCreateColumnModelsInvalidFacetType(){
		ColumnModel in1 = new ColumnModel();
		in1.setName("abb1");
		ColumnModel in2 = new ColumnModel();
		in2.setName("abb2");
		in2.setColumnType(ColumnType.LARGETEXT);
		in2.setFacetType(FacetType.enumeration);
		
		columnModelManager.createColumnModels(user, Lists.newArrayList(in1, in2));
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
	public void testCreateColumnModelInvalidFacetType(){
		ColumnModel cm = new ColumnModel();
		cm.setName("abc");
		cm.setColumnType(ColumnType.LARGETEXT);
		cm.setFacetType(FacetType.enumeration);
		
		columnModelManager.createColumnModel(user, cm);
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
		assertTrue(columnModelManager.bindColumnToObject(user, ids, objectId));
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
		assertFalse("Binding null columns should trigger a rest for a new object",columnModelManager.bindColumnToObject(user, ids, objectId));
	}
	
	
	@Test (expected =IllegalArgumentException.class)
	public void testListObjectsBoundToColumnNullUser(){
		Set<String> columnId = new HashSet<String>();
		columnModelManager.listObjectsBoundToColumn(null, columnId, true, Long.MAX_VALUE, 0);
	}
	
	@Test (expected =IllegalArgumentException.class)
	public void testListObjectsBoundToColumnNullSet(){
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
		columnModelManager.getColumnModelsForTable(user, objectId);
	}
	
	@Test
	public void testValidateSchemaSizeUnderLimit(){
		List<String> scheamIds = Lists.newArrayList();
		List<ColumnModel> schema = Lists.newArrayList();
		// Currently the breaking point is 23 string columns of size 1000.
		for(int i=0; i<21; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.STRING);
			cm.setMaximumSize(1000L);
			schema.add(cm);
			scheamIds.add(""+cm.getId());
		}
		when(mockColumnModelDAO.getColumnModel(scheamIds, false)).thenReturn(schema);
		
		columnModelManager.validateSchemaSize(scheamIds);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateSchemaSizeOverLimit(){
		List<String> scheamIds = Lists.newArrayList();
		List<ColumnModel> schema = Lists.newArrayList();
		// Currently the breaking point is 23 string columns of size 1000.
		for(int i=0; i<23; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.STRING);
			cm.setMaximumSize(1000L);
			schema.add(cm);
			scheamIds.add(""+cm.getId());
		}
		when(mockColumnModelDAO.getColumnModel(scheamIds, false)).thenReturn(schema);
		
		columnModelManager.validateSchemaSize(scheamIds);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateSchemaTooManyColumns(){
		List<String> scheamIds = Lists.newArrayList();
		List<ColumnModel> schema = Lists.newArrayList();
		int numberOfColumns = ColumnModelManagerImpl.MY_SQL_MAX_COLUMNS_PER_TABLE+1;
		for(int i=0; i<numberOfColumns; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.BOOLEAN);
			cm.setMaximumSize(1L);
			schema.add(cm);
			scheamIds.add(""+cm.getId());
		}
		when(mockColumnModelDAO.getColumnModel(scheamIds, false)).thenReturn(schema);
		columnModelManager.validateSchemaSize(scheamIds);
	}
	
	@Test
	public void testValidateSchemaMaxColumns(){
		List<String> scheamIds = Lists.newArrayList();
		List<ColumnModel> schema = Lists.newArrayList();
		int numberOfColumns = ColumnModelManagerImpl.MY_SQL_MAX_COLUMNS_PER_TABLE;
		for(int i=0; i<numberOfColumns; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.BOOLEAN);
			cm.setMaximumSize(1L);
			schema.add(cm);
			scheamIds.add(""+cm.getId());
		}
		when(mockColumnModelDAO.getColumnModel(scheamIds, false)).thenReturn(schema);
		List<ColumnModel> l = columnModelManager.validateSchemaSize(scheamIds);
		assertNotNull(l);
	}
	
	@Test
	public void testValidateSchemaMaxColumnsEmpty(){
		List<ColumnModel> results = columnModelManager.validateSchemaSize(new LinkedList<String>());
		assertNotNull(results);
	}
	
	
	/**
	 * See PLFM-3619.  This schema should be just under the limit.
	 */
	@Test
	public void testUnderDataPerColumnLimit(){
		String objectId = "syn123";
		when(mockColumnModelDAO.getColumnModel(underLimitSchemaIds, false)).thenReturn(underLimitSchema);
		//call under test
		columnModelManager.bindColumnToObject(user, underLimitSchemaIds, objectId);
	}
	
	/**
	 * See PLFM-3619.  This schema should be just over the limit.
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testOverDataPerColumnLimit(){
		String objectId = "syn123";
		when(mockColumnModelDAO.getColumnModel(overLimitSchemaIds, false)).thenReturn(overLimitSchema);
		//call under test
		columnModelManager.bindColumnToObject(user, overLimitSchemaIds, objectId);
	}
	
	/**
	 * Check the max columns per table.
	 */
	@Test
	public void testOverMaxColumnsPerTable(){
		String objectId = "syn123";
		List<String> scheamIds = Lists.newArrayList();
		List<ColumnModel> schema = Lists.newArrayList();
		int numberOfColumns = ColumnModelManagerImpl.MY_SQL_MAX_COLUMNS_PER_TABLE+1;
		for(int i=0; i<numberOfColumns; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.BOOLEAN);
			cm.setMaximumSize(1000L);
			schema.add(cm);
			scheamIds.add(""+cm.getId());
		}
		when(mockColumnModelDAO.getColumnModel(scheamIds, false)).thenReturn(schema);
		try {
			//call under test
			columnModelManager.bindColumnToObject(user, scheamIds, objectId);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().startsWith("Too many columns"));
		}
	}
	
	@Test
	public void testGetColumnChangeDetails(){
		List<ColumnChange> changes = TableModelTestUtils.createAddUpdateDeleteColumnChange();
		List<ColumnModel> columns = TableModelTestUtils.createColumnsForChanges(changes);
		
		when(mockColumnModelDAO.getColumnModel(anyListOf(String.class), anyBoolean())).thenReturn(columns);
		
		List<ColumnChangeDetails> expected = Lists.newArrayList(
				new ColumnChangeDetails(null, columns.get(0)),
				new ColumnChangeDetails(columns.get(1), columns.get(2)),
				new ColumnChangeDetails(columns.get(3), null));
		
		// Call under test
		List<ColumnChangeDetails> results = columnModelManager.getColumnChangeDetails(changes);
		assertEquals(expected, results);
	}

	@Test
	public void testCalculateNewSchemaIdsAndValidateWithNullOrderedColumnIds(){
		assertEquals(expectedNewSchemaIds,
				columnModelManager.calculateNewSchemaIdsAndValidate(tableId, changes, null));
	}

	@Test
	public void testCalculateNewSchemaIdsAndValidate(){
		assertEquals(expectedNewSchemaIds,
				columnModelManager.calculateNewSchemaIdsAndValidate(tableId, changes, expectedNewSchemaIds));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCalculateNewSchemaIdsAndValidateMissingInUserProvidedColumns(){
		columnModelManager.calculateNewSchemaIdsAndValidate(tableId, changes, new LinkedList<String>());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCalculateNewSchemaIdsAndValidateMissingFromUserProvidedColumns(){
		expectedNewSchemaIds.add("columnId that does not exist");
		columnModelManager.calculateNewSchemaIdsAndValidate(tableId, changes, expectedNewSchemaIds);
	}
	
	@Test
	public void testCalculateNewSchemaIdsAndValidatePLFM_4188(){
		changes = TableModelTestUtils.createAllDeleteColumnChange();
		assertEquals(new LinkedList<String>(),
				columnModelManager.calculateNewSchemaIdsAndValidate(tableId, changes, new LinkedList<String>()));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateNewSchemaIdsAndValidateOverSizeLimit(){
		// setup the new schema to be over the limit.
		when(mockColumnModelDAO.getColumnModel(anyListOf(String.class), anyBoolean())).thenReturn(overLimitSchema);
		// call under test.
		columnModelManager.calculateNewSchemaIdsAndValidate(tableId, changes, expectedNewSchemaIds);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateNewSchemaIdsAndValidateMissingUpdate(){
		String tableId = "syn567";
		List<ColumnModel> currentSchema = Lists.newArrayList(
				TableModelTestUtils.createColumn(111L)
		);
		
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(currentSchema);
		// update a column that does not exist.
		ColumnChange update = new ColumnChange();
		update.setOldColumnId("222");
		update.setNewColumnId("444");
		
		changes = Lists.newArrayList(update);
		// call under test.
		columnModelManager.calculateNewSchemaIdsAndValidate(tableId, changes, expectedNewSchemaIds);
	}
	
	@Test
	public void testIsFileHandleColumnTrue(){
		ColumnModel column = new ColumnModel();
		column.setColumnType(ColumnType.FILEHANDLEID);
		// call under test
		assertTrue(ColumnModelManagerImpl.isFileHandleColumn(column));
	}
	
	@Test
	public void testIsFileHandleColumnFalse(){
		ColumnModel column = new ColumnModel();
		column.setColumnType(ColumnType.STRING);
		// call under test
		assertFalse(ColumnModelManagerImpl.isFileHandleColumn(column));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testIsFileHandleColumnNull(){
		ColumnModel column = null;
		// call under test
		assertFalse(ColumnModelManagerImpl.isFileHandleColumn(column));
	}
	
	@Test
	public void testValidateColumnChangeFileToNonFile() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setColumnType(ColumnType.FILEHANDLEID);
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.BOOLEAN);
		// Call under test
		try {
			ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn);
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals(String.format(
					ColumnModelManagerImpl.COLUMN_TYPE_ERROR_TEMPLATE,
					ColumnType.FILEHANDLEID, ColumnType.BOOLEAN),
					e.getMessage());
		}
	}
	
	@Test
	public void testValidateColumnChangeNonFileToFile() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setColumnType(ColumnType.STRING);
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.FILEHANDLEID);
		// Call under test
		try {
			ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn);
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals(String.format(
					ColumnModelManagerImpl.COLUMN_TYPE_ERROR_TEMPLATE,
					ColumnType.STRING, ColumnType.FILEHANDLEID),
					e.getMessage());
		}
	}
	
	@Test
	public void testValidateColumnChangeAddFile() {
		ColumnModel oldColumn = null;
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.FILEHANDLEID);
		// Call under test
		ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn);
	}
	
	@Test
	public void testValidateColumnChangeRemoveFile() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setColumnType(ColumnType.FILEHANDLEID);
		ColumnModel newColumn = null;
		// Call under test
		ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn);
	}
	
	@Test
	public void testValidateColumnChangeFileToFile() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setColumnType(ColumnType.FILEHANDLEID);
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.FILEHANDLEID);
		// Call under test
		ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn);
	}
	
	//////////////////////////////
	// validateFacetType() tests
	//////////////////////////////
	@Test
	public void testValidateFacetTypeNullFacetType(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setFacetType(null);
		//should do nothing
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test
	public void testValidateFacetTypeStringColumnEnumerationFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setFacetType(FacetType.enumeration);
		//should do nothing
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test
	public void testValidateFacetTypeStringColumnRangeFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setFacetType(FacetType.range);
		//should do nothing
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test
	public void testValidateFacetTypeIntegerColumnEnumerationFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.INTEGER);
		columnModel.setFacetType(FacetType.enumeration);
		//should do nothing
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test
	public void testValidateFacetTypeIntegerColumnRangeFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.INTEGER);
		columnModel.setFacetType(FacetType.range);
		//should do nothing
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test
	public void testValidateFacetTypeBooleanColumnEnumerationFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.BOOLEAN);
		columnModel.setFacetType(FacetType.enumeration);
		//should do nothing
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testValidateFacetTypeBooleanColumnRangeFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.BOOLEAN);
		columnModel.setFacetType(FacetType.range);
		//should throw exception
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testValidateFacetTypeDoubleColumnEnumerationFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.DOUBLE);
		columnModel.setFacetType(FacetType.enumeration);
		//should throw exception
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test 
	public void testValidateFacetTypeDoubleColumnRangeFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.DOUBLE);
		columnModel.setFacetType(FacetType.range);
		//should do nothing
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testValidateFacetTypeDateColumnEnumerationFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.DATE);
		columnModel.setFacetType(FacetType.enumeration);
		//should throw exception
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test 
	public void testValidateFacetTypeDateColumnRangeFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.DATE);
		columnModel.setFacetType(FacetType.range);
		//should do nothing
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testValidateFacetTypeOtherColumnTypeEnumerationFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.LARGETEXT);
		columnModel.setFacetType(FacetType.enumeration);
		//should throw exception
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testValidateFacetTypeOtherColumnTypeRangeFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.LARGETEXT);
		columnModel.setFacetType(FacetType.range);
		//should throw exception
		columnModelManager.validateFacetType(columnModel);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateSchemaWithProvidedOrderedColumnsWithNullSchema() {
		columnModelManager.validateSchemaWithProvidedOrderedColumns(null, new LinkedList<String>());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateSchemaWithProvidedOrderedColumnsWithNullOrderedList() {
		columnModelManager.validateSchemaWithProvidedOrderedColumns(new LinkedList<String>(), null);
	}

	@Test
	public void testValidateSchemaWithProvidedOrderedColumnsWithEmptyList() {
		columnModelManager.validateSchemaWithProvidedOrderedColumns(new LinkedList<String>(), new LinkedList<String>());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateSchemaWithProvidedOrderedColumnsWithMissingFromSchema() {
		columnModelManager.validateSchemaWithProvidedOrderedColumns(Arrays.asList("1"), Arrays.asList("1", "2"));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateSchemaWithProvidedOrderedColumnsWithMissingFromOrderedList() {
		columnModelManager.validateSchemaWithProvidedOrderedColumns(Arrays.asList("1", "2"), Arrays.asList("2"));
	}

	@Test
	public void testValidateSchemaWithProvidedOrderedColumnsWithDifferentOrder() {
		columnModelManager.validateSchemaWithProvidedOrderedColumns(Arrays.asList("1", "2"), Arrays.asList("2", "1"));
	}
}