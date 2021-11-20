package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
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

@ExtendWith(MockitoExtension.class)
public class TableEntityUpdateRequestManagerTest {

	@Mock
	private TableManagerSupport tableManagerSupport;
	@Mock
	private TransactionTemplate readCommitedTransactionTemplate;
	@Mock
	private TableEntityManager tableEntityManager;
	@Mock
	private ProgressCallback progressCallback;
	@Mock
	private TableIndexConnectionFactory tableIndexConnectionFactory;
	@Mock
	private TableIndexManager tableIndexManager;
	@Mock
	private TransactionStatus mockTransactionStatus;
	@Mock
	private TableTransactionManager mockTransactionManager;
	@Mock
	private TableTransactionContext mockTxContext;

	@InjectMocks
	private TableEntityUpdateRequestManager manager;

	private String tableId;
	private IdAndVersion idAndVersion;
	private TableUpdateTransactionRequest request;
	private UploadToTableRequest uploadRequest;

	private UserInfo userInfo;

	private TableUpdateTransactionResponse response;

	@BeforeEach
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
	}

	@SuppressWarnings("unchecked")
	private void setupCallback() {
		doAnswer(invocation -> {
			TransactionCallback<TableUpdateTransactionResponse> callback = (TransactionCallback<TableUpdateTransactionResponse>) invocation
					.getArguments()[0];
			return callback.doInTransaction(mockTransactionStatus);
		}).when(readCommitedTransactionTemplate).execute(any(TransactionCallback.class));
	}
	
	private void setupTransactionContext() {
		when(mockTransactionManager.executeInTransaction(any(), any(), any())).then((invocation) -> {
			Function<TableTransactionContext, TableUpdateTransactionResponse> func = invocation.getArgument(2);			
			return func.apply(mockTxContext);
		});
	}

	@Test
	public void testUpdateTableWithTransaction() throws Exception {
		when(tableManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenReturn(response);
		// call under test.
		TableUpdateTransactionResponse results = manager.updateTableWithTransaction(progressCallback, userInfo,
				request);
		assertEquals(response, results);
		// user must have write
		verify(tableManagerSupport).validateTableWriteAccess(userInfo, idAndVersion);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateTableWithTransactionTableUnavailableException() throws Exception {
		when(tableManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(new TableUnavailableException(null));
		assertThrows(TableUnavailableException.class, () -> {
			// call under test.
			manager.updateTableWithTransaction(progressCallback, userInfo, request);
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateTableWithTransactionLockUnavilableException() throws Exception {
		when(tableManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(new LockUnavilableException());
		assertThrows(LockUnavilableException.class, () -> {
			// call under test.
			manager.updateTableWithTransaction(progressCallback, userInfo, request);
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateTableWithTransactionRecoverableMessageException() throws Exception {

		when(tableManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(new RecoverableMessageException());
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test.
			manager.updateTableWithTransaction(progressCallback, userInfo, request);
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateTableWithTransactionRuntimeException() throws Exception {

		when(tableManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(IdAndVersion.class),
				any(ProgressingCallable.class))).thenThrow(new RuntimeException());
		assertThrows(RuntimeException.class, () -> {
			// call under test.
			manager.updateTableWithTransaction(progressCallback, userInfo, request);
		});
	}

	@Test
	public void testIsTemporaryTableNeededTrue() {
		when(tableEntityManager.isTemporaryTableNeededToValidate(uploadRequest)).thenReturn(true);
		// call under test
		boolean needed = manager.isTemporaryTableNeeded(progressCallback, request);
		assertTrue(needed);
	}

	@Test
	public void testIsTemporaryTableNeededFalse() {
		when(tableEntityManager.isTemporaryTableNeededToValidate(uploadRequest)).thenReturn(false);
		// call under test
		boolean needed = manager.isTemporaryTableNeeded(progressCallback, request);
		assertFalse(needed);
	}

	@Test
	public void testValidateUpdateRequestsWithTempTable() {
		when(tableIndexConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(tableIndexManager);

		// need to a temp table.
		when(tableEntityManager.isTemporaryTableNeededToValidate(uploadRequest)).thenReturn(true);
		// call under test
		manager.validateUpdateRequests(progressCallback, userInfo, request);
		verify(tableIndexConnectionFactory).connectToTableIndex(idAndVersion);
		verify(tableIndexManager).createTemporaryTableCopy(idAndVersion);
		verify(tableEntityManager).validateUpdateRequest(progressCallback, userInfo, uploadRequest, tableIndexManager);
		verify(tableIndexManager).deleteTemporaryTableCopy(idAndVersion);
	}

	@Test
	public void testValidateUpdateRequestsWithTempTableException() {
		when(tableIndexConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(tableIndexManager);
		// need to a temp table.
		when(tableEntityManager.isTemporaryTableNeededToValidate(uploadRequest)).thenReturn(true);
		// setup a failure
		doThrow(new RuntimeException("Wrong")).when(tableEntityManager).validateUpdateRequest(
				any(ProgressCallback.class), any(UserInfo.class), any(TableUpdateRequest.class),
				any(TableIndexManager.class));
		// call under test
		try {
			manager.validateUpdateRequests(progressCallback, userInfo, request);
			fail("Should have failed");
		} catch (RuntimeException e) {
			// expected
		}
		verify(tableIndexConnectionFactory).connectToTableIndex(idAndVersion);
		verify(tableIndexManager).createTemporaryTableCopy(idAndVersion);
		verify(tableEntityManager).validateUpdateRequest(progressCallback, userInfo, uploadRequest, tableIndexManager);
		verify(tableIndexManager).deleteTemporaryTableCopy(idAndVersion);
	}

	@Test
	public void testValidateUpdateRequestsWithoutTempTable() {
		// need to a temp table.
		when(tableEntityManager.isTemporaryTableNeededToValidate(uploadRequest)).thenReturn(false);
		// call under test
		manager.validateUpdateRequests(progressCallback, userInfo, request);
		verify(tableIndexConnectionFactory, never()).connectToTableIndex(idAndVersion);
		verify(tableIndexManager, never()).createTemporaryTableCopy(idAndVersion);
		verify(tableEntityManager).validateUpdateRequest(progressCallback, userInfo, uploadRequest, null);
		verify(tableIndexManager, never()).deleteTemporaryTableCopy(any(IdAndVersion.class));
	}

	@Test
	public void testUpdateTableWithTransactionWithExclusiveLock() throws Exception {
		setupCallback();
		setupTransactionContext();
		// call under test
		manager.updateTableWithTransactionWithExclusiveLock(progressCallback, userInfo, request);
		verify(mockTransactionManager).executeInTransaction(eq(userInfo), eq(tableId), any());
		verify(tableEntityManager).updateTable(progressCallback, userInfo, uploadRequest, mockTxContext);
		verify(readCommitedTransactionTemplate).execute(any(TransactionCallback.class));
	}

	@Test
	public void testDoIntransactionUpdateTableNullSnapshotOptions() {
		setupTransactionContext();
		request.setSnapshotOptions(null);
		// call under test
		TableUpdateTransactionResponse response = manager.doIntransactionUpdateTable(mockTransactionStatus,
				progressCallback, userInfo, request);
		assertNotNull(response);
		assertNull(response.getSnapshotVersionNumber());
		verify(mockTransactionManager).executeInTransaction(eq(userInfo), eq(tableId), any());
		verify(tableEntityManager).updateTable(eq(progressCallback), eq(userInfo), any(TableUpdateRequest.class),eq(mockTxContext));
		verify(tableEntityManager, never()).createSnapshotAndBindToTransaction(any(UserInfo.class), anyString(), any(SnapshotRequest.class), any());
	}

	@Test
	public void testDoIntransactionUpdateTableWithNewVersionFalse() {
		setupTransactionContext();
		SnapshotRequest snapshotRequest = new SnapshotRequest();
		request.setCreateSnapshot(false);
		request.setSnapshotOptions(snapshotRequest);
		// call under test
		TableUpdateTransactionResponse response = manager.doIntransactionUpdateTable(mockTransactionStatus,
				progressCallback, userInfo, request);
		assertNotNull(response);
		assertNull(response.getSnapshotVersionNumber());
		verify(mockTransactionManager).executeInTransaction(eq(userInfo), eq(tableId), any());
		verify(tableEntityManager).updateTable(eq(progressCallback), eq(userInfo), any(TableUpdateRequest.class), eq(mockTxContext));
		verify(tableEntityManager, never()).createSnapshotAndBindToTransaction(any(UserInfo.class), anyString(), any(SnapshotRequest.class), any());
	}

	@Test
	public void testDoIntransactionUpdateTableWithNewVersiontrue() {
		setupTransactionContext();
		SnapshotRequest snapshotRequest = new SnapshotRequest();
		request.setCreateSnapshot(true);
		request.setSnapshotOptions(snapshotRequest);
		// call under test
		TableUpdateTransactionResponse response = manager.doIntransactionUpdateTable(mockTransactionStatus,
				progressCallback, userInfo, request);
		assertNotNull(response);
		assertEquals(new Long(0), response.getSnapshotVersionNumber());
		verify(mockTransactionManager).executeInTransaction(eq(userInfo), eq(tableId), any());
		verify(tableEntityManager).updateTable(eq(progressCallback), eq(userInfo), any(TableUpdateRequest.class), eq(mockTxContext));
		verify(tableEntityManager).createSnapshotAndBindToTransaction(userInfo, tableId, snapshotRequest, mockTxContext);
	}

	@Test
	public void testDoIntransactionUpdateTableWithNullChangesSnapshotTrue() {
		setupTransactionContext();
		request.setChanges(null);
		SnapshotRequest snapshotRequest = new SnapshotRequest();
		request.setCreateSnapshot(true);
		request.setSnapshotOptions(snapshotRequest);
		// call under test
		TableUpdateTransactionResponse response = manager.doIntransactionUpdateTable(mockTransactionStatus,
				progressCallback, userInfo, request);
		assertNotNull(response);
		assertEquals(new Long(0), response.getSnapshotVersionNumber());
		verify(mockTransactionManager).executeInTransaction(eq(userInfo), eq(tableId), any());
		verify(tableEntityManager, never()).updateTable(any(ProgressCallback.class), any(UserInfo.class), any(TableUpdateRequest.class), any());
		verify(tableEntityManager).createSnapshotAndBindToTransaction(userInfo, tableId, snapshotRequest, mockTxContext);
	}
}
