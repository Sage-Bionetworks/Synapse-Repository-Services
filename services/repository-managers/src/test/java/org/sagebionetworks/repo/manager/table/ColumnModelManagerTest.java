package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.table.ColumnConstants.MAX_NUMBER_OF_LARGE_TEXT_COLUMNS_PER_TABLE;
import static org.sagebionetworks.repo.model.table.ColumnConstants.MY_SQL_MAX_COLUMNS_PER_TABLE;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.repo.model.table.ColumnConstants;

/**
 * Unit test for the ColumnManager.
 * @author John
 *
 */
@ExtendWith(MockitoExtension.class)
public class ColumnModelManagerTest {
	

	// Currently the breaking point is 16 string columns of size 1000.
	private static final int MAX_NUMBER_OF_MAX_STRINGS_PER_TABLE = 16;
	
	@Mock
	ColumnModelDAO mockColumnModelDAO;
	@Mock
	AuthorizationManager mockauthorizationManager;
	@Mock
	NodeDAO mockNodeDao;
	@InjectMocks
	ColumnModelManagerImpl columnModelManager;
	UserInfo user;
	
	String tableId;
	IdAndVersion idAndVersion;
	List<ColumnModel> currentSchema;
	List<ColumnChange> changes;
	List<String> expectedNewSchemaIds;
	List<ColumnModel> newSchema;
	List<ColumnModel> underLimitSchema;
	List<String> underLimitSchemaIds;
	
	List<String> overLimitSchemaIds;
	List<ColumnModel> overLimitSchema;
	
	@BeforeEach
	public void before(){
		user = new UserInfo(false, 123L);
		
		tableId = "syn567";
		idAndVersion = IdAndVersion.parse(tableId);
		currentSchema = Lists.newArrayList(
				TableModelTestUtils.createColumn(111L),
				TableModelTestUtils.createColumn(222L),
				TableModelTestUtils.createColumn(333L)
				);
		
		
		lenient().when(mockColumnModelDAO.getColumnModelsForObject(idAndVersion)).thenReturn(currentSchema);

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
		
		newSchema = Lists.newArrayList(
				TableModelTestUtils.createColumn(444L),
				TableModelTestUtils.createColumn(333L),
				TableModelTestUtils.createColumn(555L)
				);
		lenient().when(mockColumnModelDAO.getColumnModels(anyListOf(String.class))).thenReturn(newSchema);

		underLimitSchemaIds = Lists.newArrayList();
		underLimitSchema = Lists.newArrayList();
		// Currently the breaking point is 23 string columns of size 1000.
		for(int i=0; i<MAX_NUMBER_OF_MAX_STRINGS_PER_TABLE; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.STRING);
			cm.setMaximumSize(1000L);
			underLimitSchema.add(cm);
			underLimitSchemaIds.add(""+cm.getId());
		}
		
