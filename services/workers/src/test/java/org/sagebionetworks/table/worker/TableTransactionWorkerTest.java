package org.sagebionetworks.table.worker;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableTransactionManager;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;
import com.sun.star.lang.IllegalArgumentException;

public class TableTransactionWorkerTest {
	
	@Mock
	AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	TableManagerSupport mockTableManagerSupport; 
	@Mock
	UserManager mockUserManager;
	@Mock
	ProgressCallback<Void> mockProgressCallback;
	@Mock
	Message mockMessage;
	@Mock
	TableTransactionManager mockTableTransactionManager;
	
	Map<EntityType, TableTransactionManager> managerMap;
	
	
	TableTransactionWorker worker;
	
	String jobId;
	String tableId;
	AsynchronousJobStatus status;
	TableUpdateTransactionRequest request;
	EntityType tableType;
	TableUpdateTransactionResponse responseBody;
	Long userId;
	UserInfo userInfo;

	@Before
	public void before() throws RecoverableMessageException, TableUnavailableException{
		MockitoAnnotations.initMocks(this);
		worker = new TableTransactionWorker();
		ReflectionTestUtils.setField(worker, "asynchJobStatusManager", mockAsynchJobStatusManager);
		ReflectionTestUtils.setField(worker, "tableManagerSupport", mockTableManagerSupport);
		ReflectionTestUtils.setField(worker, "userManager", mockUserManager);
		
		userId = 987L;
		userInfo = new UserInfo(false);
		userInfo.setId(userId);
		
		jobId = "123";
		tableId = "syn123";
		tableType = EntityType.table;
		
		managerMap = new HashMap<>(1);
		managerMap.put(tableType, mockTableTransactionManager);
		worker.setManagerMap(managerMap);

		status = new AsynchronousJobStatus();
		status.setJobId(jobId);
		status.setStartedByUserId(userId);
		
		request = new TableUpdateTransactionRequest();
		request.setEntityId(tableId);
		status.setRequestBody(request);
		
		responseBody = new TableUpdateTransactionResponse();
		
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(jobId)).thenReturn(status);
		when(mockTableManagerSupport.getTableEntityType(tableId)).thenReturn(tableType);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		
		// simulate some progress and return a result
		doAnswer(new Answer<TableUpdateTransactionResponse>(){

			@Override
			public TableUpdateTransactionResponse answer(
					InvocationOnMock invocation) throws Throwable {
				// the first argument is the progress callback
				ProgressCallback<Void> statusCallback = (ProgressCallback<Void>) invocation.getArguments()[0];
				// make some progress
				statusCallback.progressMade(null);
				statusCallback.progressMade(null);
				statusCallback.progressMade(null);
				return responseBody;
			}}).when(mockTableTransactionManager).updateTableWithTransaction(any(ProgressCallback.class), any(UserInfo.class), any(TableUpdateTransactionRequest.class));

	}
	
	@Test
	public void testBasicRun() throws Exception{
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		// progress should be made three times
		verify(mockProgressCallback, atLeast(1)).progressMade(null);
		// status should be made three times
		verify(mockAsynchJobStatusManager).updateJobProgress(jobId, 0L, 100L, "Update: 1");
		// the job should be set complete
		verify(mockAsynchJobStatusManager).setComplete(jobId, responseBody);
		// the managers do the actual transaction work.
		verify(mockTableTransactionManager).updateTableWithTransaction(any(ProgressCallback.class), eq(userInfo), eq(request));
	}
	
	@Test
	public void testNoManagerForType() throws RecoverableMessageException, Exception{
		// setup an unknown type
		when(mockTableManagerSupport.getTableEntityType(tableId)).thenReturn(EntityType.project);
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		// job should fail.
		verify(mockAsynchJobStatusManager).setJobFailed(eq(jobId), any(IllegalArgumentException.class));
	}
	
	@Test
	public void testNullRequestBody() throws RecoverableMessageException, Exception{
		status.setRequestBody(null);
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		// job should fail.
		verify(mockAsynchJobStatusManager).setJobFailed(eq(jobId), any(IllegalArgumentException.class));
	}
	
	@Test
	public void testNullEntityId() throws RecoverableMessageException, Exception{
		request.setEntityId(null);
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		// job should fail.
		verify(mockAsynchJobStatusManager).setJobFailed(eq(jobId), any(IllegalArgumentException.class));
	}
	
	@Test
	public void testTableUnavailable() throws Exception{
		reset(mockTableTransactionManager);
		when(
				mockTableTransactionManager.updateTableWithTransaction(
						any(ProgressCallback.class), any(UserInfo.class),
						any(TableUpdateTransactionRequest.class))).thenThrow(
				new TableUnavailableException(null));
		try {
			// call under test
			worker.run(mockProgressCallback, mockMessage);
			fail("Should have thrown an exception");
		} catch (RecoverableMessageException e) {
			// expected
		}
		verify(mockAsynchJobStatusManager).updateJobProgress(jobId, 0L, 100L, TableTransactionWorker.WAITING_FOR_TABLE_LOCK);
		// should not fail
		verify(mockAsynchJobStatusManager, never()).setJobFailed(anyString(), any(Throwable.class));
	}
	
	@Test
	public void testLockUnavilable() throws Exception{
		reset(mockTableTransactionManager);
		when(
				mockTableTransactionManager.updateTableWithTransaction(
						any(ProgressCallback.class), any(UserInfo.class),
						any(TableUpdateTransactionRequest.class))).thenThrow(
				new LockUnavilableException());
		try {
			// call under test
			worker.run(mockProgressCallback, mockMessage);
			fail("Should have thrown an exception");
		} catch (RecoverableMessageException e) {
			// expected
		}
		verify(mockAsynchJobStatusManager).updateJobProgress(jobId, 0L, 100L, TableTransactionWorker.WAITING_FOR_TABLE_LOCK);
		// should not fail
		verify(mockAsynchJobStatusManager, never()).setJobFailed(anyString(), any(Throwable.class));
	}
	
	@Test
	public void testRecoverableMessageException() throws Exception{
		reset(mockTableTransactionManager);
		when(
				mockTableTransactionManager.updateTableWithTransaction(
						any(ProgressCallback.class), any(UserInfo.class),
						any(TableUpdateTransactionRequest.class))).thenThrow(
				new RecoverableMessageException("message"));
		try {
			// call under test
			worker.run(mockProgressCallback, mockMessage);
			fail("Should have thrown an exception");
		} catch (RecoverableMessageException e) {
			// expected
		}
		verify(mockAsynchJobStatusManager).updateJobProgress(jobId, 0L, 100L, "message");
		// should not fail
		verify(mockAsynchJobStatusManager, never()).setJobFailed(anyString(), any(Throwable.class));
	}
	
	@Test
	public void testUnknownException() throws Exception{
		reset(mockTableTransactionManager);
		RuntimeException exception = new RuntimeException("message");
		when(
				mockTableTransactionManager.updateTableWithTransaction(
						any(ProgressCallback.class), any(UserInfo.class),
						any(TableUpdateTransactionRequest.class))).thenThrow(
				exception);
		try {
			// call under test
			worker.run(mockProgressCallback, mockMessage);
			fail("Should have thrown an exception");
		} catch (Throwable e) {
			// expected
		}
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, exception);
	}
}
