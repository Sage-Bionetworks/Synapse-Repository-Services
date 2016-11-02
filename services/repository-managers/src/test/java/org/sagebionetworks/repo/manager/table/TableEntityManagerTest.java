package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.RowSetAccessor;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.exception.ReadOnlyException;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnChangeDetails;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TableEntityManagerTest {
	
	@Mock
	ProgressCallback<Long> mockProgressCallback;
	@Mock
	StackStatusDao mockStackStatusDao;
	@Mock
	TableRowTruthDAO mockTruthDao;
	@Mock
	ConnectionFactory mockTableConnectionFactory;
	@Mock
	TableIndexDAO mockTableIndexDAO;
	@Mock
	ProgressCallback<Object> mockProgressCallback2;
	@Mock
	ProgressCallback<Void> mockProgressCallbackVoid;
	@Mock
	FileHandleDao mockFileDao;
	@Mock
	ColumnModelManager mockColumModelManager;
	@Mock
	TableManagerSupport mockTableManagerSupport;
	@Mock
	TableIndexManager mockIndexManager;
	@Captor
	ArgumentCaptor<List<String>> stringListCaptor;

	
	List<ColumnModel> models;
	String schemaMD5Hex;
	
	TableEntityManagerImpl manager;
	UserInfo user;
	String tableId;
	Long tableIdLong;
	List<Row> rows;
	RowSet set;
	RawRowSet rawSet;
	PartialRowSet partialSet;
	RawRowSet expectedRawRows;
	RowReferenceSet refSet;
	long rowIdSequence;
	long rowVersionSequence;
	int maxBytesPerRequest;
	List<FileHandleAuthorizationStatus> fileAuthResults;
	TableStatus status;
	String ETAG;
	IdRange range;
	
	TableSchemaChangeRequest schemaChangeRequest;
	List<ColumnChangeDetails> columChangedetails;
	List<String> newColumnIds;
	
	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception {
		Assume.assumeTrue(StackConfiguration.singleton().getTableEnabled());
		MockitoAnnotations.initMocks(this);
		
		manager = new TableEntityManagerImpl();
		ReflectionTestUtils.setField(manager, "stackStatusDao", mockStackStatusDao);
		ReflectionTestUtils.setField(manager, "tableRowTruthDao", mockTruthDao);
		ReflectionTestUtils.setField(manager, "tableConnectionFactory", mockTableConnectionFactory);
		ReflectionTestUtils.setField(manager, "fileHandleDao", mockFileDao);
		ReflectionTestUtils.setField(manager, "columModelManager", mockColumModelManager);
		ReflectionTestUtils.setField(manager, "tableManagerSupport", mockTableManagerSupport);
		
		maxBytesPerRequest = 10000000;
		manager.setMaxBytesPerRequest(maxBytesPerRequest);
		manager.setMaxBytesPerChangeSet(1000000000);
		user = new UserInfo(false, 7L);
		models = TableModelTestUtils.createOneOfEachType(true);
		schemaMD5Hex = TableModelUtils.createSchemaMD5HexCM(models);
		tableId = "syn123";
		tableIdLong = KeyFactory.stringToKey(tableId);
		rows = TableModelTestUtils.createRows(models, 10);
		set = new RowSet();
		set.setTableId(tableId);
		set.setHeaders(TableModelUtils.getSelectColumns(models));
		set.setRows(rows);
		rawSet = new RawRowSet(TableModelUtils.getIds(models), null, tableId, Lists.newArrayList(rows));

		List<PartialRow> partialRows = TableModelTestUtils.createPartialRows(models, 10);
		partialSet = new PartialRowSet();
		partialSet.setTableId(tableId);
		partialSet.setRows(partialRows);
		
		rows = TableModelTestUtils.createExpectedFullRows(models, 10);
		expectedRawRows = new RawRowSet(TableModelUtils.getIds(models), null, tableId, rows);
		
		refSet = new RowReferenceSet();
		refSet.setTableId(tableId);
		refSet.setHeaders(TableModelUtils.getSelectColumns(models));
		refSet.setRows(new LinkedList<RowReference>());
		refSet.setEtag("etag123");
		
		when(mockTableManagerSupport.getColumnModelsForTable(tableId)).thenReturn(models);
		when(mockTableConnectionFactory.getConnection(tableId)).thenReturn(mockTableIndexDAO);
		
		// Just call the caller.
		stub(mockTableManagerSupport.tryRunWithTableNonexclusiveLock(any(ProgressCallback.class),anyString(), anyInt(), any(ProgressingCallable.class))).toAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				if(invocation == null) return null;
				ProgressingCallable<Object, Object> callable = (ProgressingCallable<Object, Object>) invocation.getArguments()[3];
						if (callable != null) {
							return callable.call(mockProgressCallback2);
						} else {
							return null;
						}
			}
		});

		// read-write be default.
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
		rowIdSequence = 0;
		rowVersionSequence = 0;		
		// By default set the user as the creator of all files handles
		doAnswer(new Answer<Set<String>>() {

			@Override
			public Set<String> answer(InvocationOnMock invocation)
					throws Throwable {
				// returning all passed files
				List<String> input = (List<String>) invocation.getArguments()[1];
				if(input != null){
					return new HashSet<String>(input);
				}
				return null;
			}
		}).when(mockFileDao).getFileHandleIdsCreatedByUser(anyLong(), any(List.class));
		
		status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.PROCESSING);
		status.setChangedOn(new Date(123));
		status.setLastTableChangeEtag("etag");
		ETAG = "";
		
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		
		List<ColumnChange> changes = TableModelTestUtils.createAddUpdateDeleteColumnChange();
		schemaChangeRequest = new TableSchemaChangeRequest();
		schemaChangeRequest.setChanges(changes);
		schemaChangeRequest.setEntityId(tableId);
		
		columChangedetails = TableModelTestUtils.createDetailsForChanges(changes);
		
		when(mockColumModelManager.getColumnChangeDetails(changes)).thenReturn(columChangedetails);
		newColumnIds = Lists.newArrayList("111","333");
		when(mockColumModelManager.calculateNewSchemaIdsAndValidate(tableId, changes)).thenReturn(newColumnIds);
		
		when(mockTableManagerSupport.getTableEntityType(tableId)).thenReturn(EntityType.table);
		
		doAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				RowHandler handler = (RowHandler) invocation.getArguments()[2];
				// pass each row
				long rowId = 0;
				for(Row row: rows){
					Row copy = new Row();
					copy.setRowId(rowId++);
					copy.setValues(new LinkedList<String>(row.getValues()));
					handler.nextRow(copy);
				}
				return true;
			}
		}).when(mockTableIndexDAO).queryAsStream(any(ProgressCallback.class), any(SqlQuery.class), any(RowHandler.class));
		
		range = new IdRange();
		range.setEtag("rangeEtag");
		range.setVersionNumber(3L);
		range.setMaximumId(100L);
		range.setMaximumUpdateId(50L);
		range.setMinimumId(51L);
		
		IdRange range2 = new IdRange();
		range2.setEtag("rangeEtag");
		range2.setVersionNumber(4L);
		range2.setMaximumId(100L);
		range2.setMaximumUpdateId(50L);
		range2.setMinimumId(51L);
		
		IdRange range3 = new IdRange();
		range3.setEtag("rangeEtag");
		range3.setVersionNumber(5L);
		range3.setMaximumId(100L);
		range3.setMaximumUpdateId(50L);
		range3.setMinimumId(51L);
		
		when(mockTruthDao.reserveIdsInRange(eq(tableId), anyInt())).thenReturn(range, range2, range3);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testAppendRowsUnauthroized() throws DatastoreException, NotFoundException, IOException{
		doThrow(new UnauthorizedException()).when(mockTableManagerSupport).validateTableWriteAccess(user, tableId);
		manager.appendRows(user, tableId, models, set, mockProgressCallback);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testAppendRowsAsStreamUnauthroized() throws DatastoreException, NotFoundException, IOException{
		doThrow(new UnauthorizedException()).when(mockTableManagerSupport).validateTableWriteAccess(user, tableId);
		manager.appendRowsAsStream(user, tableId, models, set.getRows().iterator(),
				"etag",
				null, mockProgressCallback);
	}
	
	@Test
	public void testAppendRowsToTable() throws IOException{
		// assign a rowId and version to trigger a row level conflict test.
		rawSet.getRows().get(0).setRowId(1L);
		rawSet.getRows().get(0).setVersionNumber(0L);
		
		int rowCount = rawSet.getRows().size();
		// Call under test
		RowReferenceSet refSet = manager.appendRowsToTable(user, models, rawSet);
		assertNotNull(refSet);
		assertEquals(tableId, refSet.getTableId());
		assertEquals(range.getEtag(), refSet.getEtag());
		assertEquals(TableModelUtils.getSelectColumns(models),refSet.getHeaders());
		assertNotNull(refSet.getRows());
		assertEquals(rowCount, refSet.getRows().size());
		RowReference firstRef = refSet.getRows().get(0);
		assertEquals(new Long(1), firstRef.getRowId());
		assertEquals(new Long(3), firstRef.getVersionNumber());
		// the rest should be assigned a version
		for(int i=1; i<rowCount; i++){
			RowReference ref = refSet.getRows().get(i);
			assertEquals(new Long(i+range.getMinimumId()-1), ref.getRowId());
			assertEquals(new Long(3), ref.getVersionNumber());
		}
		
		// check stack status
		verify(mockStackStatusDao).getCurrentStatus();
		// check file handles
		verify(mockFileDao).getFileHandleIdsCreatedByUser(eq(user.getId()), stringListCaptor.capture());
		List<String> fileHandes = stringListCaptor.getValue();
		assertNotNull(fileHandes);
		assertEquals(rowCount, fileHandes.size());
		verify(mockTruthDao).reserveIdsInRange(tableId, new Long(rowCount-1));
		// row level conflict test
		verify(mockTruthDao).checkForRowLevelConflict(tableId, rawSet);
		// save the row set
		verify(mockTruthDao).appendRowSetToTable(""+user.getId(), tableId, range.getEtag(), range.getVersionNumber(), models, rawSet);
	}
	
	@Test
	public void testAppendRowsHappy() throws DatastoreException, NotFoundException, IOException{
		RowReferenceSet results = manager.appendRows(user, tableId, models, set, mockProgressCallback);
		assertNotNull(results);
		// verify the table status was set
		verify(mockTableManagerSupport, times(1)).setTableToProcessingAndTriggerUpdate(tableId);
		verify(mockTableManagerSupport).validateTableWriteAccess(user, tableId);
	}
	
	@Test
	public void testAppendPartialRowsHappy() throws DatastoreException, NotFoundException, IOException {
		RowReferenceSet results = manager.appendPartialRows(user, tableId, models, partialSet, mockProgressCallback);
		assertNotNull(results);
		// verify the table status was set
		verify(mockTableManagerSupport, times(1)).setTableToProcessingAndTriggerUpdate(tableId);
		verify(mockTableManagerSupport).validateTableWriteAccess(user, tableId);
	}
	
	/**
	 * This is a test for PLFM-3386
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@Test
	public void testAppendPartialRowsColumnIdNotFound() throws DatastoreException, NotFoundException, IOException {
		PartialRow partialRow = new PartialRow();
		partialRow.setRowId(null);
		partialRow.setValues(ImmutableMap.of("foo", "updated value 2"));
		partialSet = new PartialRowSet();
		partialSet.setTableId(tableId);
		partialSet.setRows(Arrays.asList(partialRow));
		try {
			manager.appendPartialRows(user, tableId, models, partialSet, mockProgressCallback);
			fail("Should have failed since a column name was used and not an ID.");
		} catch (IllegalArgumentException e) {
			assertEquals("PartialRow.value.key: 'foo' is not a valid column ID for row ID: null", e.getMessage());
		}
		verify(mockTableManagerSupport, never()).setTableToProcessingAndTriggerUpdate(tableId);
	}
	
	@Test
	public void testValidatePartialRowString(){
		PartialRow partialRow = new PartialRow();
		partialRow.setRowId(null);
		partialRow.setValues(ImmutableMap.of("foo", "updated value 2"));
	
		Set<Long> columnIds = ImmutableSet.of(123l,456L);
		try {
			TableEntityManagerImpl.validatePartialRow(partialRow, columnIds);
			fail("Should have failed since a column name was used and not an ID.");
		} catch (Exception e) {
			assertEquals("PartialRow.value.key: 'foo' is not a valid column ID for row ID: null", e.getMessage());
		}
	}
	
	@Test
	public void testValidatePartialRowNoMatch(){
		PartialRow partialRow = new PartialRow();
		partialRow.setRowId(999L);
		partialRow.setValues(ImmutableMap.of("789", "updated value 2"));
	
		Set<Long> columnIds = ImmutableSet.of(123l,456L);
		try {
			TableEntityManagerImpl.validatePartialRow(partialRow, columnIds);
			fail("Should have failed since a column name was used and not an ID.");
		} catch (Exception e) {
			assertEquals("PartialRow.value.key: '789' is not a valid column ID for row ID: 999", e.getMessage());
		}
	}
	
	@Test
	public void testValidatePartialRowHappy(){
		PartialRow partialRow = new PartialRow();
		partialRow.setRowId(999L);
		partialRow.setValues(ImmutableMap.of("456", "updated value 2"));
		Set<Long> columnIds = ImmutableSet.of(123l,456L);
		TableEntityManagerImpl.validatePartialRow(partialRow, columnIds);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidatePartialRowNullRow(){
		PartialRow partialRow = null;
		Set<Long> columnIds = ImmutableSet.of(123l,456L);
		TableEntityManagerImpl.validatePartialRow(partialRow, columnIds);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidatePartialRowNullSet(){
		PartialRow partialRow = new PartialRow();
		partialRow.setRowId(null);
		partialRow.setValues(ImmutableMap.of("foo", "updated value 2"));
		Set<Long> columnIds = null;
		TableEntityManagerImpl.validatePartialRow(partialRow, columnIds);
	}

	@Test
	public void testAppendRowsAsStreamHappy() throws DatastoreException, NotFoundException, IOException{
		RowReferenceSet results = new RowReferenceSet();
		String etag = manager.appendRowsAsStream(user, tableId, models, set.getRows().iterator(), "etag", results, mockProgressCallback);
		assertNotNull(results);
		assertEquals(tableId, results.getTableId());
		assertEquals(range.getEtag(), results.getEtag());
		assertEquals(TableModelUtils.getSelectColumns(models),results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(10, results.getRows().size());
		assertEquals(results.getEtag(), etag);
		// verify the table status was set
		verify(mockTableManagerSupport, times(1)).setTableToProcessingAndTriggerUpdate(tableId);
		verify(mockProgressCallback).progressMade(anyLong());
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(tableId);
		verify(mockTableManagerSupport).validateTableWriteAccess(user, tableId);
	}
	
	@Test
	public void testAppendRowsTooLarge() throws DatastoreException, NotFoundException, IOException{
		// What is the row size for the model?
		int rowSizeBytes = TableModelUtils
				.calculateMaxRowSize(models);
		// Create a rowSet that is too big
		maxBytesPerRequest = 1000;
		manager.setMaxBytesPerRequest(maxBytesPerRequest);
		int tooManyRows = maxBytesPerRequest/rowSizeBytes+1;
		List<Row> rows = TableModelTestUtils.createRows(models, tooManyRows);
		RowSet tooBigSet = new RowSet();
		tooBigSet.setTableId(tableId);
		tooBigSet.setHeaders(TableModelUtils.getSelectColumns(models));
		tooBigSet.setRows(rows);
		try {
			manager.appendRows(user, tableId, models, tooBigSet, mockProgressCallback);
			fail("The passed RowSet should have been too large");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Request exceed the maximum number of bytes per request"));
		}
	}
	
	@Test
	public void testAppendRowsAsStreamMultipleBatches() throws DatastoreException, NotFoundException, IOException{
		// calculate the actual size of the first row
		int actualSizeFristRowBytes = TableModelUtils.calculateActualRowSize(set.getRows().get(0));
		// With this max, there should be three batches (4,8,2)
		manager.setMaxBytesPerChangeSet(actualSizeFristRowBytes*3);
		RowReferenceSet results = new RowReferenceSet();
		String etag = manager.appendRowsAsStream(user, tableId, models, set.getRows()
				.iterator(), "etag", results, mockProgressCallback);
		assertEquals(range.getEtag(), etag);
		assertEquals(tableId, results.getTableId());
		assertEquals(etag, results.getEtag());
		// All ten rows should be referenced
		assertNotNull(results.getRows());
		assertEquals(10, results.getRows().size());
		// Each batch should be assigned its own version number
		assertEquals(new Long(3), results.getRows().get(0).getVersionNumber());
		assertEquals(new Long(4), results.getRows().get(5).getVersionNumber());
		assertEquals(new Long(5), results.getRows().get(9).getVersionNumber());
		// verify the table status was set
		verify(mockTableManagerSupport, times(1)).setTableToProcessingAndTriggerUpdate(tableId);
		verify(mockProgressCallback, times(3)).progressMade(anyLong());
		verify(mockTableManagerSupport).validateTableWriteAccess(user, tableId);
	}


	@Test
	public void testDeleteRowsHappy() throws DatastoreException, NotFoundException, IOException{
		Row row1 = TableModelTestUtils.createDeletionRow(1L, null);
		Row row2 = TableModelTestUtils.createDeletionRow(2L, null);
		rawSet = new RawRowSet(rawSet.getIds(), "aa", tableId, Lists.newArrayList(row1, row2));
		set.setRows(Lists.newArrayList(row1, row2));

		RowSelection rowSelection = new RowSelection();
		rowSelection.setRowIds(Lists.newArrayList(1L, 2L));
		rowSelection.setEtag("aa");

		RowReferenceSet deleteRows = manager.deleteRows(user, tableId, rowSelection);
		assertNotNull(deleteRows);

		// verify the correct row set was generated
		verify(mockTruthDao).appendRowSetToTable(eq(user.getId().toString()), eq(tableId), eq(range.getEtag()), eq(range.getVersionNumber()), anyListOf(ColumnModel.class), any(RawRowSet.class));
		// verify the table status was set
		verify(mockTableManagerSupport, times(1)).setTableToProcessingAndTriggerUpdate(tableId);
		verify(mockTableManagerSupport).validateTableWriteAccess(user, tableId);
	}
	
	@Test
	public void testValidateFileHandlesAuthorizedCreatedBy(){
		List<SelectColumn> cols = new ArrayList<SelectColumn>();
		cols.add(TableModelTestUtils.createSelectColumn(1L, "a", ColumnType.FILEHANDLEID));
		
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, 0L, "1"));
		rows.add(TableModelTestUtils.createRow(1L, 0L, "5"));
		
		RowSet rowset = new RowSet();
		rowset.setHeaders(cols);
		rowset.setRows(rows);
		
		// Setup the user as the creator of the files handles
		when(mockFileDao.getFileHandleIdsCreatedByUser(anyLong(), any(List.class))).thenReturn(Sets.newHashSet("1", "5"));
		
		// call under test
		manager.validateFileHandles(user, tableId, rowset);
		// should check the files created by the user.
		verify(mockFileDao).getFileHandleIdsCreatedByUser(user.getId(), Lists.newArrayList("1","5"));
		// since all of the files were created by the user there is no need to lookup the associated files.
		verify(mockTableIndexDAO, never()).getFileHandleIdsAssociatedWithTable(any(Set.class), anyString());
	}
	
	@Test
	public void testValidateFileHandlesAuthorizedCreatedByAndAssociated(){
		List<SelectColumn> cols = new ArrayList<SelectColumn>();
		cols.add(TableModelTestUtils.createSelectColumn(1L, "a", ColumnType.FILEHANDLEID));
		
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, 0L, "1"));
		rows.add(TableModelTestUtils.createRow(1L, 0L, "5"));
		
		RowSet rowset = new RowSet();
		rowset.setHeaders(cols);
		rowset.setRows(rows);
		
		// Setup 1 to be created by.
		when(mockFileDao.getFileHandleIdsCreatedByUser(anyLong(), any(List.class))).thenReturn(Sets.newHashSet("1"));
		// setup 5 to be associated with.
		when(mockTableIndexDAO.getFileHandleIdsAssociatedWithTable(any(Set.class), anyString())).thenReturn(Sets.newHashSet( 5L ));
		
		// call under test
		manager.validateFileHandles(user, tableId, rowset);
		// should check the files created by the user.
		verify(mockFileDao).getFileHandleIdsCreatedByUser(user.getId(), Lists.newArrayList("1","5"));
		// since 1 was created by the user only 5 should be tested for association.
		verify(mockTableIndexDAO).getFileHandleIdsAssociatedWithTable(Sets.newHashSet( 5L ), tableId);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testValidateFileHandlesUnAuthorized(){
		List<SelectColumn> cols = new ArrayList<SelectColumn>();
		cols.add(TableModelTestUtils.createSelectColumn(1L, "a", ColumnType.FILEHANDLEID));
		
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, 0L, "1"));
		rows.add(TableModelTestUtils.createRow(1L, 0L, "5"));
		
		RowSet rowset = new RowSet();
		rowset.setHeaders(cols);
		rowset.setRows(rows);
		
		// Setup 1 to be created by.
		when(mockFileDao.getFileHandleIdsCreatedByUser(anyLong(), any(List.class))).thenReturn(Sets.newHashSet("1"));
		// setup 5 to be not associated with.
		when(mockTableIndexDAO.getFileHandleIdsAssociatedWithTable(any(Set.class), anyString())).thenReturn(new HashSet<Long>());
		
		// call under test
		manager.validateFileHandles(user, tableId, rowset);
	}
	
	@Test
	public void testChangeFileHandles() throws DatastoreException, NotFoundException, IOException {
		RowSet replace = new RowSet();
		replace.setTableId(tableId);
		replace.setHeaders(TableModelUtils.getSelectColumns(models));
		replace.setEtag("etag");

		List<Row> replaceRows = TableModelTestUtils.createRows(models, 3);
		for (int i = 0; i < 3; i++) {
			replaceRows.get(i).setRowId((long) i);
			replaceRows.get(i).setVersionNumber(0L);
		}
		// different owned filehandle
		replaceRows.get(0).getValues().set(ColumnType.FILEHANDLEID.ordinal(), "3333");
		// erase file handle
		replaceRows.get(1).getValues().set(ColumnType.FILEHANDLEID.ordinal(), null);
		// unowned, but unchanged replaceRows[2]
		replace.setRows(replaceRows);

		RowSetAccessor originalAccessor = mock(RowSetAccessor.class);
		RowAccessor row2Accessor = mock(RowAccessor.class);
		when(originalAccessor.getRow(2L)).thenReturn(row2Accessor);
		when(row2Accessor.getCellById(models.get(ColumnType.FILEHANDLEID.ordinal()).getId())).thenReturn("505002");

		when(mockTruthDao.getLatestVersionsWithRowData(tableId, Sets.newHashSet(2L), 0L, models)).thenReturn(originalAccessor);
		// call under test
		manager.appendRows(user, tableId, models, replace, mockProgressCallback);

		verify(mockTruthDao).appendRowSetToTable(eq(user.getId().toString()), eq(tableId), eq(range.getEtag()), eq(range.getVersionNumber()), anyListOf(ColumnModel.class), any(RawRowSet.class));

		verify(mockFileDao).getFileHandleIdsCreatedByUser(anyLong(), any(List.class));
		verify(mockTableManagerSupport).validateTableWriteAccess(user, tableId);
	}

	@Test
	public void testAddFileHandles() throws DatastoreException, NotFoundException, IOException {
		RowSet replace = new RowSet();
		replace.setTableId(tableId);
		replace.setHeaders(TableModelUtils.getSelectColumns(models));

		List<Row> updateRows = TableModelTestUtils.createRows(models, 2);
		for (int i = 0; i < 2; i++) {
			updateRows.get(i).setRowId(null);
		}
		// owned filehandle
		updateRows.get(0).getValues().set(ColumnType.FILEHANDLEID.ordinal(), "3333");
		// null file handle
		updateRows.get(1).getValues().set(ColumnType.FILEHANDLEID.ordinal(), null);
		replace.setRows(updateRows);

		manager.appendRows(user, tableId, models, replace, mockProgressCallback);

		verify(mockTruthDao).appendRowSetToTable(eq(user.getId().toString()), eq(tableId), eq(range.getEtag()), eq(range.getVersionNumber()), anyListOf(ColumnModel.class), any(RawRowSet.class));
		verify(mockFileDao).getFileHandleIdsCreatedByUser(anyLong(), any(List.class));
		verify(mockTableManagerSupport).validateTableWriteAccess(user, tableId);
	}

	@Test
	public void testGetCellValues() throws DatastoreException, NotFoundException, IOException {
		RowReferenceSet rows = new RowReferenceSet();
		rows.setTableId(tableId);
		rows.setHeaders(TableModelUtils.getSelectColumns(models));
		rows.setEtag("444");
		rows.setRows(Lists.newArrayList(TableModelTestUtils.createRowReference(1L, 2L), TableModelTestUtils.createRowReference(3L, 4L)));

		RowSet returnValue = new RowSet();
		when(mockTruthDao.getRowSet(rows, models)).thenReturn(returnValue);
		RowSet result = manager.getCellValues(user, tableId, rows.getRows(), models);
		assertNotNull(result);
		assertEquals(tableId, tableId);
		assertEquals(rows.getHeaders(), result.getHeaders());
		assertNotNull(result.getRows());
		assertEquals(2, result.getRows().size());
		Row row = result.getRows().get(0);
		assertEquals(new Long(1), row.getRowId());
		row = result.getRows().get(1);
		assertEquals(new Long(3), row.getRowId());
		
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
		verify(mockTableManagerSupport).getTableEntityType(tableId);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetCellValuesNonTable() throws DatastoreException, NotFoundException, IOException {
		// get cell values is only authorized for tables.
		when(mockTableManagerSupport.getTableEntityType(tableId)).thenReturn(EntityType.entityview);
		RowReferenceSet rows = new RowReferenceSet();
		rows.setTableId(tableId);
		rows.setHeaders(TableModelUtils.getSelectColumns(models));
		rows.setEtag("444");
		rows.setRows(Lists.newArrayList(TableModelTestUtils.createRowReference(1L, 2L), TableModelTestUtils.createRowReference(3L, 4L)));

		RowSet returnValue = new RowSet();
		when(mockTruthDao.getRowSet(rows, models)).thenReturn(returnValue);
		// call under test
		manager.getCellValues(user, tableId, rows.getRows(), models);
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetCellValuesFailNoAccess() throws DatastoreException, NotFoundException, IOException {
		doThrow(new UnauthorizedException()).when(mockTableManagerSupport).validateTableReadAccess(user, tableId);
		manager.getCellValues(user, tableId, null, null);
	}

	@Test
	public void testGetCellValue() throws Exception {
		final int columnIndex = 1;
		RowReference rowRef = new RowReference();
		rowRef.setRowId(1L);
		Row result = manager.getCellValue(user, tableId, rowRef, models.get(columnIndex));
		assertNotNull(result);
		assertEquals("string1", result.getValues().get(0));
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetColumnValuesFailReadAccess() throws Exception {
		doThrow(new UnauthorizedException()).when(mockTableManagerSupport).validateTableReadAccess(user, tableId);
		manager.getCellValue(user, tableId, null, null);
	}
	
	@Test (expected=ReadOnlyException.class)
	public void testPLFM_3041ReadOnly() throws Exception{
		// Start in read-write then go to read-only
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE, StatusEnum.READ_WRITE, StatusEnum.READ_ONLY);

		// three batches with this size
		manager.setMaxBytesPerChangeSet(300);
		RowReferenceSet results = new RowReferenceSet();
		manager.appendRowsAsStream(user, tableId, models, set.getRows().iterator(),
				"etag",
				results, mockProgressCallback);
		verify(mockProgressCallback, times(3)).progressMade(anyLong());
	}
	
	@Test (expected=ReadOnlyException.class)
	public void testPLFM_3041Down() throws Exception{
		// Start in read-write then go to down
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE, StatusEnum.READ_WRITE, StatusEnum.DOWN);
		// three batches with this size
		manager.setMaxBytesPerChangeSet(300);
		RowReferenceSet results = new RowReferenceSet();
		manager.appendRowsAsStream(user, tableId, models, set.getRows().iterator(),
				"etag",
				results, mockProgressCallback);
		verify(mockProgressCallback, times(3)).progressMade(anyLong());
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithTableHappy(){
		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(3L);
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(lastChange);
		when(mockTableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(lastChange.getRowVersion());
		Set<Long> input = Sets.newHashSet(0L, 1L, 2L, 3L);
		Set<Long> results = Sets.newHashSet(1L,2L);
		when(mockTableIndexDAO.getFileHandleIdsAssociatedWithTable(input, tableId)).thenReturn(results);
		// call under test.
		Set<Long> out = manager.getFileHandleIdsAssociatedWithTable(tableId, input);
		assertEquals(results, out);
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithTableNoChanges(){
		// no changes applied to the table.
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(null);
		when(mockTableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(3L);
		Set<Long> input = Sets.newHashSet(0L, 1L, 2L, 3L);
		Set<Long> results = Sets.newHashSet(1L,2L);
		when(mockTableIndexDAO.getFileHandleIdsAssociatedWithTable(input, tableId)).thenReturn(results);
		// call under test.
		Set<Long> out = manager.getFileHandleIdsAssociatedWithTable(tableId, input);
		assertNotNull(out);
		assertTrue(out.isEmpty());
	}
	
	@Test (expected=TemporarilyUnavailableException.class)
	public void testGetFileHandleIdsAssociatedWithTableLockFailed() throws LockUnavilableException, Exception{
		// setup LockUnavilableException.
		when(mockTableManagerSupport.tryRunWithTableNonexclusiveLock(any(ProgressCallback.class),anyString(), anyInt(), any(ProgressingCallable.class))).thenThrow(new LockUnavilableException());
		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(3L);
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(lastChange);
		when(mockTableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(lastChange.getRowVersion());
		Set<Long> input = Sets.newHashSet(0L, 1L, 2L, 3L);
		Set<Long> results = Sets.newHashSet(1L,2L);
		when(mockTableIndexDAO.getFileHandleIdsAssociatedWithTable(input, tableId)).thenReturn(results);
		// call under test.
		Set<Long> out = manager.getFileHandleIdsAssociatedWithTable(tableId, input);
		assertEquals(results, out);
	}
	
	@Test (expected=TemporarilyUnavailableException.class)
	public void testGetFileHandleIdsAssociatedWithTableIndexBehind(){
		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(3L);
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(lastChange);
		// set the index behind the truth version
		when(mockTableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(lastChange.getRowVersion()-1);
		Set<Long> input = Sets.newHashSet(0L, 1L, 2L, 3L);
		Set<Long> results = Sets.newHashSet(1L,2L);
		when(mockTableIndexDAO.getFileHandleIdsAssociatedWithTable(input, tableId)).thenReturn(results);
		// call under test.
		Set<Long> out = manager.getFileHandleIdsAssociatedWithTable(tableId, input);
		assertEquals(results, out);
	}
	
	@Test (expected=TemporarilyUnavailableException.class)
	public void testGetFileHandleIdsAssociatedWithTableNoIndexConnection(){
		// null means no index connection.
		when(mockTableConnectionFactory.getConnection(tableId)).thenReturn(null);
		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(3L);
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(lastChange);
		when(mockTableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(lastChange.getRowVersion());
		Set<Long> input = Sets.newHashSet(0L, 1L, 2L, 3L);
		Set<Long> results = Sets.newHashSet(1L,2L);
		when(mockTableIndexDAO.getFileHandleIdsAssociatedWithTable(input, tableId)).thenReturn(results);
		// call under test.
		Set<Long> out = manager.getFileHandleIdsAssociatedWithTable(tableId, input);
		assertEquals(results, out);
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithTableAlternateSignature(){
		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(3L);
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(lastChange);
		when(mockTableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(lastChange.getRowVersion());
		List<String> input = Lists.newArrayList("0","1","2","3");
		Set<Long> results = Sets.newHashSet(1L,2L);
		when(mockTableIndexDAO.getFileHandleIdsAssociatedWithTable(any(Set.class), anyString())).thenReturn(results);
		// call under test.
		Set<String> out = manager.getFileHandleIdsAssociatedWithTable(tableId, input);
		Set<String> expected = Sets.newHashSet("1","2");
		assertEquals(expected, out);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testSetTableSchema() throws Exception{
		// setup success.
		doAnswer(new Answer<Void>(){
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				ProgressCallback callback = (ProgressCallback) invocation.getArguments()[0];
				ProgressingCallable runner = (ProgressingCallable) invocation.getArguments()[3];
				runner.call(callback);
				return null;
			}}).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(ProgressCallback.class), anyString(), anyInt(), any(ProgressingCallable.class));
		
		List<String> schema = Lists.newArrayList("111","222");
		// call under test.
		manager.setTableSchema(user, schema, tableId);
		verify(mockColumModelManager).bindColumnToObject(user, schema, tableId);
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(tableId);
	}
	
	@SuppressWarnings("unchecked")
	@Test (expected=TemporarilyUnavailableException.class)
	public void testSetTableSchemaLockUnavailableException() throws Exception{
		// setup success.
		doAnswer(new Answer<Void>(){
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				throw new LockUnavilableException("No Lock for you!");
			}}).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(ProgressCallback.class), anyString(), anyInt(), any(ProgressingCallable.class));
		
		List<String> schema = Lists.newArrayList("111","222");
		// call under test.
		manager.setTableSchema(user, schema, tableId);
	}
	
	@Test
	public void testDeleteTable(){
		// call under test
		manager.deleteTable(tableId);
		verify(mockColumModelManager).unbindAllColumnsAndOwnerFromObject(tableId);
		verify(mockTruthDao).deleteAllRowDataForTable(tableId);
		verify(mockTableManagerSupport).setTableDeleted(tableId, ObjectType.TABLE);
	}
	
	@Test
	public void testIsTemporaryTableNeededToValidateAddAndDelete(){
		List<ColumnChange> changes = new LinkedList<ColumnChange>();
		
		ColumnChange delete = new ColumnChange();
		delete.setOldColumnId("123");
		delete.setNewColumnId(null);
		
		ColumnChange add = new ColumnChange();
		add.setOldColumnId(null);
		add.setNewColumnId("123");
		
		
		changes.add(delete);
		changes.add(add);
		// deletes and adds do not require a temp table.
		assertFalse(TableEntityManagerImpl.containsColumnUpdate(changes));
	}
	
	@Test
	public void testIsTemporaryTableNeededToValidateUpdate(){
		List<ColumnChange> changes = new LinkedList<ColumnChange>();
		ColumnChange update = new ColumnChange();
		update.setOldColumnId("123");
		update.setNewColumnId("456");
		changes.add(update);
		assertTrue(TableEntityManagerImpl.containsColumnUpdate(changes));
	}
	
	@Test
	public void testIsTemporaryTableNeededToValidateUpdateNoChange(){
		List<ColumnChange> changes = new LinkedList<ColumnChange>();
		ColumnChange update = new ColumnChange();
		update.setOldColumnId("123");
		update.setNewColumnId("123");
		changes.add(update);
		assertFalse(TableEntityManagerImpl.containsColumnUpdate(changes));
	}
	
	@Test
	public void testIsTemporaryTableNeededToValidate(){
		List<ColumnChange> changes = new LinkedList<ColumnChange>();
		ColumnChange update = new ColumnChange();
		update.setOldColumnId("123");
		update.setNewColumnId("456");
		changes.add(update);
		TableSchemaChangeRequest request = new TableSchemaChangeRequest();
		request.setChanges(changes);
		// call under test
		assertTrue(manager.isTemporaryTableNeededToValidate(request));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testIsTemporaryTableNeededToValidateUnknown(){
		TableUpdateRequest mockRequest = Mockito.mock(TableUpdateRequest.class);
		// call under test
		manager.isTemporaryTableNeededToValidate(mockRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateUpdateNullProgress(){
		mockProgressCallbackVoid = null;
		// Call under test
		manager.validateUpdateRequest(mockProgressCallbackVoid, user, schemaChangeRequest, mockIndexManager);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateUpdateNullUser(){
		user = null;
		// Call under test
		manager.validateUpdateRequest(mockProgressCallbackVoid, user, schemaChangeRequest, mockIndexManager);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateUpdateNullRequest(){
		schemaChangeRequest = null;
		// Call under test
		manager.validateUpdateRequest(mockProgressCallbackVoid, user, schemaChangeRequest, mockIndexManager);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateUpdateUnknownRequest(){
		TableUpdateRequest unknown = Mockito.mock(TableUpdateRequest.class);
		// Call under test
		manager.validateUpdateRequest(mockProgressCallbackVoid, user, unknown, mockIndexManager);
	}
	
	@Test
	public void testValidateUpdateRequestSchema(){
		// Call under test
		manager.validateSchemaUpdateRequest(mockProgressCallbackVoid, user, schemaChangeRequest, mockIndexManager);
		verify(mockColumModelManager).calculateNewSchemaIdsAndValidate(tableId, schemaChangeRequest.getChanges());
		verify(mockIndexManager).alterTempTableSchmea(mockProgressCallbackVoid, tableId, columChangedetails);
	}
	
	@Test (expected=IllegalStateException.class)
	public void testValidateUpdateRequestSchemaNullManagerWithUpdate(){
		mockIndexManager = null;
		// Call under test
		manager.validateSchemaUpdateRequest(mockProgressCallbackVoid, user, schemaChangeRequest, mockIndexManager);
	}
	
	@Test
	public void testValidateUpdateRequestSchemaNoUpdate(){
		// this case only contains an add (no update)
		ColumnChange add = new ColumnChange();
		add.setNewColumnId("111");
		add.setOldColumnId(null);
		
		List<ColumnChange> changes = Lists.newArrayList(add);
		TableSchemaChangeRequest request = new TableSchemaChangeRequest();
		request.setChanges(changes);
		request.setEntityId(tableId);
		
		List<ColumnChangeDetails> details = TableModelTestUtils.createDetailsForChanges(changes);
		
		List<String> newColumnIds = Lists.newArrayList("111");
		when(mockColumModelManager.getColumnChangeDetails(changes)).thenReturn(details);		
		
		when(mockColumModelManager.calculateNewSchemaIdsAndValidate(tableId, changes)).thenReturn(newColumnIds);
		// Call under test
		manager.validateSchemaUpdateRequest(mockProgressCallbackVoid, user, request, null);
		verify(mockColumModelManager).calculateNewSchemaIdsAndValidate(tableId, changes);
		// temp table should not be used.
		verify(mockIndexManager, never()).alterTempTableSchmea(any(ProgressCallback.class), anyString(), anyListOf(ColumnChangeDetails.class));
	}
	
	@Test
	public void testUpdateTable(){
		// call under test.
		manager.updateTable(mockProgressCallbackVoid, user, schemaChangeRequest);
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(tableId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateTableNullProgress(){
		mockProgressCallbackVoid = null;
		// call under test.
		manager.updateTable(mockProgressCallbackVoid, user, schemaChangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateTableNullUser(){
		user = null;
		// call under test.
		manager.updateTable(mockProgressCallbackVoid, user, schemaChangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateTableNullRequset(){
		schemaChangeRequest = null;
		// call under test.
		manager.updateTable(mockProgressCallbackVoid, user, schemaChangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateTableUnkownRequset(){
		TableUpdateRequest unknown = Mockito.mock(TableUpdateRequest.class);
		// call under test.
		manager.updateTable(mockProgressCallbackVoid, user, unknown);
	}
	
	@Test
	public void testUpdateTableSchema() throws IOException{
		when(mockColumModelManager.getColumnModel(user, newColumnIds, true)).thenReturn(models);
		List<String> newSchemaIdsLong = TableModelUtils.getIds(models);
		// call under test.
		TableSchemaChangeResponse response = manager.updateTableSchema(mockProgressCallbackVoid, user, schemaChangeRequest);
		assertNotNull(response);
		assertEquals(models, response.getSchema());
		verify(mockColumModelManager).calculateNewSchemaIdsAndValidate(tableId, schemaChangeRequest.getChanges());
		verify(mockColumModelManager).bindColumnToObject(user, newColumnIds, tableId);
		verify(mockColumModelManager).getColumnModel(user, newColumnIds, true);
		verify(mockTruthDao).appendSchemaChangeToTable(""+user.getId(), tableId, newSchemaIdsLong, schemaChangeRequest.getChanges());
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(tableId);
	}
	
	@Test
	public void testUpdateTableSchemaNoUpdate() throws IOException{
		// this case only contains an add (no update)
		ColumnChange add = new ColumnChange();
		add.setNewColumnId("111");
		add.setOldColumnId(null);
		
		List<ColumnChange> changes = Lists.newArrayList(add);
		schemaChangeRequest = new TableSchemaChangeRequest();
		schemaChangeRequest.setChanges(changes);
		schemaChangeRequest.setEntityId(tableId);
		
		models = Lists.newArrayList(TableModelTestUtils.createColumn(111l));
		newColumnIds = Lists.newArrayList("111");
		when(mockColumModelManager.calculateNewSchemaIdsAndValidate(tableId, changes)).thenReturn(newColumnIds);
		when(mockColumModelManager.getColumnModel(user, newColumnIds, true)).thenReturn(models);
		// call under test.
		TableSchemaChangeResponse response = manager.updateTableSchema(mockProgressCallbackVoid, user, schemaChangeRequest);
		assertNotNull(response);
		assertEquals(models, response.getSchema());
		verify(mockColumModelManager).calculateNewSchemaIdsAndValidate(tableId, schemaChangeRequest.getChanges());
		verify(mockColumModelManager).bindColumnToObject(user, newColumnIds, tableId);
		verify(mockColumModelManager).getColumnModel(user, newColumnIds, true);
		verify(mockTruthDao, never()).appendSchemaChangeToTable(anyString(), anyString(), anyListOf(String.class), anyListOf(ColumnChange.class));
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(tableId);
	}
	
	@Test
	public void  testGetSchemaChangeForVersion() throws IOException{
		long versionNumber = 123L;
		when(mockTruthDao.getSchemaChangeForVersion(tableId, versionNumber)).thenReturn(schemaChangeRequest.getChanges());
		// call under test
		List<ColumnChangeDetails> details = manager.getSchemaChangeForVersion(tableId, versionNumber);
		assertEquals(columChangedetails, details);
		verify(mockTruthDao).getSchemaChangeForVersion(tableId, versionNumber);
		verify(mockColumModelManager).getColumnChangeDetails(schemaChangeRequest.getChanges());
	}
	
	@Test
	public void testGetTableSchema(){
		when(mockColumModelManager.getColumnIdForTable(tableId)).thenReturn(newColumnIds);
		List<String> retrievedSchema = manager.getTableSchema(tableId);
		assertEquals(newColumnIds, retrievedSchema);
	}
}
