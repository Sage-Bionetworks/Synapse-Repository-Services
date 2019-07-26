package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransactionDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(MockitoJUnitRunner.class)
public class TableEntityTransactionManagerTest {

	@Mock
	TableManagerSupport tableManagerSupport;
	@Mock
	TransactionTemplate readCommitedTransactionTemplate;
	@Mock
	TableEntityManager tableEntityManager;
	@Mock
	ProgressCallback progressCallback;
	@Mock
	TableIndexConnectionFactory tableIndexConnectionFactory;
	@Mock
	TableIndexManager tableIndexManager;
	@Mock
	TransactionStatus mockTransactionStatus;
	@Mock
	TableTransactionDao mockTransactionDao;

	@InjectMocks
	TableEntityTransactionManager manager;

	TableStatus status;

	String tableId;
	IdAndVersion idAndVersion;
	TableUpdateTransactionRequest request;
	UploadToTableRequest uploadRequest;

	UserInfo userInfo;

	TableUpdateTransactionResponse response;
	
	Long transactionId;

	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception {
		userInfo = new UserInfo(false, 2222L);

		tableId = "syn213";
		idAndVersion = IdAndVersion.parse(tableId);
		request = new TableUpdateTransactionRequest();
		request.setEntityId(tableId);
		List<TableUpdateRequest> changes = new LinkedList<>();
		request.setChanges(changes);

		uploadRequest = new UploadToTableRequest();
		uploadRequest.setEntityId(tableId);
		changes.add(uploadRequest);

		response = new TableUpdateTransactionResponse();

		when(
				tableManagerSupport.tryRunWithTableExclusiveLock(
						any(ProgressCallback.class), any(IdAndVersion.class), anyInt(),
						any(ProgressingCallable.class))).thenReturn(response);
		
		when(tableIndexConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(tableIndexManager);
		
		doAnswer(new Answer<TableUpdateTransactionResponse>() {
			@Override
			public TableUpdateTransactionResponse answer(
					InvocationOnMock invocation) throws Throwable {
				TransactionCallback<TableUpdateTransactionResponse> callback = (TransactionCallback<TableUpdateTransactionResponse>) invocation.getArguments()[0];
				return callback.doInTransaction(mockTransactionStatus);
			}
		}).when(readCommitedTransactionTemplate).execute(any(TransactionCallback.class));
		
		transactionId = 987L;
		when(mockTransactionDao.startTransaction(any(String.class), any(Long.class))).thenReturn(transactionId);
	}

	@Test
	public void testUpdateTableWithTransaction() throws Exception {
		// call under test.
		TableUpdateTransactionResponse results = manager
				.updateTableWithTransaction(progressCallback, userInfo, request);
		assertEquals(response, results);
		// user must have write
		verify(tableManagerSupport).validateTableWriteAccess(userInfo, idAndVersion);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = TableUnavailableException.class)
	public void testUpdateTableWithTransactionTableUnavailableException()
			throws Exception {

		when(
				tableManagerSupport.tryRunWithTableExclusiveLock(
						any(ProgressCallback.class), any(IdAndVersion.class), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new TableUnavailableException(null));
		// call under test.
		manager.updateTableWithTransaction(progressCallback, userInfo, request);
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected = LockUnavilableException.class)
	public void testUpdateTableWithTransactionLockUnavilableException()
			throws Exception {

		when(
				tableManagerSupport.tryRunWithTableExclusiveLock(
						any(ProgressCallback.class), any(IdAndVersion.class), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new LockUnavilableException());
		// call under test.
		manager.updateTableWithTransaction(progressCallback, userInfo, request);
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected = RecoverableMessageException.class)
	public void testUpdateTableWithTransactionRecoverableMessageException()
			throws Exception {

		when(
				tableManagerSupport.tryRunWithTableExclusiveLock(
						any(ProgressCallback.class), any(IdAndVersion.class), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new RecoverableMessageException());
		// call under test.
		manager.updateTableWithTransaction(progressCallback, userInfo, request);
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected = RuntimeException.class)
	public void testUpdateTableWithTransactionRuntimeException()
			throws Exception {

		when(
				tableManagerSupport.tryRunWithTableExclusiveLock(
						any(ProgressCallback.class), any(IdAndVersion.class), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new RuntimeException());
		// call under test.
		manager.updateTableWithTransaction(progressCallback, userInfo, request);
	}
	
	@Test
	public void testIsTemporaryTableNeededTrue(){
		when(tableEntityManager.isTemporaryTableNeededToValidate(uploadRequest)).thenReturn(true);
		//call under test
		boolean needed = manager.isTemporaryTableNeeded(progressCallback, request);
		assertTrue(needed);
	}
	
	@Test
	public void testIsTemporaryTableNeededFalse(){
		when(tableEntityManager.isTemporaryTableNeededToValidate(uploadRequest)).thenReturn(false);
		//call under test
		boolean needed = manager.isTemporaryTableNeeded(progressCallback, request);
		assertFalse(needed);
	}

	@Test
	public void testValidateUpdateRequestsWithTempTable(){
		// need to a temp table.
		when(tableEntityManager.isTemporaryTableNeededToValidate(uploadRequest)).thenReturn(true);
		// call under test
		manager.validateUpdateRequests(progressCallback, userInfo, request);
		verify(tableIndexConnectionFactory).connectToTableIndex(idAndVersion);
		verify(tableIndexManager).createTemporaryTableCopy(idAndVersion, progressCallback);
		verify(tableEntityManager).validateUpdateRequest(progressCallback, userInfo, uploadRequest, tableIndexManager);
		verify(tableIndexManager).deleteTemporaryTableCopy(idAndVersion, progressCallback);
	}
	
	@Test
	public void testValidateUpdateRequestsWithTempTableException(){
		// need to a temp table.
		when(tableEntityManager.isTemporaryTableNeededToValidate(uploadRequest)).thenReturn(true);
		// setup a failure
		doThrow(new RuntimeException("Wrong")).when(tableEntityManager).validateUpdateRequest(any(ProgressCallback.class), any(UserInfo.class), any(TableUpdateRequest.class), any(TableIndexManager.class));
		// call under test
		try {
			manager.validateUpdateRequests(progressCallback, userInfo, request);
			fail("Should have failed");
		} catch (RuntimeException e) {
			// expected
		}
		verify(tableIndexConnectionFactory).connectToTableIndex(idAndVersion);
		verify(tableIndexManager).createTemporaryTableCopy(idAndVersion, progressCallback);
		verify(tableEntityManager).validateUpdateRequest(progressCallback, userInfo, uploadRequest, tableIndexManager);
		verify(tableIndexManager).deleteTemporaryTableCopy(idAndVersion, progressCallback);
	}
	
	@Test
	public void testValidateUpdateRequestsWithoutTempTable(){
		// need to a temp table.
		when(tableEntityManager.isTemporaryTableNeededToValidate(uploadRequest)).thenReturn(false);
		// call under test
		manager.validateUpdateRequests(progressCallback, userInfo, request);
		verify(tableIndexConnectionFactory, never()).connectToTableIndex(idAndVersion);
		verify(tableIndexManager, never()).createTemporaryTableCopy(idAndVersion, progressCallback);
		verify(tableEntityManager).validateUpdateRequest(progressCallback, userInfo, uploadRequest, null);
		verify(tableIndexManager, never()).deleteTemporaryTableCopy(any(IdAndVersion.class), any(ProgressCallback.class));
	}
	
	@Test
	public void testUpdateTableWithTransactionWithExclusiveLock(){
		// call under test
		manager.updateTableWithTransactionWithExclusiveLock(progressCallback, userInfo, request);
		verify(tableEntityManager).updateTable(progressCallback, userInfo, uploadRequest, transactionId);
		verify(readCommitedTransactionTemplate).execute(any(TransactionCallback.class));
	}
	
	@Test
	public void testDoIntransactionUpdateTableNullSnapshotOptions()
			throws RecoverableMessageException, TableUnavailableException {
		request.setSnapshotOptions(null);
		// call under test
		TableUpdateTransactionResponse response = manager.doIntransactionUpdateTable(mockTransactionStatus,
				progressCallback, userInfo, request);
		assertNotNull(response);
		assertNull(response.getSnapshotVersionNumber());
		verify(mockTransactionDao).startTransaction(tableId, userInfo.getId());
		verify(tableEntityManager).updateTable(eq(progressCallback), eq(userInfo), any(TableUpdateRequest.class),
				eq(transactionId));
		verify(tableEntityManager, never()).createSnapshotAndBindToTransaction(any(UserInfo.class), anyString(),
				any(SnapshotRequest.class), anyLong());
	}
	
	@Test
	public void testDoIntransactionUpdateTableWithNewVersionFalse()
			throws RecoverableMessageException, TableUnavailableException {
		SnapshotRequest snapshotRequest = new SnapshotRequest();
		request.setCreateSnapshot(false);
		request.setSnapshotOptions(snapshotRequest);
		// call under test
		TableUpdateTransactionResponse response = manager.doIntransactionUpdateTable(mockTransactionStatus,
				progressCallback, userInfo, request);
		assertNotNull(response);
		assertNull(response.getSnapshotVersionNumber());
		verify(mockTransactionDao).startTransaction(tableId, userInfo.getId());
		verify(tableEntityManager).updateTable(eq(progressCallback), eq(userInfo), any(TableUpdateRequest.class),
				eq(transactionId));
		verify(tableEntityManager, never()).createSnapshotAndBindToTransaction(any(UserInfo.class), anyString(),
				any(SnapshotRequest.class), anyLong());
	}
	
	@Test
	public void testDoIntransactionUpdateTableWithNewVersiontrue()
			throws RecoverableMessageException, TableUnavailableException {
		SnapshotRequest snapshotRequest = new SnapshotRequest();
		request.setCreateSnapshot(true);
		request.setSnapshotOptions(snapshotRequest);
		// call under test
		TableUpdateTransactionResponse response = manager.doIntransactionUpdateTable(mockTransactionStatus,
				progressCallback, userInfo, request);
		assertNotNull(response);
		assertEquals(new Long(0), response.getSnapshotVersionNumber());
		verify(mockTransactionDao).startTransaction(tableId, userInfo.getId());
		verify(tableEntityManager).updateTable(eq(progressCallback), eq(userInfo), any(TableUpdateRequest.class),
				eq(transactionId));
		verify(tableEntityManager).createSnapshotAndBindToTransaction(userInfo, tableId, snapshotRequest, transactionId);
	}
}
