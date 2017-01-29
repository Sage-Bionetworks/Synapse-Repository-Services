package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationStatus;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.dao.table.RowAndHeaderHandler;
import org.sagebionetworks.repo.model.dao.table.RowSetAccessor;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.exception.ReadOnlyException;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelMapper;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphoreRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TableRowManagerImplTest {
	
	ProgressCallback<Long> mockProgressCallback;
	StackStatusDao mockStackStatusDao;
	TableRowTruthDAO mockTruthDao;
	AuthorizationManager mockAuthManager;
	TableStatusDAO mockTableStatusDAO;
	NodeDAO mockNodeDAO;
	TableRowManagerImpl manager;
	ColumnModelDAO mockColumnModelDAO;
	ConnectionFactory mockTableConnectionFactory;
	TableIndexDAO mockTableIndexDAO;
	WriteReadSemaphoreRunner mockWriteReadSemaphoreRunner;
	List<ColumnModel> models;
	ProgressCallback<Object> mockProgressCallback2;
	ProgressCallback<Void> mockProgressCallbackVoid;
	UserInfo user;
	String tableId;
	RowSet set;
	RawRowSet rawSet;
	PartialRowSet partialSet;
	RawRowSet expectedRawRows;
	RowReferenceSet refSet;
	long rowIdSequence;
	long rowVersionSequence;
	int maxBytesPerRequest;
	List<FileHandleAuthorizationStatus> fileAuthResults;
	
	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception {
		Assume.assumeTrue(StackConfiguration.singleton().getTableEnabled());
		mockTruthDao = Mockito.mock(TableRowTruthDAO.class);
		mockAuthManager = Mockito.mock(AuthorizationManager.class);
		mockTableStatusDAO = Mockito.mock(TableStatusDAO.class);
		mockNodeDAO = Mockito.mock(NodeDAO.class);
		mockColumnModelDAO = Mockito.mock(ColumnModelDAO.class);
		mockTableConnectionFactory = Mockito.mock(ConnectionFactory.class);
		mockTableIndexDAO = Mockito.mock(TableIndexDAO.class);
		mockWriteReadSemaphoreRunner = Mockito.mock(WriteReadSemaphoreRunner.class);
		mockStackStatusDao = Mockito.mock(StackStatusDao.class);
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		mockProgressCallback2 = Mockito.mock(ProgressCallback.class);
		mockProgressCallbackVoid = Mockito.mock(ProgressCallback.class);

		// Just call the caller.
		stub(mockWriteReadSemaphoreRunner.tryRunWithReadLock(any(ProgressCallback.class),anyString(), anyInt(), any(ProgressingCallable.class))).toAnswer(new Answer<Object>() {
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
		
		manager = new TableRowManagerImpl();
		maxBytesPerRequest = 10000000;
		manager.setMaxBytesPerRequest(maxBytesPerRequest);
		manager.setMaxBytesPerChangeSet(1000000000);
		user = new UserInfo(false, 7L);
		models = TableModelTestUtils.createOneOfEachType(true);
		tableId = "syn123";
		List<Row> rows = TableModelTestUtils.createRows(models, 10);
		set = new RowSet();
		set.setTableId(tableId);
		set.setHeaders(TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns());
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
		refSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns());
		refSet.setRows(new LinkedList<RowReference>());
		refSet.setEtag("etag123");
		
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(models);
		when(mockTableConnectionFactory.getConnection(tableId)).thenReturn(mockTableIndexDAO);
		when(mockTableIndexDAO.query(any(ProgressCallback.class),any(SqlQuery.class))).thenReturn(set);
		stub(mockTableIndexDAO.queryAsStream(any(ProgressCallback.class),any(SqlQuery.class), any(RowAndHeaderHandler.class))).toAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				SqlQuery query = (SqlQuery) invocation.getArguments()[1];
				RowAndHeaderHandler handler =  (RowAndHeaderHandler) invocation.getArguments()[2];
				boolean isCount = false;
				if (query.getModel().getSelectList().getColumns() != null) {
					String sql = query.getModel().getSelectList().getColumns().get(0).toString();
					if (sql.equals("COUNT(*)")) {
						isCount = true;
					} else if (sql.contains("FOUND_ROWS()")) {
						isCount = true;
					}
				}
				if (isCount) {
					handler.writeHeader();
					handler.nextRow(TableModelTestUtils.createRow(null, null, "10"));
				} else {
					// Pass all rows to the handler
					handler.writeHeader();
					for (Row row : set.getRows()) {
						handler.nextRow(row);
					}
				}
				return true;
			}
		});	
		// Just call the caller.
		stub(mockTableIndexDAO.executeInReadTransaction(any(TransactionCallback.class))).toAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				if (invocation == null)
					return null;
				TransactionCallback<Void> callable = (TransactionCallback<Void>) invocation.getArguments()[0];
				return callable.doInTransaction(null);
			}
		});
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(
				AuthorizationManagerUtil.AUTHORIZED);
		ReflectionTestUtils.setField(manager, "stackStatusDao", mockStackStatusDao);
		ReflectionTestUtils.setField(manager, "tableRowTruthDao", mockTruthDao);
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthManager);
		ReflectionTestUtils.setField(manager, "tableStatusDAO", mockTableStatusDAO);
		ReflectionTestUtils.setField(manager, "nodeDao", mockNodeDAO);
		ReflectionTestUtils.setField(manager, "columnModelDAO", mockColumnModelDAO);
		ReflectionTestUtils.setField(manager, "tableConnectionFactory", mockTableConnectionFactory);
		ReflectionTestUtils.setField(manager, "writeReadSemaphoreRunner", mockWriteReadSemaphoreRunner);
		// read-write be default.
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
		rowIdSequence = 0;
		rowVersionSequence = 0;
		// Stub the dao 
		stub(mockTruthDao.appendRowSetToTable(any(String.class), any(String.class), any(ColumnModelMapper.class), any(RawRowSet.class)))
				.toAnswer(new Answer<RowReferenceSet>() {

					@Override
					public RowReferenceSet answer(InvocationOnMock invocation) throws Throwable {
						RowReferenceSet results = new RowReferenceSet();
						String tableId = (String) invocation.getArguments()[1];
						ColumnMapper mapper = (ColumnMapper) invocation.getArguments()[2];
						assertNotNull(mapper);
						RawRowSet rowset = (RawRowSet) invocation.getArguments()[3];
						results.setTableId(tableId);
						results.setEtag("etag" + rowVersionSequence);
						List<RowReference> resultsRefs = new LinkedList<RowReference>();
						results.setRows(resultsRefs);
						if (rowset != null) {
							// Set the id an version for each row
							for (@SuppressWarnings("unused")
							Row row : rowset.getRows()) {
								RowReference ref = new RowReference();
								ref.setRowId(rowIdSequence);
								ref.setVersionNumber(rowVersionSequence);
								resultsRefs.add(ref);
								rowIdSequence++;
							}
							rowVersionSequence++;
						}
						return results;
					}
				});
		
		
		// Results of the auth check.
		fileAuthResults = Lists.newArrayList(
				new FileHandleAuthorizationStatus("1", new AuthorizationStatus(true, null)),
				new FileHandleAuthorizationStatus("5", new AuthorizationStatus(true, null))
		);
		when(mockAuthManager.canDownloadFile(any(UserInfo.class), any(List.class), anyString(), any(FileHandleAssociateType.class))).thenReturn(fileAuthResults);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testAppendRowsUnauthroized() throws DatastoreException, NotFoundException, IOException{
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.appendRows(user, tableId, TableModelUtils.createColumnModelColumnMapper(models, false), set, mockProgressCallback);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testAppendRowsAsStreamUnauthroized() throws DatastoreException, NotFoundException, IOException{
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.appendRowsAsStream(user, tableId, TableModelUtils.createColumnModelColumnMapper(models, false), set.getRows().iterator(),
				"etag",
				null, mockProgressCallback);
	}
	
	@Test
	public void testAppendRowsHappy() throws DatastoreException, NotFoundException, IOException{
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthManager.canAccessRawFileHandleById(eq(user), anyString())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		ColumnMapper mapper = TableModelUtils.createColumnModelColumnMapper(models, false);
		when(mockTruthDao.appendRowSetToTable(user.getId().toString(), tableId, mapper, rawSet)).thenReturn(refSet);
		RowReferenceSet results = manager.appendRows(user, tableId, mapper, set, mockProgressCallback);
		assertEquals(refSet, results);
		// verify the table status was set
		verify(mockTableStatusDAO, times(1)).resetTableStatusToProcessing(tableId);
	}
	
	@Test
	public void testAppendPartialRowsHappy() throws DatastoreException, NotFoundException, IOException {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthManager.canAccessRawFileHandleById(eq(user), anyString())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		ColumnMapper mapper = TableModelUtils.createColumnModelColumnMapper(models, false);
		when(mockTruthDao.appendRowSetToTable(user.getId().toString(), tableId, mapper, expectedRawRows)).thenReturn(refSet);
		RowReferenceSet results = manager.appendPartialRows(user, tableId, mapper, partialSet, mockProgressCallback);
		assertEquals(refSet, results);
		// verify the table status was set
		verify(mockTableStatusDAO, times(1)).resetTableStatusToProcessing(tableId);
	}
	
	/**
	 * This is a test for PLFM-3386
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@Test
	public void testAppendPartialRowsColumnIdNotFound() throws DatastoreException, NotFoundException, IOException {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthManager.canAccessRawFileHandleById(eq(user), anyString())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		ColumnMapper mapper = TableModelUtils.createColumnModelColumnMapper(models, false);
		when(mockTruthDao.appendRowSetToTable(user.getId().toString(), tableId, mapper, expectedRawRows)).thenReturn(refSet);
		
		PartialRow partialRow = new PartialRow();
		partialRow.setRowId(null);
		partialRow.setValues(ImmutableMap.of("foo", "updated value 2"));
		partialSet = new PartialRowSet();
		partialSet.setTableId(tableId);
		partialSet.setRows(Arrays.asList(partialRow));
		try {
			manager.appendPartialRows(user, tableId, mapper, partialSet, mockProgressCallback);
			fail("Should have failed since a column name was used and not an ID.");
		} catch (IllegalArgumentException e) {
			assertEquals("PartialRow.value.key: 'foo' is not a valid column ID for row ID: null", e.getMessage());
		}
		// verify the table status was set
		verify(mockTableStatusDAO, never()).resetTableStatusToProcessing(tableId);
	}
	
	@Test
	public void testValidatePartialRowString(){
		PartialRow partialRow = new PartialRow();
		partialRow.setRowId(null);
		partialRow.setValues(ImmutableMap.of("foo", "updated value 2"));
	
		Set<Long> columnIds = ImmutableSet.of(123l,456L);
		try {
			TableRowManagerImpl.validatePartialRow(partialRow, columnIds);
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
			TableRowManagerImpl.validatePartialRow(partialRow, columnIds);
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
		TableRowManagerImpl.validatePartialRow(partialRow, columnIds);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidatePartialRowNullRow(){
		PartialRow partialRow = null;
		Set<Long> columnIds = ImmutableSet.of(123l,456L);
		TableRowManagerImpl.validatePartialRow(partialRow, columnIds);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidatePartialRowNullSet(){
		PartialRow partialRow = new PartialRow();
		partialRow.setRowId(null);
		partialRow.setValues(ImmutableMap.of("foo", "updated value 2"));
		Set<Long> columnIds = null;
		TableRowManagerImpl.validatePartialRow(partialRow, columnIds);
	}

	@Test
	public void testAppendRowsAsStreamHappy() throws DatastoreException, NotFoundException, IOException{
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthManager.canAccessRawFileHandleById(eq(user), anyString())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		ColumnMapper mapper = TableModelUtils.createColumnModelColumnMapper(models, false);
		Mockito.reset(mockTruthDao);
		when(mockTruthDao.appendRowSetToTable(any(String.class), any(String.class), eq(mapper), any(RawRowSet.class)))
				.thenReturn(refSet);
		RowReferenceSet results = new RowReferenceSet();
		String etag = manager.appendRowsAsStream(user, tableId, mapper, set.getRows().iterator(), "etag", results, mockProgressCallback);
		assertEquals(refSet, results);
		assertEquals(refSet.getEtag(), etag);
		// verify the table status was set
		verify(mockTableStatusDAO, times(1)).resetTableStatusToProcessing(tableId);
		verify(mockProgressCallback).progressMade(anyLong());
	}
	
	@Test
	public void testAppendRowsTooLarge() throws DatastoreException, NotFoundException, IOException{
		// What is the row size for the model?
		int rowSizeBytes = TableModelUtils
				.calculateMaxRowSize(TableModelUtils.createColumnModelColumnMapper(models, false).getColumnModels());
		// Create a rowSet that is too big
		maxBytesPerRequest = 1000;
		manager.setMaxBytesPerRequest(maxBytesPerRequest);
		int tooManyRows = maxBytesPerRequest/rowSizeBytes+1;
		List<Row> rows = TableModelTestUtils.createRows(models, tooManyRows);
		RowSet tooBigSet = new RowSet();
		tooBigSet.setTableId(tableId);
		tooBigSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns());
		tooBigSet.setRows(rows);
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		ColumnMapper mapper = TableModelUtils.createColumnModelColumnMapper(models, false);
		when(mockTruthDao.appendRowSetToTable(user.getId().toString(), tableId, mapper, rawSet)).thenReturn(refSet);
		try {
			manager.appendRows(user, tableId, mapper, tooBigSet, mockProgressCallback);
			fail("The passed RowSet should have been too large");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Request exceed the maximum number of bytes per request"));
		}
	}
	
	@Test
	public void testAppendRowsAsStreamMultipleBatches() throws DatastoreException, NotFoundException, IOException{
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthManager.canAccessRawFileHandleById(eq(user), anyString())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		int maxBytesPerRow = TableModelUtils.calculateMaxRowSize(TableModelUtils.createColumnModelColumnMapper(models, false)
				.getColumnModels());
		// With a batch size of three, a total of ten rows should end up in 4 batches (3,3,3,1).	
		manager.setMaxBytesPerChangeSet(maxBytesPerRow*3);
		RowReferenceSet results = new RowReferenceSet();
		String etag = manager.appendRowsAsStream(user, tableId, TableModelUtils.createColumnModelColumnMapper(models, false), set.getRows()
				.iterator(), "etag", results, mockProgressCallback);
		assertEquals("etag3", etag);
		assertEquals(tableId, results.getTableId());
		assertEquals(etag, results.getEtag());
		// All ten rows should be referenced
		assertNotNull(results.getRows());
		assertEquals(10, results.getRows().size());
		// Each batch should be assigned its own version number
		assertEquals(new Long(0), results.getRows().get(0).getVersionNumber());
		assertEquals(new Long(1), results.getRows().get(3).getVersionNumber());
		assertEquals(new Long(2), results.getRows().get(6).getVersionNumber());
		assertEquals(new Long(3), results.getRows().get(9).getVersionNumber());
		// verify the table status was set
		verify(mockTableStatusDAO, times(1)).resetTableStatusToProcessing(tableId);
		verify(mockProgressCallback, times(4)).progressMade(anyLong());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAppendNullRowValuesFails() throws DatastoreException, NotFoundException, IOException {
		Row emptyValueRow = new Row();
		emptyValueRow.setValues(null);
		set.setRows(Collections.singletonList(emptyValueRow));
		rawSet = new RawRowSet(rawSet.getIds(), rawSet.getEtag(), tableId, Collections.singletonList(emptyValueRow));
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		ColumnMapper mapper = TableModelUtils.createColumnModelColumnMapper(models, false);
		reset(mockTruthDao);
		when(mockTruthDao.appendRowSetToTable(user.getId().toString(), tableId, mapper, rawSet)).thenThrow(new IllegalArgumentException());
		manager.appendRows(user, tableId, mapper, set, mockProgressCallback);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAppendEmptyRowValuesFails() throws DatastoreException, NotFoundException, IOException {
		Row emptyValueRow = new Row();
		emptyValueRow.setValues(Lists.<String> newArrayList());
		set.setRows(Collections.singletonList(emptyValueRow));
		rawSet = new RawRowSet(rawSet.getIds(), rawSet.getEtag(), tableId, Collections.singletonList(emptyValueRow));
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		ColumnMapper mapper = TableModelUtils.createColumnModelColumnMapper(models, false);
		when(mockTruthDao.appendRowSetToTable(user.getId().toString(), tableId, mapper, rawSet)).thenThrow(new IllegalArgumentException());
		manager.appendRows(user, tableId, mapper, set, mockProgressCallback);
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

		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		Mockito.reset(mockTruthDao);
		when(mockTruthDao.appendRowSetToTable(eq(user.getId().toString()), eq(tableId), any(ColumnMapper.class), eq(rawSet))).thenReturn(
				refSet);

		RowReferenceSet deleteRows = manager.deleteRows(user, tableId, rowSelection);
		assertEquals(refSet, deleteRows);

		// verify the correct row set was generated
		verify(mockTruthDao).appendRowSetToTable(eq(user.getId().toString()), eq(tableId), any(ColumnMapper.class), eq(rawSet));
		// verify the table status was set
		verify(mockTableStatusDAO, times(1)).resetTableStatusToProcessing(tableId);
	}
	
	@Test
	public void testValidateFileHandlesAuthorized(){
		List<SelectColumn> cols = new ArrayList<SelectColumn>();
		cols.add(TableModelTestUtils.createSelectColumn(1L, "a", ColumnType.FILEHANDLEID));
		
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, 0L, "1"));
		rows.add(TableModelTestUtils.createRow(1L, 0L, "5"));
		// The file ID to be extracted from the set.
		List<String> fileIds = Lists.newArrayList("1","5");
		
		RowSet rowset = new RowSet();
		rowset.setHeaders(cols);
		rowset.setRows(rows);
		
		// call under test
		manager.validateFileHandles(user, tableId, rowset);
		// Should do a download check of both files.
		verify(mockAuthManager).canDownloadFile(user, fileIds, tableId, FileHandleAssociateType.TableEntity);
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
		
		// setup failed results.
		fileAuthResults = Lists.newArrayList(
				new FileHandleAuthorizationStatus("1", new AuthorizationStatus(true, null)),
				new FileHandleAuthorizationStatus("5", new AuthorizationStatus(false, "No access for you"))
		);
		when(mockAuthManager.canDownloadFile(any(UserInfo.class), any(List.class), anyString(), any(FileHandleAssociateType.class))).thenReturn(fileAuthResults);
		
		// call under test
		manager.validateFileHandles(user, tableId, rowset);
	}
	
	@Test
	public void testChangeFileHandles() throws DatastoreException, NotFoundException, IOException {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		RowSet replace = new RowSet();
		replace.setTableId(tableId);
		ColumnMapper mapper = TableModelUtils.createColumnModelColumnMapper(models, false);
		replace.setHeaders(mapper.getSelectColumns());
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
		when(row2Accessor.getCellById(Long.parseLong(models.get(ColumnType.FILEHANDLEID.ordinal()).getId()))).thenReturn("505002");

		when(mockTruthDao.getLatestVersionsWithRowData(tableId, Sets.newHashSet(2L), 0L, mapper)).thenReturn(originalAccessor);
		// call under test
		manager.appendRows(user, tableId, mapper, replace, mockProgressCallback);

		verify(mockTruthDao).appendRowSetToTable(anyString(), anyString(), any(ColumnMapper.class), any(RawRowSet.class));
		verify(mockAuthManager).canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthManager).canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD);
		verify(mockAuthManager).canDownloadFile(user, Lists.newArrayList("505002", "3333"), tableId, FileHandleAssociateType.TableEntity);
		verifyNoMoreInteractions(mockAuthManager, mockTruthDao);
	}

	@Test
	public void testAddFileHandles() throws DatastoreException, NotFoundException, IOException {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		RowSet replace = new RowSet();
		replace.setTableId(tableId);
		replace.setHeaders(TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns());

		List<Row> updateRows = TableModelTestUtils.createRows(models, 2);
		for (int i = 0; i < 2; i++) {
			updateRows.get(i).setRowId(null);
		}
		// owned filehandle
		updateRows.get(0).getValues().set(ColumnType.FILEHANDLEID.ordinal(), "3333");
		// null file handle
		updateRows.get(1).getValues().set(ColumnType.FILEHANDLEID.ordinal(), null);
		replace.setRows(updateRows);

		manager.appendRows(user, tableId, TableModelUtils.createColumnModelColumnMapper(models, false), replace, mockProgressCallback);

		verify(mockTruthDao).appendRowSetToTable(anyString(), anyString(), any(ColumnMapper.class), any(RawRowSet.class));
		verify(mockAuthManager).canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthManager).canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD);
		verify(mockAuthManager).canDownloadFile(user, Lists.newArrayList("3333"), tableId, FileHandleAssociateType.TableEntity);
		verifyNoMoreInteractions(mockAuthManager, mockTruthDao);
	}

	@Test
	public void testGetCellValues() throws DatastoreException, NotFoundException, IOException {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		RowReferenceSet rows = new RowReferenceSet();
		rows.setTableId(tableId);
		rows.setHeaders(TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns());
		rows.setEtag("444");
		rows.setRows(Lists.newArrayList(TableModelTestUtils.createRowReference(1L, 2L), TableModelTestUtils.createRowReference(3L, 4L)));

		RowSet returnValue = new RowSet();
		ColumnMapper mapper = TableModelUtils.createColumnModelColumnMapper(models, false);
		when(mockTruthDao.getRowSet(rows, mapper)).thenReturn(returnValue);
		RowSet result = manager.getCellValues(user, tableId, rows, mapper);
		assertTrue(result == returnValue);

		verify(mockAuthManager).canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthManager).canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
		verify(mockTruthDao).getRowSet(rows, mapper);
		verifyNoMoreInteractions(mockAuthManager, mockTruthDao);
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetCellValuesFailNoAccess() throws DatastoreException, NotFoundException, IOException {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.getCellValues(user, tableId, null, null);
	}

	@Test
	public void testGetColumnValuesHappy() throws Exception {
		final int columnIndex = 1;
		RowReference rowRef = new RowReference();
		Row row = new Row();
		row.setValues(Lists.newArrayList("yy"));
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		reset(mockTruthDao);
		when(mockTruthDao.getRowOriginal(eq(tableId), eq(rowRef), any(ColumnMapper.class))).thenReturn(row);
		String result = manager.getCellValue(user, tableId, rowRef, models.get(columnIndex));
		assertEquals("yy", result);
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetColumnValuesFailReadAccess() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.getCellValue(user, tableId, null, null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testQueryUnauthroized() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.query(mockProgressCallbackVoid, user, "select * from " + tableId, null, null, null, true, false, true);
	}
	
	@Test 
	public void testQueryHappyCaseIsConsistentFalse() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		RowSet expected = new RowSet();
		expected.setTableId(tableId);
		when(mockTableIndexDAO.query(any(ProgressCallback.class),any(SqlQuery.class))).thenReturn(expected);
		Pair<QueryResult, Long> results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, false);
		// The etag should be null for this case
		assertEquals("The etag must be null for non-consistent query results.  These results cannot be used for a table update.", null,
				results.getFirst().getQueryResults().getEtag());
		assertEquals(expected, results.getFirst().getQueryResults());
		// The table status should not be checked for this case
		verify(mockTableStatusDAO, never()).getTableStatus(tableId);
	}
	
	@Test
	public void testQueryHappyCaseIsConsistentTrue() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		TableStatus status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.AVAILABLE);
		status.setLastTableChangeEtag(UUID.randomUUID().toString());
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		Pair<QueryResult, Long> results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
		// The etag should be set
		assertEquals(status.getLastTableChangeEtag(), results.getFirst().getQueryResults().getEtag());
		// Clear the etag for the test
		results.getFirst().getQueryResults().setEtag(null);
		assertEquals(set, results.getFirst().getQueryResults());
	}
	
	@Test
	public void testQueryCountHappyCaseIsConsistentTrue() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		TableStatus status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.AVAILABLE);
		status.setLastTableChangeEtag(UUID.randomUUID().toString());
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		Pair<QueryResult, Long> results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, true, true);
		// The etag should be set
		assertEquals(status.getLastTableChangeEtag(), results.getFirst().getQueryResults().getEtag());
		// Clear the etag for the test
		results.getFirst().getQueryResults().setEtag(null);
		assertEquals(set, results.getFirst().getQueryResults());
		assertEquals(1L, results.getSecond().longValue());
	}

	@Test
	public void testQueryAndCountHappyCaseIsConsistentTrue() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		TableStatus status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.AVAILABLE);
		status.setLastTableChangeEtag(UUID.randomUUID().toString());
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		Pair<QueryResult, Long> results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
		// The etag should be set
		assertEquals(status.getLastTableChangeEtag(), results.getFirst().getQueryResults().getEtag());
		// Clear the etag for the test
		results.getFirst().getQueryResults().setEtag(null);
		assertEquals(set, results.getFirst().getQueryResults());
	}

	@Test 
	public void testQueryNoColumns() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		// Return no columns
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(new LinkedList<ColumnModel>());
		TableStatus status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.AVAILABLE);
		status.setLastTableChangeEtag(UUID.randomUUID().toString());
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		Pair<QueryResult, Long> results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
		assertNotNull(results);
		assertEquals(tableId, results.getFirst().getQueryResults().getTableId());
		assertNull(results.getFirst().getQueryResults().getEtag());
		assertNull(results.getFirst().getQueryResults().getHeaders());
		assertNull(results.getFirst().getQueryResults().getRows());
	}
	
	/**
	 * Test for a consistent query when the table index is not available.
	 * @throws Exception
	 */
	@Test
	public void testQueryIsConsistentTrueNotAvailable() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		TableStatus status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.PROCESSING);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		try{
			manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
			fail("should have failed");
		}catch(TableUnavilableException e){
			// expected
			assertEquals(status, e.getStatus());
		}
		verify(mockTableStatusDAO, times(1)).getTableStatus(tableId);
	}
	
	/**
	 * Test for a consistent query when the table index is not available and not yet being build
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQueryIsConsistentTrueNotFound() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		TableStatus status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.PROCESSING);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenThrow(new NotFoundException("fake")).thenReturn(status);
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(new TableRowChange());
		when(mockNodeDAO.doesNodeExist(123L)).thenReturn(true);
		try{
			manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
			fail("should have failed");
		}catch(TableUnavilableException e){
			// expected
			assertEquals(status, e.getStatus());
		}
		verify(mockTableStatusDAO, times(2)).getTableStatus(tableId);
		verify(mockTableStatusDAO).resetTableStatusToProcessing(tableId);
		verify(mockNodeDAO).doesNodeExist(123L);

	}

	/**
	 * Test for a consistent query when the table index worker is holding a write-lock-precursor on the index. For this
	 * case the tryRunWithSharedLock() will throw a LockUnavilableException(), which should then be translated into a
	 * TableUnavilableException that contains the Table's status.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testQueryIsConsistentTrueLockUnavilableException() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		// First the status is checked show available.
		TableStatus status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// Throw a lock LockUnavilableException
		when(mockWriteReadSemaphoreRunner.tryRunWithReadLock(any(ProgressCallback.class),anyString(), anyInt(), any(ProgressingCallable.class))).thenThrow(new LockUnavilableException());
		try{
			manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
			fail("should have failed");
		}catch(TableUnavilableException e){
			// expected
			assertEquals(status, e.getStatus());
		}
	}

	@Test
	public void testQueryBundle() throws Exception {
		RowSet selectStar = new RowSet();
		selectStar.setEtag("etag");
		selectStar.setHeaders(TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns());
		selectStar.setTableId(tableId);
		selectStar.setRows(TableModelTestUtils.createRows(models, 10));
		QueryResult selectStarResult = new QueryResult();
		selectStarResult.setNextPageToken(null);
		selectStarResult.setQueryResults(selectStar);

		runQueryBundleTest("select * from " + tableId, selectStar, 10L, TableModelUtils.createColumnModelColumnMapper(models, false)
				.getSelectColumns().toString(), 24154L);
	}

	@Test
	public void testQueryBundleColumnsExpanded() throws Exception {
		RowSet selectStar = new RowSet();
		selectStar.setEtag("etag");
		selectStar.setHeaders(TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns());
		selectStar.setTableId(tableId);
		selectStar.setRows(TableModelTestUtils.createRows(models, 10));
		QueryResult selectStarResult = new QueryResult();
		selectStarResult.setNextPageToken(null);
		selectStarResult.setQueryResults(selectStar);

		runQueryBundleTest("select " + StringUtils.join(Lists.transform(models, TableModelTestUtils.convertToNameFunction), ",") + " from "
				+ tableId, selectStar, 10L, TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns().toString(),
				24154L);
	}

	@Test
	public void testQueryBundleWithAggregate() throws Exception {
		RowSet totals = new RowSet();
		totals.setEtag("etag");
		totals.setHeaders(Lists.newArrayList(TableModelTestUtils.createSelectColumn(null, "COUNT(*)", ColumnType.INTEGER)));
		totals.setTableId(tableId);
		totals.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "10")));
		QueryResult selectStarResult = new QueryResult();
		selectStarResult.setNextPageToken(null);
		selectStarResult.setQueryResults(totals);

		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName("COUNT(*)");
		selectColumn.setColumnType(ColumnType.INTEGER);
		runQueryBundleTest("select count(*) from " + tableId, totals, 10L, "[" + selectColumn.toString() + "]", 500000L);
	}

	private void runQueryBundleTest(String sql, RowSet selectResult, Long countResult, String selectColumns, Long maxRowsPerPage)
			throws Exception {
		Query query = new Query();
		query.setSql(sql);
		query.setIsConsistent(true);
		query.setOffset(0L);
		query.setLimit(Long.MAX_VALUE);
		QueryBundleRequest queryBundle = new QueryBundleRequest();
		queryBundle.setQuery(query);

		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		TableStatus status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.AVAILABLE);
		status.setLastTableChangeEtag("etag");
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);

		// Request query only
		queryBundle.setPartMask(TableRowManagerImpl.BUNDLE_MASK_QUERY_RESULTS);
		QueryResultBundle bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(selectResult, bundle.getQueryResult().getQueryResults());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(null, bundle.getMaxRowsPerPage());

		// Count only
		queryBundle.setPartMask(TableRowManagerImpl.BUNDLE_MASK_QUERY_COUNT);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(null, bundle.getQueryResult());
		assertEquals(countResult, bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(null, bundle.getMaxRowsPerPage());

		// select columns
		queryBundle.setPartMask(TableRowManagerImpl.BUNDLE_MASK_QUERY_SELECT_COLUMNS);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(null, bundle.getQueryResult());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(selectColumns, bundle.getSelectColumns().toString());
		assertEquals(null, bundle.getMaxRowsPerPage());

		// max rows per page
		queryBundle.setPartMask(TableRowManagerImpl.BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(null, bundle.getQueryResult());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(maxRowsPerPage, bundle.getMaxRowsPerPage());

		// now combine them all
		queryBundle.setPartMask(TableRowManagerImpl.BUNDLE_MASK_QUERY_RESULTS | TableRowManagerImpl.BUNDLE_MASK_QUERY_COUNT
				| TableRowManagerImpl.BUNDLE_MASK_QUERY_SELECT_COLUMNS | TableRowManagerImpl.BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(selectResult, bundle.getQueryResult().getQueryResults());
		assertEquals(countResult, bundle.getQueryCount());
		assertEquals(selectColumns, bundle.getSelectColumns().toString());
		assertEquals(maxRowsPerPage, bundle.getMaxRowsPerPage());
	}

	@Test
	public void testGetCurrentRowVersionsOneBatch() throws Exception {
		Map<Long, Long> map1 = Collections.singletonMap(1L, 1L);
		when(mockTruthDao.getLatestVersions(tableId, 0L, 0L, 11L)).thenReturn(map1);
		Map<Long, Long> currentRowVersions = manager.getCurrentRowVersions(tableId, 0L, 0L, 11L);
		assertEquals(map1, currentRowVersions);
	}

	@Test
	public void testGetCurrentRowVersionsOneBatchAfterVersion() throws Exception {
		Map<Long, Long> map1 = Collections.singletonMap(1L, 1L);
		when(mockTruthDao.getLatestVersions(tableId, 4L, 0L, 11L)).thenReturn(map1);
		Map<Long, Long> currentRowVersions = manager.getCurrentRowVersions(tableId, 4L, 0L, 11L);
		assertEquals(map1, currentRowVersions);
	}

	@Test
	public void testGetCurrentRowVersionsZeroEntries() throws Exception {
		Map<Long, Long> map1 = Collections.emptyMap();
		when(mockTruthDao.getLatestVersions(tableId, 0L, 0L, 10)).thenReturn(map1);
		Map<Long, Long> currentRowVersions = manager.getCurrentRowVersions(tableId, 0L, 0L, 10L);
		assertEquals(map1, currentRowVersions);
	}

	@Test
	public void testGetCurrentRowVersionsSecondBatch() throws Exception {
		Map<Long, Long> map2 = Collections.singletonMap(2L, 2L);
		when(mockTruthDao.getLatestVersions(tableId, 0L, 16000L, 16000L)).thenReturn(map2);
		Map<Long, Long> currentRowVersions = manager.getCurrentRowVersions(tableId, 0L, 16000L, 16000L);
		assertEquals(map2, currentRowVersions);
	}
	
	@Test
	public void testGetMaxRowsPerPage(){
		Long maxRows = this.manager.getMaxRowsPerPage(TableModelUtils.createColumnModelColumnMapper(models, false));
		int maxRowSize = TableModelUtils.calculateMaxRowSize(TableModelUtils.createColumnModelColumnMapper(models, false).getColumnModels());
		Long expected = (long) (this.maxBytesPerRequest/maxRowSize);
		assertEquals(expected, maxRows);
	}
	
	@Test
	public void testGetMaxRowsPerPageEmpty(){
		Long maxRows = this.manager.getMaxRowsPerPage(TableModelUtils.createColumnModelColumnMapper(new LinkedList<ColumnModel>(), false));
		assertEquals(null, maxRows);
	}
	
	@Test
	public void testGetColumnsForHeaders() throws DatastoreException, NotFoundException{
		// Headers can be a mix of column ids and aggregate functions.  The non-column model id headers should be ignored.
		List<String> headers = Arrays.asList("1","2","count(2)","3");
		when(mockColumnModelDAO.getColumnModel(Arrays.asList("1","2","3"), true)).thenReturn(Arrays.asList(models.get(1),models.get(2), models.get(3)));
		List<ColumnModel> models = manager.getColumnsForHeaders(headers);
		assertNotNull(models);
		assertEquals(3, models.size());
	}
	
	@Test (expected=ReadOnlyException.class)
	public void testPLFM_3041ReadOnly() throws Exception{
		// Start in read-write then go to read-only
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE, StatusEnum.READ_WRITE, StatusEnum.READ_ONLY);
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthManager.canAccessRawFileHandleById(eq(user), anyString())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		int maxBytesPerRow = TableModelUtils.calculateMaxRowSize(TableModelUtils.createColumnModelColumnMapper(models, false)
				.getColumnModels());
		// With a batch size of three, a total of ten rows should end up in 4 batches (3,3,3,1).	
		manager.setMaxBytesPerChangeSet(maxBytesPerRow*3);
		RowReferenceSet results = new RowReferenceSet();
		manager.appendRowsAsStream(user, tableId, TableModelUtils.createColumnModelColumnMapper(models, false), set.getRows().iterator(),
				"etag",
				results, mockProgressCallback);
		verify(mockProgressCallback, times(2)).progressMade(anyLong());
	}
	
	@Test (expected=ReadOnlyException.class)
	public void testPLFM_3041Down() throws Exception{
		// Start in read-write then go to down
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE, StatusEnum.READ_WRITE, StatusEnum.DOWN);
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthManager.canAccessRawFileHandleById(eq(user), anyString())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		int maxBytesPerRow = TableModelUtils.calculateMaxRowSize(TableModelUtils.createColumnModelColumnMapper(models, false)
				.getColumnModels());
		// With a batch size of three, a total of ten rows should end up in 4 batches (3,3,3,1).	
		manager.setMaxBytesPerChangeSet(maxBytesPerRow*3);
		RowReferenceSet results = new RowReferenceSet();
		manager.appendRowsAsStream(user, tableId, TableModelUtils.createColumnModelColumnMapper(models, false), set.getRows().iterator(),
				"etag",
				results, mockProgressCallback);
		verify(mockProgressCallback, times(2)).progressMade(anyLong());
	}
	
	@Test
	public void testGetTableStatusOrCreateIfNotExistsNoRows() throws Exception {
		// Since there are now rows this should return null.
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(null);
		
		TableStatus status = new TableStatus();
		status.setState(TableState.AVAILABLE);
		// this is null when there are no change sets applied to the table.
		status.setLastTableChangeEtag(null);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);

		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertEquals(status, result);
		verify(mockTruthDao).getLastTableRowChange(tableId);
		verify(mockTableStatusDAO, never()).resetTableStatusToProcessing(tableId);
	}
	
	@Test
	public void testGetTableStatusOrCreateIfNotExistsWithRowsAndUpToDate() throws Exception {
		// setup the last change to this table.
		TableRowChange lastChange = new TableRowChange();
		lastChange.setEtag("etagOfLastChange");
		// There are no table changes for this case.
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(lastChange);
		
		// This table is available and up-to-date.
		TableStatus status = new TableStatus();
		status.setState(TableState.AVAILABLE);
		// Set the status to match the last change.
		status.setLastTableChangeEtag(lastChange.getEtag());
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);

		// call under test.
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertEquals(status, result);
		verify(mockTruthDao).getLastTableRowChange(tableId);
		verify(mockTableStatusDAO, never()).resetTableStatusToProcessing(tableId);
	}
	
	/**
	 * This is a test PLFM-3383 and PLFM-3379 where a table's status is marked as 
	 * AVAILABLE but the index is not up-to-date with the table row changes.
	 * For this case, the table status must be rest to to PROCESSING.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsAVAILABLEButNotUpToDate() throws Exception {
		// setup the last change to this table.
		TableRowChange lastChange = new TableRowChange();
		lastChange.setEtag("etagOfLastChange");
		// There are no table changes for this case.
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(lastChange);
		
		// This table status is available but not up-to-date.
		TableStatus startStatus = new TableStatus();
		startStatus.setState(TableState.AVAILABLE);
		// Set the status to not match the last table change.
		startStatus.setLastTableChangeEtag("not the etag of the last change");
		
		// This is the status for the second call to getTableStatus()
		TableStatus processingStatus = new TableStatus();
		processingStatus.setState(TableState.PROCESSING);
		processingStatus.setLastTableChangeEtag("not the etag of the last change");
		// setup both the start (AVAILABLE) and the processing status.
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(startStatus, processingStatus);

		// call under test.
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		// The processing status should be returned.
		assertEquals(processingStatus, result);
		verify(mockTruthDao).getLastTableRowChange(tableId);
		// The table status must get rest here.
		verify(mockTableStatusDAO).resetTableStatusToProcessing(tableId);
	}
	
	@Test
	public void testGetTableStatusOrCreateIfNotExistsProcessing() throws Exception {
		// This table is processing
		TableStatus status = new TableStatus();
		status.setState(TableState.PROCESSING);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);

		// call under test.
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertEquals(status, result);
		// The last row should only be checked for for AVAILABLE tables.
		verify(mockTruthDao, never()).getLastTableRowChange(tableId);
		verify(mockTableStatusDAO, never()).resetTableStatusToProcessing(tableId);
	}
	
	@Test
	public void testGetTableStatusOrCreateIfNotExistsNotFound() throws Exception {
		TableStatus status = new TableStatus();
		status.setState(TableState.PROCESSING);
		// Setup a case where the first time the status does not exists, but does exist the second call.
		when(mockTableStatusDAO.getTableStatus(tableId)).thenThrow(new NotFoundException("No status for this table.")).thenReturn(status);
		// The table exists
		when(mockNodeDAO.doesNodeExist(KeyFactory.stringToKey(tableId))).thenReturn(true);
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertEquals(status, result);
		verify(mockTruthDao, never()).getLastTableRowChange(tableId);
		// The status should be set to processing in this case.
		verify(mockTableStatusDAO).resetTableStatusToProcessing(tableId);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetTableStatusOrCreateIfNotExistsTableDoesNotExist() throws Exception {
		TableStatus status = new TableStatus();
		status.setState(TableState.PROCESSING);
		// Setup a case where the first time the status does not exists, but does exist the second call.
		when(mockTableStatusDAO.getTableStatus(tableId)).thenThrow(new NotFoundException("No status for this table."));
		// The table does not exists
		when(mockNodeDAO.doesNodeExist(KeyFactory.stringToKey(tableId))).thenReturn(false);
		// This should fail as the table does not exist
		manager.getTableStatusOrCreateIfNotExists(tableId);
	}

	@Test
	public void testNextPageToken() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		TableStatus tableStatus = new TableStatus();
		tableStatus.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(tableStatus);
		RowSet rowSet = new RowSet();
		rowSet.setRows(Collections.nCopies(100000, new Row()));
		when(mockTableIndexDAO.query(any(ProgressCallback.class), any(SqlQuery.class))).thenReturn(rowSet);

		Pair<QueryResult, Long> query = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId, null, 0L, 100000L, true, false, false);
		assertNotNull(query.getFirst().getNextPageToken());
	}

	@Test
	public void testNextPageTokenEscaping() throws Exception {
		when(mockAuthManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		TableStatus tableStatus = new TableStatus();
		tableStatus.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(tableStatus);
		RowSet rowSet = new RowSet();
		rowSet.setRows(Collections.nCopies(100000, new Row()));
		when(mockTableIndexDAO.query(any(ProgressCallback.class), any(SqlQuery.class))).thenReturn(rowSet);

		// introduce escape-needed column names
		for (int i = 0; i < models.size(); i++) {
			String name = models.get(i).getName();
			name = name.substring(0, 1) + "-" + name.substring(1);
			models.get(i).setName(name);
		}

		Pair<QueryResult, Long> query = manager.query(mockProgressCallbackVoid, user, "select \"i-0\" from " + tableId, null, 0L, 100000L, true, false, false);
		assertNotNull(query.getFirst().getNextPageToken());
		assertTrue(query.getFirst().getNextPageToken().getToken().indexOf("&quot;i-0&quot") != -1);

		query = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId, null, 0L, 100000L, true, false, false);
		assertNotNull(query.getFirst().getNextPageToken());
		assertTrue(query.getFirst().getNextPageToken().getToken().indexOf("&quot;i-0&quot") != -1);
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
		when(mockWriteReadSemaphoreRunner.tryRunWithReadLock(any(ProgressCallback.class),anyString(), anyInt(), any(ProgressingCallable.class))).thenThrow(new LockUnavilableException());
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
	
}
