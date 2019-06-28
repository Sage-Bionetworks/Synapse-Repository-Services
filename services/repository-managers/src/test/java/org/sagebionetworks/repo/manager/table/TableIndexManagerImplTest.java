package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.DatabaseColumnInfo;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.ChangeData;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.model.SchemaChange;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.model.SparseRow;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
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
	ProgressCallback mockCallback;
	@Captor 
	ArgumentCaptor<List<ColumnChangeDetails>> changeCaptor;
	
	TableIndexManagerImpl manager;
	IdAndVersion tableId;
	Long versionNumber;
	SparseChangeSet sparseChangeSet;
	List<ColumnModel> schema;
	String schemaMD5Hex;
	List<SelectColumn> selectColumns;
	Long crc32;
	
	Grouping groupOne;
	Grouping groupTwo;
	
	HashSet<Long> containerIds;
	Long limit;
	Long offset;
	NextPageToken nextPageToken;
	String tokenString;
	List<String> scopeSynIds;
	Set<Long> scopeIds;
	ViewScope scope;
	
	Long viewType;
	ColumnModel newColumn;
	List<ColumnChangeDetails> columnChanges;
	
	
	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception{
		MockitoAnnotations.initMocks(this);
		tableId = IdAndVersion.parse("syn123");
		manager = new TableIndexManagerImpl(mockIndexDao, mockManagerSupport);
		
		versionNumber = 99L;		
		schema = Arrays.asList(
				TableModelTestUtils.createColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createColumn(101L, "aFile", ColumnType.FILEHANDLEID)
				);
		schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(TableModelUtils.getIds(schema));
		selectColumns = Arrays.asList(
				TableModelTestUtils.createSelectColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createSelectColumn(101L, "aFile", ColumnType.FILEHANDLEID)
				);
		
		sparseChangeSet = new SparseChangeSet(tableId.toString(), schema);
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
		
		crc32 = 5678L;
		when(mockIndexDao.calculateCRC32ofTableView(any(Long.class))).thenReturn(crc32);
		
		containerIds = Sets.newHashSet(1l,2L,3L);
		limit = 10L;
		offset = 0L;
		nextPageToken = new NextPageToken(limit, offset);
		tokenString = nextPageToken.toToken();
		scopeSynIds = Lists.newArrayList("syn123","syn345");
		scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scopeSynIds));
		viewType = ViewTypeMask.File.getMask();
		when(mockManagerSupport.getViewTypeMask(tableId)).thenReturn(viewType);
		when(mockIndexDao.getPossibleColumnModelsForContainers(anySet(), any(Long.class), anyLong(), anyLong())).thenReturn(schema);
		when(mockManagerSupport.getAllContainerIdsForViewScope(tableId, viewType)).thenReturn(containerIds);
		when(mockManagerSupport.getAllContainerIdsForScope(scopeIds, viewType)).thenReturn(containerIds);
		scope = new ViewScope();
		scope.setScope(scopeSynIds);
		scope.setViewTypeMask(viewType);
		
		ColumnModel oldColumn = null;
		newColumn = new ColumnModel();
		newColumn.setId("12");
		columnChanges = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testNullDao(){
		new TableIndexManagerImpl(null, mockManagerSupport);	
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullSupport(){
		new TableIndexManagerImpl(mockIndexDao, null);			
	}
	
	@Test
	public void testApplyChangeSetToIndexHappy(){
		//call under test.
		manager.applyChangeSetToIndex(tableId, sparseChangeSet, versionNumber);
		// All changes should be executed in a transaction
		verify(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		// both groups should be written
		verify(mockIndexDao).createOrUpdateOrDeleteRows(tableId, groupOne);
		verify(mockIndexDao).createOrUpdateOrDeleteRows(tableId, groupTwo);
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
		manager.applyChangeSetToIndex(tableId, sparseChangeSet, versionNumber);
		// nothing do do.
		verify(mockIndexDao, never()).executeInWriteTransaction(any(TransactionCallback.class));
		verify(mockIndexDao, never()).createOrUpdateOrDeleteRows(any(IdAndVersion.class), any(Grouping.class));
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(any(IdAndVersion.class), anySet());
		verify(mockIndexDao, never()).setMaxCurrentCompleteVersionForTable(any(IdAndVersion.class), anyLong());
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
		sparseChangeSet = new SparseChangeSet(tableId.toString(), schema);
		SparseRow row = sparseChangeSet.addEmptyRow();
		row.setRowId(0L);
		row.setCellValue("99", "some string");
		
		//call under test.
		manager.applyChangeSetToIndex(tableId, sparseChangeSet, versionNumber);
		// All changes should be executed in a transaction
		verify(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		// change should be written to the index
		verify(mockIndexDao).createOrUpdateOrDeleteRows(tableId, groupOne);
		// there are no files
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(any(IdAndVersion.class), anySet());
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
		when(mockIndexDao.alterTableAsNeeded(any(IdAndVersion.class), anyList(), anyBoolean())).thenReturn(true);
		boolean isTableView = false;
		// call under test
		manager.setIndexSchema(tableId, isTableView, schema);
		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(Lists.newArrayList(column.getId()));
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}
	
	@Test
	public void testSetIndexSchemaWithNoColumns(){
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(new LinkedList<DatabaseColumnInfo>());
		when(mockIndexDao.alterTableAsNeeded(any(IdAndVersion.class), anyList(), anyBoolean())).thenReturn(true);
		boolean isTableView = false;
		// call under test
		manager.setIndexSchema(tableId, isTableView, new LinkedList<ColumnModel>());
		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(new LinkedList<>());
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}

	
	@Test
	public void testIsVersionAppliedToIndexNoVersionApplied(){
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		versionNumber = 1L;
		assertFalse(manager.isVersionAppliedToIndex(tableId, versionNumber));
	}
	
	@Test
	public void testIsVersionAppliedToIndexVersionMatches(){
		versionNumber = 1L;
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber);
		assertTrue(manager.isVersionAppliedToIndex(tableId, versionNumber));
	}
	
	@Test
	public void testIsVersionAppliedToIndexVersionGreater(){
		versionNumber = 1L;
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber+1);
		assertTrue(manager.isVersionAppliedToIndex(tableId, versionNumber));
	}
	
	@Test
	public void testDeleteTableIndex(){
		manager.deleteTableIndex(tableId);
		verify(mockIndexDao).deleteSecondaryTables(tableId);
		verify(mockIndexDao).deleteTable(tableId);
	}
	
	@Test
	public void testOptimizeTableIndices(){
		List<DatabaseColumnInfo> infoList = new LinkedList<DatabaseColumnInfo>();
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(infoList);
		// call under test
		manager.optimizeTableIndices(tableId);
		// column data must be gathered.
		verify(mockIndexDao).getDatabaseInfo(tableId);
		verify(mockIndexDao).provideCardinality(infoList, tableId);
		verify(mockIndexDao).provideIndexName(infoList, tableId);
		// optimization called.
		verify(mockIndexDao).optimizeTableIndices(infoList, tableId, TableIndexManagerImpl.MAX_MYSQL_INDEX_COUNT);
	}
	
	@Test
	public void testUpdateTableSchemaAddColumn(){
		boolean alterTemp = false;
		when(mockIndexDao.alterTableAsNeeded(tableId, columnChanges, alterTemp)).thenReturn(true);
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C12_");
		info.setColumnType(ColumnType.BOOLEAN);
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(Lists.newArrayList(info));
		boolean isTableView = false;
		// call under test
		manager.updateTableSchema(tableId, isTableView, columnChanges);
		verify(mockIndexDao).createTableIfDoesNotExist(tableId, isTableView);
		verify(mockIndexDao).createSecondaryTables(tableId);
		// The new schema is not empty so do not truncate.
		verify(mockIndexDao, never()).truncateTable(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);
		
		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(Lists.newArrayList(newColumn.getId()));
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
		DatabaseColumnInfo current = new DatabaseColumnInfo();
		current.setColumnName(SQLUtils.getColumnNameForId(oldColumn.getId()));
		current.setColumnType(ColumnType.STRING);
		List<DatabaseColumnInfo> startSchema = Lists.newArrayList(current);
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(startSchema, new LinkedList<DatabaseColumnInfo>());
		boolean isTableView = true;
		// call under test
		manager.updateTableSchema(tableId, isTableView, changes);
		verify(mockIndexDao).createTableIfDoesNotExist(tableId, isTableView);
		verify(mockIndexDao).createSecondaryTables(tableId);
		verify(mockIndexDao, times(2)).getDatabaseInfo(tableId);
		// The new schema is empty so the table is truncated.
		verify(mockIndexDao).truncateTable(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, changes, alterTemp);
		
		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(new LinkedList<>());
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}
	
	@Test
	public void testUpdateTableSchemaNoChange(){
		List<ColumnChangeDetails> changes = new LinkedList<ColumnChangeDetails>();
		boolean alterTemp = false;
		when(mockIndexDao.alterTableAsNeeded(tableId, changes, alterTemp)).thenReturn(false);
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(new LinkedList<DatabaseColumnInfo>());
		boolean isTableView = true;
		// call under test
		manager.updateTableSchema(tableId, isTableView, changes);
		verify(mockIndexDao).createTableIfDoesNotExist(tableId, isTableView);
		verify(mockIndexDao).createSecondaryTables(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, changes, alterTemp);
		verify(mockIndexDao).getDatabaseInfo(tableId);
		verify(mockIndexDao, never()).truncateTable(tableId);
		verify(mockIndexDao, never()).setCurrentSchemaMD5Hex(any(IdAndVersion.class), anyString());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateTemporaryTableCopy() throws Exception{
		// call under test
		manager.createTemporaryTableCopy(tableId, mockCallback);
		verify(mockIndexDao).createTemporaryTable(tableId);
		verify(mockIndexDao).copyAllDataToTemporaryTable(tableId);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDeleteTemporaryTableCopy() throws Exception{
		// call under test
		manager.deleteTemporaryTableCopy(tableId, mockCallback);
		verify(mockIndexDao).deleteTemporaryTable(tableId);
	}
	
	@Test
	public void testPopulateViewFromEntityReplication(){
		viewType = ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		// call under test
		Long resultCrc = manager.populateViewFromEntityReplication(tableId.getId(), mockCallback, viewType, scope, schema);
		assertEquals(crc32, resultCrc);
		verify(mockIndexDao).copyEntityReplicationToTable(tableId.getId(), viewType, scope, schema);
		// the CRC should be calculated with the etag column.
		verify(mockIndexDao).calculateCRC32ofTableView(tableId.getId());
	}
	
	/**
	 * Etag is no long a required column.
	 */
	@Test
	public void testPopulateViewFromEntityReplicationMissingEtagColumn(){
		viewType = ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		ColumnModel etagColumn = EntityField.findMatch(schema, EntityField.etag);
		// remove the etag column
		schema.remove(etagColumn);
		// call under test
		manager.populateViewFromEntityReplication(tableId.getId(), mockCallback, viewType, scope, schema);
	}
	
	/**
	 * Etag column is no longer requierd.
	 */
	@Test
	public void testPopulateViewFromEntityReplicationMissingBenefactorColumn(){
		viewType = ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		ColumnModel benefactorColumn = EntityField.findMatch(schema, EntityField.benefactorId);
		// remove the benefactor column
		schema.remove(benefactorColumn);
		// call under test
		manager.populateViewFromEntityReplication(tableId.getId(), mockCallback, viewType, scope, schema);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPopulateViewFromEntityReplicationNullViewType(){
		viewType = null;
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		// call under test
		manager.populateViewFromEntityReplication(tableId.getId(), mockCallback, viewType, scope, schema);;
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPopulateViewFromEntityReplicationScopeNull(){
		viewType = ViewTypeMask.File.getMask();
		Set<Long> scope = null;
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		// call under test
		manager.populateViewFromEntityReplication(tableId.getId(), mockCallback, viewType, scope, schema);;
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPopulateViewFromEntityReplicationSchemaNull(){
		viewType = ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = null;
		// call under test
		manager.populateViewFromEntityReplication(tableId.getId(), mockCallback, viewType, scope, schema);;
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPopulateViewFromEntityReplicationCallbackNull(){
		viewType = ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		mockCallback = null;
		// call under test
		manager.populateViewFromEntityReplication(tableId.getId(), mockCallback, viewType, scope, schema);;
	}
	
	@Test
	public void testPopulateViewFromEntityReplicationWithProgress() throws Exception{
		viewType = ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		// call under test
		Long resultCrc = manager.populateViewFromEntityReplicationWithProgress(tableId.getId(), viewType, scope, schema);
		assertEquals(crc32, resultCrc);
		verify(mockIndexDao).copyEntityReplicationToTable(tableId.getId(), viewType, scope, schema);
		// the CRC should be calculated with the etag column.
		verify(mockIndexDao).calculateCRC32ofTableView(tableId.getId());
	}
	
	@Test
	public void testPopulateViewFromEntityReplicationUnknownCause() throws Exception{
		viewType = ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		// setup a failure
		IllegalArgumentException error = new IllegalArgumentException("Something went wrong");
		doThrow(error).when(mockIndexDao).copyEntityReplicationToTable(tableId.getId(), viewType, scope, schema);
		try {
			// call under test
			manager.populateViewFromEntityReplicationWithProgress(tableId.getId(), viewType, scope, schema);
			fail("Should have failed");
		} catch (IllegalArgumentException expected) {
			// when the cause cannot be determined the original exception is thrown.
			assertEquals(error, expected);
		}
	}
	
	@Test
	public void testPopulateViewFromEntityReplicationKnownCause() throws Exception{
		viewType = ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		
		ColumnModel column = new ColumnModel();
		column.setId("123");
		column.setName("foo");
		column.setColumnType(ColumnType.STRING);
		column.setMaximumSize(10L);
		schema.add(column);
		// Setup an annotation that is larger than the columns.
		ColumnModel annotation = new ColumnModel();
		annotation.setName("foo");
		annotation.setMaximumSize(11L);
		annotation.setColumnType(ColumnType.STRING);
		
		// setup the annotations 
		when(mockIndexDao.getPossibleColumnModelsForContainers(scope, viewType, Long.MAX_VALUE, 0L)).thenReturn(Lists.newArrayList(annotation));
		// setup a failure
		IllegalArgumentException error = new IllegalArgumentException("Something went wrong");
		doThrow(error).when(mockIndexDao).copyEntityReplicationToTable(tableId.getId(), viewType, scope, schema);
		try {
			// call under test
			manager.populateViewFromEntityReplicationWithProgress(tableId.getId(), viewType, scope, schema);
			fail("Should have failed");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().startsWith("The size of the column 'foo' is too small"));
			// the cause should match the original error.
			assertEquals(error, expected.getCause());
		}
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerLastPage(){
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(containerIds, viewType, tokenString);
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
		// should request one more than the limit
		verify(mockIndexDao).getPossibleColumnModelsForContainers(containerIds, viewType, limit+1, offset);
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerLastPageNullToken(){
		tokenString = null;
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(containerIds, viewType, tokenString);
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
		// should request one more than the limit
		verify(mockIndexDao).getPossibleColumnModelsForContainers(containerIds, viewType, NextPageToken.DEFAULT_LIMIT+1, NextPageToken.DEFAULT_OFFSET);
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerHasNextPage(){
		List<ColumnModel> pagePluseOne = new LinkedList<ColumnModel>(schema);
		pagePluseOne.add(new ColumnModel());
		when(mockIndexDao.getPossibleColumnModelsForContainers(anySet(), any(Long.class), anyLong(), anyLong())).thenReturn(pagePluseOne);
		nextPageToken =  new NextPageToken(schema.size(), 0L);
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(containerIds, viewType, nextPageToken.toToken());
		assertNotNull(results);
		assertEquals(new NextPageToken(2L, 2L).toToken(), results.getNextPageToken());
		assertEquals(schema, results.getResults());
		// should request one more than the limit
		verify(mockIndexDao).getPossibleColumnModelsForContainers(containerIds, viewType, nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetPossibleAnnotationDefinitionsForContainerIsNullContainerIds(){
		String token = nextPageToken.toToken();
		containerIds = null;
		// call under test
		manager.getPossibleAnnotationDefinitionsForContainerIds(containerIds, viewType, token);
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerIsEmpty(){
		String token = nextPageToken.toToken();
		containerIds = new HashSet<>();
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(containerIds, viewType, token);
		assertNotNull(results);
		assertNotNull(results.getResults());
		assertEquals(null, results.getNextPageToken());
		// should not call the dao
		verify(mockIndexDao, never()).getPossibleColumnModelsForContainers(anySet(), any(Long.class), anyLong(), anyLong());
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetPossibleAnnotationDefinitionsForContainerIsOverLimit(){
		limit = NextPageToken.MAX_LIMIT+1;
		nextPageToken = new NextPageToken(limit, offset);
		// call under test
		manager.getPossibleAnnotationDefinitionsForContainerIds(containerIds, viewType, nextPageToken.toToken());
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForView(){
		// call under test
		ColumnModelPage results = manager.getPossibleColumnModelsForView(tableId.getId(), tokenString);
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetPossibleAnnotationDefinitionsForViewNullId(){
		Long viewId = null;
		// call under test
		manager.getPossibleColumnModelsForView(viewId, tokenString);
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForScope(){
		// call under test
		ColumnModelPage results = manager.getPossibleColumnModelsForScope(scope, tokenString);
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForScopeTypeNull(){
		viewType = null;
		// call under test
		ColumnModelPage results = manager.getPossibleColumnModelsForScope(scope, tokenString);
		assertNotNull(results);
		// should default to file view.
		verify(mockIndexDao).getPossibleColumnModelsForContainers(containerIds, ViewTypeMask.File.getMask(), nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetPossibleAnnotationDefinitionsForScopeNullScope(){
		scope.setScope(null);
		// call under test
		manager.getPossibleColumnModelsForScope(scope, tokenString);
	}
	
	/**
	 * Test added for PLFM-4155.
	 */
	@Test
	public void testAlterTableAsNeededWithinAutoProgress(){
		DatabaseColumnInfo rowId = new DatabaseColumnInfo();
		rowId.setColumnName(TableConstants.ROW_ID);
		DatabaseColumnInfo one = new DatabaseColumnInfo();
		one.setColumnName("_C111_");
		DatabaseColumnInfo two = new DatabaseColumnInfo();
		two.setColumnName("_C222_");
		List<DatabaseColumnInfo> curretIndexSchema = Lists.newArrayList(rowId, one, two);
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(curretIndexSchema);
		
		// the old does not exist in the current
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("333");
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("444");
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);
		List<ColumnChangeDetails> changes = Lists.newArrayList(change);
		
		// call under test
		manager.alterTableAsNeededWithinAutoProgress(tableId, changes, true);
		verify(mockIndexDao).provideIndexName(curretIndexSchema, tableId);
		verify(mockIndexDao).alterTableAsNeeded(eq(tableId), changeCaptor.capture(), eq(true));
		List<ColumnChangeDetails> captured = changeCaptor.getValue();
		// the results should be changed
		assertNotNull(captured);
		assertEquals(1, captured.size());
		ColumnChangeDetails updated = captured.get(0);
		// should not be the same instance.
		assertFalse(change == updated);
		assertEquals(null, updated.getOldColumn());
		assertEquals(newColumn, updated.getNewColumn());
	}
	
	@Test
	public void testApplyRowChangeToIndex() {
		long changeNumber = 333l;
		ChangeData<SparseChangeSet> change = new ChangeData<SparseChangeSet>(changeNumber, sparseChangeSet);
		// call under test
		manager.applyRowChangeToIndex(tableId, change);
		
		// set schema
		verify(mockIndexDao).createTableIfDoesNotExist(tableId, false);
		verify(mockIndexDao).createSecondaryTables(tableId);
		// apply change
		verify(mockIndexDao, times(2)).createOrUpdateOrDeleteRows(any(IdAndVersion.class), any(Grouping.class));
	}
	
	@Test
	public void testApplySchemaChangeToIndex() {
		long changeNumber = 333l;
		SchemaChange schemaChange = new SchemaChange(columnChanges);
		ChangeData<SchemaChange> change = new ChangeData<SchemaChange>(changeNumber, schemaChange);
		
		// Call under test
		manager.applySchemaChangeToIndex(tableId, change);
		
		boolean alterTemp = false;
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);
	}
	
	@Test
	public void testAppleyChangeToIndexRow() throws NotFoundException, IOException {
		long changeNumber = 444L;
		TableChangeMetaData mockChange = setupMockRowChange(changeNumber);
		//call under test
		manager.appleyChangeToIndex(tableId, mockChange);
		// set schema
		verify(mockIndexDao).createTableIfDoesNotExist(tableId, false);
		verify(mockIndexDao).createSecondaryTables(tableId);
		// apply change
		verify(mockIndexDao, times(2)).createOrUpdateOrDeleteRows(any(IdAndVersion.class), any(Grouping.class));
	}
	
	@Test
	public void testAppleyChangeToIndexRowColumn() throws NotFoundException, IOException {
		long changeNumber = 444L;
		TableChangeMetaData mockChange = setupMockColumnChange(changeNumber);
		//call under test
		manager.appleyChangeToIndex(tableId, mockChange);
		// set schema
		boolean alterTemp = false;
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);
	}
	
	@Test
	public void testBuildIndexToChangeNumberWithExclusiveLock() throws Exception {
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1L;
		String resetToken = "resetToken";
		// call under test
		String lastEtag = manager.buildIndexToChangeNumberWithExclusiveLock(tableId, iterator, targetChangeNumber, resetToken);
		assertEquals(list.get(1).getETag(), lastEtag);
		// Progress should be made for both changes
		verify(mockManagerSupport).attemptToUpdateTableProgress(tableId, resetToken, "Applying change: 0", 0L, 1L);
		verify(mockManagerSupport).attemptToUpdateTableProgress(tableId, resetToken, "Applying change: 1", 1L, 1L);
		// row changes should be applied
		verify(mockIndexDao, times(2)).createOrUpdateOrDeleteRows(any(IdAndVersion.class), any(Grouping.class));
		// column changes should be applied
		boolean alterTemp = false;
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);
		// The table should be optimized
		verify(mockIndexDao).optimizeTableIndices(anyList(), any(IdAndVersion.class), anyInt());
		// Building without a version should attempt to set the current schema on the index.
		verify(mockManagerSupport).getColumnModelsForTable(any(IdAndVersion.class));
	}
	
	@Test
	public void testBuildIndexToChangeNumberWithExclusiveLockWithVersion() throws Exception {
		tableId = IdAndVersion.parse("syn123.1");
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1L;
		String resetToken = "resetToken";
		// call under test
		String lastEtag = manager.buildIndexToChangeNumberWithExclusiveLock(tableId, iterator, targetChangeNumber, resetToken);
		assertEquals(list.get(1).getETag(), lastEtag);
		// Progress should be made for both changes
		verify(mockManagerSupport).attemptToUpdateTableProgress(tableId, resetToken, "Applying change: 0", 0L, 1L);
		verify(mockManagerSupport).attemptToUpdateTableProgress(tableId, resetToken, "Applying change: 1", 1L, 1L);
		// row changes should be applied
		verify(mockIndexDao, times(2)).createOrUpdateOrDeleteRows(any(IdAndVersion.class), any(Grouping.class));
		// column changes should be applied
		boolean alterTemp = false;
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);
		// The table should be optimized
		verify(mockIndexDao).optimizeTableIndices(anyList(), any(IdAndVersion.class), anyInt());
		// Building to a version should not attempt to set the current schema on the index.
		verify(mockManagerSupport, never()).getColumnModelsForTable(any(IdAndVersion.class));
	}
	
	@Test
	public void testBuildIndexToChangeNumberWithExclusiveLockFirstChangeOnly() throws Exception {
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L,0L);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		// no version means there are no table changes.
		long targetChangeNumber = 0L;
		String resetToken = "resetToken";
		// call under test
		String lastEtag = manager.buildIndexToChangeNumberWithExclusiveLock(tableId, iterator, targetChangeNumber, resetToken);
		assertEquals(list.get(0).getETag(), lastEtag);
		// Progress should be made for both changes
		verify(mockManagerSupport).attemptToUpdateTableProgress(tableId, resetToken, "Applying change: 0", 0L, 0L);
		verify(mockManagerSupport, times(1)).attemptToUpdateTableProgress(any(IdAndVersion.class), anyString(), anyString(), anyLong(), anyLong());
	}
	
	@Test
	public void testBuildIndexToChangeNumberWithExclusiveLockNoWorkNeeded() throws Exception {
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(1L);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		// no version means there are no table changes.
		long targetChangeNumber = 1L;
		String resetToken = "resetToken";
		// call under test
		String lastEtag = manager.buildIndexToChangeNumberWithExclusiveLock(tableId, iterator, targetChangeNumber, resetToken);
		assertEquals(null, lastEtag);
		// Progress should be made for both changes
		verify(mockManagerSupport, never()).attemptToUpdateTableProgress(any(IdAndVersion.class), anyString(), anyString(), anyLong(), anyLong());
		verify(mockIndexDao, never()).createOrUpdateOrDeleteRows(any(IdAndVersion.class), any(Grouping.class));
		// one time for applying the table schema to the index.
		verify(mockIndexDao, times(1)).alterTableAsNeeded(any(IdAndVersion.class), anyList(), anyBoolean());
		// The table should be optimized
		verify(mockIndexDao).optimizeTableIndices(anyList(), any(IdAndVersion.class), anyInt());
	}
	
	@Test
	public void testBuildIndexToChangeNumber() throws Exception {
		when(mockManagerSupport.isIndexWorkRequired(tableId)).thenReturn(true);
		String resetToken = "resetToken";
		when(mockManagerSupport.startTableProcessing(tableId)).thenReturn(resetToken);
		String lastEtag = "lastEtag";
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				anyInt(), any(ProgressingCallable.class))).thenReturn(lastEtag);

		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1;
		// call under test
		manager.buildIndexToChangeNumber(mockCallback, tableId, iterator, targetChangeNumber);
		verify(mockManagerSupport).attemptToSetTableStatusToAvailable(tableId, resetToken, lastEtag);
		verify(mockManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class), anyString(),
				any(Exception.class));
	}
	
	@Test
	public void testBuildIndexToChangeNumberNoWorkNeeded() throws Exception {
		// no work is needed
		when(mockManagerSupport.isIndexWorkRequired(tableId)).thenReturn(false);
		String resetToken = "resetToken";
		when(mockManagerSupport.startTableProcessing(tableId)).thenReturn(resetToken);
		String lastEtag = "lastEtag";
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				anyInt(), any(ProgressingCallable.class))).thenReturn(lastEtag);

		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1;
		// call under test
		manager.buildIndexToChangeNumber(mockCallback, tableId, iterator, targetChangeNumber);
		verify(mockManagerSupport, never()).startTableProcessing(any(IdAndVersion.class));
		verify(mockManagerSupport, never()).attemptToSetTableStatusToAvailable(tableId, resetToken, lastEtag);
		verify(mockManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class), anyString(),
				any(Exception.class));
	}
	
	/**
	 * LockUnavilableException should translate to RecoverableMessageException
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumberLockUnavilableException() throws Exception {
		when(mockManagerSupport.isIndexWorkRequired(tableId)).thenReturn(true);
		String resetToken = "resetToken";
		when(mockManagerSupport.startTableProcessing(tableId)).thenReturn(resetToken);
		String lastEtag = "lastEtag";
		LockUnavilableException exception = new LockUnavilableException("no lock for you!");
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				anyInt(), any(ProgressingCallable.class))).thenThrow(exception);

		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1;
		try {
			// call under test
			manager.buildIndexToChangeNumber(mockCallback, tableId, iterator, targetChangeNumber);
			fail();
		} catch (RecoverableMessageException e) {
			assertEquals(exception, e.getCause());
		}
		verify(mockManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class), anyString(),
				any(Exception.class));
	}
	
	/**
	 * TableUnavailableException should translate to RecoverableMessageException
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumberTableUnavailableException() throws Exception {
		when(mockManagerSupport.isIndexWorkRequired(tableId)).thenReturn(true);
		String resetToken = "resetToken";
		when(mockManagerSupport.startTableProcessing(tableId)).thenReturn(resetToken);
		String lastEtag = "lastEtag";
		TableUnavailableException exception = new TableUnavailableException(null);
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				anyInt(), any(ProgressingCallable.class))).thenThrow(exception);

		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1;
		try {
			// call under test
			manager.buildIndexToChangeNumber(mockCallback, tableId, iterator, targetChangeNumber);
			fail();
		} catch (RecoverableMessageException e) {
			assertEquals(exception, e.getCause());
		}
		verify(mockManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class), anyString(),
				any(Exception.class));
	}
	
	/**
	 * InterruptedException should translate to RecoverableMessageException
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumberInterruptedException() throws Exception {
		when(mockManagerSupport.isIndexWorkRequired(tableId)).thenReturn(true);
		String resetToken = "resetToken";
		when(mockManagerSupport.startTableProcessing(tableId)).thenReturn(resetToken);
		String lastEtag = "lastEtag";
		InterruptedException exception = new InterruptedException("interrupted");
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				anyInt(), any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1;
		try {
			// call under test
			manager.buildIndexToChangeNumber(mockCallback, tableId, iterator, targetChangeNumber);
			fail();
		} catch (RecoverableMessageException e) {
			assertEquals(exception, e.getCause());
		}
		verify(mockManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class), anyString(),
				any(Exception.class));
	}
	
	/**
	 * IOException should translate to RecoverableMessageException
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumberIOException() throws Exception {
		when(mockManagerSupport.isIndexWorkRequired(tableId)).thenReturn(true);
		String resetToken = "resetToken";
		when(mockManagerSupport.startTableProcessing(tableId)).thenReturn(resetToken);
		String lastEtag = "lastEtag";
		IOException exception = new IOException("IO things went wrong");
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				anyInt(), any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1;
		try {
			// call under test
			manager.buildIndexToChangeNumber(mockCallback, tableId, iterator, targetChangeNumber);
			fail();
		} catch (RecoverableMessageException e) {
			assertEquals(exception, e.getCause());
		}
		verify(mockManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class), anyString(),
				any(Exception.class));
	}
	
	@Test
	public void testBuildIndexToChangeNumberNonRetryException() throws Exception {
		when(mockManagerSupport.isIndexWorkRequired(tableId)).thenReturn(true);
		String resetToken = "resetToken";
		when(mockManagerSupport.startTableProcessing(tableId)).thenReturn(resetToken);
		String lastEtag = "lastEtag";
		IllegalArgumentException exception = new IllegalArgumentException("Cannot retry this");
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				anyInt(), any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1;
		// call under test
		manager.buildIndexToChangeNumber(mockCallback, tableId, iterator, targetChangeNumber);
		// should fail the table.
		verify(mockManagerSupport).attemptToSetTableStatusToFailed(tableId, resetToken, exception);
	}
	
	
	/**
	 * Helper to setup both a row and column change within a list.
	 * @return
	 * @throws NotFoundException
	 * @throws IOException
	 */
	List<TableChangeMetaData> setupMockChanges() throws NotFoundException, IOException{
		// add a row change
		TableChangeMetaData rowChange = setupMockRowChange(0L);
		TableChangeMetaData columnChange = setupMockColumnChange(1L);
		return Lists.newArrayList(rowChange, columnChange);
	}
	
	/**
	 * Helper to setup a mock TableChangeMetaData for a Row change.
	 * @return
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	public TableChangeMetaData setupMockRowChange(long changeNumber) throws NotFoundException, IOException {
		TableChangeMetaData mockChange = Mockito.mock(TableChangeMetaData.class);
		when(mockChange.getChangeNumber()).thenReturn(changeNumber);
		when(mockChange.getETag()).thenReturn("etag-"+changeNumber);
		when(mockChange.getChangeType()).thenReturn(TableChangeType.ROW);
		ChangeData<SparseChangeSet> change = new ChangeData<SparseChangeSet>(changeNumber, sparseChangeSet);
		when(mockChange.loadChangeData(SparseChangeSet.class)).thenReturn(change);
		return mockChange;
	}
	
	/**
	 * Helper to setup a mock TableChangeMetaData for a Column change.
	 * @return
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	public TableChangeMetaData setupMockColumnChange(long changeNumber) throws NotFoundException, IOException {
		TableChangeMetaData mockChange = Mockito.mock(TableChangeMetaData.class);
		when(mockChange.getChangeNumber()).thenReturn(changeNumber);
		when(mockChange.getETag()).thenReturn("etag-"+changeNumber);
		when(mockChange.getChangeType()).thenReturn(TableChangeType.COLUMN);
		SchemaChange schemaChange = new SchemaChange(columnChanges);
		ChangeData<SchemaChange> change = new ChangeData<SchemaChange>(changeNumber, schemaChange);
		when(mockChange.loadChangeData(SchemaChange.class)).thenReturn(change);
		return mockChange;
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
