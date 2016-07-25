package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

public class TableEntityTransactionManagerTest {

	@Mock
	TableManagerSupport tableManagerSupport;
	@Mock
	TransactionTemplate readCommitedTransactionTemplate;
	@Mock
	TableEntityManager tableEntityManager;
	@Mock
	ProgressCallback<Void> progressCallback;
	@Mock
	TableIndexConnectionFactory tableIndexConnectionFactory;
	@Mock
	TableIndexManager tableIndexManager;

	TableEntityTransactionManager manager;

	TableStatus status;

	String tableId;
	TableUpdateTransactionRequest request;
	UploadToTableRequest uploadRequest;

	UserInfo userInfo;

	TableUpdateTransactionResponse response;

	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		manager = new TableEntityTransactionManager();
		ReflectionTestUtils.setField(manager, "tableManagerSupport",
				tableManagerSupport);
		ReflectionTestUtils.setField(manager,
				"readCommitedTransactionTemplate",
				readCommitedTransactionTemplate);
		ReflectionTestUtils.setField(manager, "tableEntityManager",
				tableEntityManager);
		ReflectionTestUtils.setField(manager, "tableIndexConnectionFactory",
				tableIndexConnectionFactory);

		userInfo = new UserInfo(false, 2222L);

		tableId = "syn213";
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
						any(ProgressCallback.class), anyString(), anyInt(),
						any(ProgressingCallable.class))).thenReturn(response);
		
		when(tableIndexConnectionFactory.connectToTableIndex(tableId)).thenReturn(tableIndexManager);
	}

	@Test
	public void testValidateRequestValid() {
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
	}

	@Test
	public void testValidateRequestMissingId() {
		uploadRequest.setEntityId(null);
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
		// the root table ID is passed to each change if null
		assertEquals(tableId, uploadRequest.getEntityId());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateRequestChangeIdDoesNotMatch() {
		uploadRequest.setEntityId("doesNotMatch");
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
		// the root table ID is passed to each change if null
		assertEquals(tableId, uploadRequest.getEntityId());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateRequestNullRequest() {
		request = null;
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateRequestEntityIdNull() {
		request.setEntityId(null);
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateRequestChagnesNull() {
		request.setChanges(null);
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateRequestChagnesEmpty() {
		request.setChanges(new LinkedList<TableUpdateRequest>());
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
	}

	@Test
	public void testUpdateTableWithTransaction() throws Exception {
		// call under test.
		TableUpdateTransactionResponse results = manager
				.updateTableWithTransaction(progressCallback, userInfo, request);
		assertEquals(response, results);
		// user must have write
		verify(tableManagerSupport).validateTableWriteAccess(userInfo, tableId);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = TableUnavailableException.class)
	public void testUpdateTableWithTransactionTableUnavailableException()
			throws Exception {

		when(
				tableManagerSupport.tryRunWithTableExclusiveLock(
						any(ProgressCallback.class), anyString(), anyInt(),
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
						any(ProgressCallback.class), anyString(), anyInt(),
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
						any(ProgressCallback.class), anyString(), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new RecoverableMessageException());
		// call under test.
		manager.updateTableWithTransaction(progressCallback, userInfo, request);
	}
	
	@Test
	public void testIsTemporaryTableNeededTrue(){
		when(tableEntityManager.isTemporaryTableNeededToValidate(uploadRequest)).thenReturn(true);
		//call under test
		boolean needed = manager.isTemporaryTableNeeded(progressCallback, request);
		assertTrue(needed);
		// progress should be made
		verify(progressCallback).progressMade(null);
	}
	
	@Test
	public void testIsTemporaryTableNeededFalse(){
		when(tableEntityManager.isTemporaryTableNeededToValidate(uploadRequest)).thenReturn(false);
		//call under test
		boolean needed = manager.isTemporaryTableNeeded(progressCallback, request);
		assertFalse(needed);
		// progress should be made
		verify(progressCallback).progressMade(null);
	}

}
