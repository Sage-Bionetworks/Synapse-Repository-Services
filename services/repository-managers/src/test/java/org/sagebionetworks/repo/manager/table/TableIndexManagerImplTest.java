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
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.InvalidStatusTokenException;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableSnapshot;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.repo.model.semaphore.LockContext.ContextType;
import org.sagebionetworks.repo.model.table.ColumnConstants;
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
import org.sagebionetworks.table.cluster.description.MaterializedViewIndexDescription;
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
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockType;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.Lists;

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
	private ArgumentCaptor<List<ColumnChangeDetails>> changeCaptor;
	
	@Captor
	private ArgumentCaptor<Long> longCaptor;

	private TableIndexManagerImpl manager;
	private TableIndexManagerImpl managerSpy;

	private IdAndVersion tableId;
	private Long versionNumber;
	private SparseChangeSet sparseChangeSet;
	private List<ColumnModel> schema;
	private String schemaMD5Hex;
	private List<SelectColumn> selectColumns;

	private Grouping groupOne;
	private Grouping groupTwo;

	private Set<IdAndVersion> containerIds;
	private Long limit;
	private Long offset;
	private NextPageToken nextPageToken;
	private String tokenString;
	private List<String> scopeSynIds;
	private Set<Long> scopeIds;
	private ViewScope scope;

	private ViewObjectType objectType;
	private ColumnModel newColumn;
	private List<ColumnChangeDetails> columnChanges;

	private Set<Long> rowsIdsWithChanges;
	private ViewScopeType scopeType;
	private ObjectFieldModelResolver objectFieldModelResolver;
	
	private LockContext expectedContext;

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

		containerIds = Set.of(
			IdAndVersion.parse("1"),
			IdAndVersion.parse("2"),
			IdAndVersion.parse("3")
		);
				
		limit = 10L;
		offset = 0L;
		nextPageToken = new NextPageToken(limit, offset);
		tokenString = nextPageToken.toToken();
		scopeSynIds = List.of("syn123", "syn345");
		scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scopeSynIds));

		scope = new ViewScope();
		scope.setScope(scopeSynIds);
		scope.setViewTypeMask(ViewTypeMask.File.getMask());
		scope.setViewEntityType(ViewEntityType.entityview);

		ColumnModel oldColumn = null;
		newColumn = new ColumnModel();
		newColumn.setId("12");
		columnChanges = List.of(new ColumnChangeDetails(oldColumn, newColumn));

		rowsIdsWithChanges = Set.of(444L, 555L);
		scopeType = new ViewScopeType(objectType, ViewTypeMask.File.getMask());

		objectFieldModelResolver = new ObjectFieldModelResolverImpl(mockMetadataProvider);
		
		expectedContext = new LockContext(ContextType.BuildTableIndex, tableId);
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
	public void testNullSearchProcessor() {
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
		verify(mockIndexDao).applyFileHandleIdsToTable(tableId, Set.of(2L, 6L));
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

		Set<Long> expectedRows = Set.of(0L, 5L);
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
		verify(mockIndexDao).getTableDataForRowIds(tableId, Arrays.asList(schema.get(0), schema.get(1)), Set.of(0L, 5L));
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
		schema = List.of(column);

		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C44_");
		info.setColumnType(ColumnType.BOOLEAN);

		when(mockIndexDao.doesIndexHashMatchSchemaHash(any(), any())).thenReturn(false);
		when(mockIndexDao.getDatabaseInfo(any())).thenReturn(List.of(info));
		when(mockIndexDao.alterTableAsNeeded(any(IdAndVersion.class), anyList(), anyBoolean())).thenReturn(true);
		when(mockIndexDao.getMultivalueColumnIndexTableColumnIds(any())).thenReturn(Collections.emptySet());
		// call under test
		manager.setIndexSchema(new TableIndexDescription(tableId), schema);
		
		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(List.of(column.getId()));
		verify(mockIndexDao).doesIndexHashMatchSchemaHash(tableId, schema);
		verify(mockIndexDao, times(3)).getDatabaseInfo(tableId);
		verify(mockIndexDao).getMultivalueColumnIndexTableColumnIds(tableId);
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}
	
	@Test
	public void testSetIndexSchemaWithHashMatch() {
		ColumnModel column = new ColumnModel();
		column.setId("44");
		column.setColumnType(ColumnType.BOOLEAN);
		schema = List.of(column);
		
		when(mockIndexDao.doesIndexHashMatchSchemaHash(any(), any())).thenReturn(true);
		// call under test
		List<ColumnChangeDetails> changes = manager.setIndexSchema(new TableIndexDescription(tableId), schema);
		assertEquals(Collections.emptyList(), changes);
		
		verify(mockIndexDao).doesIndexHashMatchSchemaHash(tableId, schema);
		verify(mockIndexDao, never()).getDatabaseInfo(any());
		// it is critical that we do not call this unless needed. See: PLFM-7458.
		verify(mockIndexDao, never()).getMultivalueColumnIndexTableColumnIds(any());
		verify(mockIndexDao, never()).setCurrentSchemaMD5Hex(any(), any());
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
		schema = List.of(column);

		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C44_");
		info.setColumnType(ColumnType.BOOLEAN);

		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(List.of(info));
		when(mockIndexDao.alterTableAsNeeded(any(IdAndVersion.class), anyList(), anyBoolean())).thenReturn(true);
		when(mockIndexDao.getMultivalueColumnIndexTableColumnIds(tableId)).thenReturn(Collections.emptySet());
		// call under test
		manager.setIndexSchema(new TableIndexDescription(tableId), schema);
		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(List.of(column.getId()));
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
	}

	@Test
	public void testUpdateTableSchemaRemoveAllColumns() {
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("12");
		ColumnModel newColumn = null;
		boolean alterTemp = false;

		List<ColumnChangeDetails> changes = List.of(new ColumnChangeDetails(oldColumn, newColumn));
		when(mockIndexDao.alterTableAsNeeded(tableId, changes, alterTemp)).thenReturn(true);
		DatabaseColumnInfo current = new DatabaseColumnInfo();
		current.setColumnName(SQLUtils.getColumnNameForId(oldColumn.getId()));
		current.setColumnType(ColumnType.STRING);
		List<DatabaseColumnInfo> startSchema = List.of(current);
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(startSchema, new LinkedList<DatabaseColumnInfo>());
		doNothing().when(managerSpy).createTableIfDoesNotExist(any());
		// call under test
		managerSpy.updateTableSchema(new ViewIndexDescription(tableId, TableType.entityview), changes);
		verify(managerSpy).createTableIfDoesNotExist(new ViewIndexDescription(tableId, TableType.entityview));
		verify(mockIndexDao, times(2)).getDatabaseInfo(tableId);
		// The new schema is empty so the table is truncated.
		verify(mockIndexDao).truncateTable(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, changes, alterTemp);

		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(new LinkedList<>());
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}

	@Test
	public void testUpdateTableSchemaNoChange() {
		List<ColumnChangeDetails> changes = new LinkedList<ColumnChangeDetails>();
		boolean alterTemp = false;
		when(mockIndexDao.alterTableAsNeeded(tableId, changes, alterTemp)).thenReturn(false);
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(Collections.emptyList());
		doNothing().when(managerSpy).createTableIfDoesNotExist(any());
		// call under test
		managerSpy.updateTableSchema(new ViewIndexDescription(tableId, TableType.entityview), changes);
		verify(managerSpy).createTableIfDoesNotExist(new ViewIndexDescription(tableId, TableType.entityview));
		verify(mockIndexDao).alterTableAsNeeded(tableId, changes, alterTemp);
		verify(mockIndexDao, times(2)).getDatabaseInfo(tableId);
		verify(mockIndexDao).truncateTable(tableId);
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, TableModelUtils.createSchemaMD5Hex(Collections.emptyList()));
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
		when(mockIndexDao.getMultivalueColumnIndexTableColumnIds(tableId)).thenReturn(Set.of(123L, 456L));

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
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(any())).thenReturn(0L);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getViewFilter(tableId.getId())).thenReturn(mockFilter);
		
		List<ColumnModel> schema = createDefaultColumnsWithIds();

		// call under test
		Long version = managerSpy.populateViewFromEntityReplication(tableId.getId(), scopeType, schema);
		assertEquals(1L, version);
		verify(mockIndexDao).copyObjectReplicationToView(tableId.getId(), mockFilter, schema, mockMetadataProvider);
		verify(mockIndexDao).getMaxCurrentCompleteVersionForTable(tableId);
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
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(any())).thenReturn(0L);
		when(mockMetadataProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataProvider);
		when(mockMetadataProvider.getViewFilter(tableId.getId())).thenReturn(mockFilter);

		List<ColumnModel> schema = createDefaultColumnsWithIds();
		// call under test
		Long version = managerSpy.populateViewFromEntityReplication(tableId.getId(), scopeType, schema);
		assertEquals(1L, version);
		verify(mockIndexDao).copyObjectReplicationToView(tableId.getId(), mockFilter, schema, mockMetadataProvider);
		verify(mockIndexDao).getMaxCurrentCompleteVersionForTable(tableId);
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

		List<ColumnModel> schema = List.of(column);

		ColumnModel annotationModel = new ColumnModel();
		annotationModel.setName("foo");
		annotationModel.setColumnType(ColumnType.STRING);
		annotationModel.setMaximumSize(11L);

		when(mockIndexDao.getPossibleColumnModelsForContainers(any(), any(), any()))
				.thenReturn(List.of(annotationModel));

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
		verify(mockFilterBuilder).addExcludeAnnotationKeys(Set.of("testKey"));
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
		verify(mockFilterBuilder).addExcludeAnnotationKeys(Set.of("testKey"));
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
		
		Set<IdAndVersion> expectedScope = scope.getScope().stream().map(id -> IdAndVersion.parse(id)).collect(Collectors.toSet());
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

		when(mockDefaultColumnModel.getCustomFields()).thenReturn(List.of(customField));
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
		List<DatabaseColumnInfo> curretIndexSchema = List.of(rowId, one, two);
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(curretIndexSchema);

		// the old does not exist in the current
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("333");
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("444");
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);
		List<ColumnChangeDetails> changes = List.of(change);

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
		doReturn(columnChanges).when(managerSpy).setIndexSchema(any(), any());
		doNothing().when(managerSpy).updateSearchIndexFromSchemaChange(any(), any());
		doNothing().when(managerSpy).applyChangeSetToIndex(any(), any(), anyLong());
		
		// call under test
		managerSpy.applyRowChangeToIndex(tableId, change);

		TableIndexDescription index = new TableIndexDescription(tableId);
		
		verify(managerSpy).setIndexSchema(index, sparseChangeSet.getSchema());
		verify(managerSpy).updateSearchIndexFromSchemaChange(index, columnChanges);
		verify(managerSpy).applyChangeSetToIndex(tableId, sparseChangeSet, changeNumber);
	}

	@Test
	public void testApplySchemaChangeToIndex() {
		TableIndexDescription index = new TableIndexDescription(tableId);
		long changeNumber = 333l;
		SchemaChange schemaChange = new SchemaChange(columnChanges);
		ChangeData<SchemaChange> change = new ChangeData<SchemaChange>(changeNumber, schemaChange);
		doReturn(true).when(managerSpy).updateTableSchema(any(), any());
		doNothing().when(managerSpy).updateSearchIndexFromSchemaChange(any(), any());
		doNothing().when(managerSpy).alterListColumnIndexTableWithSchemaChange(any(), any(), anyBoolean());
		doNothing().when(managerSpy).setIndexVersion(tableId, changeNumber);
		
		// Call under test
		managerSpy.applySchemaChangeToIndex(tableId, change);
		
		verify(managerSpy).updateTableSchema(index, columnChanges);
		verify(managerSpy).updateSearchIndexFromSchemaChange(index, columnChanges);
		verify(managerSpy).alterListColumnIndexTableWithSchemaChange(tableId, columnChanges, false);
		verify(managerSpy).setIndexVersion(tableId, changeNumber);
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
		verify(mockManagerSupport).tryRunWithTableExclusiveLock(eq(mockCallback), eq(expectedContext), eq(tableId), any());
		verify(managerSpy).buildTableIndexWithLock(mockCallback, tableId, iterator);
	}

	/**
	 * LockUnavilableException translates to RecoverableMessageException
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBuildIndexToChangeNumber_LockUnavilableException() throws Exception {
		LockUnavilableException exception = new LockUnavilableException(LockType.Read, "key", "context");
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(LockContext.class), any(IdAndVersion.class),
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
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(), any(IdAndVersion.class),
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
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(), any(IdAndVersion.class),
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
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(), any(IdAndVersion.class),
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
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(), any(IdAndVersion.class),
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
		when(mockManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(), any(IdAndVersion.class),
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
			ProgressingCallable callable = invocation.getArgument(3, ProgressingCallable.class);
			callable.call(callback);
			return null;
		}).when(mockManagerSupport).tryRunWithTableExclusiveLock(any(), any(), any(IdAndVersion.class), any());
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
		verify(managerSpy).attemptToRestoreTableFromExistingSnapshot(tableId, resetToken, targetChangeNumber);
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
		schema = List.of(notAList, listOne, listTwo);

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

		schema = List.of(listOne);
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

		IndexDescription indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		
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
		
		IndexDescription indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		
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
		
		IndexDescription indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		
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
			manager.updateViewRowsInTransaction(new ViewIndexDescription(tableId, TableType.entityview), scopeType, schema, mockFilter);
		});
	}

	@Test
	public void testUpdateViewRowsInTransaction_Schema() {
		schema = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateViewRowsInTransaction(new ViewIndexDescription(tableId, TableType.entityview), scopeType, schema, mockFilter);
		});
	}

	@Test
	public void testListColumnIndexTableChangesFromExpectedSchema_nullExpectedSchema() {
		assertThrows(IllegalArgumentException.class,
				() -> TableIndexManagerImpl.listColumnIndexTableChangesFromExpectedSchema(null, Set.of(123L)));
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
		Set<Long> existingIndexTableColumns = Set.of(removeListColId, unchangedListColId);

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
				() -> TableIndexManagerImpl.listColumnIndexTableChangesFromChangeDetails(null, Set.of(123L)));
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

		Set<Long> existingColumnChangeIds = Set.of(Long.parseLong(columnModel.getId()));

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

		Set<Long> existingColumnChangeIds = Set.of(Long.parseLong(columnModel.getId()));

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
		Set<Long> existingColumnChangeIds = Set.of(columnModelId);

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
		Set<Long> existingColumnChangeIds = Set.of(oldListId, newListId);

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

		Set<Long> existingColumnChangeIds = Set.of(oldListId);

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
		Set<Long> existingColumnChangeIds = Set.of(newListId);

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

		Set<Long> existingColumnChangeIds = Set.of(oldListId);

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
				.thenReturn(List.of(annotationModel));

		List<ColumnModel> schema = List.of(columnModel);

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
				.thenReturn(List.of(a1, a2));

		List<ColumnModel> schema = List.of(columnModel);

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
	public void testDetermineCauseOfExceptionWithPessimisticLockException() {
		Exception original = new PessimisticLockingFailureException("Some exception");
		
		RecoverableMessageException ex = assertThrows(RecoverableMessageException.class, () -> {
			// Call under test
			manager.determineCauseOfReplicationFailure(original, schema, mockMetadataProvider, scope.getViewTypeMask(),
					mockFilter);
		});

		assertEquals(original, ex.getCause());
		assertEquals("org.springframework.dao.PessimisticLockingFailureException: Some exception", ex.getMessage());
		
		verifyZeroInteractions(mockMetadataProvider);
		verifyZeroInteractions(mockObjectFieldModelResolver);
		verifyZeroInteractions(mockIndexDao);
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
		
		manager.createTableIfDoesNotExist(new ViewIndexDescription(tableId, TableType.entityview));
		
		verify(mockIndexDao).createTableIfDoesNotExist(new ViewIndexDescription(tableId, TableType.entityview));
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
	public void testAttemptToRestoreTableFromExistingSnapshot() {
		
		tableId = IdAndVersion.parse("123");
		IdAndVersion snapshotId = IdAndVersion.parse("123.12"); 
		IndexDescription index = new TableIndexDescription(tableId);
		String resetToken = "restToken";
		long snapshotChangeNumber = 456;
		long targetChangeNumber = 789;
		
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(any())).thenReturn(-1L);
		when(mockManagerSupport.getMostRecentTableSnapshot(any())).thenReturn(Optional.of(new TableSnapshot()
			.withBucket("bucket")
			.withKey("key")
			.withTableId(snapshotId.getId())
			.withVersion(snapshotId.getVersion().get())
		));
		when(mockManagerSupport.getLastTableChangeNumber(any())).thenReturn(Optional.of(snapshotChangeNumber));
		when(mockManagerSupport.getTableSchema(any())).thenReturn(schema);
		when(mockManagerSupport.isTableSearchEnabled(any())).thenReturn(true);
		
		doReturn(schema).when(managerSpy).resetTableIndex(any(), any(), anyBoolean());
		doNothing().when(mockManagerSupport).restoreTableIndexFromS3(any(), any(), any());
		doNothing().when(managerSpy).buildTableIndexIndices(any(), any());
		doNothing().when(managerSpy).setIndexVersion(any(), any());
		
		// Call under test
		managerSpy.attemptToRestoreTableFromExistingSnapshot(tableId, resetToken, targetChangeNumber);
		
		verify(mockIndexDao).getMaxCurrentCompleteVersionForTable(tableId);
		verify(mockManagerSupport).getMostRecentTableSnapshot(tableId);
		verify(mockManagerSupport).getLastTableChangeNumber(snapshotId);
		verify(mockManagerSupport).attemptToUpdateTableProgress(tableId, resetToken, "Restoring table syn123 from snapshot syn123.12", snapshotChangeNumber, targetChangeNumber);
		verify(mockManagerSupport).getTableSchema(snapshotId);
		verify(mockManagerSupport).isTableSearchEnabled(snapshotId);
		verify(managerSpy).resetTableIndex(index, schema, true);
		verify(mockManagerSupport).restoreTableIndexFromS3(tableId, "bucket", "key");
		verify(managerSpy).buildTableIndexIndices(index, schema);
		verify(managerSpy).setIndexVersion(tableId, snapshotChangeNumber);
	}
	
	@Test
	public void testAttemptToRestoreTableFromExistingSnapshotWithMissingChangeNumber() {
		
		tableId = IdAndVersion.parse("123");
		IdAndVersion snapshotId = IdAndVersion.parse("123.12");
		String resetToken = "restToken";
		long targetChangeNumber = 789;
		
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(any())).thenReturn(-1L);
		when(mockManagerSupport.getMostRecentTableSnapshot(any())).thenReturn(Optional.of(new TableSnapshot()
				.withBucket("bucket")
				.withKey("key")
				.withTableId(snapshotId.getId())
				.withVersion(snapshotId.getVersion().get())
				));
		when(mockManagerSupport.getLastTableChangeNumber(any())).thenReturn(Optional.empty());

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			managerSpy.attemptToRestoreTableFromExistingSnapshot(tableId, resetToken, targetChangeNumber);
		});
		
		assertEquals("Expected a change number for snapshot syn123.12, but found none.", result.getMessage());
		
		verify(mockIndexDao).getMaxCurrentCompleteVersionForTable(tableId);
		verify(mockManagerSupport).getMostRecentTableSnapshot(tableId);
		verify(mockManagerSupport).getLastTableChangeNumber(snapshotId);
		verifyNoMoreInteractions(mockIndexDao);
		verifyNoMoreInteractions(mockManagerSupport);
	}
	
	@Test
	public void testAttemptToRestoreTableFromExistingSnapshotWithExistingChanges() {
		
		tableId = IdAndVersion.parse("123");
		String resetToken = "restToken";
		long targetChangeNumber = 789;
		
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(any())).thenReturn(123L);
		
		// Call under test
		managerSpy.attemptToRestoreTableFromExistingSnapshot(tableId, resetToken, targetChangeNumber);
		
		verify(mockIndexDao).getMaxCurrentCompleteVersionForTable(tableId);
		verifyNoMoreInteractions(mockIndexDao);
		verifyZeroInteractions(mockManagerSupport);
	}
	
	@Test
	public void testResetTableIndex() {
		
		TableIndexDescription index = new TableIndexDescription(tableId);
		
		when(mockManagerSupport.getTableSchema(any())).thenReturn(schema);
		when(mockManagerSupport.isTableSearchEnabled(any())).thenReturn(true);
		
		doReturn(schema).when(managerSpy).resetTableIndex(any(), any(), anyBoolean());
		
		// Call under test
		managerSpy.resetTableIndex(index);
		
		verify(mockManagerSupport).getTableSchema(index.getIdAndVersion());
		verify(mockManagerSupport).isTableSearchEnabled(index.getIdAndVersion());
		verify(managerSpy).resetTableIndex(index, schema, true);
	}
	
	@Test
	public void testResetTableIndexWithSchema() {
		
		TableIndexDescription index = new TableIndexDescription(tableId);
		
		doNothing().when(managerSpy).deleteTableIndex(any());
		doReturn(Collections.emptyList()).when(managerSpy).setIndexSchema(any(), any());
		doNothing().when(managerSpy).setSearchEnabled(any(), anyBoolean());
		
		// Call under test
		managerSpy.resetTableIndex(index, schema, true);
		
		verify(managerSpy).deleteTableIndex(tableId);
		verify(managerSpy).setIndexSchema(index, schema);
		verify(managerSpy).setSearchEnabled(tableId, true);
	}
	
	@Test
	public void testResetTableIndexWithSchemaAndSearchDisabled() {
		
		TableIndexDescription index = new TableIndexDescription(tableId);
		
		doNothing().when(managerSpy).deleteTableIndex(any());
		doReturn(Collections.emptyList()).when(managerSpy).setIndexSchema(any(), any());
		doNothing().when(managerSpy).setSearchEnabled(any(), anyBoolean());
		
		// Call under test
		managerSpy.resetTableIndex(index, schema, false);
		
		verify(managerSpy).deleteTableIndex(tableId);
		verify(managerSpy).setIndexSchema(index, schema);
		verify(managerSpy).setSearchEnabled(tableId, false);
	}
	
	@Test
	public void testBuildTableIndexIndices() {
		TableIndexDescription index = new TableIndexDescription(tableId);
		
		doNothing().when(managerSpy).optimizeTableIndices(any());
		doNothing().when(managerSpy).populateListColumnIndexTables(any(), any());
		when(mockIndexDao.isSearchEnabled(any())).thenReturn(true);
		doNothing().when(managerSpy).updateSearchIndex(any());
		doNothing().when(managerSpy).populateFileHandleIndex(any(), any());
		
		// Call under test
		managerSpy.buildTableIndexIndices(index, schema);
		
		verify(managerSpy).optimizeTableIndices(tableId);
		verify(managerSpy).populateListColumnIndexTables(tableId, schema);
		verify(mockIndexDao).isSearchEnabled(tableId);
		verify(managerSpy).updateSearchIndex(index);
		verify(managerSpy).populateFileHandleIndex(index, schema);
	}
	
	@Test
	public void testBuildTableIndexIndicesWithView() {
		ViewIndexDescription index = new ViewIndexDescription(tableId, TableType.entityview);
		
		doNothing().when(managerSpy).optimizeTableIndices(any());
		doNothing().when(managerSpy).populateListColumnIndexTables(any(), any());
		when(mockIndexDao.isSearchEnabled(any())).thenReturn(true);
		doNothing().when(managerSpy).updateSearchIndex(any());
		
		// Call under test
		managerSpy.buildTableIndexIndices(index, schema);
		
		verify(managerSpy).optimizeTableIndices(tableId);
		verify(managerSpy).populateListColumnIndexTables(tableId, schema);
		verify(mockIndexDao).isSearchEnabled(tableId);
		verify(managerSpy).updateSearchIndex(index);
		verify(managerSpy, never()).populateFileHandleIndex(index, schema);
	}
	
	@Test
	public void testBuildTableIndexIndicesWithMaterializedView() {
		MaterializedViewIndexDescription index = new MaterializedViewIndexDescription(tableId, Collections.emptyList());
		
		doNothing().when(managerSpy).optimizeTableIndices(any());
		doNothing().when(managerSpy).populateListColumnIndexTables(any(), any());
		when(mockIndexDao.isSearchEnabled(any())).thenReturn(true);
		doNothing().when(managerSpy).updateSearchIndex(any());
		
		// Call under test
		managerSpy.buildTableIndexIndices(index, schema);
		
		verify(managerSpy).optimizeTableIndices(tableId);
		verify(managerSpy).populateListColumnIndexTables(tableId, schema);
		verify(mockIndexDao).isSearchEnabled(tableId);
		verify(managerSpy).updateSearchIndex(index);
		verify(managerSpy, never()).populateFileHandleIndex(index, schema);
	}
	
	@Test
	public void testBuildTableIndexIndicesWithSearchDisabled() {
		TableIndexDescription index = new TableIndexDescription(tableId);
		
		doNothing().when(managerSpy).optimizeTableIndices(any());
		doNothing().when(managerSpy).populateListColumnIndexTables(any(), any());
		when(mockIndexDao.isSearchEnabled(any())).thenReturn(false);
		doNothing().when(managerSpy).populateFileHandleIndex(any(), any());
		
		// Call under test
		managerSpy.buildTableIndexIndices(index, schema);
		
		verify(managerSpy).optimizeTableIndices(tableId);
		verify(managerSpy).populateListColumnIndexTables(tableId, schema);
		verify(mockIndexDao).isSearchEnabled(tableId);
		verify(managerSpy, never()).updateSearchIndex(any());
		verify(managerSpy).populateFileHandleIndex(index, schema);
	}
	
	@Test
	public void testPopulateFileHandleIndex() {
		TableIndexDescription index = new TableIndexDescription(tableId);
		
		List<TableRowData> batch = List.of(
			new TableRowData(1L, List.of(new TypedCellValue(ColumnType.FILEHANDLEID, "123"), new TypedCellValue(ColumnType.FILEHANDLEID, null), new TypedCellValue(ColumnType.FILEHANDLEID, "456"))),
			new TableRowData(2L, List.of(new TypedCellValue(ColumnType.FILEHANDLEID, "789"), new TypedCellValue(ColumnType.FILEHANDLEID, ""))),
			new TableRowData(3L, List.of(new TypedCellValue(ColumnType.FILEHANDLEID, "123"), new TypedCellValue(ColumnType.FILEHANDLEID, "789")))
		);
		
		when(mockIndexDao.getTableDataPage(any(), any(), anyLong(), anyLong())).thenReturn(batch, Collections.emptyList());
		
		List<ColumnModel> expectedSchema = schema.stream().filter(column -> ColumnType.FILEHANDLEID.equals(column.getColumnType())).collect(Collectors.toList());
		Set<Long> expectedFileIds = Set.of(123L, 456L, 789L);
		
		// Call under test
		manager.populateFileHandleIndex(index, schema);
		
		verify(mockIndexDao).getTableDataPage(tableId, expectedSchema, TableIndexManagerImpl.BATCH_SIZE, 0);
		verify(mockIndexDao).applyFileHandleIdsToTable(tableId, expectedFileIds);
	}
	
	@Test
	public void testPopulateFileHandleIndexWithNoFileColumns() {
		TableIndexDescription index = new TableIndexDescription(tableId);
		
		schema = List.of(
			TableModelTestUtils.createColumn(99L, "aString", ColumnType.STRING),
			TableModelTestUtils.createColumn(101L, "aNumber", ColumnType.INTEGER)
		);
		
		// Call under test
		manager.populateFileHandleIndex(index, schema);

		verifyZeroInteractions(mockIndexDao);
	}
	
	@Test
	public void testUpdateSearchIndexFromSchemaChange() {
		TableIndexDescription index = new TableIndexDescription(tableId);
		
		columnChanges = Arrays.asList(
			new ColumnChangeDetails(new ColumnModel().setId("1").setColumnType(ColumnType.STRING), null)
		);
		
		when(mockIndexDao.isSearchEnabled(any())).thenReturn(true);
		doNothing().when(managerSpy).updateSearchIndex(any());
		
		// Call under test
		managerSpy.updateSearchIndexFromSchemaChange(index, columnChanges);
		
		verify(mockIndexDao).isSearchEnabled(tableId);
		verify(managerSpy).updateSearchIndex(index);
		
	}
	
	@Test
	public void testUpdateSearchIndexFromSchemaChangeWithNoEligibleChange() {
		TableIndexDescription index = new TableIndexDescription(tableId);

		columnChanges = Arrays.asList(
			// Adding a new column does not required re-indexing as there is no data yet
			new ColumnChangeDetails(null, new ColumnModel().setId("1").setColumnType(ColumnType.STRING))
		);
		
		// Call under test
		managerSpy.updateSearchIndexFromSchemaChange(index, columnChanges);
		
		verify(mockIndexDao, never()).isSearchEnabled(tableId);
		verify(managerSpy, never()).updateSearchIndex(index);
		
	}
	
	@Test
	public void testUpdateSearchIndexFromSchemaChangeWithNoChanges() {
		TableIndexDescription index = new TableIndexDescription(tableId);
		
		columnChanges = Collections.emptyList();
		
		// Call under test
		managerSpy.updateSearchIndexFromSchemaChange(index, columnChanges);
		
		verify(mockIndexDao, never()).isSearchEnabled(tableId);
		verify(managerSpy, never()).updateSearchIndex(index);
		
	}
	
	@Test
	public void testAlterTempTableSchema() {
		doNothing().when(managerSpy).validateTableMaximumListLengthChanges(any(),  anyList());
		doNothing().when(managerSpy).validateSchemaChangeToMediumText(any(), anyList());
		doReturn(true).when(managerSpy).alterTableAsNeededWithinAutoProgress(any(), anyList(), anyBoolean());
		doNothing().when(managerSpy).alterListColumnIndexTableWithSchemaChange(any(), anyList(), anyBoolean());
		
		// Call under test
		managerSpy.alterTempTableSchema(tableId, columnChanges);
		
		verify(managerSpy).validateTableMaximumListLengthChanges(tableId, columnChanges);
		verify(managerSpy).validateSchemaChangeToMediumText(tableId, columnChanges);
		verify(managerSpy).alterTableAsNeededWithinAutoProgress(tableId, columnChanges, true);
		verify(managerSpy).alterListColumnIndexTableWithSchemaChange(tableId, columnChanges, true);
		
	}
	
	@Test
	public void testValidateSchemaChangeToMediumText() {
		when(mockIndexDao.tempTableColumnExceedsCharacterLimit(any(), any(), anyLong())).thenReturn(Optional.empty());
		
		columnChanges = List.of(
			new ColumnChangeDetails(
				new ColumnModel().setId("1").setColumnType(ColumnType.LARGETEXT).setName("oldColumn"),
				new ColumnModel().setId("2").setColumnType(ColumnType.MEDIUMTEXT).setName("newColumn")
			)	
		);
		
		// Call under test
		manager.validateSchemaChangeToMediumText(tableId, columnChanges);
		
		verify(mockIndexDao).tempTableColumnExceedsCharacterLimit(tableId, "1", ColumnConstants.MAX_MEDIUM_TEXT_CHARACTERS);
	}
	
	@Test
	public void testValidateSchemaChangeToMediumTextWithOldColumnExceedsLimitTrue() {
		when(mockIndexDao.tempTableColumnExceedsCharacterLimit(any(), any(), anyLong())).thenReturn(Optional.of(456L));
		
		columnChanges = List.of(
			new ColumnChangeDetails(
				new ColumnModel().setId("1").setColumnType(ColumnType.LARGETEXT).setName("oldColumn"),
				new ColumnModel().setId("2").setColumnType(ColumnType.MEDIUMTEXT).setName("newColumn")
			)	
		);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.validateSchemaChangeToMediumText(tableId, columnChanges);
		}).getMessage();
		
		assertEquals("Cannot change column \"oldColumn\" to MEDIUMTEXT: The data at row 456 exceeds the MEDIUMTEXT limit of 2000 characters.", result);
		
		verify(mockIndexDao).tempTableColumnExceedsCharacterLimit(tableId, "1", ColumnConstants.MAX_MEDIUM_TEXT_CHARACTERS);
	}
	
	@Test
	public void testValidateSchemaChangeToMediumTextWithOldColumnNull() {
		columnChanges = List.of(
			new ColumnChangeDetails(
				null,
				new ColumnModel().setId("2").setColumnType(ColumnType.MEDIUMTEXT).setName("newColumn")
			)	
		);
					
		// Call under test
		manager.validateSchemaChangeToMediumText(tableId, columnChanges);
		
		verifyZeroInteractions(mockIndexDao);
	}
	
	@Test
	public void testValidateSchemaChangeToMediumTextWithOldColumnNotLargeText() {
		columnChanges = List.of(
			new ColumnChangeDetails(
				new ColumnModel().setId("1").setColumnType(ColumnType.STRING).setName("oldColumn"),
				new ColumnModel().setId("2").setColumnType(ColumnType.MEDIUMTEXT).setName("newColumn")
			)	
		);
					
		// Call under test
		manager.validateSchemaChangeToMediumText(tableId, columnChanges);
		
		verifyZeroInteractions(mockIndexDao);
	}
	
	@Test
	public void testValidateSchemaChangeToMediumTextWithNewColumnNull() {
		columnChanges = List.of(
			new ColumnChangeDetails(
				new ColumnModel().setId("1").setColumnType(ColumnType.LARGETEXT).setName("oldColumn"),
				null
			)	
		);
					
		// Call under test
		manager.validateSchemaChangeToMediumText(tableId, columnChanges);
		
		verifyZeroInteractions(mockIndexDao);
	}
	
	@Test
	public void testValidateSchemaChangeToMediumTextWithNewColumnNotMediumText() {
		columnChanges = List.of(
			new ColumnChangeDetails(
				new ColumnModel().setId("1").setColumnType(ColumnType.LARGETEXT).setName("oldColumn"),
				new ColumnModel().setId("2").setColumnType(ColumnType.STRING).setName("newColumn")
			)	
		);
					
		// Call under test
		manager.validateSchemaChangeToMediumText(tableId, columnChanges);
		
		verifyZeroInteractions(mockIndexDao);
	}
	
	@Test
	public void testSwapTableIndex() {
		TableIndexDescription source = new TableIndexDescription(IdAndVersion.parse("123"));
		TableIndexDescription target = new TableIndexDescription(IdAndVersion.parse("456"));
		
		// Call under test
		manager.swapTableIndex(source, target);
		
		verify(mockIndexDao).swapTableIndex(source.getIdAndVersion(), target.getIdAndVersion());
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
		return List.of(rowChange, columnChange);
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
