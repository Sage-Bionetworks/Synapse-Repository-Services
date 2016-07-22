package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
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
	
	TableEntityTransactionManager manager;
	
	TableStatus status;
	
	String tableId;
	TableUpdateTransactionRequest request;
	UploadToTableRequest uploadRequest;
	
	UserInfo userInfo;
	
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		manager = new TableEntityTransactionManager();
		ReflectionTestUtils.setField(manager, "tableManagerSupport", tableManagerSupport);
		ReflectionTestUtils.setField(manager, "readCommitedTransactionTemplate", readCommitedTransactionTemplate);
		ReflectionTestUtils.setField(manager, "tableEntityManager", tableEntityManager);
		
		userInfo = new UserInfo(false, 2222L);
		
		tableId = "syn213";
		request = new TableUpdateTransactionRequest();
		request.setEntityId(tableId);
		List<TableUpdateRequest> changes = new LinkedList<>();
		request.setChanges(changes);
		
		uploadRequest = new UploadToTableRequest();
		uploadRequest.setEntityId(tableId);
		changes.add(uploadRequest);
	}
	
	@Test
	public void testValidateRequestValid(){
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
	}
	
	@Test
	public void testValidateRequestMissingId(){
		uploadRequest.setEntityId(null);
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
		// the root table ID is passed to each change if null
		assertEquals(tableId, uploadRequest.getEntityId());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateRequestChangeIdDoesNotMatch(){
		uploadRequest.setEntityId("doesNotMatch");
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
		// the root table ID is passed to each change if null
		assertEquals(tableId, uploadRequest.getEntityId());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateRequestNullRequest(){
		request = null;
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateRequestEntityIdNull(){
		request.setEntityId(null);
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateRequestChagnesNull(){
		request.setChanges(null);
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateRequestChagnesEmpty(){
		request.setChanges(new LinkedList<TableUpdateRequest>());
		// call under test.
		TableEntityTransactionManager.validateRequest(request);
	}
	
	@Test
	public void testUpdateTableWithTransaction() throws Exception{
		doAnswer(new Answer<TableUpdateTransactionResponse>(){

			@Override
			public TableUpdateTransactionResponse answer(
					InvocationOnMock invocation) throws Throwable {
				// TODO Auto-generated method stub
				return null;
			}}).when(tableManagerSupport).tryRunWithTableExclusiveLock(any(ProgressCallback.class), anyString(), anyInt(), any(ProgressingCallable.class));
		
		// call under test.
		manager.updateTableWithTransaction(progressCallback, userInfo, request);
		// user must have write
		verify(tableManagerSupport).validateTableWriteAccess(userInfo, tableId);
	}
}
