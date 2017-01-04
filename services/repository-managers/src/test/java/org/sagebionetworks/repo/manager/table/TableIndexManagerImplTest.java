package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnChangeDetails;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.table.cluster.DatabaseColumnInfo;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.model.SparseRow;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TableIndexManagerImplTest {
	
	@Mock
	TableIndexDAO mockIndexDao;
	@Mock
	TransactionStatus mockTransactionStatus;
	@Mock
	TableManagerSupport mockManagerSupport;
	@Mock
	ProgressCallback<Void> mockCallback;
	
	TableIndexManagerImpl manager;
	String tableId;
	Long versionNumber;
	SparseChangeSet sparseChangeSet;
	List<ColumnModel> schema;
	List<SelectColumn> selectColumns;
	Long crc32;
	
	Grouping groupOne;
	Grouping groupTwo;
	
	HashSet<Long> containerIds;
	Long limit;
	Long offset;
	Long count;
	List<String> scopeSynIds;
	Set<Long> scopeIds;
	
	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception{
		MockitoAnnotations.initMocks(this);
		tableId = "syn123";
		manager = new TableIndexManagerImpl(mockIndexDao, mockManagerSupport, tableId);
		
		versionNumber = 99L;		
		schema = Arrays.asList(
				TableModelTestUtils.createColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createColumn(101L, "aFile", ColumnType.FILEHANDLEID)
				);
		
		selectColumns = Arrays.asList(
				TableModelTestUtils.createSelectColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createSelectColumn(101L, "aFile", ColumnType.FILEHANDLEID)
				);
			
		sparseChangeSet = new SparseChangeSet(tableId, schema);
		SparseRow row = sparseChangeSet.addEmptyRow();
		row.setRowId(0L);
		row.setCellValue("99", "some string");
		
		row = sparseChangeSet.addEmptyRow();
		row.setRowId(1l);
		row.setCellValue("101", "2");
		row = sparseChangeSet.addEmptyRow();
		row.setRowId(2l);
		row.setCellValue("101", "6");
		
		Iterator<Grouping> it = sparseChangeSet.groupByValidValues().iterator();
		groupOne = it.next();
		groupTwo = it.next();
		
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		
		// When a write transaction callback is used, we need to call the callback.
		doAnswer(new Answer<Void>(){

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				TransactionCallback callback = (TransactionCallback) invocation.getArguments()[0];
				callback.doInTransaction(mockTransactionStatus);
				return null;
			}}).when(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		
		// setup callable.
		when(mockManagerSupport.callWithAutoProgress(any(ProgressCallback.class), any(Callable.class))).then(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Callable<Object> callable = (Callable<Object>) invocation.getArguments()[1];
				return callable.call();
			}
		});
		crc32 = 5678L;
		when(mockIndexDao.calculateCRC32ofTableView(anyString(), anyString())).thenReturn(crc32);
		
		containerIds = Sets.newHashSet(1l,2L,3L);
		limit = 10L;
		offset = 0L;
		count = 101L;
		scopeSynIds = Lists.newArrayList("syn123","syn345");
		scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scopeSynIds));
		
		when(mockIndexDao.getPossibleAnnotationsForContainers(anySet(), anyLong(), anyLong())).thenReturn(schema);
		when(mockIndexDao.getPossibleAnnotationsForContainersCount(anySet())).thenReturn(count);
		when(mockManagerSupport.getAllContainerIdsForViewScope(tableId)).thenReturn(containerIds);
		when(mockManagerSupport.getAllContainerIdsForScope(scopeIds)).thenReturn(containerIds);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testNullDao(){
		new TableIndexManagerImpl(null, mockManagerSupport, tableId);	
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullTableId(){
		new TableIndexManagerImpl(mockIndexDao, mockManagerSupport, null);			
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullSupport(){
		new TableIndexManagerImpl(mockIndexDao, null, tableId);			
	}
	
	@Test
	public void testApplyChangeSetToIndexHappy(){
		//call under test.
		manager.applyChangeSetToIndex(sparseChangeSet, versionNumber);
		// All changes should be executed in a transaction
		verify(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		// both groups should be written
		verify(mockIndexDao).createOrUpdateOrDeleteRows(groupOne);
		verify(mockIndexDao).createOrUpdateOrDeleteRows(groupTwo);
		// files handles should be applied.
		verify(mockIndexDao).applyFileHandleIdsToTable(tableId, Sets.newHashSet(2L, 6L));
		// The new version should be set
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, versionNumber);
	}
	
	@Test
	public void testApplyChangeSetToIndexAlreadyApplied(){
		// For this case the index already has this change set applied.
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber+1);
		//call under test.
		manager.applyChangeSetToIndex(sparseChangeSet, versionNumber);
		// nothing do do.
		verify(mockIndexDao, never()).executeInWriteTransaction(any(TransactionCallback.class));
		verify(mockIndexDao, never()).createOrUpdateOrDeleteRows(any(Grouping.class));
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(anyString(), anySet());
		verify(mockIndexDao, never()).setMaxCurrentCompleteVersionForTable(anyString(), anyLong());
	}
	
	@Test
	public void testApplyChangeSetToIndexNoFiles(){
		// no files in the schema
		schema = Arrays.asList(
				TableModelTestUtils.createColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createColumn(101L, "moreStrings", ColumnType.STRING)
				);
		selectColumns = Arrays.asList(
				TableModelTestUtils.createSelectColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createSelectColumn(101L, "moreStrings", ColumnType.STRING)
				);
		sparseChangeSet = new SparseChangeSet(tableId, schema);
		SparseRow row = sparseChangeSet.addEmptyRow();
		row.setRowId(0L);
		row.setCellValue("99", "some string");
		
		//call under test.
		manager.applyChangeSetToIndex(sparseChangeSet, versionNumber);
		// All changes should be executed in a transaction
		verify(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		// change should be written to the index
		verify(mockIndexDao).createOrUpdateOrDeleteRows(groupOne);
		// there are no files
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(anyString(), anySet());
		// The new version should be set
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, versionNumber);
	}
	
	@Test
	public void testSetIndexSchemaWithColumns(){
		ColumnModel column = new ColumnModel();
		column.setId("44");
		column.setColumnType(ColumnType.BOOLEAN);
		schema = Lists.newArrayList(column);
		
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C44_");
		info.setColumnType(ColumnType.BOOLEAN);
		
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(Lists.newArrayList(info));
		when(mockIndexDao.alterTableAsNeeded(anyString(), anyList(), anyBoolean())).thenReturn(true);
		// call under test
		manager.setIndexSchema(mockCallback, schema);
		String schemaMD5Hex = TableModelUtils. createSchemaMD5HexCM(Lists.newArrayList(schema));
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}
	
	@Test
	public void testSetIndexSchemaWithNoColumns(){
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(new LinkedList<DatabaseColumnInfo>());
		when(mockIndexDao.alterTableAsNeeded(anyString(), anyList(), anyBoolean())).thenReturn(true);
		// call under test
		manager.setIndexSchema(mockCallback, new LinkedList<ColumnModel>());
		String schemaMD5Hex = TableModelUtils. createSchemaMD5HexCM(Lists.newArrayList(new LinkedList<ColumnModel>()));
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}

	
	@Test
	public void testIsVersionAppliedToIndexNoVersionApplied(){
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		versionNumber = 1L;
		assertFalse(manager.isVersionAppliedToIndex(versionNumber));
	}
	
	@Test
	public void testIsVersionAppliedToIndexVersionMatches(){
		versionNumber = 1L;
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber);
		assertTrue(manager.isVersionAppliedToIndex(versionNumber));
	}
	
	@Test
	public void testIsVersionAppliedToIndexVersionGreater(){
		versionNumber = 1L;
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber+1);
		assertTrue(manager.isVersionAppliedToIndex(versionNumber));
	}
	
	@Test
	public void testDeleteTableIndex(){
		manager.deleteTableIndex();
		verify(mockIndexDao).deleteSecondayTables(tableId);
		verify(mockIndexDao).deleteTable(tableId);
	}
	
	@Test
	public void testOptimizeTableIndices(){
		List<DatabaseColumnInfo> infoList = new LinkedList<DatabaseColumnInfo>();
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(infoList);
		// call under test
		manager.optimizeTableIndices();
		// column data must be gathered.
		verify(mockIndexDao).getDatabaseInfo(tableId);
		verify(mockIndexDao).provideCardinality(infoList, tableId);
		verify(mockIndexDao).provideIndexName(infoList, tableId);
		// optimization called.
		verify(mockIndexDao).optimizeTableIndices(infoList, tableId, 63);
	}
	
	@Test
	public void testUpdateTableSchemaAddColumn(){
		ColumnModel oldColumn = null;
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("12");
		List<ColumnChangeDetails> changes = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
		boolean alterTemp = false;
		when(mockIndexDao.alterTableAsNeeded(tableId, changes, alterTemp)).thenReturn(true);
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C12_");
		info.setColumnType(ColumnType.BOOLEAN);
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(Lists.newArrayList(info));
		
		// call under test
		manager.updateTableSchema(mockCallback, changes);
		verify(mockIndexDao).createTableIfDoesNotExist(tableId);
		verify(mockIndexDao).createSecondaryTables(tableId);
		// The new schema is not empty so do not truncate.
		verify(mockIndexDao, never()).truncateTable(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, changes, alterTemp);
		
		String schemaMD5Hex = TableModelUtils. createSchemaMD5HexCM(Lists.newArrayList(newColumn));
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}
	
	@Test
	public void testUpdateTableSchemaRemoveAllColumns(){
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("12");
		ColumnModel newColumn = null;
		boolean alterTemp = false;

		List<ColumnChangeDetails> changes = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
		when(mockIndexDao.alterTableAsNeeded(tableId, changes, alterTemp)).thenReturn(true);
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(new LinkedList<DatabaseColumnInfo>());
		// call under test
		manager.updateTableSchema(mockCallback, changes);
		verify(mockIndexDao).createTableIfDoesNotExist(tableId);
		verify(mockIndexDao).createSecondaryTables(tableId);
		verify(mockIndexDao).getDatabaseInfo(tableId);
		// The new schema is empty so the table is truncated.
		verify(mockIndexDao).truncateTable(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, changes, alterTemp);
		
		String schemaMD5Hex = TableModelUtils. createSchemaMD5HexCM(new LinkedList<ColumnModel>());
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}
	
	@Test
	public void testUpdateTableSchemaNoChange(){
		List<ColumnChangeDetails> changes = new LinkedList<ColumnChangeDetails>();
		boolean alterTemp = false;
		when(mockIndexDao.alterTableAsNeeded(tableId, changes, alterTemp)).thenReturn(false);
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(new LinkedList<DatabaseColumnInfo>());
		// call under test
		manager.updateTableSchema(mockCallback, changes);
		verify(mockIndexDao).createTableIfDoesNotExist(tableId);
		verify(mockIndexDao).createSecondaryTables(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, changes, alterTemp);
		verify(mockIndexDao, never()).getDatabaseInfo(tableId);
		verify(mockIndexDao, never()).truncateTable(tableId);
		verify(mockIndexDao, never()).setCurrentSchemaMD5Hex(anyString(), anyString());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateTemporaryTableCopy() throws Exception{
		// call under test
		manager.createTemporaryTableCopy(mockCallback);
		// auto progress should be used.
		verify(mockManagerSupport).callWithAutoProgress(any(ProgressCallback.class), any(Callable.class));
		verify(mockIndexDao).createTemporaryTable(tableId);
		verify(mockIndexDao).copyAllDataToTemporaryTable(tableId);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDeleteTemporaryTableCopy() throws Exception{
		// call under test
		manager.deleteTemporaryTableCopy(mockCallback);
		// auto progress should be used.
		verify(mockManagerSupport).callWithAutoProgress(any(ProgressCallback.class), any(Callable.class));
		verify(mockIndexDao).deleteTemporaryTable(tableId);
	}
	
	@Test
	public void testPopulateViewFromEntityReplication(){
		ViewType viewType = ViewType.file;
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		ColumnModel etagColumn = EntityField.findMatch(schema, EntityField.etag);
		// call under test
		Long resultCrc = manager.populateViewFromEntityReplication(mockCallback, viewType, scope, schema);
		assertEquals(crc32, resultCrc);
		verify(mockIndexDao).copyEntityReplicationToTable(tableId, viewType, scope, schema);
		// the CRC should be calculated with the etag column.
		verify(mockIndexDao).calculateCRC32ofTableView(tableId, etagColumn.getId());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPopulateViewFromEntityReplicationMissingEtagColumn(){
		ViewType viewType = ViewType.file;
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		ColumnModel etagColumn = EntityField.findMatch(schema, EntityField.etag);
		// remove the etag column
		schema.remove(etagColumn);
		// call under test
		manager.populateViewFromEntityReplication(mockCallback, viewType, scope, schema);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPopulateViewFromEntityReplicationMissingBenefactorColumn(){
		ViewType viewType = ViewType.file;
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		ColumnModel benefactorColumn = EntityField.findMatch(schema, EntityField.benefactorId);
		// remove the benefactor column
		schema.remove(benefactorColumn);
		// call under test
		manager.populateViewFromEntityReplication(mockCallback, viewType, scope, schema);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPopulateViewFromEntityReplicationNullViewType(){
		ViewType viewType = null;
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		// call under test
		manager.populateViewFromEntityReplication(mockCallback, viewType, scope, schema);;
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPopulateViewFromEntityReplicationScopeNull(){
		ViewType viewType = ViewType.file;
		Set<Long> scope = null;
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		// call under test
		manager.populateViewFromEntityReplication(mockCallback, viewType, scope, schema);;
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPopulateViewFromEntityReplicationSchemaNull(){
		ViewType viewType = ViewType.file;
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = null;
		// call under test
		manager.populateViewFromEntityReplication(mockCallback, viewType, scope, schema);;
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPopulateViewFromEntityReplicationCallbackNull(){
		ViewType viewType = ViewType.file;
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		mockCallback = null;
		// call under test
		manager.populateViewFromEntityReplication(mockCallback, viewType, scope, schema);;
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerIs(){
		// call under test
		PaginatedResults<ColumnModel> results = manager.getPossibleAnnotationDefinitionsForContainerIs(containerIds, limit, offset);
		assertNotNull(results);
		assertEquals(count.longValue(), results.getTotalNumberOfResults());
		assertEquals(schema, results.getResults());
		
		verify(mockIndexDao).getPossibleAnnotationsForContainers(containerIds, limit, offset);
		verify(mockIndexDao).getPossibleAnnotationsForContainersCount(containerIds);
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerIsNullOffset(){
		offset = null;
		// call under test
		PaginatedResults<ColumnModel> results = manager.getPossibleAnnotationDefinitionsForContainerIs(containerIds, limit, offset);
		assertNotNull(results);
		// offset should default to zero
		verify(mockIndexDao).getPossibleAnnotationsForContainers(containerIds, limit, 0L);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetPossibleAnnotationDefinitionsForContainerIsNullLimit(){
		limit = null;
		// call under test
		manager.getPossibleAnnotationDefinitionsForContainerIs(containerIds, limit, offset);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetPossibleAnnotationDefinitionsForContainerIsNullContainerIds(){
		containerIds = null;
		// call under test
		manager.getPossibleAnnotationDefinitionsForContainerIs(containerIds, limit, offset);
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerIsEmpty(){
		containerIds = new HashSet<>();
		// call under test
		PaginatedResults<ColumnModel> results = manager.getPossibleAnnotationDefinitionsForContainerIs(containerIds, limit, offset);
		assertNotNull(results);
		assertNotNull(results.getResults());
		assertEquals(0L, results.getTotalNumberOfResults());
		// should not call the dao
		verify(mockIndexDao, never()).getPossibleAnnotationsForContainers(anySet(), anyLong(), anyLong());
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetPossibleAnnotationDefinitionsForContainerIsOverLimit(){
		limit = TableIndexManagerImpl.MAX_LIMIT+1;
		// call under test
		manager.getPossibleAnnotationDefinitionsForContainerIs(containerIds, limit, offset);
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForView(){
		// call under test
		PaginatedResults<ColumnModel> results = manager.getPossibleAnnotationDefinitionsForView(tableId, limit, offset);
		assertNotNull(results);
		assertEquals(count.longValue(), results.getTotalNumberOfResults());
		assertEquals(schema, results.getResults());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetPossibleAnnotationDefinitionsForViewNullId(){
		tableId = null;
		// call under test
		manager.getPossibleAnnotationDefinitionsForView(tableId, limit, offset);
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForScope(){
		// call under test
		PaginatedResults<ColumnModel> results = manager.getPossibleAnnotationDefinitionsForScope(scopeSynIds, limit, offset);
		assertNotNull(results);
		assertEquals(count.longValue(), results.getTotalNumberOfResults());
		assertEquals(schema, results.getResults());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetPossibleAnnotationDefinitionsForScopeNullScope(){
		scopeSynIds = null;
		// call under test
		manager.getPossibleAnnotationDefinitionsForScope(scopeSynIds, limit, offset);
	}
	
	
	/**
	 * Create the default EntityField schema with IDs for each column.
	 * 
	 * @return
	 */
	public static List<ColumnModel> createDefaultColumnsWithIds(){
		List<ColumnModel> schema = EntityField.getAllColumnModels();
		for(int i=0; i<schema.size(); i++){
			ColumnModel cm = schema.get(i);
			cm.setId(""+i);
		}
		return schema;
	}
	
}
