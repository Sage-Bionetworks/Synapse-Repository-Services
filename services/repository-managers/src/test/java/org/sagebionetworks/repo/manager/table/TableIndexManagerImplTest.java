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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import java.util.stream.LongStream;

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
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.dbo.dao.table.InvalidStatusTokenException;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.DatabaseColumnInfo;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverImpl;
import org.sagebionetworks.table.cluster.search.RowSearchContent;
import org.sagebionetworks.table.cluster.search.TableRowData;
import org.sagebionetworks.table.cluster.search.TableRowSearchProcessor;
import org.sagebionetworks.table.cluster.search.TypedCellValue;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.sagebionetworks.table.cluster.view.filter.ViewFilterBuilder;
import org.sagebionetworks.table.model.ChangeData;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.model.SchemaChange;
import org.sagebionetworks.table.model.SearchChange;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.model.SparseRow;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class TableIndexManagerImplTest {

	@Mock
	private TableIndexDAO mockIndexDao;
	@Mock
	private TransactionStatus mockTransactionStatus;
	@Mock
	private TableManagerSupport mockManagerSupport;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private MetadataIndexProviderFactory mockMetadataProviderFactory;
	@Mock
	private MetadataIndexProvider mockMetadataProvider;
	@Mock
	private DefaultColumnModel mockDefaultColumnModel;
	@Mock
	private ObjectFieldModelResolverFactory mockObjectFieldModelResolverFactory;
	@Mock
	private ObjectFieldModelResolver mockObjectFieldModelResolver;
	@Mock
	private TableRowSearchProcessor mockSearchProcessor;
	@Mock
	private ViewFilter mockFilter;
	@Mock
	private ViewFilterBuilder mockFilterBuilder;
	@Mock
	private ViewFilter mockNewFilter;

	@Captor
	ArgumentCaptor<List<ColumnChangeDetails>> changeCaptor;
	
	@Captor
	ArgumentCaptor<Long> longCaptor;

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
	ObjectFieldModelResolver objectFieldModelResolver;

	@BeforeEach
	public void before() throws Exception {

		objectType = ViewObjectType.ENTITY;
		tableId = IdAndVersion.parse("syn123");
		manager = new TableIndexManagerImpl(mockIndexDao, mockManagerSupport, mockMetadataProviderFactory,
				mockObjectFieldModelResolverFactory, mockSearchProcessor);
		managerSpy = Mockito.spy(manager);
		versionNumber = 99L;
		schema = Arrays.asList(TableModelTestUtils.createColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createColumn(101L, "aFile", ColumnType.FILEHANDLEID));
		schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(TableModelUtils.getIds(schema));
		selectColumns = Arrays.asList(TableModelTestUtils.createSelectColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createSelectColumn(101L, "aFile", ColumnType.FILEHANDLEID));

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

		containerIds = Sets.newHashSet(1l, 2L, 3L);
		limit = 10L;
		offset = 0L;
		nextPageToken = new NextPageToken(limit, offset);
		tokenString = nextPageToken.toToken();
		scopeSynIds = Lists.newArrayList("syn123", "syn345");
		scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scopeSynIds));

		scope = new ViewScope();
		scope.setScope(scopeSynIds);
		scope.setViewTypeMask(ViewTypeMask.File.getMask());
		scope.setViewEntityType(ViewEntityType.entityview);

		ColumnModel oldColumn = null;
		newColumn = new ColumnModel();
		newColumn.setId("12");
		columnChanges = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));

		rowsIdsWithChanges = Sets.newHashSet(444L, 555L);
		scopeType = new ViewScopeType(objectType, ViewTypeMask.File.getMask());

		objectFieldModelResolver = new ObjectFieldModelResolverImpl(mockMetadataProvider);
	}

	@Test
	public void testNullDao() {
		assertThrows(IllegalArgumentException.class, () -> {
			new TableIndexManagerImpl(null, mockManagerSupport, mockMetadataProviderFactory,
					mockObjectFieldModelResolverFactory, mockSearchProcessor);
		});
	}

	@Test
	public void testNullSupport() {
		assertThrows(IllegalArgumentException.class, () -> {
			new TableIndexManagerImpl(mockIndexDao, null, mockMetadataProviderFactory,
					mockObjectFieldModelResolverFactory, mockSearchProcessor);
		});
	}

	@Test
	public void testNullProviderFactory() {
		assertThrows(IllegalArgumentException.class, () -> {
			new TableIndexManagerImpl(mockIndexDao, mockManagerSupport, null, mockObjectFieldModelResolverFactory, mockSearchProcessor);
		});
	}

	@Test
	public void testNullObjectFieldFactory() {
		assertThrows(IllegalArgumentException.class, () -> {
			new TableIndexManagerImpl(mockIndexDao, mockManagerSupport, mockMetadataProviderFactory, null, mockSearchProcessor);
		});
	}
	
	@Test
	public void testNullSSearchProcessor() {
		assertThrows(IllegalArgumentException.class, () -> {
			new TableIndexManagerImpl(mockIndexDao, mockManagerSupport, mockMetadataProviderFactory, mockObjectFieldModelResolverFactory, null);
		});
	}

	@Test
	public void testApplyChangeSetToIndexHappy() {
		setupExecuteInWriteTransaction();
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);

		// call under test.
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
		verify(mockIndexDao, never()).updateSearchIndex(any(), any());
	}

	@Test
	public void testApplyChangeSetToIndexAlreadyApplied() {
		// For this case the index already has this change set applied.
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber + 1);
		// call under test.
		manager.applyChangeSetToIndex(tableId, sparseChangeSet, versionNumber);
		// nothing do do.
		verify(mockIndexDao, never()).executeInWriteTransaction(any(TransactionCallback.class));
		verify(mockIndexDao, never()).createOrUpdateOrDeleteRows(any(IdAndVersion.class), any(Grouping.class));
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(any(IdAndVersion.class), anySet());
		verify(mockIndexDao, never()).setMaxCurrentCompleteVersionForTable(any(IdAndVersion.class), anyLong());
		verify(mockIndexDao, never()).updateSearchIndex(any(), any());
	}

	@Test
	public void testApplyChangeSetToIndexNoFiles() {
		setupExecuteInWriteTransaction();
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		// no files in the schema
		schema = Arrays.asList(TableModelTestUtils.createColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createColumn(101L, "moreStrings", ColumnType.STRING));
		selectColumns = Arrays.asList(TableModelTestUtils.createSelectColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createSelectColumn(101L, "moreStrings", ColumnType.STRING));
		sparseChangeSet = new SparseChangeSet(tableId.toString(), schema);
		SparseRow row = sparseChangeSet.addEmptyRow();
		row.setRowId(0L);
		row.setCellValue("99", "some string");

		// call under test.
		manager.applyChangeSetToIndex(tableId, sparseChangeSet, versionNumber);
		// All changes should be executed in a transaction
		verify(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		// change should be written to the index
		verify(mockIndexDao).createOrUpdateOrDeleteRows(tableId, groupOne);
		// there are no files
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(any(IdAndVersion.class), anySet());
		// The new version should be set
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, versionNumber);
		verify(mockIndexDao, never()).updateSearchIndex(any(), any());
	}

	@Test
	public void testApplyChangeSetToIndexPopulateListColumns() {
		setupExecuteInWriteTransaction();
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		// no files in the schema
		schema = Arrays.asList(TableModelTestUtils.createColumn(99L, "strList", ColumnType.STRING_LIST),
				TableModelTestUtils.createColumn(101L, "intList", ColumnType.INTEGER_LIST));

		sparseChangeSet = new SparseChangeSet(tableId.toString(), schema);
		SparseRow row = sparseChangeSet.addEmptyRow();
		row.setRowId(0L);
		row.setCellValue("99", "[\"some string\", \"some other string\"]");
		row.setCellValue("101", "[1,2,3,4]");

		SparseRow row2 = sparseChangeSet.addEmptyRow();
		row2.setRowId(5L);
		row2.setCellValue("99", "[\"some string\", \"some other string\"]");
		row2.setCellValue("101", "[1,2,3,4]");

		// call under test.
		manager.applyChangeSetToIndex(tableId, sparseChangeSet, versionNumber);
		// All changes should be executed in a transaction
		verify(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		// change should be written to the index
		verify(mockIndexDao).createOrUpdateOrDeleteRows(eq(tableId), any(Grouping.class));
		// there are no files
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(any(IdAndVersion.class), anySet());
		// The new version should be set
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, versionNumber);

		Set<Long> expectedRows = Sets.newHashSet(0L, 5L);
		verify(mockIndexDao).deleteFromListColumnIndexTable(tableId, schema.get(0), expectedRows);
		verify(mockIndexDao).deleteFromListColumnIndexTable(tableId, schema.get(1), expectedRows);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, schema.get(0), expectedRows, false);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, schema.get(1), expectedRows, false);
		verify(mockIndexDao, never()).updateSearchIndex(any(), any());
	}
	
	@Test
	public void testApplyChangeSetToIndexWithSearchEnabled() {
		setupExecuteInWriteTransaction();
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		when(mockIndexDao.isSearchEnabled(tableId)).thenReturn(true);
		
		// no files in the schema
		schema = Arrays.asList(
				TableModelTestUtils.createColumn(99L, "strList", ColumnType.STRING_LIST),
				TableModelTestUtils.createColumn(101L, "str", ColumnType.STRING),
				TableModelTestUtils.createColumn(102L, "integer", ColumnType.INTEGER)
		);

		sparseChangeSet = new SparseChangeSet(tableId.toString(), schema);
		SparseRow row = sparseChangeSet.addEmptyRow();
		row.setRowId(0L);
		row.setCellValue("99", "[\"some string\", \"some other string\"]");
		row.setCellValue("101", "some value");

		SparseRow row2 = sparseChangeSet.addEmptyRow();
		row2.setRowId(5L);
		row2.setCellValue("101", "some value");

		SparseRow row3 = sparseChangeSet.addEmptyRow();
		row3.setRowId(6L);
		row3.setCellValue("102", "1");
		
		TableRowData rowData = new TableRowData(102L, Arrays.asList(new TypedCellValue(schema.get(0).getColumnType(), "some data")));
		RowSearchContent searchRowContent = new RowSearchContent(102L, "processed row");
		
		when(mockIndexDao.getTableDataForRowIds(any(), any(), any())).thenReturn(Arrays.asList(rowData, rowData));
		when(mockSearchProcessor.process(any(), anyBoolean())).thenReturn(searchRowContent.getSearchContent());
		
		
		// call under test.
		manager.applyChangeSetToIndex(tableId, sparseChangeSet, versionNumber);
		
		// All changes should be executed in a transaction
		verify(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		// change should be written to the index
		verify(mockIndexDao, times(3)).createOrUpdateOrDeleteRows(eq(tableId), any(Grouping.class));
		// there are no files
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(any(IdAndVersion.class), anySet());
		verify(mockIndexDao).getTableDataForRowIds(tableId, Arrays.asList(schema.get(0), schema.get(1)), ImmutableSet.of(0L, 5L));
		verify(mockSearchProcessor, times(2)).process(rowData, false);
		verify(mockIndexDao).updateSearchIndex(tableId, Arrays.asList(searchRowContent, searchRowContent));
		// The new version should be set
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, versionNumber);
	}
	
	@Test
	public void testApplyChangeSetToIndexWithSearchEnabledAndNoUpdate() {
		setupExecuteInWriteTransaction();
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		when(mockIndexDao.isSearchEnabled(tableId)).thenReturn(true);
		// no files in the schema
		schema = Arrays.asList(
				TableModelTestUtils.createColumn(99L, "integer", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(101L, "double", ColumnType.DOUBLE),
				TableModelTestUtils.createColumn(102L, "integerList", ColumnType.INTEGER_LIST)
		);

		sparseChangeSet = new SparseChangeSet(tableId.toString(), schema);
		SparseRow row = sparseChangeSet.addEmptyRow();
		row.setRowId(0L);
		row.setCellValue("99", "1");
		row.setCellValue("101", "1.0");

		SparseRow row2 = sparseChangeSet.addEmptyRow();
		row2.setRowId(5L);
		row2.setCellValue("101", "2.0");

		SparseRow row3 = sparseChangeSet.addEmptyRow();
		row3.setRowId(6L);
		row3.setCellValue("102", "[1, 2, 3]");
		
		// call under test.
		manager.applyChangeSetToIndex(tableId, sparseChangeSet, versionNumber);
		
		// All changes should be executed in a transaction
		verify(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		// change should be written to the index
		verify(mockIndexDao, times(3)).createOrUpdateOrDeleteRows(eq(tableId), any(Grouping.class));
		// there are no files
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(any(IdAndVersion.class), anySet());
		
		verify(mockIndexDao, never()).updateSearchIndex(any(), any());
		// The new version should be set
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, versionNumber);
	}

	@Test
	public void testSetIndexSchemaWithColumns() {
		ColumnModel column = new ColumnModel();
		column.setId("44");
		column.setColumnType(ColumnType.BOOLEAN);
		schema = Lists.newArrayList(column);

		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C44_");
		info.setColumnType(ColumnType.BOOLEAN);

		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(Lists.newArrayList(info));
		when(mockIndexDao.alterTableAsNeeded(any(IdAndVersion.class), anyList(), anyBoolean())).thenReturn(true);
		// call under test
		manager.setIndexSchema(new TableIndexDescription(tableId), schema);
		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(Lists.newArrayList(column.getId()));
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}

	@Test
	public void testSetIndexSchemaWithNoColumns() {
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(new LinkedList<DatabaseColumnInfo>());
		when(mockIndexDao.alterTableAsNeeded(any(IdAndVersion.class), anyList(), anyBoolean())).thenReturn(true);
		// call under test
		manager.setIndexSchema(new TableIndexDescription(tableId), new LinkedList<ColumnModel>());
		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(new LinkedList<>());
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}

	@Test
	public void testSetIndexSchemaWithListColumns() {
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
		// call under test
		manager.setIndexSchema(new TableIndexDescription(tableId), schema);
		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(Lists.newArrayList(column.getId()));
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
		verify(mockIndexDao).getMultivalueColumnIndexTableColumnIds(tableId);
		verify(mockIndexDao).createMultivalueColumnIndexTable(tableId, column, false);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, column, null, false);
	}

	@Test
	public void testIsVersionAppliedToIndexNoVersionApplied() {
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		versionNumber = 1L;
		assertFalse(manager.isVersionAppliedToIndex(tableId, versionNumber));
	}

	@Test
	public void testIsVersionAppliedToIndexVersionMatches() {
		versionNumber = 1L;
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber);
		assertTrue(manager.isVersionAppliedToIndex(tableId, versionNumber));
	}

	@Test
	public void testIsVersionAppliedToIndexVersionGreater() {
		versionNumber = 1L;
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber + 1);
		assertTrue(manager.isVersionAppliedToIndex(tableId, versionNumber));
	}

	@Test
	public void testDeleteTableIndex() {
		manager.deleteTableIndex(tableId);
		verify(mockIndexDao).deleteTable(tableId);
	}

	@Test
	public void testOptimizeTableIndices() {
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
	public void testUpdateTableSchemaAddColumn() {
		boolean alterTemp = false;
		when(mockIndexDao.alterTableAsNeeded(tableId, columnChanges, alterTemp)).thenReturn(true);
		String existingColumnId = "11";
		DatabaseColumnInfo existingColumn = new DatabaseColumnInfo();
		existingColumn.setColumnName("_C" + existingColumnId + "_");
		existingColumn.setColumnType(ColumnType.BOOLEAN);

		DatabaseColumnInfo createdColumn = new DatabaseColumnInfo();
		createdColumn.setColumnName("_C12_");
		createdColumn.setColumnType(ColumnType.BOOLEAN);
		when(mockIndexDao.getDatabaseInfo(tableId))
				// first time called we only have 1 existing column
				.thenReturn(Collections.singletonList(existingColumn))
				// on the second time, our new column has been added
				.thenReturn(Arrays.asList(existingColumn, createdColumn));
		doNothing().when(managerSpy).createTableIfDoesNotExist(any());
		// call under test
		managerSpy.updateTableSchema(new TableIndexDescription(tableId), columnChanges);
		verify(managerSpy).createTableIfDoesNotExist(new TableIndexDescription(tableId));
		// The new schema is not empty so do not truncate.
		verify(mockIndexDao, never()).truncateTable(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);

		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(Arrays.asList(existingColumnId, newColumn.getId()));
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
		verify(mockIndexDao, never()).updateSearchIndex(any(), any());
	}

	@Test
	public void testUpdateTableSchemaRemoveAllColumns() {
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
		doNothing().when(managerSpy).createTableIfDoesNotExist(any());
		// call under test
		managerSpy.updateTableSchema(new ViewIndexDescription(tableId, EntityType.entityview), changes);
		verify(managerSpy).createTableIfDoesNotExist(new ViewIndexDescription(tableId, EntityType.entityview));
		verify(mockIndexDao, times(2)).getDatabaseInfo(tableId);
		// The new schema is empty so the table is truncated.
		verify(mockIndexDao).truncateTable(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, changes, alterTemp);

		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(new LinkedList<>());
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
		verify(mockIndexDao, never()).updateSearchIndex(any(), any());
	}

	@Test
	public void testUpdateTableSchemaNoChange() {
		List<ColumnChangeDetails> changes = new LinkedList<ColumnChangeDetails>();
		boolean alterTemp = false;
		when(mockIndexDao.alterTableAsNeeded(tableId, changes, alterTemp)).thenReturn(false);
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(new LinkedList<DatabaseColumnInfo>());
		doNothing().when(managerSpy).createTableIfDoesNotExist(any());
		// call under test
		managerSpy.updateTableSchema(new ViewIndexDescription(tableId, EntityType.entityview), changes);
		verify(managerSpy).createTableIfDoesNotExist(new ViewIndexDescription(tableId, EntityType.entityview));
		verify(mockIndexDao).alterTableAsNeeded(tableId, changes, alterTemp);
		verify(mockIndexDao).getDatabaseInfo(tableId);
		verify(mockIndexDao, never()).truncateTable(tableId);
		verify(mockIndexDao, never()).setCurrentSchemaMD5Hex(any(IdAndVersion.class), anyString());
		verify(mockIndexDao, never()).updateSearchIndex(any(), any());
	}
	
	@Test
	public void testUpdateTableSchemaWithSearchEnabledAndNoReindex() {
		boolean alterTemp = false;
		when(mockIndexDao.alterTableAsNeeded(tableId, columnChanges, alterTemp)).thenReturn(true);
		String existingColumnId = "11";
		DatabaseColumnInfo existingColumn = new DatabaseColumnInfo();
		existingColumn.setColumnName("_C" + existingColumnId + "_");
		existingColumn.setColumnType(ColumnType.BOOLEAN);

		DatabaseColumnInfo createdColumn = new DatabaseColumnInfo();
		createdColumn.setColumnName("_C12_");
		createdColumn.setColumnType(ColumnType.STRING);
		when(mockIndexDao.getDatabaseInfo(tableId))
				// first time called we only have 1 existing column
				.thenReturn(Collections.singletonList(existingColumn))
				// on the second time, our new column has been added
				.thenReturn(Arrays.asList(existingColumn, createdColumn));
		doNothing().when(managerSpy).createTableIfDoesNotExist(any());
		// Simulate a table with search enabled
		when(mockIndexDao.isSearchEnabled(any())).thenReturn(true);
		
		// call under test
		managerSpy.updateTableSchema(new TableIndexDescription(tableId), columnChanges);
		
		verify(managerSpy).createTableIfDoesNotExist(new TableIndexDescription(tableId));
		// The new schema is not empty so do not truncate.
		verify(mockIndexDao, never()).truncateTable(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);

		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(Arrays.asList(existingColumnId, newColumn.getId()));
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
		verify(mockIndexDao).isSearchEnabled(tableId);
		verify(mockIndexDao, never()).updateSearchIndex(any(), any());
	}
	
	@Test
	public void testUpdateTableSchemaWithSearchEnabledAndReindex() {
		ColumnModel oldColumn = new ColumnModel().setId("11").setColumnType(ColumnType.INTEGER);
		ColumnModel newColumn = new ColumnModel().setId("11").setColumnType(ColumnType.STRING);
		
		columnChanges = Arrays.asList(new ColumnChangeDetails(oldColumn, newColumn));
		
		boolean alterTemp = false;
		when(mockIndexDao.alterTableAsNeeded(tableId, columnChanges, alterTemp)).thenReturn(true);
		DatabaseColumnInfo existingColumn = new DatabaseColumnInfo();
		existingColumn.setColumnName("_C11_");
		existingColumn.setColumnType(ColumnType.INTEGER);

		DatabaseColumnInfo updatedColumn = new DatabaseColumnInfo();
		updatedColumn.setColumnName("_C11_");
		updatedColumn.setColumnType(ColumnType.STRING);
		
		when(mockIndexDao.getDatabaseInfo(tableId))
				// first time called we only have 1 existing column
				.thenReturn(Collections.singletonList(existingColumn))
				// on the second time, our column has been updated
				.thenReturn(Arrays.asList(updatedColumn));
		doNothing().when(managerSpy).createTableIfDoesNotExist(any());
		doNothing().when(managerSpy).updateSearchIndex(any());
		// Simulate a table with search enabled
		when(mockIndexDao.isSearchEnabled(any())).thenReturn(true);
		
		IndexDescription indexDescription = new TableIndexDescription(tableId);
		
		// call under test
		managerSpy.updateTableSchema(indexDescription, columnChanges);
		
		verify(managerSpy).createTableIfDoesNotExist(new TableIndexDescription(tableId));
		
		// The new schema is not empty so do not truncate.
		verify(mockIndexDao, never()).truncateTable(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);

		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(Arrays.asList("11"));
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
		verify(mockIndexDao).isSearchEnabled(tableId);
		verify(managerSpy).updateSearchIndex(indexDescription);
	}

	@Test
	public void testCreateTemporaryTableCopy_noMultiValueColumnIndexTables() {
		// call under test
		manager.createTemporaryTableCopy(tableId);
		verify(mockIndexDao).createTemporaryTable(tableId);
		verify(mockIndexDao).copyAllDataToTemporaryTable(tableId);
		verify(mockIndexDao).getMultivalueColumnIndexTableColumnIds(tableId);
		verify(mockIndexDao, never()).createTemporaryMultiValueColumnIndexTable(any(IdAndVersion.class), anyString());
		verify(mockIndexDao, never()).copyAllDataToTemporaryMultiValueColumnIndexTable(any(IdAndVersion.class),
				anyString());
	}

	@Test
	public void testCreateTemporaryTableCopy_hasMultiValueColumnIndexTables() {
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
	public void testDeleteTemporaryTableCopy() {
		// call under test
		manager.deleteTemporaryTableCopy(tableId);
		verify(mockIndexDao).deleteTemporaryTable(tableId);
		verify(mockIndexDao).deleteAllTemporaryMultiValueColumnIndexTable(tableId);
	}

	@Test
	public void testPopulateViewFromEntityReplication() {
		when(mockIndexDao.calculateCRC32ofTableView(any(Long.class))).thenReturn(crc32);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getViewFilter(tableId.getId())).thenReturn(mockFilter);
		when(mockIndexDao.isSearchEnabled(any())).thenReturn(false);
		
		List<ColumnModel> schema = createDefaultColumnsWithIds();

		// call under test
		Long resultCrc = managerSpy.populateViewFromEntityReplication(tableId.getId(), scopeType, schema);
		assertEquals(crc32, resultCrc);
		verify(mockIndexDao).copyObjectReplicationToView(tableId.getId(), mockFilter, schema, mockMetadataProvider);
		verify(mockIndexDao, never()).updateSearchIndex(any(), any());
		// the CRC should be calculated with the etag column.
		verify(mockIndexDao).calculateCRC32ofTableView(tableId.getId());
	}
	
	@Test
	public void testPopulateViewFromEntityReplicationWithSearchEnabled() {
		when(mockIndexDao.calculateCRC32ofTableView(any(Long.class))).thenReturn(crc32);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getViewFilter(tableId.getId())).thenReturn(mockFilter);
		when(mockIndexDao.isSearchEnabled(any())).thenReturn(true);
		IndexDescription indexDescription = new ViewIndexDescription(tableId, EntityType.entityview);
		when(mockManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		doNothing().when(managerSpy).updateSearchIndex(any());
		
		List<ColumnModel> schema = createDefaultColumnsWithIds();

		// call under test
		Long resultCrc = managerSpy.populateViewFromEntityReplication(tableId.getId(), scopeType, schema);
		assertEquals(crc32, resultCrc);
		verify(mockIndexDao).copyObjectReplicationToView(tableId.getId(), mockFilter, schema, mockMetadataProvider);
		verify(managerSpy).updateSearchIndex(indexDescription);
		// the CRC should be calculated with the etag column.
		verify(mockIndexDao).calculateCRC32ofTableView(tableId.getId());
	}

	/**
	 * Etag is no long a required column.
	 */
	@Test
	public void testPopulateViewFromEntityReplicationMissingEtagColumn() {
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getViewFilter(tableId.getId())).thenReturn(mockFilter);

		List<ColumnModel> schema = createDefaultColumnsWithIds(ObjectField.etag);

		// call under test
		manager.populateViewFromEntityReplication(tableId.getId(), scopeType, schema);
	}

	@Test
	public void testPopulateViewFromEntityReplicationMissingBenefactorColumn() {
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getViewFilter(tableId.getId())).thenReturn(mockFilter);

		List<ColumnModel> schema = createDefaultColumnsWithIds(ObjectField.benefactorId);
		// call under test
		manager.populateViewFromEntityReplication(tableId.getId(), scopeType, schema);
	}

	@Test
	public void testPopulateViewFromEntityReplicationNullViewType() {
		scopeType = null;
		List<ColumnModel> schema = createDefaultColumnsWithIds();
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.populateViewFromEntityReplication(tableId.getId(), scopeType, schema);
		});
	}

	@Test
	public void testPopulateViewFromEntityReplicationSchemaNull() {
		List<ColumnModel> schema = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.populateViewFromEntityReplication(tableId.getId(), scopeType, schema);
		});
	}

	@Test
	public void testPopulateViewFromEntityReplicationWithProgress() throws Exception {
		when(mockIndexDao.calculateCRC32ofTableView(any(Long.class))).thenReturn(crc32);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getViewFilter(tableId.getId())).thenReturn(mockFilter);

		List<ColumnModel> schema = createDefaultColumnsWithIds();
		// call under test
		Long resultCrc = managerSpy.populateViewFromEntityReplication(tableId.getId(), scopeType, schema);
		assertEquals(crc32, resultCrc);
		verify(mockIndexDao).copyObjectReplicationToView(tableId.getId(), mockFilter, schema, mockMetadataProvider);
		// the CRC should be calculated with the etag column.
		verify(mockIndexDao).calculateCRC32ofTableView(tableId.getId());
	}

	@Test
	public void testPopulateViewFromEntityReplicationUnknownCause() throws Exception {
		List<ColumnModel> schema = createDefaultColumnsWithIds();

		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getViewFilter(tableId.getId())).thenReturn(mockFilter);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any()))
				.thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.findMatch(any())).thenReturn(Optional.empty());

		// setup a failure
		IllegalArgumentException expected = new IllegalArgumentException("Something went wrong");
		doThrow(expected).when(mockIndexDao).copyObjectReplicationToView(any(), any(), any(), any());

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.populateViewFromEntityReplication(tableId.getId(), scopeType, schema);
		});
		// when the cause cannot be determined the original exception is thrown.
		assertEquals(expected, ex);
	}

	@Test
	public void testPopulateViewFromEntityReplicationKnownCause() throws Exception {
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any()))
				.thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.findMatch(any())).thenReturn(Optional.empty());

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

		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any()))
				.thenReturn(ImmutableList.of(annotationModel));

		// setup a failure
		IllegalArgumentException error = new IllegalArgumentException("Something went wrong");

		doThrow(error).when(mockIndexDao).copyObjectReplicationToView(any(), any(), any(), any());

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.populateViewFromEntityReplication(tableId.getId(), scopeType, schema);
		});

		assertNotEquals(error, ex);
		assertEquals("The size of the column 'foo' is too small.  The column size needs to be at least 11 characters.",
				ex.getMessage());
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerIds() {
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any())).thenReturn(schema);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockDefaultColumnModel.getCustomFields()).thenReturn(Arrays.asList(new ColumnModel().setName("testKey")));
		when(mockMetadataProvider.getViewFilter(any(), any())).thenReturn(mockFilter);
		
		when(mockFilter.newBuilder()).thenReturn(mockFilterBuilder);
		when(mockFilterBuilder.addExcludeAnnotationKeys(any())).thenReturn(mockFilterBuilder);
		when(mockFilterBuilder.build()).thenReturn(mockNewFilter);

		boolean excludeDerivedKeys = false;
		
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds,
				tokenString, excludeDerivedKeys);
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
		verify(mockFilterBuilder).addExcludeAnnotationKeys(Sets.newHashSet("testKey"));
		verify(mockFilterBuilder).build();
		// should request one more than the limit
		verify(mockIndexDao).getPossibleColumnModelsForContainers(mockNewFilter, limit + 1, offset);
	}
	
	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerIdsWithExcludeDerivedKeys() {
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any())).thenReturn(schema);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockDefaultColumnModel.getCustomFields()).thenReturn(Arrays.asList(new ColumnModel().setName("testKey")));
		when(mockMetadataProvider.getViewFilter(any(), any())).thenReturn(mockFilter);
		
		when(mockFilter.newBuilder()).thenReturn(mockFilterBuilder);
		when(mockFilterBuilder.addExcludeAnnotationKeys(any())).thenReturn(mockFilterBuilder);
		when(mockFilterBuilder.build()).thenReturn(mockNewFilter);
		when(mockNewFilter.newBuilder()).thenReturn(mockFilterBuilder);
		when(mockFilterBuilder.setExcludeDerivedKeys(anyBoolean())).thenReturn(mockFilterBuilder);

		boolean excludeDerivedKeys = true;
		
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds,
				tokenString, excludeDerivedKeys);
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
		verify(mockFilterBuilder).addExcludeAnnotationKeys(Sets.newHashSet("testKey"));
		verify(mockFilterBuilder).setExcludeDerivedKeys(true);
		verify(mockFilterBuilder, times(2)).build();
		// should request one more than the limit
		verify(mockIndexDao).getPossibleColumnModelsForContainers(mockNewFilter, limit + 1, offset);
	}

	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerLastPage() {
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any())).thenReturn(schema);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockMetadataProvider.getViewFilter(any(), any())).thenReturn(mockFilter);

		boolean excludeDerivedKeys = false;
		
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds,
				tokenString, excludeDerivedKeys);
		
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
		// should request one more than the limit
		verify(mockIndexDao).getPossibleColumnModelsForContainers(mockFilter, limit + 1, offset);
	}

	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerLastPageNullToken() {
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any())).thenReturn(schema);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockMetadataProvider.getViewFilter(any(), any())).thenReturn(mockFilter);

		tokenString = null;
		boolean excludeDerivedKeys = false;
		
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds,
				tokenString, excludeDerivedKeys);
		
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
		// should request one more than the limit
		verify(mockIndexDao).getPossibleColumnModelsForContainers(mockFilter, NextPageToken.DEFAULT_LIMIT + 1,
				NextPageToken.DEFAULT_OFFSET);
	}

	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerHasNextPage() {
		List<ColumnModel> pagePluseOne = new LinkedList<ColumnModel>(schema);
		pagePluseOne.add(new ColumnModel());
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any())).thenReturn(pagePluseOne);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockMetadataProvider.getViewFilter(any(), any())).thenReturn(mockFilter);

		nextPageToken = new NextPageToken(schema.size(), 0L);
		boolean excludeDerivedKeys = false;
		
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds,
				nextPageToken.toToken(), excludeDerivedKeys);
		
		assertNotNull(results);
		assertEquals(new NextPageToken(2L, 2L).toToken(), results.getNextPageToken());
		assertEquals(schema, results.getResults());
		// should request one more than the limit
		verify(mockIndexDao).getPossibleColumnModelsForContainers(mockFilter, nextPageToken.getLimitForQuery(),
				nextPageToken.getOffset());
	}

	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerIsNullContainerIds() {
		String token = nextPageToken.toToken();
		containerIds = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds, token, false);
		});
	}

	@Test
	public void testGetPossibleAnnotationDefinitionsWithEmptyFilter() {
		
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getViewFilter(any(), any())).thenReturn(mockFilter);
		when(mockFilter.isEmpty()).thenReturn(true);
		
		String token = nextPageToken.toToken();
		boolean excludeDerivedKeys = false;
		
		// call under test
		ColumnModelPage results = manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds,
				token, excludeDerivedKeys);
		
		assertNotNull(results);
		assertNotNull(results.getResults());
		assertEquals(null, results.getNextPageToken());
		// should not call the dao
		verify(mockIndexDao, never()).getPossibleColumnModelsForContainers(any(), anyLong(), anyLong());
		verify(mockFilter).isEmpty();
	}

	@Test
	public void testGetPossibleAnnotationDefinitionsForContainerIsOverLimit() {
		limit = NextPageToken.MAX_LIMIT + 1;
		nextPageToken = new NextPageToken(limit, offset);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds, nextPageToken.toToken(), false);
		});
	}

	@Test
	public void testGetPossibleColumnModelsForScope() {
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any())).thenReturn(schema);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockMetadataProvider.getViewFilter(any(), any())).thenReturn(mockFilter);
		when(mockFilter.isEmpty()).thenReturn(false);

		boolean excludeDerivedKeys = false;
		
		// call under test
		ColumnModelPage results = manager.getPossibleColumnModelsForScope(scope, tokenString, excludeDerivedKeys);
		
		assertNotNull(results);
		assertEquals(null, results.getNextPageToken());
		assertEquals(schema, results.getResults());
		
		Set<Long> expectedScope = new HashSet<Long>(KeyFactory.stringToKey(scope.getScope()));
		verify(mockMetadataProvider).getViewFilter(scopeType.getTypeMask(), expectedScope);
		verify(mockFilter).isEmpty();

	}
	
	@Test
	public void testGetPossibleColumnModelsForScopeWithTypeNull() {
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any())).thenReturn(schema);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockMetadataProvider.getViewFilter(any(), any())).thenReturn(mockFilter);
		when(mockFilter.isEmpty()).thenReturn(false);

		scope.setViewEntityType(null);
		boolean excludeDerivedKeys = false;

		// call under test
		ColumnModelPage results = manager.getPossibleColumnModelsForScope(scope, tokenString, excludeDerivedKeys);

		assertNotNull(results);
		// should default to file view.
		verify(mockIndexDao).getPossibleColumnModelsForContainers(mockFilter, nextPageToken.getLimitForQuery(),
				nextPageToken.getOffset());
	}

	@Test
	public void testGetPossibleColumnModelsForScopeWithCustomFields() {
		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any())).thenReturn(schema);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockMetadataProvider.getViewFilter(any(), any())).thenReturn(mockFilter);
		when(mockFilter.isEmpty()).thenReturn(false);
		
		when(mockFilter.newBuilder()).thenReturn(mockFilterBuilder);
		when(mockFilterBuilder.addExcludeAnnotationKeys(any())).thenReturn(mockFilterBuilder);
		when(mockFilterBuilder.build()).thenReturn(mockNewFilter);

		ColumnModel customField = new ColumnModel();
		customField.setName("CustomField");

		when(mockDefaultColumnModel.getCustomFields()).thenReturn(ImmutableList.of(customField));
		boolean excludeDerivedKeys = false;
		
		// call under test
		ColumnModelPage results = manager.getPossibleColumnModelsForScope(scope, tokenString, excludeDerivedKeys);

		assertNotNull(results);
		// Makes sure the exclude list is passed correctly.
		verify(mockIndexDao).getPossibleColumnModelsForContainers(mockNewFilter, nextPageToken.getLimitForQuery(),
				nextPageToken.getOffset());
		
		verify(mockFilter).newBuilder();
	}

	@Test
	public void testGetPossibleColumnModelsForScopeWithNullScope() {
		scope.setScope(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getPossibleColumnModelsForScope(scope, tokenString, false);
		});
	}

	/**
	 * Test added for PLFM-4155.
	 */
	@Test
	public void testAlterTableAsNeededWithinAutoProgress() {
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
		doNothing().when(managerSpy).createTableIfDoesNotExist(any());
		
		// call under test
		managerSpy.applyRowChangeToIndex(tableId, change);

		verify(managerSpy).createTableIfDoesNotExist(new TableIndexDescription(tableId));
		// apply change
		verify(mockIndexDao, times(2)).createOrUpdateOrDeleteRows(any(IdAndVersion.class), any(Grouping.class));
	}

	@Test
	public void testApplySchemaChangeToIndex() {
		long changeNumber = 333l;
		SchemaChange schemaChange = new SchemaChange(columnChanges);
		ChangeData<SchemaChange> change = new ChangeData<SchemaChange>(changeNumber, schemaChange);
		doNothing().when(managerSpy).createTableIfDoesNotExist(any());
		// Call under test
		managerSpy.applySchemaChangeToIndex(tableId, change);

		verify(managerSpy).createTableIfDoesNotExist(new TableIndexDescription(tableId));
		boolean alterTemp = false;
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);
	}

	@Test
	public void testApplyChangeToIndexRow() throws NotFoundException, IOException {
		setupExecuteInWriteTransaction();
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		long changeNumber = 444L;
		TableChangeMetaData mockChange = setupMockRowChange(changeNumber);
		doNothing().when(managerSpy).createTableIfDoesNotExist(any());
		
		// call under test
		managerSpy.applyChangeToIndex(tableId, mockChange);
		// set schema
		verify(managerSpy).createTableIfDoesNotExist(new TableIndexDescription(tableId));
		// apply change
		verify(mockIndexDao, times(2)).createOrUpdateOrDeleteRows(any(IdAndVersion.class), any(Grouping.class));
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, mockChange.getChangeNumber());

	}

	@Test
	public void testApplyChangeToIndexRowColumn() throws NotFoundException, IOException {
		long changeNumber = 444L;
		TableChangeMetaData mockChange = setupMockColumnChange(changeNumber);
		// call under test
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
		// call under test
		manager.applyChangeToIndex(tableId, mockChange);
		// set schema
		boolean alterTemp = false;
		verify(mockIndexDao).alterTableAsNeeded(tableId, columnChanges, alterTemp);

		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, mockChange.getChangeNumber());
		verify(mockIndexDao).createMultivalueColumnIndexTable(tableId, newColumn, false);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, newColumn, null, false);
	}
	
	@Test
	public void testApplyChangeToIndexSearchEnable() throws NotFoundException, IOException {
		long changeNumber = 123L;
		
		boolean enableSearch = true;
		
		TableChangeMetaData mockChange = setupMockSearchChange(changeNumber, enableSearch);
		
		doNothing().when(managerSpy).createTableIfDoesNotExist(any());
		doNothing().when(managerSpy).updateSearchIndex(any());

		IndexDescription indexDescription = new TableIndexDescription(tableId);
		
		// call under test
		managerSpy.applyChangeToIndex(tableId, mockChange);
		
		verify(managerSpy).createTableIfDoesNotExist(indexDescription);
		verify(mockIndexDao).setSearchEnabled(tableId, enableSearch);
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, mockChange.getChangeNumber());
		verify(managerSpy).updateSearchIndex(indexDescription);
		verifyNoMoreInteractions(mockIndexDao);
	}
	
	@Test
	public void testApplyChangeToIndexSearchDisable() throws NotFoundException, IOException {
		long changeNumber = 123L;
		
		boolean enableSearch = false;
		
		TableChangeMetaData mockChange = setupMockSearchChange(changeNumber, enableSearch);
		doNothing().when(managerSpy).createTableIfDoesNotExist(any());
		
		// call under test
		managerSpy.applyChangeToIndex(tableId, mockChange);
		
		verify(managerSpy).createTableIfDoesNotExist(new TableIndexDescription(tableId));
		verify(mockIndexDao).clearSearchIndex(tableId);
		verify(mockIndexDao).setSearchEnabled(tableId, enableSearch);
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, mockChange.getChangeNumber());
		verifyNoMoreInteractions(mockIndexDao);
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
		// Building without a version should attempt to set the current schema on the
		// index.
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
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L, 0L);
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
		verify(mockManagerSupport, times(1)).attemptToUpdateTableProgress(any(IdAndVersion.class), anyString(),
				anyString(), anyLong(), anyLong());
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
		verify(mockManagerSupport, never()).attemptToUpdateTableProgress(any(IdAndVersion.class), anyString(),
				anyString(), anyLong(), anyLong());
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
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_LockUnavilableException() throws Exception {
		LockUnavilableException exception = new LockUnavilableException("no lock for you!");
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		});
		assertEquals(result.getCause(), exception);
		verify(managerSpy, never()).buildTableIndexWithLock(any(), any(), any());
	}

	/**
	 * TableUnavailableException translates to RecoverableMessageException
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_TableUnavilableException() throws Exception {
		TableUnavailableException exception = new TableUnavailableException(null);
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		});
		assertEquals(result.getCause(), exception);
		verify(managerSpy, never()).buildTableIndexWithLock(any(), any(), any());
	}

	/**
	 * InterruptedException translates to RecoverableMessageException
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_InterruptedException() throws Exception {
		InterruptedException exception = new InterruptedException();
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		});
		assertEquals(result.getCause(), exception);
		verify(managerSpy, never()).buildTableIndexWithLock(any(), any(), any());
	}

	/**
	 * IOException translates to RecoverableMessageException
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_IOException() throws Exception {
		IOException exception = new IOException();
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		});
		assertEquals(result.getCause(), exception);
		verify(managerSpy, never()).buildTableIndexWithLock(any(), any(), any());
	}

	/**
	 * RuntimeException is just thrown
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_RuntimeException() throws Exception {
		IllegalArgumentException exception = new IllegalArgumentException("some runtime");
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		RuntimeException result = assertThrows(RuntimeException.class, () -> {
			// call under test
			managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		});
		assertEquals(exception, result);
		verify(managerSpy, never()).buildTableIndexWithLock(any(), any(), any());
	}

	/**
	 * Checked Exception is wrapped in runtime.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_CheckedException() throws Exception {
		Exception exception = new Exception("nope");
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(exception);
		List<TableChangeMetaData> list = setupMockChanges();
		Iterator<TableChangeMetaData> iterator = list.iterator();
		RuntimeException result = assertThrows(RuntimeException.class, () -> {
			// call under test
			managerSpy.buildIndexToChangeNumber(mockCallback, tableId, iterator);
		});
		assertEquals(result.getCause(), exception);
		verify(managerSpy, never()).buildTableIndexWithLock(any(), any(), any());
	}

	/**
	 * Helper to setup mockManagerSupport.tryRunWithTableExclusiveLock to forward to
	 * the callback
	 * 
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
	 * An InvalidStatusTokenException should not cause the table's state to be set
	 * to failed. Instead the rebuild should be restarted by pushing the message
	 * back on the queue.
	 * 
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

		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {
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
		verify(mockManagerSupport, never()).attemptToSetTableStatusToAvailable(any(IdAndVersion.class), anyString(),
				anyString());
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
		verify(mockManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
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
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.populateListColumnIndexTables(tableId, schema, rowsIdsWithChanges);
		});
	}

	@Test
	public void testPopulateListColumnIndexTable_NullSchema() {
		schema = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.populateListColumnIndexTables(tableId, schema, rowsIdsWithChanges);
		});
	}

	@Test
	public void testUpdateViewRowsInTransaction() {
		setupExecuteInWriteTransaction();
		Long[] rowsIdsArray = rowsIdsWithChanges.stream().toArray(Long[]::new);
		
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockFilter.getLimitObjectIds()).thenReturn(Optional.of(rowsIdsWithChanges));
		when(mockIndexDao.isSearchEnabled(any())).thenReturn(false);

		IndexDescription indexDescription = new ViewIndexDescription(tableId, EntityType.entityview);
		
		// call under test
		managerSpy.updateViewRowsInTransaction(indexDescription, scopeType, schema, mockFilter);

		verify(mockIndexDao).executeInWriteTransaction(any());
		verify(mockIndexDao).deleteRowsFromViewBatch(tableId, rowsIdsArray);
		verify(mockIndexDao).copyObjectReplicationToView(tableId.getId(), mockFilter, schema, mockMetadataProvider);
		verify(mockIndexDao).isSearchEnabled(tableId);
		verify(managerSpy).populateListColumnIndexTables(tableId, schema, rowsIdsWithChanges);
		verify(managerSpy, never()).updateSearchIndex(any(), any());
	}
	
	@Test
	public void testUpdateViewRowsInTransactionWithSearchEnabled() {
		setupExecuteInWriteTransaction();
		Long[] rowsIdsArray = rowsIdsWithChanges.stream().toArray(Long[]::new);
		
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockFilter.getLimitObjectIds()).thenReturn(Optional.of(rowsIdsWithChanges));
		when(mockIndexDao.isSearchEnabled(any())).thenReturn(true);
		doReturn(schema).when(managerSpy).getSchemaForSearchIndex(any());
		List<TableRowData> mockedData = Mockito.mock(List.class);
		when(mockIndexDao.getTableDataForRowIds(any(), any(), any())).thenReturn(mockedData);
		Iterator<TableRowData> mockedDataIterator = Mockito.mock(Iterator.class);
		when(mockedData.iterator()).thenReturn(mockedDataIterator);
		
		IndexDescription indexDescription = new ViewIndexDescription(tableId, EntityType.entityview);
		
		// call under test
		managerSpy.updateViewRowsInTransaction(indexDescription, scopeType, schema, mockFilter);

		verify(mockIndexDao).executeInWriteTransaction(any());
		verify(mockIndexDao).deleteRowsFromViewBatch(tableId, rowsIdsArray);
		verify(mockIndexDao).copyObjectReplicationToView(tableId.getId(), mockFilter, schema, mockMetadataProvider);
		verify(managerSpy).populateListColumnIndexTables(tableId, schema, rowsIdsWithChanges);
		verify(mockIndexDao).isSearchEnabled(tableId);
		verify(managerSpy).getSchemaForSearchIndex(schema);
		verify(mockIndexDao).getTableDataForRowIds(tableId, schema, rowsIdsWithChanges);
		verify(managerSpy).updateSearchIndex(indexDescription, mockedDataIterator);
	}

	@Test
	public void testUpdateViewRowsInTransaction_ExceptionDuringUpdate() {
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any()))
				.thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.findMatch(any())).thenReturn(Optional.empty());

		setupExecuteInWriteTransaction();
		
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockFilter.getLimitObjectIds()).thenReturn(Optional.of(rowsIdsWithChanges));
		
		// setup an exception on copy
		IllegalArgumentException exception = new IllegalArgumentException("something wrong");
		doThrow(exception).when(mockIndexDao).copyObjectReplicationToView(any(), any(), any(), any());
		
		Long[] rowsIdsArray = rowsIdsWithChanges.stream().toArray(Long[]::new);
		
		IndexDescription indexDescription = new ViewIndexDescription(tableId, EntityType.entityview);
		
		Exception thrown = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			managerSpy.updateViewRowsInTransaction(indexDescription, scopeType, schema, mockFilter);
		});
		assertEquals(exception, thrown);

		verify(mockIndexDao).executeInWriteTransaction(any());
		verify(mockIndexDao).deleteRowsFromViewBatch(tableId, rowsIdsArray);
		verify(mockIndexDao).copyObjectReplicationToView(tableId.getId(), mockFilter, schema, mockMetadataProvider);
		// must attempt to determine the type of exception.
		verify(managerSpy, never()).populateListColumnIndexTables(any(), any(), any());
	}

	@Test
	public void testUpdateViewRowsInTransaction_NullIndexDescription() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateViewRowsInTransaction(null, scopeType, schema, mockFilter);
		});
	}

	@Test
	public void testUpdateViewRowsInTransaction_ViewType() {
		scopeType = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateViewRowsInTransaction(new ViewIndexDescription(tableId, EntityType.entityview), scopeType, schema, mockFilter);
		});
	}

	@Test
	public void testUpdateViewRowsInTransaction_Schema() {
		schema = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateViewRowsInTransaction(new ViewIndexDescription(tableId, EntityType.entityview), scopeType, schema, mockFilter);
		});
	}

	@Test
	public void testListColumnIndexTableChangesFromExpectedSchema_nullExpectedSchema() {
		assertThrows(IllegalArgumentException.class,
				() -> TableIndexManagerImpl.listColumnIndexTableChangesFromExpectedSchema(null, Sets.newHashSet(123L)));
	}

	@Test
	public void testListColumnIndexTableChangesFromExpectedSchema_nullExistingIndexListColumns() {
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("222");
		columnModel.setColumnType(ColumnType.INTEGER_LIST);

		assertThrows(IllegalArgumentException.class, () -> TableIndexManagerImpl
				.listColumnIndexTableChangesFromExpectedSchema(Arrays.asList(columnModel), null));
	}

	@Test
	public void testListColumnIndexTableChangesFromExpectedSchema_addAndRemove() {
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

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromExpectedSchema(schema, existingIndexTableColumns);

		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newAddition(addListCol),
				ListColumnIndexTableChange.newRemoval(removeListColId)

		);
		assertEquals(expected, result);

	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_nullChanges() {
		assertThrows(IllegalArgumentException.class,
				() -> TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(null, Sets.newHashSet(123L)));
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_nulExistingListIndexColumns() {
		assertThrows(IllegalArgumentException.class,
				() -> TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(columnChanges, null));
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_noListColumnChange_NotInExistingTablesSet() {
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		// same column model for old and new
		ColumnChangeDetails noChange = new ColumnChangeDetails(columnModel, columnModel);
		// does not already exist as an index table
		Set<Long> existingColumnChangeIds = Collections.emptySet();

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(noChange), existingColumnChangeIds);
		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newAddition(columnModel));
		assertEquals(expected, result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_noListColumnChange_InExistingTablesSet() {
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		// same column model for old and new
		ColumnChangeDetails noChange = new ColumnChangeDetails(columnModel, columnModel);

		Set<Long> existingColumnChangeIds = Sets.newHashSet(Long.parseLong(columnModel.getId()));

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(noChange), existingColumnChangeIds);
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_AddListColumn_NotInExistingTablesSet() {
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		// same column model for old and new
		ColumnChangeDetails addColumn = new ColumnChangeDetails(null, columnModel);
		// does not already exist as an index table
		Set<Long> existingColumnChangeIds = Collections.emptySet();

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(addColumn), existingColumnChangeIds);
		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newAddition(columnModel));
		assertEquals(expected, result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_AddListColumn_InExistingTablesSet() {
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		// same column model for old and new
		ColumnChangeDetails addColumn = new ColumnChangeDetails(null, columnModel);

		Set<Long> existingColumnChangeIds = Sets.newHashSet(Long.parseLong(columnModel.getId()));

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(addColumn), existingColumnChangeIds);
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_RemoveListColumn_NotInExistingTablesSet() {
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		// same column model for old and new
		ColumnChangeDetails removeColumn = new ColumnChangeDetails(columnModel, null);
		// does not already exist as an index table
		Set<Long> existingColumnChangeIds = Collections.emptySet();

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(removeColumn), existingColumnChangeIds);
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_RemoveListColumn_InExistingTablesSet() {
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		// same column model for old and new
		ColumnChangeDetails removeColumn = new ColumnChangeDetails(columnModel, null);
		long columnModelId = Long.parseLong(columnModel.getId());
		Set<Long> existingColumnChangeIds = Sets.newHashSet(columnModelId);

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(removeColumn), existingColumnChangeIds);

		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newRemoval(columnModelId));
		assertEquals(expected, result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_changeListType_oldAndNewColumnInExistingTablesSet() {
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

		// same column model for old and new
		ColumnChangeDetails listChange = new ColumnChangeDetails(oldList, newList);
		// does not already exist as an index table
		Set<Long> existingColumnChangeIds = Sets.newHashSet(oldListId, newListId);

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(listChange), existingColumnChangeIds);

		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newRemoval(oldListId));
		assertEquals(expected, result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_changeListType_oldColumnInExistingTablesSet() {
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

		// same column model for old and new
		ColumnChangeDetails listChange = new ColumnChangeDetails(oldList, newList);

		Set<Long> existingColumnChangeIds = Sets.newHashSet(oldListId);

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(listChange), existingColumnChangeIds);
		List<ListColumnIndexTableChange> expected = Arrays
				.asList(ListColumnIndexTableChange.newUpdate(oldListId, newList));

		assertEquals(expected, result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_changeListType_newColumnInExistingTablesSet() {
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

		// same column model for old and new
		ColumnChangeDetails listChange = new ColumnChangeDetails(oldList, newList);
		// does not already exist as an index table
		Set<Long> existingColumnChangeIds = Sets.newHashSet(newListId);

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(listChange), existingColumnChangeIds);
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_changeListType_noColumnInExistingTablesSet() {
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

		// same column model for old and new
		ColumnChangeDetails listChange = new ColumnChangeDetails(oldList, newList);

		Set<Long> existingColumnChangeIds = Collections.emptySet();

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromChangeDetails(Arrays.asList(listChange), existingColumnChangeIds);
		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newAddition(newList));

		assertEquals(expected, result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_NonListChanges() {
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

		// same column model for old and new
		List<ColumnChangeDetails> nonListChanges = Arrays.asList(new ColumnChangeDetails(oldNonList, newNonList),
				new ColumnChangeDetails(oldNonList, null), new ColumnChangeDetails(null, newNonList));

		Set<Long> existingColumnChangeIds = Collections.emptySet();

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromChangeDetails(nonListChanges, existingColumnChangeIds);

		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void listColumnIndexTableChangesFromChangeDetails_multipleChanges() {
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

		// same column model for old and new
		List<ColumnChangeDetails> listChanges = Arrays.asList(new ColumnChangeDetails(oldList, null),
				new ColumnChangeDetails(null, newList));

		Set<Long> existingColumnChangeIds = Sets.newHashSet(oldListId);

		// method under test
		List<ListColumnIndexTableChange> result = TableIndexManagerImpl
				.listColumnIndexTableChangesFromChangeDetails(listChanges, existingColumnChangeIds);
		List<ListColumnIndexTableChange> expected = Arrays.asList(ListColumnIndexTableChange.newRemoval(oldListId),
				ListColumnIndexTableChange.newAddition(newList));

		assertEquals(expected, result);
	}

	@Test
	public void applyListColumnIndexTableChanges_add() {
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);
		ListColumnIndexTableChange addChange = ListColumnIndexTableChange.newAddition(columnModel);

		boolean alterTemp = false;

		// method under test
		manager.applyListColumnIndexTableChanges(tableId, Collections.singletonList(addChange), alterTemp);

		verify(mockIndexDao).createMultivalueColumnIndexTable(tableId, columnModel, alterTemp);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, columnModel, null, alterTemp);
	}

	@Test
	public void applyListColumnIndexTableChanges_remove() {
		long columnIdToRemove = 1234L;
		ListColumnIndexTableChange removeChange = ListColumnIndexTableChange.newRemoval(columnIdToRemove);

		boolean alterTemp = false;

		// method under test
		manager.applyListColumnIndexTableChanges(tableId, Collections.singletonList(removeChange), alterTemp);

		verify(mockIndexDao).deleteMultivalueColumnIndexTable(tableId, columnIdToRemove, alterTemp);
	}

	@Test
	public void applyListColumnIndexTableChanges_update() {
		long oldColumnId = 1234L;
		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		ListColumnIndexTableChange addChange = ListColumnIndexTableChange.newUpdate(oldColumnId, columnModel);

		boolean alterTemp = false;
		// method under test
		manager.applyListColumnIndexTableChanges(tableId, Collections.singletonList(addChange), alterTemp);

		verify(mockIndexDao).updateMultivalueColumnIndexTable(tableId, oldColumnId, columnModel, alterTemp);
	}

	@Test
	public void applyListColumnIndexTableChanges_multipleChanges() {

		ColumnModel columnModel = new ColumnModel();
		columnModel.setId("9876");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(46L);

		ListColumnIndexTableChange addChange = ListColumnIndexTableChange.newAddition(columnModel);

		long columnIdToRemove = 456L;
		ListColumnIndexTableChange removeChange = ListColumnIndexTableChange.newRemoval(columnIdToRemove);
		boolean alterTemp = false;
		// method under test
		manager.applyListColumnIndexTableChanges(tableId, Arrays.asList(addChange, removeChange), alterTemp);

		verify(mockIndexDao).createMultivalueColumnIndexTable(tableId, columnModel, alterTemp);
		verify(mockIndexDao).populateListColumnIndexTable(tableId, columnModel, null, alterTemp);
		verify(mockIndexDao).deleteMultivalueColumnIndexTable(tableId, columnIdToRemove, alterTemp);
	}

	@Test
	public void testValidateTableMaximumListLengthChanges_noListTypes() {
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

		verify(mockIndexDao, never()).tempTableListColumnMaxLength(any(), any());
	}

	@Test
	public void testValidateTableMaximumListLengthChanges_bothListTypes_ListLengthNewGreaterThanOrEqualOld() {
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

		verify(mockIndexDao, never()).tempTableListColumnMaxLength(any(), any());
	}

	@Test
	public void testValidateTableMaximumListLengthChanges_bothListTypes_ListLengthNewLessThanOld_underExistingTableMaxLength() {
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

		verify(mockIndexDao).tempTableListColumnMaxLength(tableId, oldCol.getId());
	}

	@Test
	public void testValidateTableMaximumListLengthChanges_bothListTypes_ListLengthNewLessThanOld_aboveExistingTableMaxLength() {
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
		manager.validateTableMaximumListLengthChanges(tableId, change)).getMessage();

		assertEquals("maximumListLength for ColumnModel \"new\" must be at least: 3", errorMessage);

		verify(mockIndexDao).tempTableListColumnMaxLength(tableId, oldCol.getId());
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

		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any()))
				.thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.findMatch(any())).thenReturn(Optional.empty());
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);

		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any()))
				.thenReturn(ImmutableList.of(annotationModel));

		List<ColumnModel> schema = ImmutableList.of(columnModel);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.determineCauseOfReplicationFailure(original, schema, mockMetadataProvider, scope.getViewTypeMask(),
					mockFilter);
		});

		assertEquals(original, ex.getCause());
		assertEquals("The size of the column 'foo' is too small.  The column size needs to be at least 11 characters.",
				ex.getMessage());
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

		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any()))
				.thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.findMatch(any())).thenReturn(Optional.empty());
		when(mockMetadataProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultColumnModel);

		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any()))
				.thenReturn(ImmutableList.of(a1, a2));

		List<ColumnModel> schema = ImmutableList.of(columnModel);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.determineCauseOfReplicationFailure(original, schema, mockMetadataProvider, scope.getViewTypeMask(),
					mockFilter);
		});

		assertEquals(original, ex.getCause());
		assertEquals("The size of the column 'foo' is too small.  The column size needs to be at least 11 characters.",
				ex.getMessage());
	}

	@Test
	public void testRefreshViewBenefactors() {
		IdAndVersion viewId = IdAndVersion.parse("syn123");
		when(mockManagerSupport.getViewScopeType(any())).thenReturn(scopeType);
		// call under test
		manager.refreshViewBenefactors(viewId);
		verify(mockManagerSupport).getViewScopeType(viewId);
		verify(mockIndexDao).refreshViewBenefactors(viewId, scopeType.getObjectType().getMainType());
	}

	@Test
	public void testRefreshViewBenefactorsNullId() {
		IdAndVersion viewId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.refreshViewBenefactors(viewId);
		});
	}

	@Test
	public void testUpdateObjectReplication() {
		ReplicationType type = ReplicationType.ENTITY;
		
		List<ObjectDataDTO> toUpdate = Arrays.asList(
				new ObjectDataDTO().setId(0L).setVersion(1L),
				new ObjectDataDTO().setId(0L).setVersion(2L),
				new ObjectDataDTO().setId(1L).setVersion(1L),
				new ObjectDataDTO().setId(1L).setVersion(2L)
		);

		int batchSize = 3;

		setupExecuteInWriteTransaction();

		// call under test
		manager.updateObjectReplication(type, toUpdate.iterator(), batchSize);

		// batch one
		verify(mockIndexDao).deleteObjectData(type, Arrays.asList(0L,1L));
		verify(mockIndexDao).addObjectData(type, toUpdate.subList(0, 3));
		// batch two
		verify(mockIndexDao).deleteObjectData(type, Arrays.asList(1L));
		verify(mockIndexDao).addObjectData(type, toUpdate.subList(3, 4));
	}
	
	
	/**
	 * A failure with a batch greater than one needs to be retried.
	 */
	@Test
	public void testUpdateObjectReplicationWithExceptionBatchGreaterThanOne() {
		ReplicationType type = ReplicationType.ENTITY;
		
		List<ObjectDataDTO> toUpdate = Arrays.asList(
				new ObjectDataDTO().setId(0L).setVersion(1L),
				new ObjectDataDTO().setId(0L).setVersion(2L),
				new ObjectDataDTO().setId(1L).setVersion(1L),
				new ObjectDataDTO().setId(1L).setVersion(2L)
		);

		int batchSize = 3;

		setupExecuteInWriteTransaction();
		
		DataIntegrityViolationException e = new DataIntegrityViolationException("something is wrong!");
		doThrow(e).when(mockIndexDao).addObjectData(any(), any());
		
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class, ()->{
			// call under test
			manager.updateObjectReplication(type, toUpdate.iterator(), batchSize);
		});
		assertEquals(thrown.getCause(), e);
	}
	
	/**
	 * Failure on a single object is permanent.
	 */
	@Test
	public void testUpdateObjectReplicationWithExceptionBatchOfOne() {
		ReplicationType type = ReplicationType.ENTITY;
		
		List<ObjectDataDTO> toUpdate = Arrays.asList(
				new ObjectDataDTO().setId(0L).setVersion(1L)
		);

		int batchSize = 3;

		setupExecuteInWriteTransaction();
		
		DataIntegrityViolationException e = new DataIntegrityViolationException("something is wrong!");
		doThrow(e).when(mockIndexDao).addObjectData(any(), any());
		
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateObjectReplication(type, toUpdate.iterator(), batchSize);
		});
		assertEquals(thrown.getCause(), e);
	}
	
	@Test
	public void testDeleteObjectData() {
		ReplicationType type = ReplicationType.ENTITY;
		
		List<Long> toDeleteIds = Arrays.asList(1L,2L,3L);

		setupExecuteInWriteTransaction();

		// call under test
		manager.deleteObjectData(type, toDeleteIds);

		verify(mockIndexDao).deleteObjectData(type, toDeleteIds);
	}
	
	@Test
	public void testIsViewSynchronizeLockExpiredWithEmpty() {
		ReplicationType type = ReplicationType.ENTITY;
		when(mockIndexDao.isSynchronizationLockExpiredForObject(any(), any())).thenReturn(false);
		// call under test
		assertFalse(manager.isViewSynchronizeLockExpired(type, tableId));
		verify(mockIndexDao).isSynchronizationLockExpiredForObject(type, tableId.getId());
	}
	
	@Test
	public void testIsViewSynchronizeLockExpiredWithExpired() {
		ReplicationType type = ReplicationType.ENTITY;
		when(mockIndexDao.isSynchronizationLockExpiredForObject(any(), any())).thenReturn(true);
		// call under test
		assertTrue(manager.isViewSynchronizeLockExpired(type, tableId));
		verify(mockIndexDao).isSynchronizationLockExpiredForObject(type, tableId.getId());
	}
	
	
	@Test
	public void testResetViewSynchronizeLock() {
		ReplicationType type = ReplicationType.ENTITY;
		long start = System.currentTimeMillis();
		// call under test
		manager.resetViewSynchronizeLock(type, tableId);
		verify(mockIndexDao).setSynchronizationLockExpiredForObject(eq(type), eq(tableId.getId()), longCaptor.capture());
		long expires = longCaptor.getValue();
		long expectedExpires = start+TableIndexManagerImpl.SYNCHRONIZATION_FEQUENCY_MS;
		assertTrue(expectedExpires >= expires);
	}
	
	@Test
	public void testCreateTableIfDoesNotExists() {
		
		manager.createTableIfDoesNotExist(new TableIndexDescription(tableId));
		
		verify(mockIndexDao).createTableIfDoesNotExist(new TableIndexDescription(tableId));
		verify(mockIndexDao).createSecondaryTables(tableId);
	}
	
	@Test
	public void testCreateTableIfDoesNotExistsForView() {
		
		manager.createTableIfDoesNotExist(new ViewIndexDescription(tableId, EntityType.entityview));
		
		verify(mockIndexDao).createTableIfDoesNotExist(new ViewIndexDescription(tableId, EntityType.entityview));
		verify(mockIndexDao).createSecondaryTables(tableId);
	}
	
	@Test
	public void testUpdateSearchIndex() {
		
		schema = Arrays.asList(
				new ColumnModel().setId("44").setColumnType(ColumnType.STRING),
				new ColumnModel().setId("45").setColumnType(ColumnType.INTEGER)
		);
		
		DatabaseColumnInfo column1Info = new DatabaseColumnInfo();
		column1Info.setColumnName("_C44_");
		column1Info.setColumnType(ColumnType.STRING);
		
		DatabaseColumnInfo column2Info = new DatabaseColumnInfo();
		column2Info.setColumnName("_C45_");
		column2Info.setColumnType(ColumnType.INTEGER);
		
		when(mockIndexDao.getDatabaseInfo(any())).thenReturn(Arrays.asList(column1Info, column2Info));
		
		// This is the expected sub-schema for the search index
		List<ColumnModel> expectedSearchSchema = Arrays.asList(
			new ColumnModel().setId("44").setColumnType(ColumnType.STRING)		
		);
		
		TableRowData tableRow1Data = new TableRowData(1L, 
			expectedSearchSchema.stream().map(model -> new TypedCellValue(model.getColumnType(), "some value")).collect(Collectors.toList())
		);
		
		TableRowData tableRow2Data = new TableRowData(2L, 
			expectedSearchSchema.stream().map(model -> new TypedCellValue(model.getColumnType(), "some value")).collect(Collectors.toList())
		);
		
		// The pagination iterator calls this twice unconditionally, so the second time we return an empty list
		when(mockIndexDao.getTableDataPage(any(), any(), anyLong(), anyLong())).thenReturn(Arrays.asList(tableRow1Data, tableRow2Data), Collections.emptyList());
		
		when(mockSearchProcessor.process(any(), anyBoolean())).thenReturn("processed value");
		
		IndexDescription indexDescription = new TableIndexDescription(tableId);
		
		// Call under test
		manager.updateSearchIndex(indexDescription);
		
		verify(mockIndexDao).getTableDataPage(tableId, expectedSearchSchema, TableIndexManagerImpl.BATCH_SIZE, 0);
		verify(mockIndexDao).getTableDataPage(tableId, expectedSearchSchema, TableIndexManagerImpl.BATCH_SIZE, TableIndexManagerImpl.BATCH_SIZE);
		verify(mockIndexDao).updateSearchIndex(tableId, Arrays.asList(new RowSearchContent(1L, "processed value"), new RowSearchContent(2L, "processed value")));
	}
	
	@Test
	public void testUpdateSearchIndexWithNoEligibleColumns() {
		
		schema = Arrays.asList(
				new ColumnModel().setId("44").setColumnType(ColumnType.DOUBLE),
				new ColumnModel().setId("45").setColumnType(ColumnType.INTEGER)
		);
		
		DatabaseColumnInfo column1Info = new DatabaseColumnInfo();
		column1Info.setColumnName("_C44_");
		column1Info.setColumnType(ColumnType.DOUBLE);
		
		DatabaseColumnInfo column2Info = new DatabaseColumnInfo();
		column2Info.setColumnName("_C45_");
		column2Info.setColumnType(ColumnType.INTEGER);
		
		when(mockIndexDao.getDatabaseInfo(any())).thenReturn(Arrays.asList(column1Info, column2Info));
		
		IndexDescription indexDescription = new TableIndexDescription(tableId);
		
		// Call under test
		manager.updateSearchIndex(indexDescription);
		
		verify(mockIndexDao).clearSearchIndex(tableId);
		verifyNoMoreInteractions(mockIndexDao);
	}
	
	@Test
	public void testUpdateSearchIndexWithIterator() {
		
		List<TableRowData> rows = Arrays.asList(
			new TableRowData(1L, Arrays.asList(new TypedCellValue(ColumnType.STRING, "column 1 value"), new TypedCellValue(ColumnType.STRING, "column 2 value"))),
			new TableRowData(2L, Arrays.asList(new TypedCellValue(ColumnType.STRING, "column 1 value"), new TypedCellValue(ColumnType.STRING, "column 2 value")))
		);
		
		when(mockSearchProcessor.process(any(), anyBoolean())).thenReturn("processed value");

		IndexDescription indexDescription = new TableIndexDescription(tableId);
		
		// Call under test
		manager.updateSearchIndex(indexDescription, rows.iterator());

		ArgumentCaptor<TableRowData> dataCaptor = ArgumentCaptor.forClass(TableRowData.class);
		
		verify(mockSearchProcessor, times(rows.size())).process(dataCaptor.capture(), eq(false));
		
		assertEquals(rows, dataCaptor.getAllValues());
		
		verifyNoMoreInteractions(mockSearchProcessor);
		
		List<RowSearchContent> expectedBatch = rows.stream().map( data -> new RowSearchContent(data.getRowId(), "processed value")).collect(Collectors.toList());
		
		verify(mockIndexDao).updateSearchIndex(tableId, expectedBatch);
		
	}
	
	@Test
	public void testUpdateSearchIndexWithIteratorWithIncludesRowId() {
		
		List<TableRowData> rows = Arrays.asList(
			new TableRowData(1L, Arrays.asList(new TypedCellValue(ColumnType.STRING, "column 1 value"), new TypedCellValue(ColumnType.STRING, "column 2 value"))),
			new TableRowData(2L, Arrays.asList(new TypedCellValue(ColumnType.STRING, "column 1 value"), new TypedCellValue(ColumnType.STRING, "column 2 value")))
		);
		
		when(mockSearchProcessor.process(any(), anyBoolean())).thenReturn("processed value");

		IndexDescription indexDescription = Mockito.mock(IndexDescription.class);
		
		when(indexDescription.getIdAndVersion()).thenReturn(tableId);
		when(indexDescription.addRowIdToSearchIndex()).thenReturn(true);
		
		// Call under test
		manager.updateSearchIndex(indexDescription, rows.iterator());

		ArgumentCaptor<TableRowData> dataCaptor = ArgumentCaptor.forClass(TableRowData.class);
		
		verify(mockSearchProcessor, times(rows.size())).process(dataCaptor.capture(), eq(true));
		
		assertEquals(rows, dataCaptor.getAllValues());
		
		verifyNoMoreInteractions(mockSearchProcessor);
		
		List<RowSearchContent> expectedBatch = rows.stream().map( data -> new RowSearchContent(data.getRowId(), "processed value")).collect(Collectors.toList());
		
		verify(mockIndexDao).updateSearchIndex(tableId, expectedBatch);
		
	}
	
	@Test
	public void testUpdateSearchIndexWithIteratorAndNullSearchContent() {
		
		List<TableRowData> rows = Arrays.asList(
			new TableRowData(1L, Arrays.asList(new TypedCellValue(ColumnType.STRING, "column 1 value"), new TypedCellValue(ColumnType.STRING, "column 2 value"))),
			new TableRowData(2L, Arrays.asList(new TypedCellValue(ColumnType.STRING, "column 1 value"), new TypedCellValue(ColumnType.STRING, "column 2 value")))
		);
		
		when(mockSearchProcessor.process(any(), anyBoolean())).thenReturn("processed value", null);

		IndexDescription indexDescription = new TableIndexDescription(tableId);
		
		// Call under test
		manager.updateSearchIndex(indexDescription, rows.iterator());
		
		ArgumentCaptor<TableRowData> dataCaptor = ArgumentCaptor.forClass(TableRowData.class);
		
		verify(mockSearchProcessor, times(rows.size())).process(dataCaptor.capture(), eq(false));
		
		assertEquals(rows, dataCaptor.getAllValues());
		
		verifyNoMoreInteractions(mockSearchProcessor);
		
		verify(mockIndexDao).updateSearchIndex(tableId, Arrays.asList(new RowSearchContent(1L, "processed value"), new RowSearchContent(2L, null)));
		
	}
	
	@Test
	public void testUpdateSearchIndexWithIteratorAndNoContent() {
		
		List<TableRowData> rows = Collections.emptyList();
		
		IndexDescription indexDescription = new TableIndexDescription(tableId);
		
		// Call under test
		manager.updateSearchIndex(indexDescription, rows.iterator());
		
		verifyZeroInteractions(mockSearchProcessor);
		
		verifyZeroInteractions(mockIndexDao);
		
	}
	
	@Test
	public void testUpdateSearchIndexWithIteratorAndMultipleBatches() {
		
		int size = TableIndexManagerImpl.BATCH_SIZE * 2 - TableIndexManagerImpl.BATCH_SIZE/2;
		
		List<TableRowData> rows = LongStream.range(0, size).boxed().map(rowId -> 
			new TableRowData(rowId, Arrays.asList(new TypedCellValue(ColumnType.STRING, "column 1 value"), new TypedCellValue(ColumnType.STRING, "column 2 value")))
		).collect(Collectors.toList());
		
		when(mockSearchProcessor.process(any(), anyBoolean())).thenReturn("processed value");

		IndexDescription indexDescription = new TableIndexDescription(tableId);
		
		// Call under test
		manager.updateSearchIndex(indexDescription, rows.iterator());
		
		ArgumentCaptor<TableRowData> dataCaptor = ArgumentCaptor.forClass(TableRowData.class);
		
		verify(mockSearchProcessor, times(rows.size())).process(dataCaptor.capture(), eq(false));
		
		assertEquals(rows, dataCaptor.getAllValues());

		List<List<TableRowData>> expectedBatches = Lists.partition(rows, TableIndexManagerImpl.BATCH_SIZE);
		
		for (List<TableRowData> batch : expectedBatches) {
			List<RowSearchContent> expectedTransformedBatch = batch.stream().map( data -> new RowSearchContent(data.getRowId(), "processed value")).collect(Collectors.toList());
			verify(mockIndexDao).updateSearchIndex(tableId, expectedTransformedBatch);
		}

		verifyNoMoreInteractions(mockIndexDao);
		
	}
	
	@Test
	public void testUpdateSearchIndexWithIteratorAndEmptyIterator() {
		Iterator<TableRowData> rowData = Collections.emptyIterator();

		IndexDescription indexDescription = new TableIndexDescription(tableId);
		
		// Call under test
		manager.updateSearchIndex(indexDescription, rowData);
		
		verifyZeroInteractions(mockSearchProcessor);
		verifyZeroInteractions(mockIndexDao);
		
	}
		
	@Test
	public void testGetSchemaForSearchIndex() {
		schema = TableModelTestUtils.createOneOfEachType();
		
		// Call under test
		List<ColumnModel> searchSchema = manager.getSchemaForSearchIndex(schema);
		
		assertFalse(searchSchema.isEmpty());
		
		searchSchema.forEach(columnModel -> {
			assertTrue(TableConstants.SEARCH_TYPES.contains(columnModel.getColumnType()));
		});

	}
	
	@Test
	public void testGetSchemaForSearchIndexWithNoneElegible() {
		schema = Arrays.asList(
			TableModelTestUtils.createColumn(99L, "integer", ColumnType.INTEGER),
			TableModelTestUtils.createColumn(101L, "double", ColumnType.DOUBLE),
			TableModelTestUtils.createColumn(102L, "filehandleid", ColumnType.FILEHANDLEID)
		);
		
		// Call under test
		List<ColumnModel> searchSchema = manager.getSchemaForSearchIndex(schema);
		
		assertTrue(searchSchema.isEmpty());

	}
	
	@Test
	public void testIsRequireSearchIndexUpdateWithMix() {
		columnChanges = Arrays.asList(
			// Delete of non-eligible
			new ColumnChangeDetails(new ColumnModel().setColumnType(ColumnType.INTEGER), null),
			// Add of a new column
			new ColumnChangeDetails(null, new ColumnModel().setColumnType(ColumnType.STRING)),
			// Update of non-eligible
			new ColumnChangeDetails(new ColumnModel().setColumnType(ColumnType.DOUBLE), new ColumnModel().setColumnType(ColumnType.INTEGER)),
			// Update to eligible
			new ColumnChangeDetails(new ColumnModel().setColumnType(ColumnType.INTEGER), new ColumnModel().setColumnType(ColumnType.STRING))
		);
		
		// Call under test
		boolean result = TableIndexManagerImpl.isRequireSearchIndexUpdate(tableId, columnChanges);
		
		assertTrue(result);
		
	}
	
	@Test
	public void testIsRequireSearchIndexUpdateWithDeleteAndEligible() {
		columnChanges = Arrays.asList(
			new ColumnChangeDetails(new ColumnModel().setColumnType(ColumnType.STRING), null)	
		);
		
		// Call under test
		boolean result = TableIndexManagerImpl.isRequireSearchIndexUpdate(tableId, columnChanges);
		
		assertTrue(result);
		
	}
	
	@Test
	public void testIsRequireSearchIndexUpdateWithDeleteAndNotEligible() {
		columnChanges = Arrays.asList(
			new ColumnChangeDetails(new ColumnModel().setColumnType(ColumnType.INTEGER), null)	
		);
		
		// Call under test
		boolean result = TableIndexManagerImpl.isRequireSearchIndexUpdate(tableId, columnChanges);
		
		assertFalse(result);
		
	}
	
	@Test
	public void testIsRequireSearchIndexUpdateWithNewColumn() {
		columnChanges = Arrays.asList(
			new ColumnChangeDetails(null, new ColumnModel().setColumnType(ColumnType.STRING))	
		);
		
		// Call under test
		boolean result = TableIndexManagerImpl.isRequireSearchIndexUpdate(tableId, columnChanges);
		
		assertFalse(result);
		
	}
	
	@Test
	public void testIsRequireSearchIndexUpdateWithUpdateAndOldEligible() {
		columnChanges = Arrays.asList(
			new ColumnChangeDetails(new ColumnModel().setColumnType(ColumnType.STRING), new ColumnModel().setColumnType(ColumnType.INTEGER))	
		);
		
		// Call under test
		boolean result = TableIndexManagerImpl.isRequireSearchIndexUpdate(tableId, columnChanges);
		
		assertTrue(result);
		
	}
	
	@Test
	public void testIsRequireSearchIndexUpdateWithUpdateAndNewEligible() {
		columnChanges = Arrays.asList(
			new ColumnChangeDetails(new ColumnModel().setColumnType(ColumnType.INTEGER), new ColumnModel().setColumnType(ColumnType.STRING))	
		);
		
		// Call under test
		boolean result = TableIndexManagerImpl.isRequireSearchIndexUpdate(tableId, columnChanges);
		
		assertTrue(result);
		
	}
	
	@Test
	public void testIsRequireSearchIndexUpdateWithUpdateAndNoneEligible() {
		columnChanges = Arrays.asList(
			new ColumnChangeDetails(new ColumnModel().setColumnType(ColumnType.INTEGER), new ColumnModel().setColumnType(ColumnType.DOUBLE))	
		);
		
		// Call under test
		boolean result = TableIndexManagerImpl.isRequireSearchIndexUpdate(tableId, columnChanges);
		
		assertFalse(result);
		
	}
	
	@Test
	public void testIsRequireSearchIndexUpdateWithNoChanges() {
		columnChanges = Collections.emptyList();
		
		// Call under test
		boolean result = TableIndexManagerImpl.isRequireSearchIndexUpdate(tableId, columnChanges);
		
		assertFalse(result);
		
	}
	
	@Test
	public void testStreamTableToCSV() {
		CSVWriterStream stream = new CSVWriterStream() {
			
			@Override
			public void writeNext(String[] nextLine) {
				// nothing
			}
		};
		
		// Call under test
		manager.streamTableToCSV(tableId, stream);
		
		verify(mockIndexDao).streamTableToCSV(tableId, stream);
	}
	
	@SuppressWarnings("unchecked")
	public void setupExecuteInWriteTransaction() {
		// When a write transaction callback is used, we need to call the callback.
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				TransactionCallback callback = (TransactionCallback) invocation.getArguments()[0];
				callback.doInTransaction(mockTransactionStatus);
				return null;
			}
		}).when(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
	}

	/**
	 * Helper to setup both a row and column change within a list.
	 * 
	 * @return
	 * @throws NotFoundException
	 * @throws IOException
	 */
	List<TableChangeMetaData> setupMockChanges() throws NotFoundException, IOException {
		// add a row change
		TableChangeMetaData rowChange = setupMockRowChange(0L);
		TableChangeMetaData columnChange = setupMockColumnChange(1L);
		return Lists.newArrayList(rowChange, columnChange);
	}

	/**
	 * Helper to setup a mock TableChangeMetaData for a Row change.
	 * 
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public TableChangeMetaData setupMockRowChange(long changeNumber) throws NotFoundException, IOException {
		TestTableChangeMetaData<SparseChangeSet> testChange = new TestTableChangeMetaData<>();
		testChange.setChangeNumber(changeNumber);
		testChange.seteTag("etag-" + changeNumber);
		testChange.setChangeType(TableChangeType.ROW);
		ChangeData<SparseChangeSet> change = new ChangeData<SparseChangeSet>(changeNumber, sparseChangeSet);
		testChange.setChangeData(change);
		return testChange;
	}

	/**
	 * Helper to setup a mock TableChangeMetaData for a Column change.
	 * 
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public TableChangeMetaData setupMockColumnChange(long changeNumber) throws NotFoundException, IOException {
		TestTableChangeMetaData<SchemaChange> testChange = new TestTableChangeMetaData<>();
		testChange.setChangeNumber(changeNumber);
		testChange.seteTag("etag-" + changeNumber);
		testChange.setChangeType(TableChangeType.COLUMN);
		SchemaChange schemaChange = new SchemaChange(columnChanges);
		ChangeData<SchemaChange> change = new ChangeData<SchemaChange>(changeNumber, schemaChange);
		testChange.setChangeData(change);
		return testChange;
	}
	
	public TableChangeMetaData setupMockSearchChange(long changeNumber, boolean enableSearch) {

		TestTableChangeMetaData<SearchChange> testChange = new TestTableChangeMetaData<>();
		testChange.setChangeNumber(changeNumber);
		testChange.seteTag("etag-" + changeNumber);
		testChange.setChangeType(TableChangeType.SEARCH);

		ChangeData<SearchChange> change = new ChangeData<SearchChange>(changeNumber, new SearchChange(enableSearch));
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

		for (int i = 0; i < schema.size(); i++) {
			ColumnModel cm = schema.get(i);
			cm.setId("" + i);
		}

		return schema;
	}

	public List<ColumnModel> createDefaultColumnsWithIds(ObjectField exclude) {
		return createDefaultColumnsWithIds().stream().filter(model -> !model.getName().equals(exclude.name()))
				.collect(Collectors.toList());
	}

}