		overLimitSchemaIds = Lists.newArrayList();
		overLimitSchema = Lists.newArrayList();
		// Currently the breaking point is 17 string columns of size 1000.
		for(int i=0; i<17; i++){
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
	
	@Test
	public void testListColumnModelsLimitNegative(){
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.listColumnModels(user, "aa", -1, 0);
		});
	}
	
	@Test
	public void testListColumnModelsLimitTooLarge(){
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.listColumnModels(user, "aa", 101, 0);
		});
	}
	
	@Test
	public void testListColumnModelsOffsetNegative(){
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.listColumnModels(user, "aa", 1, -1);
		});
	}
	
	
	@Test
	public void testCreateColumnModelAnonymous() throws UnauthorizedException, DatastoreException, NotFoundException{
		ColumnModel cm = new ColumnModel();
		cm.setName("abb");
		// Setup the anonymous users
		when(mockauthorizationManager.isAnonymousUser(user)).thenReturn(true);
		assertThrows(UnauthorizedException.class, () -> {
			columnModelManager.createColumnModel(user, cm);
		});
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
	
	@Test
	public void testCreateColumnModelsInvalidFacetType(){
		ColumnModel in1 = new ColumnModel();
		in1.setName("abb1");
		ColumnModel in2 = new ColumnModel();
		in2.setName("abb2");
		in2.setColumnType(ColumnType.LARGETEXT);
		in2.setFacetType(FacetType.enumeration);

		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.createColumnModels(user, Lists.newArrayList(in1, in2));
		});
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
	
	@Test
	public void testCreateColumnModelInvalidFacetType(){
		ColumnModel cm = new ColumnModel();
		cm.setName("abc");
		cm.setColumnType(ColumnType.LARGETEXT);
		cm.setFacetType(FacetType.enumeration);

		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.createColumnModel(user, cm);
		});
	}
	
	@Test
	public void testGetColumnsNullList() throws DatastoreException, NotFoundException{
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.getAndValidateColumnModels(null);
		});
	}
	
	@Test
	public void testGetColumns() {
		//Call under test
		List<ColumnModel> resutls = columnModelManager.getAndValidateColumnModels(expectedNewSchemaIds);
		assertNotNull(resutls);
		assertEquals(newSchema, resutls);
	}
	
	@Test
	public void testGetColumnsInOrder() {
		Lists.reverse(expectedNewSchemaIds);
		//Call under test
		List<ColumnModel> resutls = columnModelManager.getAndValidateColumnModels(expectedNewSchemaIds);
		assertNotNull(resutls);
		// expected should also be in reverse order
		Lists.reverse(newSchema);
		assertEquals(newSchema, resutls);
	}
	
	@Test
	public void testGetColumnsDoesNotExist() {
		// add a column that does not exist
		expectedNewSchemaIds.add("9999");
		//Call under test
		try{
			columnModelManager.getAndValidateColumnModels(expectedNewSchemaIds);
			fail();
		}catch( NotFoundException e) {
			// expected
			assertEquals("Column does not exist for id: 9999", e.getMessage());
		}
	}
	
	@Test
	public void testGetColumnsDuplicateId() {
		// add a duplicate value to the list
		expectedNewSchemaIds.add(expectedNewSchemaIds.get(1));
		//Call under test
		try{
			columnModelManager.getAndValidateColumnModels(expectedNewSchemaIds);
			fail();
		}catch( IllegalArgumentException e) {
			// expected
			assertEquals("Duplicate column: 'col_333'", e.getMessage());
		}
	}
	
	@Test
	public void testGetColumnsDuplicateName() {
		// Setup duplicate names for two of the columns
		ColumnModel zero = newSchema.get(0);
		ColumnModel one = newSchema.get(1);
		one.setName(zero.getName());
		//Call under test
		try{
			columnModelManager.getAndValidateColumnModels(expectedNewSchemaIds);
			fail();
		}catch( IllegalArgumentException e) {
			// expected
			assertEquals("Duplicate column name: 'col_444'", e.getMessage());
		}
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
	public void testBindColumnToObject() throws DatastoreException, NotFoundException{
		String objectId = "syn123";
		IdAndVersion expectedIdAndVersion = IdAndVersion.parse(objectId);
		// call under test
		List<ColumnModel> results = columnModelManager.bindColumnsToDefaultVersionOfObject(expectedNewSchemaIds, objectId);
		assertEquals(newSchema, results);
		verify(mockColumnModelDAO).bindColumnToObject(newSchema, expectedIdAndVersion);
	}
	
	/**
	 * Test for PLFM-2636
	 */
	@Test
	public void testBindColumnToObjectEmptyList() {
		String objectId = "syn123";
		IdAndVersion expectedIdAndVersion = IdAndVersion.parse(objectId);
		List<String> columnIds = new LinkedList<>();
		// call under test
		List<ColumnModel> results = columnModelManager.bindColumnsToDefaultVersionOfObject(columnIds, objectId);
		// should be an empty list
		assertEquals(new LinkedList<>(), results);
		verify(mockColumnModelDAO).bindColumnToObject(Collections.emptyList(), expectedIdAndVersion);
	}
	
	/**
	 * Test for PLFM-2636
	 */
	@Test
	public void testBindColumnToObjectNull() {
		String objectId = "syn123";
		IdAndVersion expectedIdAndVersion = IdAndVersion.parse(objectId);
		List<String> columnIds = null;
		// call under test
		List<ColumnModel> results = columnModelManager.bindColumnsToDefaultVersionOfObject(columnIds, objectId);
		// should be an empty list
		assertEquals(new LinkedList<>(), results);
		verify(mockColumnModelDAO).bindColumnToObject(Collections.emptyList(), expectedIdAndVersion);
	}
	
	/**
	 * Test for PLFM-3113.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test
	public void testBindColumnToObjectDuplicateId() throws DatastoreException, NotFoundException{
		// bind a duplicate
		expectedNewSchemaIds.add(expectedNewSchemaIds.get(1));
		String objectId = "syn123";
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.bindColumnsToDefaultVersionOfObject(expectedNewSchemaIds, objectId);
		});
	}
	
	@Test
	public void testBindColumnsToObjectDuplicateName() {
		// Setup duplicate names for two of the columns
		ColumnModel zero = newSchema.get(0);
		ColumnModel one = newSchema.get(1);
		one.setName(zero.getName());
		String objectId = "syn123";
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.bindColumnsToDefaultVersionOfObject(expectedNewSchemaIds, objectId);
		});
	}
	
	/**
	 * Test for PLFM-3113.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test
	public void testBindColumnToObjectDoesNotExist() throws DatastoreException, NotFoundException{
		// bind a column that does not exist
		expectedNewSchemaIds.add("9999");
		String objectId = "syn123";
		// call under test
		assertThrows(NotFoundException.class, () -> {
			columnModelManager.bindColumnsToDefaultVersionOfObject(expectedNewSchemaIds, objectId);
		});
	}	
	
	@Test
	public void testBindDefaultColumnsToObjectVersion() {
		IdAndVersion targetVersion = IdAndVersion.parse("syn123.5");
		IdAndVersion defaultVersion = IdAndVersion.parse("syn123");
		when(mockColumnModelDAO.getColumnModelIdsForObject(defaultVersion)).thenReturn(expectedNewSchemaIds);
		// call under test
		List<ColumnModel> results =columnModelManager.bindCurrentColumnsToVersion(targetVersion);
		assertEquals(newSchema, results);
		verify(mockColumnModelDAO).bindColumnToObject(newSchema, targetVersion);
	}
	
	@Test
	public void testBindColumnsToVersionOfObject() {
		// call under test
		List<ColumnModel> results =columnModelManager.bindColumnsToVersionOfObject(expectedNewSchemaIds, idAndVersion);
		assertEquals(newSchema, results);
		verify(mockColumnModelDAO).bindColumnToObject(newSchema, idAndVersion);
	}
	
	@Test
	public void testUnbindColumnFromObject() throws DatastoreException, NotFoundException {
		String objectId = "syn123";
		columnModelManager.unbindAllColumnsAndOwnerFromObject(objectId);
		verify(mockColumnModelDAO).deleteOwner(objectId);
	}

	@Test
	public void testTruncateAllDataUnauthroized(){
		UserInfo user = new UserInfo(false);
		assertThrows(UnauthorizedException.class, () -> {
			columnModelManager.truncateAllColumnData(user);
		});
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
		IdAndVersion expectedIdAndVersion = IdAndVersion.parse(objectId);
		List<ColumnModel> expected = TableModelTestUtils.createOneOfEachType();
		when(mockauthorizationManager.canAccess(user, objectId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockColumnModelDAO.getColumnModelsForObject(expectedIdAndVersion)).thenReturn(expected);
		List<ColumnModel> resutls = columnModelManager.getColumnModelsForTable(user, objectId);
		assertEquals(expected, resutls);
	}
	
	@Test
	public void testGetColumnModelsForTableUnauthorized() throws DatastoreException, NotFoundException{
		String objectId = "syn123";
		IdAndVersion expectedIdAndVersion = IdAndVersion.parse(objectId);
		List<ColumnModel> expected = TableModelTestUtils.createOneOfEachType();
		when(mockauthorizationManager.canAccess(user, objectId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			columnModelManager.getColumnModelsForTable(user, objectId);
		});
	}
	
	@Test
	public void testValidateSchemaSizeUnderLimit(){
		List<String> scheamIds = Lists.newArrayList();
		List<ColumnModel> schema = Lists.newArrayList();
		// Currently the breaking point is 16 string columns of size 1000.
		for(int i=0; i<MAX_NUMBER_OF_MAX_STRINGS_PER_TABLE; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.STRING);
			cm.setMaximumSize(1000L);
			schema.add(cm);
			scheamIds.add(""+cm.getId());
		}
		when(mockColumnModelDAO.getColumnModels(scheamIds)).thenReturn(schema);
		
		columnModelManager.validateSchemaSize(scheamIds);
	}
	
	@Test
	public void testValidateSchemaSizeOverLimit(){
		List<String> scheamIds = Lists.newArrayList();
		List<ColumnModel> schema = Lists.newArrayList();
		// Currently the breaking point is 17 string columns of size 1000.
		for(int i=0; i<MAX_NUMBER_OF_MAX_STRINGS_PER_TABLE+1; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.STRING);
			cm.setMaximumSize(1000L);
			schema.add(cm);
			scheamIds.add(""+cm.getId());
		}
		when(mockColumnModelDAO.getColumnModels(scheamIds)).thenReturn(schema);

		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateSchemaSize(scheamIds);
		});
	}
	
	/**
	 * Test added for PLFM-5330 and PLFM-5457. The max number
	 * of large text columns is a percentage of the
	 * total memory available to machines.
	 */
	@Test
	public void testValidateLargeTextColumns() {
		List<String> scheamIds = Lists.newArrayList();
		List<ColumnModel> schema = Lists.newArrayList();
		for(int i=0; i<ColumnConstants.MAX_NUMBER_OF_LARGE_TEXT_COLUMNS_PER_TABLE; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.LARGETEXT);
			schema.add(cm);
			scheamIds.add(""+cm.getId());
		}
		when(mockColumnModelDAO.getColumnModels(scheamIds)).thenReturn(schema);
		
		columnModelManager.validateSchemaSize(scheamIds);
	}
	
	@Test
	public void testValidateLargeTextColumnsOverLimit() {
		List<String> scheamIds = Lists.newArrayList();
		List<ColumnModel> schema = Lists.newArrayList();
		for(int i=0; i<MAX_NUMBER_OF_LARGE_TEXT_COLUMNS_PER_TABLE+1; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.LARGETEXT);
			schema.add(cm);
			scheamIds.add(""+cm.getId());
		}
		when(mockColumnModelDAO.getColumnModels(scheamIds)).thenReturn(schema);
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateSchemaSize(scheamIds);
		});
	}
	
	@Test
	public void testValidateSchemaTooManyColumns(){
		List<String> scheamIds = Lists.newArrayList();
		List<ColumnModel> schema = Lists.newArrayList();
		int numberOfColumns = MY_SQL_MAX_COLUMNS_PER_TABLE+1;
		for(int i=0; i<numberOfColumns; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.BOOLEAN);
			cm.setMaximumSize(1L);
			schema.add(cm);
			scheamIds.add(""+cm.getId());
		}
		lenient().when(mockColumnModelDAO.getColumnModels(scheamIds)).thenReturn(schema);
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateSchemaSize(scheamIds);
		});
	}
	
	@Test
	public void testValidateSchemaMaxColumns(){
		List<String> scheamIds = Lists.newArrayList();
		List<ColumnModel> schema = Lists.newArrayList();
		int numberOfColumns = MY_SQL_MAX_COLUMNS_PER_TABLE;
		for(int i=0; i<numberOfColumns; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.BOOLEAN);
			cm.setMaximumSize(1L);
			schema.add(cm);
			scheamIds.add(""+cm.getId());
		}
		when(mockColumnModelDAO.getColumnModels(scheamIds)).thenReturn(schema);
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
		when(mockColumnModelDAO.getColumnModels(underLimitSchemaIds)).thenReturn(underLimitSchema);
		//call under test
		columnModelManager.bindColumnsToDefaultVersionOfObject(underLimitSchemaIds, objectId);
	}
	
	/**
	 * See PLFM-3619.  This schema should be just over the limit.
	 */
	@Test
	public void testOverDataPerColumnLimit(){
		String objectId = "syn123";
		when(mockColumnModelDAO.getColumnModels(overLimitSchemaIds)).thenReturn(overLimitSchema);
		//call under test
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.bindColumnsToDefaultVersionOfObject(overLimitSchemaIds, objectId);
		});
	}
	
	/**
	 * Check the max columns per table.
	 */
	@Test
	public void testOverMaxColumnsPerTable(){
		String objectId = "syn123";
		List<String> scheamIds = Lists.newArrayList();
		List<ColumnModel> schema = Lists.newArrayList();
		int numberOfColumns = MY_SQL_MAX_COLUMNS_PER_TABLE+1;
		for(int i=0; i<numberOfColumns; i++){
			ColumnModel cm = TableModelTestUtils.createColumn((long)i, "c"+i, ColumnType.BOOLEAN);
			cm.setMaximumSize(1000L);
			schema.add(cm);
			scheamIds.add(""+cm.getId());
		}
		lenient().when(mockColumnModelDAO.getColumnModels(scheamIds)).thenReturn(schema);
		try {
			//call under test
			columnModelManager.bindColumnsToDefaultVersionOfObject(scheamIds, objectId);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().startsWith("Too many columns"));
		}
	}
	
	@Test
	public void testGetColumnChangeDetails(){
		List<ColumnChange> changes = TableModelTestUtils.createAddUpdateDeleteColumnChange();
		List<ColumnModel> columns = TableModelTestUtils.createColumnsForChanges(changes);
		
		when(mockColumnModelDAO.getColumnModels(anyListOf(String.class))).thenReturn(columns);
		
		List<ColumnChangeDetails> expected = Lists.newArrayList(
				new ColumnChangeDetails(null, columns.get(0)),
				new ColumnChangeDetails(columns.get(1), columns.get(2)),
				new ColumnChangeDetails(columns.get(3), null));
		
		// Call under test
		List<ColumnChangeDetails> results = columnModelManager.getColumnChangeDetails(changes);
		assertEquals(expected, results);
	}
	
	/**
	 * For PLFM-4904 ColumnModelManager.getColumnChangeDetails() we throwing a 
	 * 'Duplicate column name' exception if two columns have the same name.
	 */
	@Test
	public void testPLFM_4904() {
		ColumnModel oldCol = new ColumnModel();
		oldCol.setName("foo");
		oldCol.setId("111");
		ColumnModel newCol = new ColumnModel();
		newCol.setName("foo");
		newCol.setId("222");
		
		List<String> colIds = Lists.newArrayList(newCol.getId(), oldCol.getId());
 
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(oldCol.getId());
		change.setNewColumnId(newCol.getId());
		List<ColumnChange> changes = Lists.newArrayList(change);
		
		// return both columns
		when(mockColumnModelDAO.getColumnModels(colIds)).thenReturn(Lists.newArrayList(oldCol, newCol));
		
		// Call under test
		List<ColumnChangeDetails> results = columnModelManager.getColumnChangeDetails(changes);
		assertNotNull(results);
		assertEquals(1, results.size());
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

	@Test
	public void testCalculateNewSchemaIdsAndValidateMissingInUserProvidedColumns(){
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.calculateNewSchemaIdsAndValidate(tableId, changes, new LinkedList<String>());
		});
	}

	@Test
	public void testCalculateNewSchemaIdsAndValidateMissingFromUserProvidedColumns(){
		expectedNewSchemaIds.add("columnId that does not exist");
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.calculateNewSchemaIdsAndValidate(tableId, changes, expectedNewSchemaIds);
		});
	}
	
	@Test
	public void testCalculateNewSchemaIdsAndValidatePLFM_4188(){
		changes = TableModelTestUtils.createAllDeleteColumnChange();
		assertEquals(new LinkedList<String>(),
				columnModelManager.calculateNewSchemaIdsAndValidate(tableId, changes, new LinkedList<String>()));
	}
	
	@Test
	public void testCalculateNewSchemaIdsAndValidateOverSizeLimit(){
		// setup the current schema as empty
		when(mockColumnModelDAO.getColumnModelsForObject(idAndVersion)).thenReturn(new LinkedList<>());
		// setup the new schema to be over the limit.
		List<String> newSchemaIds = new LinkedList<>();
		List<ColumnChange> changes = new LinkedList<>();
		for(ColumnModel cm: overLimitSchema) {
			ColumnChange change = new ColumnChange();
			change.setOldColumnId(null);
			change.setNewColumnId(cm.getId());
			changes.add(change);
			newSchemaIds.add(cm.getId());
		}
		when(mockColumnModelDAO.getColumnModels(anyListOf(String.class))).thenReturn(overLimitSchema);
		try {
			// call under test.
			columnModelManager.calculateNewSchemaIdsAndValidate(tableId, changes, newSchemaIds);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Too much data per column"));
		}
	}
	
	@Test
	public void testCalculateNewSchemaIdsAndValidateMissingUpdate(){
		String tableId = "syn567";
		List<ColumnModel> currentSchema = Lists.newArrayList(
				TableModelTestUtils.createColumn(111L)
		);
		
		when(mockColumnModelDAO.getColumnModelsForObject(idAndVersion)).thenReturn(currentSchema);
		// update a column that does not exist.
		ColumnChange update = new ColumnChange();
		update.setOldColumnId("222");
		update.setNewColumnId("444");
		
		changes = Lists.newArrayList(update);
		// call under test.
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.calculateNewSchemaIdsAndValidate(tableId, changes, expectedNewSchemaIds);
		});
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
	
	@Test
	public void testIsFileHandleColumnNull(){
		ColumnModel column = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			ColumnModelManagerImpl.isFileHandleColumn(column);
		});
	}
	
	@Test
	public void testValidateColumnChangeFileToNonFile() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setColumnType(ColumnType.FILEHANDLEID);
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.BOOLEAN);
		// Call under test
		try {
			ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn, EntityType.table);
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
			ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn, EntityType.table);
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
		ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn, EntityType.table);
	}
	
	@Test
	public void testValidateColumnChangeRemoveFile() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setColumnType(ColumnType.FILEHANDLEID);
		ColumnModel newColumn = null;
		// Call under test
		ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn, EntityType.table);
	}
	
	@Test
	public void testValidateColumnChangeFileToFile() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setColumnType(ColumnType.FILEHANDLEID);
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.FILEHANDLEID);
		// Call under test
		ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn, EntityType.table);
	}

	@Test
	public void testValidateColumnChangeListColumnToDifferntListColumn() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setColumnType(ColumnType.STRING_LIST);
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.INTEGER_LIST);
		String errMessage = assertThrows(IllegalArgumentException.class, () ->
			// Call under test
			ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn, EntityType.table)
		).getMessage();
		assertEquals("Can not perform schema change on _LIST type columns for Table Entities", errMessage);
	}
	@Test
	public void testValidateColumnChangeListColumnToNonListColumn() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setColumnType(ColumnType.STRING_LIST);
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.STRING);
		String errMessage = assertThrows(IllegalArgumentException.class, () ->
				// Call under test
				ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn, EntityType.table)
		).getMessage();
		assertEquals("Can not perform schema change on _LIST type columns for Table Entities", errMessage);
	}

	@Test
	public void testValidateColumnChangeNonListColumnToListColumn() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setColumnType(ColumnType.INTEGER);
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.INTEGER_LIST);
		String errMessage = assertThrows(IllegalArgumentException.class, () ->
				// Call under test
				ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn, EntityType.table)
		).getMessage();
		assertEquals("Can not perform schema change on _LIST type columns for Table Entities", errMessage);
	}

	@Test
	public void testValidateColumnChangeListColumnToSameListColumn() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setColumnType(ColumnType.STRING_LIST);
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.STRING_LIST);
		// Call under test
		ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn, EntityType.table);
	}

	@Test
	public void testValidateColumnChangeNotTable() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setColumnType(ColumnType.INTEGER);
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.INTEGER_LIST);
		// Call under test
		ColumnModelManagerImpl.validateColumnChange(oldColumn, newColumn, EntityType.entityview);
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
	
	@Test
	public void testValidateFacetTypeBooleanColumnRangeFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.BOOLEAN);
		columnModel.setFacetType(FacetType.range);
		//should throw exception
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateFacetType(columnModel);
		});
	}
	
	@Test
	public void testValidateFacetTypeDoubleColumnEnumerationFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.DOUBLE);
		columnModel.setFacetType(FacetType.enumeration);
		//should throw exception
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateFacetType(columnModel);
		});
	}
	
	@Test 
	public void testValidateFacetTypeDoubleColumnRangeFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.DOUBLE);
		columnModel.setFacetType(FacetType.range);
		//should do nothing
		columnModelManager.validateFacetType(columnModel);
	}
	
	@Test
	public void testValidateFacetTypeDateColumnEnumerationFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.DATE);
		columnModel.setFacetType(FacetType.enumeration);
		//should throw exception
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateFacetType(columnModel);
		});
	}
	
	@Test 
	public void testValidateFacetTypeDateColumnRangeFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.DATE);
		columnModel.setFacetType(FacetType.range);
		//should do nothing
		columnModelManager.validateFacetType(columnModel);
	}

	@Test
	public void testValidateFacetType_StringListColumn(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.STRING_LIST);

		//should pass for enumeration facets
		columnModel.setFacetType(FacetType.enumeration);
		columnModelManager.validateFacetType(columnModel);

		//should throw exception for range facets
		columnModel.setFacetType(FacetType.range);
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateFacetType(columnModel);
		});
	}

	@Test
	public void testValidateFacetType_IntegerListColumn(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.INTEGER_LIST);
		//should pass for enumeration facets
		columnModel.setFacetType(FacetType.enumeration);
		columnModelManager.validateFacetType(columnModel);

		//should throw exception for range facets
		columnModel.setFacetType(FacetType.range);
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateFacetType(columnModel);
		});
	}

	@Test
	public void testValidateFacetType_BooleanListColumn(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.BOOLEAN_LIST);

		//should pass for enumeration facets
		columnModel.setFacetType(FacetType.enumeration);
		columnModelManager.validateFacetType(columnModel);

		//should throw exception for range facets
		columnModel.setFacetType(FacetType.range);
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateFacetType(columnModel);
		});
	}

	@Test
	public void testValidateFacetType_DateListColumn(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.DATE_LIST);

		//should pass for enumeration facets
		columnModel.setFacetType(FacetType.enumeration);
		columnModelManager.validateFacetType(columnModel);

		//should throw exception for range facets
		columnModel.setFacetType(FacetType.range);
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateFacetType(columnModel);
		});
	}

	@Test
	public void testValidateFacetType_EntityIdListColumn(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.ENTITYID_LIST);

		//should pass for enumeration facets
		columnModel.setFacetType(FacetType.enumeration);
		columnModelManager.validateFacetType(columnModel);

		//should throw exception for range facets
		columnModel.setFacetType(FacetType.range);
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateFacetType(columnModel);
		});
	}

	@Test
	public void testValidateFacetType_UserIdListColumn(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.USERID_LIST);

		//should pass for enumeration facets
		columnModel.setFacetType(FacetType.enumeration);
		columnModelManager.validateFacetType(columnModel);

		//should throw exception for range facets
		columnModel.setFacetType(FacetType.range);
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateFacetType(columnModel);
		});
	}
	
	@Test
	public void testValidateFacetTypeOtherColumnTypeEnumerationFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.LARGETEXT);
		columnModel.setFacetType(FacetType.enumeration);
		//should throw exception
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateFacetType(columnModel);
		});
	}
	
	@Test
	public void testValidateFacetTypeOtherColumnTypeRangeFacet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.LARGETEXT);
		columnModel.setFacetType(FacetType.range);
		//should throw exception
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateFacetType(columnModel);
		});
	}

	@Test
	public void testValidateSchemaWithProvidedOrderedColumnsWithNullSchema() {
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateSchemaWithProvidedOrderedColumns(null, new LinkedList<String>());
		});
	}

	@Test
	public void testValidateSchemaWithProvidedOrderedColumnsWithNullOrderedList() {
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateSchemaWithProvidedOrderedColumns(new LinkedList<String>(), null);
		});
	}

	@Test
	public void testValidateSchemaWithProvidedOrderedColumnsWithEmptyList() {
			columnModelManager.validateSchemaWithProvidedOrderedColumns(new LinkedList<String>(), new LinkedList<String>());
	}

	@Test
	public void testValidateSchemaWithProvidedOrderedColumnsWithMissingFromSchema() {
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateSchemaWithProvidedOrderedColumns(Arrays.asList("1"), Arrays.asList("1", "2"));
		});
	}

	@Test
	public void testValidateSchemaWithProvidedOrderedColumnsWithMissingFromOrderedList() {
		assertThrows(IllegalArgumentException.class, () -> {
			columnModelManager.validateSchemaWithProvidedOrderedColumns(Arrays.asList("1", "2"), Arrays.asList("2"));
		});
	}

	@Test
	public void testValidateSchemaWithProvidedOrderedColumnsWithDifferentOrder() {
		columnModelManager.validateSchemaWithProvidedOrderedColumns(Arrays.asList("1", "2"), Arrays.asList("2", "1"));
	}
	
	@Test
	public void testCheckColumnNaming() {
		String name = "foo";
		// call under test
		ColumnModelManagerImpl.checkColumnNaming(name);
	}
	
	@Test
	public void testCheckColumnNamingReserved() {
		String name = TableConstants.ROW_ID;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			ColumnModelManagerImpl.checkColumnNaming(name);
		});
	}
	
	/**
	 * PLFM-4329.
	 */
	@Test
	public void testCheckColumnNamingAtLimit() {
		String name = createStringOfSize(TableConstants.MAX_COLUMN_NAME_SIZE_CHARS);
		// call under test
		ColumnModelManagerImpl.checkColumnNaming(name);
	}
	
	/**
	 * PLFM-4329.
	 */
	@Test
	public void testCheckColumnNamingOverLimit() {
		String name = createStringOfSize(TableConstants.MAX_COLUMN_NAME_SIZE_CHARS+1);
		// call under test
		try {
			ColumnModelManagerImpl.checkColumnNaming(name);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Column name must be: 256 characters or less.", e.getMessage());
		}
	}
	
	@Test
	public void testGetColumnModelsForObjectWithoutVersion() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		when(mockColumnModelDAO.getColumnModelsForObject(idAndVersion)).thenReturn(currentSchema);
		// call under test
		List<ColumnModel> results = columnModelManager.getColumnModelsForObject(idAndVersion);
		assertEquals(currentSchema, results);
		verify(mockColumnModelDAO).getColumnModelsForObject(idAndVersion);
		verify(mockNodeDao, never()).getCurrentRevisionNumber(any(String.class));
	}
	
	@Test
	public void testGetColumnModelsForObjectWithVersionNotCurrent() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123.45");
		// the current version does not match the passed version
		when(mockNodeDao.getCurrentRevisionNumber("123")).thenReturn(46L);
		when(mockColumnModelDAO.getColumnModelsForObject(idAndVersion)).thenReturn(currentSchema);
		// call under test
		List<ColumnModel> results = columnModelManager.getColumnModelsForObject(idAndVersion);
		assertEquals(currentSchema, results);
		verify(mockColumnModelDAO).getColumnModelsForObject(idAndVersion);
		verify(mockNodeDao).getCurrentRevisionNumber("123");
	}
	
	/**
	 * When getting the schema for the current version the version number passed to the dao is null.
	 */
	@Test
	public void testGetColumnModelsForObjectWithVersionCurrent() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123.45");
		// the current version matches the passed version.
		when(mockNodeDao.getCurrentRevisionNumber("123")).thenReturn(45L);
		IdAndVersion expectedIdAndVersion = IdAndVersion.parse("syn123");
		when(mockColumnModelDAO.getColumnModelsForObject(expectedIdAndVersion)).thenReturn(currentSchema);
		// call under test
		List<ColumnModel> results = columnModelManager.getColumnModelsForObject(idAndVersion);
		assertEquals(currentSchema, results);
		verify(mockColumnModelDAO).getColumnModelsForObject(expectedIdAndVersion);
		verify(mockNodeDao).getCurrentRevisionNumber("123");
	}
	
	@Test
	public void testGetColumnIdsForTableWithoutVersion() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		when(mockColumnModelDAO.getColumnModelIdsForObject(idAndVersion)).thenReturn(expectedNewSchemaIds);
		// call under test
		List<String> results = columnModelManager.getColumnIdsForTable(idAndVersion);
		assertEquals(expectedNewSchemaIds, results);
		verify(mockColumnModelDAO).getColumnModelIdsForObject(idAndVersion);
		verify(mockNodeDao, never()).getCurrentRevisionNumber(any(String.class));
	}
	
	@Test
	public void testGetGetColumnIdsForTableWithVersionNotCurrent() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123.45");
		// the current version does not match the passed version
		when(mockNodeDao.getCurrentRevisionNumber("123")).thenReturn(46L);
		when(mockColumnModelDAO.getColumnModelIdsForObject(idAndVersion)).thenReturn(expectedNewSchemaIds);
		// call under test
		List<String> results = columnModelManager.getColumnIdsForTable(idAndVersion);
		assertEquals(expectedNewSchemaIds, results);
		verify(mockColumnModelDAO).getColumnModelIdsForObject(idAndVersion);
		verify(mockNodeDao).getCurrentRevisionNumber("123");
	}
	
	/**
	 * When getting the schema for the current version the version number passed to the dao is null.
	 */
	@Test
	public void testGetGetColumnIdsForTableWithVersionCurrent() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123.45");
		// the current version matches the passed version.
		when(mockNodeDao.getCurrentRevisionNumber("123")).thenReturn(45L);
		IdAndVersion expectedIdAndVersion = IdAndVersion.parse("syn123");
		when(mockColumnModelDAO.getColumnModelIdsForObject(expectedIdAndVersion)).thenReturn(expectedNewSchemaIds);
		// call under test
		List<String> results = columnModelManager.getColumnIdsForTable(idAndVersion);
		assertEquals(expectedNewSchemaIds, results);
		verify(mockColumnModelDAO).getColumnModelIdsForObject(expectedIdAndVersion);
		verify(mockNodeDao).getCurrentRevisionNumber("123");
	}
	
	/**
	 * Create a string with the given number of characters.
	 * 
	 * @param numberChars
	 * @return
	 */
	public static String createStringOfSize(int numberChars) {
		char[] chars = new char[numberChars];
		for(int i=0; i<numberChars; i++) {
			chars[i] = (char) i;
		}
		return new String(chars);
	}
	
}