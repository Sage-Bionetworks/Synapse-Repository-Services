package org.sagebionetworks.repo.manager.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.table.change.ListColumnIndexTableChange;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.manager.table.metadata.ViewScopeFilterBuilder;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.dbo.dao.table.InvalidStatusTokenException;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewScopeFilter;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.DatabaseColumnInfo;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverImpl;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.ChangeData;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.model.SchemaChange;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.model.SparseRow;
import org.sagebionetworks.util.EnumUtils;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class TableIndexManagerImplTest {
	
	@Mock
	TableIndexDAO mockIndexDao;
	@Mock
	TransactionStatus mockTransactionStatus;
	@Mock
	TableManagerSupport mockManagerSupport;
	@Mock
	ProgressCallback mockCallback;
	@Mock
	MetadataIndexProviderFactory mockMetadataProviderFactory;
	@Mock
	MetadataIndexProvider mockMetadataProvider;
	@Mock
	DefaultColumnModel mockDefaultColumnModel;
	@Mock
	ObjectFieldModelResolverFactory mockObjectFieldModelResolverFactory;
	@Mock
	ObjectFieldModelResolver mockObjectFieldModelResolver;
	
	@Captor 
	ArgumentCaptor<List<ColumnChangeDetails>> changeCaptor;
	
	TableIndexManagerImpl manager;
	TableIndexManagerImpl managerSpy;
	
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
	
	ViewObjectType objectType;
	ColumnModel newColumn;
	List<ColumnChangeDetails> columnChanges;
	
	Set<Long> rowsIdsWithChanges;
	ViewScopeType scopeType;
	ViewScopeFilterBuilder scopeFilterBuilder;
	ObjectFieldModelResolver objectFieldModelResolver;
	
	@BeforeEach
	public void before() throws Exception{
		
		objectType = ViewObjectType.ENTITY;
		tableId = IdAndVersion.parse("syn123");
		manager = new TableIndexManagerImpl(mockIndexDao, mockManagerSupport, mockMetadataProviderFactory, mockObjectFieldModelResolverFactory);
		managerSpy = Mockito.spy(manager);
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
		
		crc32 = 5678L;
		
		containerIds = Sets.newHashSet(1l,2L,3L);
		limit = 10L;
		offset = 0L;
		nextPageToken = new NextPageToken(limit, offset);
		tokenString = nextPageToken.toToken();
		scopeSynIds = Lists.newArrayList("syn123","syn345");
		scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scopeSynIds));

		scope = new ViewScope();
		scope.setScope(scopeSynIds);
		scope.setViewTypeMask(ViewTypeMask.File.getMask());
		scope.setViewEntityType(ViewEntityType.entityview);
		
		ColumnModel oldColumn = null;
		newColumn = new ColumnModel();
		newColumn.setId("12");
		columnChanges = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
		
		rowsIdsWithChanges = Sets.newHashSet(444L,555L);
		scopeType = new ViewScopeType(objectType, ViewTypeMask.File.getMask());
		
		objectFieldModelResolver = new ObjectFieldModelResolverImpl(mockMetadataProvider);
	}

	@Test
	public void testNullDao(){
		assertThrows(IllegalArgumentException.class, ()->{
			new TableIndexManagerImpl(null, mockManagerSupport, mockMetadataProviderFactory, mockObjectFieldModelResolverFactory);	
		});
	}
	
	
	@Test
	public void testNullSupport(){
		assertThrows(IllegalArgumentException.class, ()->{
			new TableIndexManagerImpl(mockIndexDao, null, mockMetadataProviderFactory, mockObjectFieldModelResolverFactory);			
		});
	}
	
	@Test
	public void testNullProviderFactory(){
		assertThrows(IllegalArgumentException.class, ()->{
			new TableIndexManagerImpl(mockIndexDao, mockManagerSupport, null, mockObjectFieldModelResolverFactory);			
		});
	}
	
	@Test
	public void testNullObjectFieldFactory(){
		assertThrows(IllegalArgumentException.class, ()->{
			new TableIndexManagerImpl(mockIndexDao, mockManagerSupport, mockMetadataProviderFactory, null);			
		});
	}
	
	@Test
	public void testApplyChangeSetToIndexHappy(){
		setupExecuteInWriteTransaction();
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);

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
		setupExecuteInWriteTransaction();
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
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
	public void testApplyChangeSetToIndex_PopulateListColumns(){
		setupExecuteInWriteTransaction();
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		// no files in the schema
		schema = Arrays.asList(
				TableModelTestUtils.createColumn(99L, "strList", ColumnType.STRING_LIST),
				TableModelTestUtils.createColumn(101L, "intList", ColumnType.INTEGER_LIST)
		);

		sparseChangeSet = new SparseChangeSet(tableId.toString(), schema);
		SparseRow row = sparseChangeSet.addEmptyRow();
		row.setRowId(0L);
		row.setCellValue("99", "[\"some string\", \"some other string\"]");
		row.setCellValue("101", "[1,2,3,4]");

		SparseRow row2 = sparseChangeSet.addEmptyRow();
		row2.setRowId(5L);
		row2.setCellValue("99", "[\"some string\", \"some other string\"]");
		row2.setCellValue("101", "[1,2,3,4]");

		//call under test.
		manager.applyChangeSetToIndex(tableId, sparseChangeSet, versionNumber);
		// All changes should be executed in a transaction
		verify(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		// change should be written to the index
		verify(mockIndexDao).createOrUpdateOrDeleteRows(eq(tableId), any(Grouping.class));
		// there are no files
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(any(IdAndVersion.class), anySet());
		// The new version should be set
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, versionNumber);

		Set<Long> expectedRows = Sets.newHashSet(0L,5L);
		verify(mockIndexDao).deleteFromListColumnIndexTable(tableId, schema.get(0), expectedRows);
		verify(mockIndexDao).deleteFromListColumnIndexTable(tableId, schema.get(1), expectedRows);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, schema.get(0), expectedRows, false);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, schema.get(1), expectedRows, false);
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
	public void testSetIndexSchemaWithListColumns(){
		ColumnModel column = new ColumnModel();
		column.setId("44");
		column.setColumnType(ColumnType.STRING_LIST);
		column.setMaximumSize(50L);
		schema = Lists.newArrayList(column);

		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C44_");
		info.setColumnType(ColumnType.BOOLEAN);

		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(Lists.newArrayList(info));
		when(mockIndexDao.alterTableAsNeeded(any(IdAndVersion.class), anyList(), anyBoolean())).thenReturn(true);
		when(mockIndexDao.getMultivalueColumnIndexTableColumnIds(tableId)).thenReturn(Collections.emptySet());
		boolean isTableView = false;
		// call under test
		manager.setIndexSchema(tableId, isTableView, schema);
		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(Lists.newArrayList(column.getId()));
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
		verify(mockIndexDao).getMultivalueColumnIndexTableColumnIds(tableId);
		verify(mockIndexDao).createMultivalueColumnIndexTable(tableId, column, false);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, column, null, false);
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
		String existingColumnId = "11";
		DatabaseColumnInfo existingColumn = new DatabaseColumnInfo();
		existingColumn.setColumnName("_C"+existingColumnId+"_");
		existingColumn.setColumnType(ColumnType.BOOLEAN);

		DatabaseColumnInfo createdColumn = new DatabaseColumnInfo();
		createdColumn.setColumnName("_C12_");
		createdColumn.setColumnType(ColumnType.BOOLEAN);
		when(mockIndexDao.getDatabaseInfo(tableId))
				// first time called we only have 1 existing column
				.thenReturn(Collections.singletonList(existingColumn))
				// on the second time, our new column has been added
				.thenReturn(Arrays.asList(existingColumn,createdColumn));
		boolean isTableView = false;
		// call under test
		manager.updateTableSchema(tableId, isTableView, columnChanges);
		verify(mockIndexDao).createTableIfDoesNotExist(tableId, isTableView);
		verify(mockIndexDao).createSecondaryTables(tableId);
		// The new schema is not empty so do not truncate.
		verify(mockIndexDao, never()).truncateTable(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);
		
		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(Arrays.asList(existingColumnId, newColumn.getId()));
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
	
	@Test
	public void testCreateTemporaryTableCopy_noMultiValueColumnIndexTables(){
		// call under test
		manager.createTemporaryTableCopy(tableId);
		verify(mockIndexDao).createTemporaryTable(tableId);
		verify(mockIndexDao).copyAllDataToTemporaryTable(tableId);
		verify(mockIndexDao).getMultivalueColumnIndexTableColumnIds(tableId);
		verify(mockIndexDao, never()).createTemporaryMultiValueColumnIndexTable(any(IdAndVersion.class), anyString());
		verify(mockIndexDao, never()).copyAllDataToTemporaryMultiValueColumnIndexTable(any(IdAndVersion.class), anyString());
	}

	@Test
	public void testCreateTemporaryTableCopy_hasMultiValueColumnIndexTables(){
		when(mockIndexDao.getMultivalueColumnIndexTableColumnIds(tableId)).thenReturn(Sets.newHashSet(123L, 456L));

		// call under test
		manager.createTemporaryTableCopy(tableId);
		verify(mockIndexDao).createTemporaryTable(tableId);
		verify(mockIndexDao).copyAllDataToTemporaryTable(tableId);
		verify(mockIndexDao).getMultivalueColumnIndexTableColumnIds(tableId);
		verify(mockIndexDao).createTemporaryMultiValueColumnIndexTable(tableId, "123");
		verify(mockIndexDao).copyAllDataToTemporaryMultiValueColumnIndexTable(tableId, "123");
		verify(mockIndexDao).createTemporaryMultiValueColumnIndexTable(tableId, "456");
		verify(mockIndexDao).copyAllDataToTemporaryMultiValueColumnIndexTable(tableId, "456");
	}
	
	@Test
	public void testDeleteTemporaryTableCopy(){
		// call under test
		manager.deleteTemporaryTableCopy(tableId);
		verify(mockIndexDao).deleteTemporaryTable(tableId);
		verify(mockIndexDao).deleteAllTemporaryMultiValueColumnIndexTable(tableId);
	}
	
	@Test
	public void testPopulateViewFromEntityReplication(){
		when(mockIndexDao.calculateCRC32ofTableView(any(Long.class))).thenReturn(crc32);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getObjectType()).thenReturn(objectType);
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);

		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		
		ViewScopeFilter scopeFilter = new ViewScopeFilter(objectType, subTypes, filterByObjectId, scope);
		
		// call under test
		Long resultCrc = managerSpy.populateViewFromEntityReplication(tableId.getId(), scopeType, scope, schema);
		assertEquals(crc32, resultCrc);
		verify(mockIndexDao).copyObjectReplicationToView(tableId.getId(), scopeFilter, schema, mockMetadataProvider);
		// the CRC should be calculated with the etag column.
		verify(mockIndexDao).calculateCRC32ofTableView(tableId.getId());
	}
	
	/**
	 * Etag is no long a required column.
	 */
	@Test
	public void testPopulateViewFromEntityReplicationMissingEtagColumn() {
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);

		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds(ObjectField.etag);
		
		// call under test
		manager.populateViewFromEntityReplication(tableId.getId(), scopeType, scope, schema);
	}
	
	/**
	 * Etag column is no longer requierd.
	 */
	@Test
	public void testPopulateViewFromEntityReplicationMissingBenefactorColumn(){
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);

		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds(ObjectField.benefactorId);
		// call under test
		manager.populateViewFromEntityReplication(tableId.getId(), scopeType, scope, schema);
	}
	
	@Test
	public void testPopulateViewFromEntityReplicationNullViewType(){
		scopeType = null;
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.populateViewFromEntityReplication(tableId.getId(), scopeType, scope, schema);
		});
	}
	
	@Test
	public void testPopulateViewFromEntityReplicationScopeNull(){
		Set<Long> scope = null;
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.populateViewFromEntityReplication(tableId.getId(), scopeType, scope, schema);
		});
	}
	
	@Test
	public void testPopulateViewFromEntityReplicationSchemaNull(){
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.populateViewFromEntityReplication(tableId.getId(), scopeType, scope, schema);
		});
	}
	
	@Test
	public void testPopulateViewFromEntityReplicationWithProgress() throws Exception{
		when(mockIndexDao.calculateCRC32ofTableView(any(Long.class))).thenReturn(crc32);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getObjectType()).thenReturn(objectType);
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);

		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		
		ViewScopeFilter scopeFilter = new ViewScopeFilter(objectType, subTypes, filterByObjectId, scope);
		// call under test
		Long resultCrc = managerSpy.populateViewFromEntityReplication(tableId.getId(), scopeType, scope, schema);
		assertEquals(crc32, resultCrc);
		verify(mockIndexDao).copyObjectReplicationToView(tableId.getId(), scopeFilter, schema, mockMetadataProvider);
		// the CRC should be calculated with the etag column.
		verify(mockIndexDao).calculateCRC32ofTableView(tableId.getId());
	}
	
	@Test
	public void testPopulateViewFromEntityReplicationUnknownCause() throws Exception{
		Set<Long> scope = Sets.newHashSet(1L,2L);
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.findMatch(any())).thenReturn(Optional.empty());
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);
		
		// setup a failure
		IllegalArgumentException expected = new IllegalArgumentException("Something went wrong");
		doThrow(expected).when(mockIndexDao).copyObjectReplicationToView(any(), any(), any(), any());

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.populateViewFromEntityReplication(tableId.getId(), scopeType, scope, schema);
		});
		// when the cause cannot be determined the original exception is thrown.
		assertEquals(expected, ex);
	}
	
	@Test
	public void testPopulateViewFromEntityReplicationKnownCause() throws Exception{
		Set<Long> scope = Sets.newHashSet(1L,2L);
		
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.findMatch(any())).thenReturn(Optional.empty());
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);
		
		ColumnModel column = new ColumnModel();
		column.setId("123");
		column.setName("foo");
		column.setColumnType(ColumnType.STRING);
		column.setMaximumSize(10L);

		List<ColumnModel> schema = ImmutableList.of(column);
		
		ColumnModel annotationModel = new ColumnModel();
		annotationModel.setName("foo");
		annotationModel.setColumnType(ColumnType.STRING);
		annotationModel.setMaximumSize(11L);
		
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any(), any())).thenReturn(ImmutableList.of(annotationModel));

		// setup a failure
		IllegalArgumentException error = new IllegalArgumentException("Something went wrong");
		
		doThrow(error).when(mockIndexDao).copyObjectReplicationToView(any(), any(), any(), any());
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.populateViewFromEntityReplication(tableId.getId(), scopeType, scope, schema);
		});
		
		assertNotEquals(error, ex);
		assertEquals("The size of the column 'foo' is too small.  The column size needs to be at least 11 characters.", ex.getMessage());
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerLastPage(){
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any(), any())).thenReturn(schema);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getObjectType()).thenReturn(objectType);
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);

		ViewScopeFilter scopeFilter = new ViewScopeFilter(objectType, subTypes, filterByObjectId, containerIds);
		List<String> excludeKeys = null;
		
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds, tokenString);
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
		// should request one more than the limit
		verify(mockIndexDao).getPossibleColumnModelsForContainers(scopeFilter, excludeKeys, limit+1, offset);
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerLastPageNullToken(){
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any(), any())).thenReturn(schema);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getObjectType()).thenReturn(objectType);
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);

		ViewScopeFilter scopeFilter = new ViewScopeFilter(objectType, subTypes, filterByObjectId, containerIds);
		List<String> excludeKeys = null;
		tokenString = null;
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds, tokenString);
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
		// should request one more than the limit
		verify(mockIndexDao).getPossibleColumnModelsForContainers(scopeFilter, excludeKeys, NextPageToken.DEFAULT_LIMIT+1, NextPageToken.DEFAULT_OFFSET);
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerHasNextPage(){
		List<ColumnModel> pagePluseOne = new LinkedList<ColumnModel>(schema);
		pagePluseOne.add(new ColumnModel());
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any(), any())).thenReturn(pagePluseOne);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getObjectType()).thenReturn(objectType);
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);

		ViewScopeFilter scopeFilter = new ViewScopeFilter(objectType, subTypes, filterByObjectId, containerIds);
		List<String> excludeKeys = null;
		nextPageToken =  new NextPageToken(schema.size(), 0L);
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds, nextPageToken.toToken());
		assertNotNull(results);
		assertEquals(new NextPageToken(2L, 2L).toToken(), results.getNextPageToken());
		assertEquals(schema, results.getResults());
		// should request one more than the limit
		verify(mockIndexDao).getPossibleColumnModelsForContainers(scopeFilter, excludeKeys, nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
	}
	
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerIsNullContainerIds(){
		String token = nextPageToken.toToken();
		containerIds = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds, token);
		});
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerIsEmpty(){
		String token = nextPageToken.toToken();
		containerIds = new HashSet<>();
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds, token);
		assertNotNull(results);
		assertNotNull(results.getResults());
		assertEquals(null, results.getNextPageToken());
		// should not call the dao
		verify(mockIndexDao, never()).getPossibleColumnModelsForContainers(any(), any(), anyLong(), anyLong());
	}
	
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerIsOverLimit(){
		limit = NextPageToken.MAX_LIMIT+1;
		nextPageToken = new NextPageToken(limit, offset);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds, nextPageToken.toToken());
		});
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForView(){
		when(mockManagerSupport.getViewScopeType(tableId)).thenReturn(scopeType);
		
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any(), any())).thenReturn(schema);
		when(mockManagerSupport.getAllContainerIdsForViewScope(tableId, scopeType)).thenReturn(containerIds);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);
		
		// call under test
		ColumnModelPage results = manager.getPossibleColumnModelsForView(tableId.getId(), tokenString);
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForViewNullId(){
		Long viewId = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getPossibleColumnModelsForView(viewId, tokenString);
		});
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForScope(){
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any(), any())).thenReturn(schema);
		when(mockManagerSupport.getAllContainerIdsForScope(scopeIds, scopeType)).thenReturn(containerIds);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);
		
		// call under test
		ColumnModelPage results = manager.getPossibleColumnModelsForScope(scope, tokenString);
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForScopeTypeNull(){
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any(), any())).thenReturn(schema);
		when(mockManagerSupport.getAllContainerIdsForScope(scopeIds, scopeType)).thenReturn(containerIds);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getObjectType()).thenReturn(objectType);
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);
		
		scope.setViewEntityType(null);

		ViewScopeFilter scopeFilter = new ViewScopeFilter(ViewObjectType.ENTITY, subTypes, filterByObjectId, containerIds);
		List<String> excludeKeys = null;
		// call under test
		ColumnModelPage results = manager.getPossibleColumnModelsForScope(scope, tokenString);
		
		assertNotNull(results);
		// should default to file view.
		verify(mockIndexDao).getPossibleColumnModelsForContainers(scopeFilter, excludeKeys, nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsWithCustomFields(){
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any(), any())).thenReturn(schema);
		when(mockManagerSupport.getAllContainerIdsForScope(scopeIds, scopeType)).thenReturn(containerIds);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		
		ColumnModel customField = new ColumnModel();
		customField.setName("CustomField");
		
		when(mockDefaultColumnModel.getCustomFields()).thenReturn(ImmutableList.of(customField));
		List<String> excludeKeys = ImmutableList.of(customField.getName());
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getObjectType()).thenReturn(objectType);
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);
		
		ViewScopeFilter scopeFilter = new ViewScopeFilter(ViewObjectType.ENTITY, subTypes, filterByObjectId, containerIds);
		
		// call under test
		ColumnModelPage results = manager.getPossibleColumnModelsForScope(scope, tokenString);
		
		assertNotNull(results);
		// Makes sure the exclude list is passed correctly.
		verify(mockIndexDao).getPossibleColumnModelsForContainers(scopeFilter, excludeKeys, nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForScopeNullScope(){
		scope.setScope(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getPossibleColumnModelsForScope(scope, tokenString);
		});
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
		setupExecuteInWriteTransaction();
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);

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
	public void testApplyChangeToIndexRow() throws NotFoundException, IOException {
		setupExecuteInWriteTransaction();
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		long changeNumber = 444L;
		TableChangeMetaData mockChange = setupMockRowChange(changeNumber);
		//call under test
		manager.applyChangeToIndex(tableId, mockChange);
		// set schema
		verify(mockIndexDao).createTableIfDoesNotExist(tableId, false);
		verify(mockIndexDao).createSecondaryTables(tableId);
		// apply change
		verify(mockIndexDao, times(2)).createOrUpdateOrDeleteRows(any(IdAndVersion.class), any(Grouping.class));
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, mockChange.getChangeNumber());

	}
	
	@Test
	public void testApplyChangeToIndexRowColumn() throws NotFoundException, IOException {
		long changeNumber = 444L;
		TableChangeMetaData mockChange = setupMockColumnChange(changeNumber);
		//call under test
		manager.applyChangeToIndex(tableId, mockChange);
		// set schema
		boolean alterTemp = false;
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, mockChange.getChangeNumber());
	}

	@Test
	public void testApplyChangeToIndexRowColumn_ListColumns() throws NotFoundException, IOException {
		long changeNumber = 444L;

		newColumn.setColumnType(ColumnType.INTEGER_LIST);

		TableChangeMetaData mockChange = setupMockColumnChange(changeNumber);
		//call under test
		manager.applyChangeToIndex(tableId, mockChange);
		// set schema
		boolean alterTemp = false;
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);

		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, mockChange.getChangeNumber());
		verify(mockIndexDao).createMultivalueColumnIndexTable(tableId, newColumn, false);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, newColumn, null, false);
	}
	
	@Test
	public void testBuildIndexToChangeNumberWithExclusiveLock() throws Exception {
		setupExecuteInWriteTransaction();
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		when(mockManagerSupport.getTableSchema(tableId)).thenReturn(schema);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1L;
		String resetToken = "resetToken";
		// call under test
		String lastEtag = manager.buildIndexToLatestChange(tableId, iterator, targetChangeNumber, resetToken);
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
		verify(mockManagerSupport).getTableSchema(tableId);
	}
	
	@Test
	public void testBuildIndexToChangeNumberWithExclusiveLockWithVersion() throws Exception {
		setupExecuteInWriteTransaction();
		tableId = IdAndVersion.parse("syn123.1");
		when(mockManagerSupport.getTableSchema(tableId)).thenReturn(schema);
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1L;
		String resetToken = "resetToken";
		// call under test
		String lastEtag = manager.buildIndexToLatestChange(tableId, iterator, targetChangeNumber, resetToken);
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
		verify(mockManagerSupport).getTableSchema(tableId);
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
		String lastEtag = manager.buildIndexToLatestChange(tableId, iterator, targetChangeNumber, resetToken);
		assertEquals(list.get(0).getETag(), lastEtag);
		// Progress should be made for both changes
		verify(mockManagerSupport).attemptToUpdateTableProgress(tableId, resetToken, "Applying change: 0", 0L, 0L);
		verify(mockManagerSupport, times(1)).attemptToUpdateTableProgress(any(IdAndVersion.class), anyString(), anyString(), anyLong(), anyLong());
	}
	
	@Test
	public void testBuildIndexToChangeNumberWithExclusiveLockNoWorkNeeded() throws Exception {
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(1L);
		when(mockManagerSupport.getTableSchema(tableId)).thenReturn(schema);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		// no version means there are no table changes.
		long targetChangeNumber = 1L;
		String resetToken = "resetToken";
		// call under test
		String lastEtag = manager.buildIndexToLatestChange(tableId, iterator, targetChangeNumber, resetToken);
		assertEquals(null, lastEtag);
		// Progress should be made for both changes
		verify(mockManagerSupport, never()).attemptToUpdateTableProgress(any(IdAndVersion.class), anyString(), anyString(), anyLong(), anyLong());
		verify(mockIndexDao, never()).createOrUpdateOrDeleteRows(any(IdAndVersion.class), any(Grouping.class));
		// one time for applying the table schema to the index.
		verify(mockIndexDao, times(1)).alterTableAsNeeded(any(IdAndVersion.class), anyList(), anyBoolean());
		// The table should be optimized
		verify(mockIndexDao).optimizeTableIndices(anyList(), any(IdAndVersion.class), anyInt());
		verify(mockManagerSupport).getTableSchema(tableId);
	}
	
	@Test
	public void testBuildIndexToChangeNumber() throws Exception {
		setupTryRunWithTableExclusiveLock();
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		// call under test
		managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		verify(mockManagerSupport).tryRunWithTableExclusiveLock(eq(mockCallback), eq(tableId), any());
		verify(managerSpy).buildTableIndexWithLock(mockCallback, tableId, iterator);
	}
	
	/**
	 * LockUnavilableException translates to RecoverableMessageException
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_LockUnavilableException() throws Exception {
		LockUnavilableException exception = new LockUnavilableException("no lock for you!");
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, ()->{
			// call under test
			managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		});
		assertEquals(result.getCause(), exception);
		verify(managerSpy, never()).buildTableIndexWithLock(any(), any(), any());
	}
	
	/**
	 * TableUnavailableException translates to RecoverableMessageException
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_TableUnavilableException() throws Exception {
		TableUnavailableException exception = new TableUnavailableException(null);
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, ()->{
			// call under test
			managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		});
		assertEquals(result.getCause(), exception);
		verify(managerSpy, never()).buildTableIndexWithLock(any(), any(), any());
	}
	
	/**
	 * InterruptedException translates to RecoverableMessageException
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_InterruptedException() throws Exception {
		InterruptedException exception = new InterruptedException();
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, ()->{
			// call under test
			managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		});
		assertEquals(result.getCause(), exception);
		verify(managerSpy, never()).buildTableIndexWithLock(any(), any(), any());
	}
	
	/**
	 * IOException translates to RecoverableMessageException
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_IOException() throws Exception {
		IOException exception = new IOException();
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				 any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, ()->{
			// call under test
			managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		});
		assertEquals(result.getCause(), exception);
		verify(managerSpy, never()).buildTableIndexWithLock(any(), any(), any());
	}
	
	/**
	 * RuntimeException is just thrown
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_RuntimeException() throws Exception {
		IllegalArgumentException exception = new IllegalArgumentException("some runtime");
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		RuntimeException result = assertThrows(RuntimeException.class, ()->{
			// call under test
			managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		});
		assertEquals(exception, result);
		verify(managerSpy, never()).buildTableIndexWithLock(any(), any(), any());
	}
	
	/**
	 * Checked Exception is wrapped in runtime.
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_CheckedException() throws Exception {
		Exception exception = new Exception("nope");
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		RuntimeException result = assertThrows(RuntimeException.class, ()->{
			// call under test
			managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		});
		assertEquals(result.getCause(), exception);
		verify(managerSpy, never()).buildTableIndexWithLock(any(), any(), any());
	}
	
	/**
	 * Helper to setup mockManagerSupport.tryRunWithTableExclusiveLock to
	 * forward to the callback
	 * @throws Exception 
	 */
	public void setupTryRunWithTableExclusiveLock() throws Exception {
		doAnswer((InvocationOnMock invocation) -> {
			ProgressCallback callback = invocation.getArgument(0, ProgressCallback.class);
			ProgressingCallable callable = invocation.getArgument(2, ProgressingCallable.class);
			callable.call(callback);
			return null;
		}).when(mockManagerSupport).tryRunWithTableExclusiveLock(any(), any(IdAndVersion.class), any());
	}
	
	@Test
	public void testBuildTableIndexWithLock() throws Exception {
		when(mockManagerSupport.isIndexWorkRequired(tableId)).thenReturn(true);
		String resetToken = "resetToken";
		when(mockManagerSupport.startTableProcessing(tableId)).thenReturn(resetToken);
		String lastEtag = "etag-1";
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1;
		when(mockManagerSupport.getLastTableChangeNumber(tableId)).thenReturn(Optional.of(targetChangeNumber));
		// call under test
		managerSpy.buildTableIndexWithLock(mockCallback, tableId, iterator);
		verify(mockManagerSupport).attemptToSetTableStatusToAvailable(tableId, resetToken, lastEtag);
		verify(mockManagerSupport).getLastTableChangeNumber(tableId);
		verify(mockManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class), 
				any(Exception.class));
	}
	
	/**
	 * An InvalidStatusTokenException should not cause the table's state to be set to failed.  Instead
	 * the rebuild should be restarted by pushing the message back on the queue.  
	 * @throws Exception
	 */
	@Test
	public void testBuildTableIndexWithLockInvalidStatusTokenException() throws Exception {
		when(mockManagerSupport.isIndexWorkRequired(tableId)).thenReturn(true);
		String resetToken = "resetToken";
		when(mockManagerSupport.startTableProcessing(tableId)).thenReturn(resetToken);
		String lastEtag = "etag-1";
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		long targetChangeNumber = 1;
		when(mockManagerSupport.getLastTableChangeNumber(tableId)).thenReturn(Optional.of(targetChangeNumber));
		
		InvalidStatusTokenException exception = new InvalidStatusTokenException("wrong token");
		doThrow(exception).when(mockManagerSupport).attemptToSetTableStatusToAvailable(tableId, resetToken, lastEtag);
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, ()->{
			// call under test
			managerSpy.buildTableIndexWithLock(mockCallback, tableId, iterator);
		});
		assertEquals(exception, result.getCause());

		verify(mockManagerSupport).attemptToSetTableStatusToAvailable(tableId, resetToken, lastEtag);
		verify(mockManagerSupport).getLastTableChangeNumber(tableId);
		// should not fail
		verify(mockManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class), 
				any(Exception.class));
	}
	
	@Test
	public void testBuildTableIndexWithLockNoSnapshot() throws Exception {
		when(mockManagerSupport.isIndexWorkRequired(tableId)).thenReturn(true);
		String resetToken = "resetToken";
		when(mockManagerSupport.startTableProcessing(tableId)).thenReturn(resetToken);

		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		// No change number for this case.
		when(mockManagerSupport.getLastTableChangeNumber(tableId)).thenReturn(Optional.empty());
		// call under test
		manager.buildTableIndexWithLock(mockCallback, tableId, iterator);
		verify(mockManagerSupport, never()).attemptToSetTableStatusToAvailable(any(IdAndVersion.class), anyString(), anyString());
		verify(mockManagerSupport).getLastTableChangeNumber(tableId);
		// should fail
		verify(mockManagerSupport).attemptToSetTableStatusToFailed(eq(tableId), any(Exception.class));
	}
	
	@Test
	public void testBuildTableIndexWithLockNoWorkNeeded() throws Exception {
		// no work is needed
		when(mockManagerSupport.isIndexWorkRequired(tableId)).thenReturn(false);
		String resetToken = "resetToken";
		String lastEtag = "lastEtag";
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		// call under test
		manager.buildTableIndexWithLock(mockCallback, tableId, iterator);
		verify(mockManagerSupport, never()).startTableProcessing(any(IdAndVersion.class));
		verify(mockManagerSupport, never()).attemptToSetTableStatusToAvailable(tableId, resetToken, lastEtag);
		verify(mockManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class), any(Exception.class));
	}
	
	@Test
	public void testBuildTableIndexWithLockErrorOnIsIndexWorkRequired() throws Exception {
		IllegalArgumentException exception = new IllegalArgumentException("wrong");
		when(mockManagerSupport.isIndexWorkRequired(tableId)).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		// call under test
		manager.buildTableIndexWithLock(mockCallback, tableId, iterator);
		verify(mockManagerSupport).attemptToSetTableStatusToFailed(tableId, exception);
	}
	
	@Test
	public void testBuildTableIndexWithLockWithInvalidState() throws Exception {
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		when(mockManagerSupport.isTableIndexStateInvalid(tableId)).thenReturn(true);
		// call under test
		managerSpy.buildTableIndexWithLock(mockCallback, tableId, iterator);
		// when the state is invalid, the index must get deleted.
		verify(managerSpy).deleteTableIndex(tableId);
	}
	
	@Test
	public void testBuildTableIndexWithLockWithValidState() throws Exception {
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		when(mockManagerSupport.isTableIndexStateInvalid(tableId)).thenReturn(false);
		// call under test
		managerSpy.buildTableIndexWithLock(mockCallback, tableId, iterator);
		// when the state is invalid, the index must get deleted.
		verify(managerSpy, never()).deleteTableIndex(tableId);
	}
	
	/**
	 * The default schema does not contain any list columns.
	 */
	@Test
	public void testPopulateListColumnIndexTables_NoListsColumns() {
		// call under test
		manager.populateListColumnIndexTables(tableId, schema, rowsIdsWithChanges);
		verify(mockIndexDao, never()).populateListColumnIndexTable(any(IdAndVersion.class), any(ColumnModel.class),
				anySet(), anyBoolean());
	}
	
	@Test
	public void testPopulateListColumnIndexTables_WithListColumns() {
		ColumnModel notAList = new ColumnModel();
		notAList.setId("111");
		notAList.setColumnType(ColumnType.STRING);
		
		ColumnModel listOne = new ColumnModel();
		listOne.setId("222");
		listOne.setColumnType(ColumnType.STRING_LIST);
		
		ColumnModel listTwo = new ColumnModel();
		listTwo.setId("333");
		listTwo.setColumnType(ColumnType.STRING_LIST);
		schema = Lists.newArrayList(notAList, listOne, listTwo);
		
		// call under test
		manager.populateListColumnIndexTables(tableId, schema, rowsIdsWithChanges);
		verify(mockIndexDao, never()).populateListColumnIndexTable(tableId, notAList, rowsIdsWithChanges, false);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, listOne, rowsIdsWithChanges, false);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, listTwo, rowsIdsWithChanges, false);
		verifyNoMoreInteractions(mockIndexDao);
	}
	
	@Test
	public void testPopulateListColumnIndexTables_NoChange() {
		// call under test
		managerSpy.populateListColumnIndexTables(tableId, schema);
		// pass null changes
		rowsIdsWithChanges = null;
		verify(managerSpy).populateListColumnIndexTables(tableId, schema, rowsIdsWithChanges);
	}
	
	@Test
	public void testPopulateListColumnIndexTables_NullRowChanges() {
		ColumnModel listOne = new ColumnModel();
		listOne.setId("222");
		listOne.setColumnType(ColumnType.STRING_LIST);
		
		schema = Lists.newArrayList(listOne);
		// null rowID is allowed and means apply to all rows.
		rowsIdsWithChanges = null;
		// call under test
		manager.populateListColumnIndexTables(tableId, schema, rowsIdsWithChanges);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, listOne, null, false);
	}
	
	@Test
	public void testPopulateListColumnIndexTable_NullTableId() {
		tableId = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.populateListColumnIndexTables(tableId, schema, rowsIdsWithChanges);
		});
	}
	
	@Test
	public void testPopulateListColumnIndexTable_NullSchema() {
		schema = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.populateListColumnIndexTables(tableId, schema, rowsIdsWithChanges);
		});
	}
	
	@Test
	public void testUpdateViewRowsInTransaction() {
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getObjectType()).thenReturn(objectType);
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);

		ViewScopeFilter scopeFilter = new ViewScopeFilter(objectType, subTypes, filterByObjectId, scopeIds);
		
		setupExecuteInWriteTransaction();
		Long[] rowsIdsArray = rowsIdsWithChanges.stream().toArray(Long[] ::new); 
		// call under test
		managerSpy.updateViewRowsInTransaction(tableId, rowsIdsWithChanges, scopeType, scopeIds, schema);
		
		verify(mockIndexDao).executeInWriteTransaction(any());
		verify(mockIndexDao).deleteRowsFromViewBatch(tableId, rowsIdsArray);
		verify(mockIndexDao).copyObjectReplicationToView(tableId.getId(), scopeFilter, schema, mockMetadataProvider, rowsIdsWithChanges);
		verify(managerSpy).populateListColumnIndexTables(tableId, schema, rowsIdsWithChanges);
	}
	
	@Test
	public void testUpdateViewRowsInTransaction_ExceptionDuringUpdate() {
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.findMatch(any())).thenReturn(Optional.empty());
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(mockMetadataProvider.getObjectType()).thenReturn(objectType);
		when(mockMetadataProvider.getSubTypesForMask(scopeType.getTypeMask())).thenReturn(subTypes);
		when(mockMetadataProvider.isFilterScopeByObjectId(scopeType.getTypeMask())).thenReturn(filterByObjectId);

		ViewScopeFilter scopeFilter = new ViewScopeFilter(objectType, subTypes, filterByObjectId, scopeIds);
		
		setupExecuteInWriteTransaction();
		// setup an exception on copy
		IllegalArgumentException exception = new IllegalArgumentException("something wrong");
		doThrow(exception).when(mockIndexDao).copyObjectReplicationToView(tableId.getId(), scopeFilter, schema, mockMetadataProvider, rowsIdsWithChanges);
		
		Long[] rowsIdsArray = rowsIdsWithChanges.stream().toArray(Long[] ::new); 
		Exception thrown = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.updateViewRowsInTransaction(tableId, rowsIdsWithChanges, scopeType, scopeIds, schema);
		});
		assertEquals(exception, thrown);

		verify(mockIndexDao).executeInWriteTransaction(any());
		verify(mockIndexDao).deleteRowsFromViewBatch(tableId, rowsIdsArray);
		verify(mockIndexDao).copyObjectReplicationToView(tableId.getId(), scopeFilter, schema, mockMetadataProvider, rowsIdsWithChanges);
		// must attempt to determine the type of exception.
		verify(managerSpy, never()).populateListColumnIndexTables(any(), any(), any());
	}
	
	@Test
	public void testUpdateViewRowsInTransaction_NullTableId() {
		tableId = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateViewRowsInTransaction(tableId, rowsIdsWithChanges, scopeType, scopeIds, schema);
		});
	}
	
	@Test
	public void testUpdateViewRowsInTransaction_Changes() {
		rowsIdsWithChanges = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateViewRowsInTransaction(tableId, rowsIdsWithChanges, scopeType, scopeIds, schema);
		});
	}
	
	@Test
	public void testUpdateViewRowsInTransaction_ViewType() {
		scopeType = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateViewRowsInTransaction(tableId, rowsIdsWithChanges, scopeType, scopeIds, schema);
		});
	}
	
	@Test
	public void testUpdateViewRowsInTransaction_ScopeIds() {
		scopeIds = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateViewRowsInTransaction(tableId, rowsIdsWithChanges, scopeType, scopeIds, schema);
		});
	}
	
	@Test
	public void testUpdateViewRowsInTransaction_Schema() {
		schema = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateViewRowsInTransaction(tableId, rowsIdsWithChanges, scopeType, scopeIds, schema);
		});
	}

	@Test
	public void testListColumnIndexTableChangesFromExpectedSchema_nullExpectedSchema(){
		assertThrows(IllegalArgumentException.class, () ->
			TableIndexManagerImpl.listColumnIndexTableChangesFromExpectedSchema(null, Sets.newHashSet(123L))
		);
	}

	@Test
	public void testListColumnIndexTableChangesFromExpectedSchema_nullExistingIndexListColumns(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("222");
		columnModel.setColumnType(ColumnType.INTEGER_LIST);

		assertThrows(IllegalArgumentException.class, () ->
				TableIndexManagerImpl.listColumnIndexTableChangesFromExpectedSchema(Arrays.asList(columnModel), null)
		);
	}

	@Test
	public void testListColumnIndexTableChangesFromExpectedSchema_addAndRemove(){
		ColumnModel nonList1 = new ColumnModel();
		nonList1.setId("111");
		nonList1.setColumnType(ColumnType.STRING);

		ColumnModel addListCol = new ColumnModel();
		addListCol.setId("222");
		addListCol.setColumnType(ColumnType.INTEGER_LIST);


		long removeListColId = 333;

		long unchangedListColId = 444;
		ColumnModel unchangedListCol = new ColumnModel();
		unchangedListCol.setId(Long.toString(unchangedListColId));
		unchangedListCol.setColumnType(ColumnType.STRING_LIST);


		List<ColumnModel> schema = Arrays.asList(nonList1, addListCol, unchangedListCol);
		Set<Long> existingIndexTableColumns = Sets.newHashSet(removeListColId, unchangedListColId);

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromExpectedSchema(schema, existingIndexTableColumns);


		List<ListColumnIndexTableChange> expected = Arrays.asList(
				ListColumnIndexTableChange.newAddition(addListCol),
				ListColumnIndexTableChange.newRemoval(removeListColId)

		);
		assertEquals(expected, result);

	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_nullChanges(){
		assertThrows(IllegalArgumentException.class, () ->
			TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(null, Sets.newHashSet(123L))
		);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_nulExistingListIndexColumns(){
		assertThrows(IllegalArgumentException.class, () ->
				TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(columnChanges, null)
		);
	}


	@Test
	public void listColumnIndexTableChangesFromChangeDetails_noListColumnChange_NotInExistingTablesSet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		//same column model for old and new
		ColumnChangeDetails noChange = new ColumnChangeDetails(columnModel, columnModel);
		//does not already exist as an index table
		Set<Long> existingColumnChangeIds = Collections.emptySet();

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(noChange), existingColumnChangeIds);
		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newAddition(columnModel));
		assertEquals(expected, result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_noListColumnChange_InExistingTablesSet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		//same column model for old and new
		ColumnChangeDetails noChange = new ColumnChangeDetails(columnModel, columnModel);

		Set<Long> existingColumnChangeIds = Sets.newHashSet(Long.parseLong(columnModel.getId()));

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(noChange), existingColumnChangeIds);
		assertEquals(Collections.emptyList(), result);
	}


	@Test
	public void listColumnIndexTableChangesFromChangeDetails_AddListColumn_NotInExistingTablesSet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		//same column model for old and new
		ColumnChangeDetails addColumn = new ColumnChangeDetails(null, columnModel);
		//does not already exist as an index table
		Set<Long> existingColumnChangeIds = Collections.emptySet();

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(addColumn), existingColumnChangeIds);
		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newAddition(columnModel));
		assertEquals(expected, result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_AddListColumn_InExistingTablesSet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		//same column model for old and new
		ColumnChangeDetails addColumn = new ColumnChangeDetails(null, columnModel);

		Set<Long> existingColumnChangeIds = Sets.newHashSet(Long.parseLong(columnModel.getId()));

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(addColumn), existingColumnChangeIds);
		assertEquals(Collections.emptyList(), result);
	}


	@Test
	public void listColumnIndexTableChangesFromChangeDetails_RemoveListColumn_NotInExistingTablesSet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		//same column model for old and new
		ColumnChangeDetails removeColumn = new ColumnChangeDetails(columnModel, null);
		//does not already exist as an index table
		Set<Long> existingColumnChangeIds = Collections.emptySet();

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(removeColumn), existingColumnChangeIds);
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_RemoveListColumn_InExistingTablesSet(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		//same column model for old and new
		ColumnChangeDetails removeColumn = new ColumnChangeDetails(columnModel, null);
		long columnModelId =Long.parseLong(columnModel.getId());
		Set<Long> existingColumnChangeIds = Sets.newHashSet(columnModelId);

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(removeColumn), existingColumnChangeIds);

		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newRemoval(columnModelId));
		assertEquals(expected, result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_changeListType_oldAndNewColumnInExistingTablesSet(){
		ColumnModel oldList = new ColumnModel();
		oldList.setId("9876");
		oldList.setColumnType(ColumnType.STRING_LIST);
		oldList.setMaximumSize(46L);

		ColumnModel newList = new ColumnModel();
		newList.setId("1234");
		newList.setColumnType(ColumnType.STRING_LIST);
		newList.setMaximumSize(46L);

		long oldListId = Long.parseLong(oldList.getId());
		long newListId = Long.parseLong(newList.getId());

		//same column model for old and new
		ColumnChangeDetails listChange = new ColumnChangeDetails(oldList, newList);
		//does not already exist as an index table
		Set<Long> existingColumnChangeIds = Sets.newHashSet(oldListId,newListId);

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(listChange), existingColumnChangeIds);

		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newRemoval(oldListId));
		assertEquals(expected, result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_changeListType_oldColumnInExistingTablesSet(){
		ColumnModel oldList = new ColumnModel();
		oldList.setId("9876");
		oldList.setColumnType(ColumnType.STRING_LIST);
		oldList.setMaximumSize(46L);

		ColumnModel newList = new ColumnModel();
		newList.setId("1234");
		newList.setColumnType(ColumnType.STRING_LIST);
		newList.setMaximumSize(46L);

		long oldListId = Long.parseLong(oldList.getId());
		long newListId = Long.parseLong(newList.getId());


		//same column model for old and new
		ColumnChangeDetails listChange = new ColumnChangeDetails(oldList, newList);

		Set<Long> existingColumnChangeIds = Sets.newHashSet(oldListId);

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(listChange), existingColumnChangeIds);
		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newUpdate(oldListId, newList));

		assertEquals(expected, result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_changeListType_newColumnInExistingTablesSet(){
		ColumnModel oldList = new ColumnModel();
		oldList.setId("9876");
		oldList.setColumnType(ColumnType.STRING_LIST);
		oldList.setMaximumSize(46L);

		ColumnModel newList = new ColumnModel();
		newList.setId("1234");
		newList.setColumnType(ColumnType.STRING_LIST);
		newList.setMaximumSize(46L);

		long oldListId = Long.parseLong(oldList.getId());
		long newListId = Long.parseLong(newList.getId());

		//same column model for old and new
		ColumnChangeDetails listChange = new ColumnChangeDetails(oldList, newList);
		//does not already exist as an index table
		Set<Long> existingColumnChangeIds = Sets.newHashSet(newListId);

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(listChange), existingColumnChangeIds);
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_changeListType_noColumnInExistingTablesSet(){
		ColumnModel oldList = new ColumnModel();
		oldList.setId("9876");
		oldList.setColumnType(ColumnType.STRING_LIST);
		oldList.setMaximumSize(46L);

		ColumnModel newList = new ColumnModel();
		newList.setId("1234");
		newList.setColumnType(ColumnType.STRING_LIST);
		newList.setMaximumSize(46L);

		long oldListId = Long.parseLong(oldList.getId());
		long newListId = Long.parseLong(newList.getId());

		//same column model for old and new
		ColumnChangeDetails listChange = new ColumnChangeDetails(oldList, newList);

		Set<Long> existingColumnChangeIds = Collections.emptySet();

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(listChange), existingColumnChangeIds);
		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newAddition(newList));

		assertEquals(expected, result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_NonListChanges(){
		ColumnModel oldNonList = new ColumnModel();
		oldNonList.setId("9876");
		oldNonList.setColumnType(ColumnType.INTEGER);
		oldNonList.setMaximumSize(46L);

		ColumnModel newNonList = new ColumnModel();
		newNonList.setId("1234");
		newNonList.setColumnType(ColumnType.STRING);
		newNonList.setMaximumSize(46L);

		long oldListId = Long.parseLong(oldNonList.getId());
		long newListId = Long.parseLong(newNonList.getId());

		//same column model for old and new
		List<ColumnChangeDetails> nonListChanges = Arrays.asList(
				new ColumnChangeDetails(oldNonList, newNonList),
				new ColumnChangeDetails(oldNonList, null),
				new ColumnChangeDetails(null, newNonList)
		);

		Set<Long> existingColumnChangeIds = Collections.emptySet();

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(nonListChanges, existingColumnChangeIds);

		assertEquals(Collections.emptyList(), result);
	}


	@Test
	public void listColumnIndexTableChangesFromChangeDetails_multipleChanges(){
		ColumnModel oldList = new ColumnModel();
		oldList.setId("9876");
		oldList.setColumnType(ColumnType.STRING_LIST);
		oldList.setMaximumSize(46L);

		ColumnModel newList = new ColumnModel();
		newList.setId("1234");
		newList.setColumnType(ColumnType.STRING_LIST);
		newList.setMaximumSize(46L);

		long oldListId = Long.parseLong(oldList.getId());
		long newListId = Long.parseLong(newList.getId());


		//same column model for old and new
		List<ColumnChangeDetails> listChanges = Arrays.asList(
				new ColumnChangeDetails(oldList, null),
				new ColumnChangeDetails(null, newList)
		);

		Set<Long> existingColumnChangeIds = Sets.newHashSet(oldListId);

		//method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(listChanges, existingColumnChangeIds);
		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newRemoval(oldListId),
				ListColumnIndexTableChange.newAddition(newList));

		assertEquals(expected, result);
	}


	@Test
	public void applyListColumnIndexTableChanges_add(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);
		ListColumnIndexTableChange addChange = ListColumnIndexTableChange.newAddition(columnModel);

		boolean alterTemp = false;

		//method under test
		manager.applyListColumnIndexTableChanges(tableId, Collections.singletonList(addChange), alterTemp);

		verify(mockIndexDao).createMultivalueColumnIndexTable(tableId, columnModel, alterTemp);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, columnModel, null, alterTemp);
	}


	@Test
	public void applyListColumnIndexTableChanges_remove(){
		long columnIdToRemove = 1234L;
		ListColumnIndexTableChange removeChange = ListColumnIndexTableChange.newRemoval(columnIdToRemove);

		boolean alterTemp = false;

		//method under test
		manager.applyListColumnIndexTableChanges(tableId, Collections.singletonList(removeChange), alterTemp);

		verify(mockIndexDao).deleteMultivalueColumnIndexTable(tableId, columnIdToRemove, alterTemp);
	}


	@Test
	public void applyListColumnIndexTableChanges_update(){
		long oldColumnId = 1234L;
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);


		ListColumnIndexTableChange addChange = ListColumnIndexTableChange.newUpdate(oldColumnId, columnModel);

		boolean alterTemp = false;
		//method under test
		manager.applyListColumnIndexTableChanges(tableId, Collections.singletonList(addChange), alterTemp);

		verify(mockIndexDao).updateMultivalueColumnIndexTable(tableId, oldColumnId, columnModel, alterTemp);
	}

	@Test
	public void applyListColumnIndexTableChanges_multipleChanges(){

		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);


		ListColumnIndexTableChange addChange = ListColumnIndexTableChange.newAddition(columnModel);

		long columnIdToRemove = 456L;
		ListColumnIndexTableChange removeChange = ListColumnIndexTableChange.newRemoval(columnIdToRemove);
		boolean alterTemp = false;
		//method under test
		manager.applyListColumnIndexTableChanges(tableId, Arrays.asList(addChange, removeChange), alterTemp);

		verify(mockIndexDao).createMultivalueColumnIndexTable(tableId, columnModel, alterTemp);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, columnModel, null, alterTemp);
		verify(mockIndexDao).deleteMultivalueColumnIndexTable(tableId, columnIdToRemove, alterTemp);
	}

	@Test
	public void testValidateTableMaximumListLengthChanges_noListTypes(){
		ColumnModel oldCol = new ColumnModel();
		oldCol.setId("5");
		oldCol.setName("old");
		oldCol.setColumnType(ColumnType.STRING);
		ColumnModel newCol = new ColumnModel();
		newCol.setId("56");
		newCol.setName("new");
		newCol.setColumnType(ColumnType.STRING);

		ColumnChangeDetails change = new ColumnChangeDetails(oldCol, newCol);

		// method under test
		manager.validateTableMaximumListLengthChanges(tableId, change);

		verify(mockIndexDao,never()).tempTableListColumnMaxLength(any(),any());
	}

	@Test
	public void testValidateTableMaximumListLengthChanges_bothListTypes_ListLengthNewGreaterThanOrEqualOld(){
		ColumnModel oldCol = new ColumnModel();
		oldCol.setId("5");
		oldCol.setName("old");
		oldCol.setColumnType(ColumnType.STRING_LIST);
		oldCol.setMaximumListLength(4L);
		ColumnModel newCol = new ColumnModel();
		newCol.setId("56");
		newCol.setName("new");
		newCol.setColumnType(ColumnType.STRING_LIST);
		newCol.setMaximumListLength(14L);

		ColumnChangeDetails change = new ColumnChangeDetails(oldCol, newCol);

		// method under test
		manager.validateTableMaximumListLengthChanges(tableId, change);

		verify(mockIndexDao,never()).tempTableListColumnMaxLength(any(),any());
	}

	@Test
	public void testValidateTableMaximumListLengthChanges_bothListTypes_ListLengthNewLessThanOld_underExistingTableMaxLength(){
		ColumnModel oldCol = new ColumnModel();
		oldCol.setId("5");
		oldCol.setName("old");
		oldCol.setColumnType(ColumnType.STRING_LIST);
		oldCol.setMaximumListLength(4L);
		ColumnModel newCol = new ColumnModel();
		newCol.setId("56");
		newCol.setName("new");
		newCol.setColumnType(ColumnType.STRING_LIST);
		newCol.setMaximumListLength(2L);

		ColumnChangeDetails change = new ColumnChangeDetails(oldCol, newCol);

		when(mockIndexDao.tempTableListColumnMaxLength(tableId, oldCol.getId())).thenReturn(2L);

		// method under test
		manager.validateTableMaximumListLengthChanges(tableId, change);

		verify(mockIndexDao).tempTableListColumnMaxLength(tableId,oldCol.getId());
	}


	@Test
	public void testValidateTableMaximumListLengthChanges_bothListTypes_ListLengthNewLessThanOld_aboveExistingTableMaxLength(){
		ColumnModel oldCol = new ColumnModel();
		oldCol.setId("5");
		oldCol.setName("old");
		oldCol.setColumnType(ColumnType.STRING_LIST);
		oldCol.setMaximumListLength(4L);
		ColumnModel newCol = new ColumnModel();
		newCol.setId("56");
		newCol.setName("new");
		newCol.setColumnType(ColumnType.STRING_LIST);
		newCol.setMaximumListLength(2L);

		ColumnChangeDetails change = new ColumnChangeDetails(oldCol, newCol);

		when(mockIndexDao.tempTableListColumnMaxLength(tableId, oldCol.getId())).thenReturn(3L);

		String errorMessage = assertThrows(IllegalArgumentException.class, () ->
				// method under test
				manager.validateTableMaximumListLengthChanges(tableId, change)
		).getMessage();

		assertEquals("maximumListLength for ColumnModel \"new\" must be at least: 3", errorMessage);


		verify(mockIndexDao).tempTableListColumnMaxLength(tableId,oldCol.getId());
	}
	
	@Test
	public void testDetermineCauseOfExceptionLists() {
		Exception original = new Exception("Some exception");
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName("foo");
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setMaximumSize(10L);

		ColumnModel annotationModel = new ColumnModel();
		annotationModel.setName("foo");
		annotationModel.setColumnType(ColumnType.STRING);
		annotationModel.setMaximumSize(11L);
		
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.findMatch(any())).thenReturn(Optional.empty());
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any(), any())).thenReturn(ImmutableList.of(annotationModel));
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		ViewScopeFilter scopeFilter = new ViewScopeFilter(objectType, subTypes, filterByObjectId, scopeIds);
		
		List<ColumnModel> schema = ImmutableList.of(columnModel);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.determineCauseOfReplicationFailure(original, schema, mockMetadataProvider, scope.getViewTypeMask(), scopeFilter);
		});
		
		assertEquals(original, ex.getCause());
		assertEquals("The size of the column 'foo' is too small.  The column size needs to be at least 11 characters.", ex.getMessage());
	}

	@Test
	public void testDetermineCauseOfExceptionListsMultipleValues() {
		Exception original = new Exception("Some exception");
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName("foo");
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setMaximumSize(10L);
		// type does not match.
		ColumnModel a1 = new ColumnModel();
		a1.setName("foo");
		a1.setColumnType(ColumnType.INTEGER);

		ColumnModel a2 = new ColumnModel();
		a2.setName("foo");
		a2.setColumnType(ColumnType.STRING);
		a2.setMaximumSize(11L);

		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.findMatch(any())).thenReturn(Optional.empty());
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any(), any())).thenReturn(ImmutableList.of(a1, a2));
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		ViewScopeFilter scopeFilter = new ViewScopeFilter(objectType, subTypes, filterByObjectId, scopeIds);
		
		List<ColumnModel> schema = ImmutableList.of(columnModel);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.determineCauseOfReplicationFailure(original, schema, mockMetadataProvider, scope.getViewTypeMask(), scopeFilter);
		});
		
		assertEquals(original, ex.getCause());
		assertEquals("The size of the column 'foo' is too small.  The column size needs to be at least 11 characters.", ex.getMessage());
	}

	@Test
	public void testRefreshViewBenefactors() {
		IdAndVersion viewId = IdAndVersion.parse("syn123");
		when(mockManagerSupport.getViewScopeType(any())).thenReturn(scopeType);
		// call under test
		manager.refreshViewBenefactors(viewId);
		verify(mockManagerSupport).getViewScopeType(viewId);
		verify(mockIndexDao).refreshViewBenefactors(viewId, scopeType.getObjectType());
	}
	
	@Test
	public void testRefreshViewBenefactorsNullId() {
		IdAndVersion viewId = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.refreshViewBenefactors(viewId);
		});
	}

	
	@SuppressWarnings("unchecked")
	public void setupExecuteInWriteTransaction() {
		// When a write transaction callback is used, we need to call the callback.
		doAnswer(new Answer<Void>(){
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				TransactionCallback callback = (TransactionCallback) invocation.getArguments()[0];
				callback.doInTransaction(mockTransactionStatus);
				return null;
			}}).when(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
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
		TestTableChangeMetaData<SparseChangeSet> testChange = new TestTableChangeMetaData<>();
		testChange.setChangeNumber(changeNumber);
		testChange.seteTag("etag-"+changeNumber);
		testChange.setChangeType(TableChangeType.ROW);
		ChangeData<SparseChangeSet> change = new ChangeData<SparseChangeSet>(changeNumber, sparseChangeSet);
		testChange.setChangeData(change);
		return testChange;
	}
	
	/**
	 * Helper to setup a mock TableChangeMetaData for a Column change.
	 * @return
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	public TableChangeMetaData setupMockColumnChange(long changeNumber) throws NotFoundException, IOException {
		TestTableChangeMetaData<SchemaChange> testChange = new TestTableChangeMetaData<>();
		testChange.setChangeNumber(changeNumber);
		testChange.seteTag("etag-"+changeNumber);
		testChange.setChangeType(TableChangeType.COLUMN);
		SchemaChange schemaChange = new SchemaChange(columnChanges);
		ChangeData<SchemaChange> change = new ChangeData<SchemaChange>(changeNumber, schemaChange);
		testChange.setChangeData(change);
		return testChange;
	}
	
	/**
	 * Create the default EntityField schema with IDs for each column.
	 * 
	 * @return
	 */
	public List<ColumnModel> createDefaultColumnsWithIds() {
		List<ColumnModel> schema = objectFieldModelResolver.getAllColumnModels();

		for(int i=0; i<schema.size(); i++){
			ColumnModel cm = schema.get(i);
			cm.setId(""+i);
		}
		
		return schema;
	}
	
	public List<ColumnModel> createDefaultColumnsWithIds(ObjectField exclude) {
		return createDefaultColumnsWithIds()
			.stream()
			.filter( model -> !model.getName().equals(exclude.name()))
			.collect(Collectors.toList());
	}
	
	
	
}
