package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationStatus;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.dao.table.RowAndHeaderHandler;
import org.sagebionetworks.repo.model.dao.table.RowSetAccessor;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.exception.ReadOnlyException;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
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
import org.sagebionetworks.util.TimeoutUtils;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphoreRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TableStatusManagerTest {
	@Mock
	ProgressCallback<Long> mockProgressCallback;
	@Mock
	StackStatusDao mockStackStatusDao;
	@Mock
	TableRowTruthDAO mockTruthDao;
	@Mock
	AuthorizationManager mockAuthManager;
	@Mock
	TableStatusDAO mockTableStatusDAO;
	@Mock
	NodeDAO mockNodeDAO;
	@Mock
	ColumnModelDAO mockColumnModelDAO;
	@Mock
	ConnectionFactory mockTableConnectionFactory;
	@Mock
	TableIndexDAO mockTableIndexDAO;
	@Mock
	ProgressCallback<Object> mockProgressCallback2;
	@Mock
	ProgressCallback<Void> mockProgressCallbackVoid;
	@Mock
	WriteReadSemaphoreRunner mockWriteReadSemaphoreRunner;
	@Mock
	FileHandleDao mockFileDao;
	@Mock
	TimeoutUtils mockTimeoutUtils;
	@Mock
	ColumnModelManager mockColumModelManager;
	@Mock
	TransactionalMessenger mockTransactionalMessenger;
	
	List<ColumnModel> models;
	String schemaMD5Hex;
	
	TableStatusManagerImpl manager;
	UserInfo user;
	String tableId;
	Long tableIdLong;
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
	
	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception {
		Assume.assumeTrue(StackConfiguration.singleton().getTableEnabled());
		MockitoAnnotations.initMocks(this);
		
		manager = new TableStatusManagerImpl();
		ReflectionTestUtils.setField(manager, "stackStatusDao", mockStackStatusDao);
		ReflectionTestUtils.setField(manager, "tableRowTruthDao", mockTruthDao);
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthManager);
		ReflectionTestUtils.setField(manager, "tableStatusDAO", mockTableStatusDAO);
		ReflectionTestUtils.setField(manager, "nodeDao", mockNodeDAO);
		ReflectionTestUtils.setField(manager, "columnModelDAO", mockColumnModelDAO);
		ReflectionTestUtils.setField(manager, "tableConnectionFactory", mockTableConnectionFactory);
		ReflectionTestUtils.setField(manager, "writeReadSemaphoreRunner", mockWriteReadSemaphoreRunner);
		ReflectionTestUtils.setField(manager, "fileHandleDao", mockFileDao);
		ReflectionTestUtils.setField(manager, "timeoutUtils", mockTimeoutUtils);
		ReflectionTestUtils.setField(manager, "columModelManager", mockColumModelManager);
		ReflectionTestUtils.setField(manager, "transactionalMessenger", mockTransactionalMessenger);

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
		
		maxBytesPerRequest = 10000000;
		user = new UserInfo(false, 7L);
		models = TableModelTestUtils.createOneOfEachType(true);
		schemaMD5Hex = TableModelUtils.createSchemaMD5HexCM(models);
		tableId = "syn123";
		tableIdLong = KeyFactory.stringToKey(tableId);
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
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		
		when(mockNodeDAO.isNodeAvailable(anyLong())).thenReturn(true);
		ETAG = "";
	}
	


	
	/**
	 * For this case the table status is available and the index is synchronized with the truth.
	 * The available status should be returned for this case.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsAvailableSynchronized() throws Exception {
		// Available
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		// not expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		// table exists
		when(mockNodeDAO.isNodeAvailable(tableIdLong)).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertNotNull(result);
		verify(mockTableStatusDAO, never()).resetTableStatusToProcessing(tableId);
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(tableId, ObjectType.TABLE, ETAG, ChangeType.UPDATE);
	}
	
	/**
	 * This is a test case for PLFM-3383, PLFM-3379, and PLFM-3762.  In all cases the table 
	 * status was available but the index was not synchronized with the truth.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsAvailableNotSynchronized() throws Exception {
		// Available
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// Not synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(false);
		// not expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		// table exists
		when(mockNodeDAO.isNodeAvailable(tableIdLong)).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertNotNull(result);
		// must trigger processing
		verify(mockTableStatusDAO).resetTableStatusToProcessing(tableId);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(tableId, ObjectType.TABLE, ETAG, ChangeType.UPDATE);
	}
	
	/**
	 * This is a case where the table status does not exist but the table exits.
	 * The table must be be set to processing for this case.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsStatusNotFoundTableExits() throws Exception {
		// Available
		status.setState(TableState.PROCESSING);
		// Setup a case where the first time the status does not exists, but does exist the second call.
		when(mockTableStatusDAO.getTableStatus(tableId)).thenThrow(new NotFoundException("No status for this table.")).thenReturn(status);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		// not expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		// table exists
		when(mockNodeDAO.isNodeAvailable(tableIdLong)).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertNotNull(result);
		verify(mockTableStatusDAO).resetTableStatusToProcessing(tableId);
		verify(mockNodeDAO).isNodeAvailable(tableIdLong);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(tableId, ObjectType.TABLE, ETAG, ChangeType.UPDATE);
	}
	
	/**
	 * This is a case where the table status does not exist and the table does not exist.
	 * Should result in a NotFoundException
	 * @throws Exception
	 */
	@Test (expected=NotFoundException.class)
	public void testGetTableStatusOrCreateIfNotExistsStatusNotFoundTableDoesNotExist() throws Exception {
		// Available
		status.setState(TableState.PROCESSING);
		// Setup a case where the first time the status does not exists, but does exist the second call.
		when(mockTableStatusDAO.getTableStatus(tableId)).thenThrow(new NotFoundException("No status for this table.")).thenReturn(status);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		// not expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		// table does not exists
		when(mockNodeDAO.isNodeAvailable(tableIdLong)).thenReturn(false);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
	}
	
	/**
	 * For this case the table is processing and making progress (not expired).
	 * So the processing status should be returned without resetting.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsProcessingNotExpired() throws Exception {
		// Available
		status.setState(TableState.PROCESSING);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		// not expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		// table exists
		when(mockNodeDAO.isNodeAvailable(tableIdLong)).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertNotNull(result);
		verify(mockTableStatusDAO, never()).resetTableStatusToProcessing(tableId);
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(tableId, ObjectType.TABLE, ETAG, ChangeType.UPDATE);
	}
	
	/**
	 * For this case the table is processing and not making progress .
	 * The status must be rest to processing to trigger another try.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsProcessingExpired() throws Exception {
		// Available
		status.setState(TableState.PROCESSING);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		//expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(true);
		// table exists
		when(mockNodeDAO.isNodeAvailable(tableIdLong)).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertNotNull(result);
		verify(mockTableStatusDAO).resetTableStatusToProcessing(tableId);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(tableId, ObjectType.TABLE, ETAG, ChangeType.UPDATE);
	}


	
	@Test
	public void testStartTableProcessing(){
		String token = "a unique token";
		when(mockTableStatusDAO.resetTableStatusToProcessing(tableId)).thenReturn(token);
		// call under test
		String resultToken = manager.startTableProcessing(tableId);
		assertEquals(token, resultToken);
	}
	
	@Test
	public void testIsIndexSynchronizedWithTruthHappy(){
		long currentVersion = 123L;
		// setup a mismatch
		when(mockTableIndexDAO.doesIndexStateMatch(tableId, currentVersion, schemaMD5Hex)).thenReturn(true);
		
		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(currentVersion);
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(lastChange);
		
		assertTrue(manager.isIndexSynchronizedWithTruth(tableId));
	}
	
	
	@Test
	public void testGetVersionOfLastTableChangeNull() throws NotFoundException, IOException{
		// no last version
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(null);
		//call under test
		assertEquals(-1, manager.getVersionOfLastTableChange(tableId));
	}
	
	@Test
	public void testGetVersionOfLastTableChange() throws NotFoundException, IOException{
		long currentVersion = 123L;
		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(currentVersion);
		when(mockTruthDao.getLastTableRowChange(tableId)).thenReturn(lastChange);
		// call under test
		assertEquals(currentVersion, manager.getVersionOfLastTableChange(tableId));
	}
	
	/**
	 * The node is available, the table status is available and the index is synchronized.
	 */
	@Test
	public void testIsIndexWorkRequiredFalse(){
		// node exists
		when(mockNodeDAO.isNodeAvailable(tableIdLong)).thenReturn(true);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		// available
		TableStatus status = new TableStatus();
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(tableId);
		assertFalse(workRequired);
	}
	
	/**
	 * The node is missing, the table status is available and the index is synchronized.
	 */
	@Test
	public void testIsIndexWorkRequiredNodeMissing(){
		// node is missing
		when(mockNodeDAO.isNodeAvailable(tableIdLong)).thenReturn(false);
		// not synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(false);
		// failed
		TableStatus status = new TableStatus();
		status.setState(TableState.PROCESSING_FAILED);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(tableId);
		assertFalse(workRequired);
	}
	
	/**
	 * The node is available, the table status is available and the index is not synchronized.
	 * Processing is needed to bring the index up-to-date.
	 */
	@Test
	public void testIsIndexWorkRequiredNotSynched(){
		// node exists
		when(mockNodeDAO.isNodeAvailable(tableIdLong)).thenReturn(true);
		// not synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(false);
		// available
		TableStatus status = new TableStatus();
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(tableId);
		assertTrue(workRequired);
	}
	
	/**
	 * The node is available, the table status is processing and the index is synchronized.
	 * Processing is needed to make the table available.
	 */
	@Test
	public void testIsIndexWorkRequiredStatusProcessing(){
		// node exists
		when(mockNodeDAO.isNodeAvailable(tableIdLong)).thenReturn(true);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		// available
		TableStatus status = new TableStatus();
		status.setState(TableState.PROCESSING);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(tableId);
		assertTrue(workRequired);
	}
}
