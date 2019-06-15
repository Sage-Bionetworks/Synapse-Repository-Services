package org.sagebionetworks.table.worker;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableTransactionManager;
import org.sagebionetworks.repo.manager.table.TableTransactionManagerProvider;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;

public class TableTransactionWorkerTest {
	
	@Mock
	AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	TableManagerSupport mockTableManagerSupport; 
	@Mock
	UserManager mockUserManager;
	@Mock
	ProgressCallback mockProgressCallback;
	@Mock
	Message mockMessage;
	@Mock
	TableTransactionManager mockTableTransactionManager;
	@Mock
	TableTransactionManagerProvider mockTransactionManagerProvider;
	@Mock
	TableExceptionTranslator mockTableExceptionTranslator;
	

	TableTransactionWorker worker;
	
	String jobId;
	String tableId;
	IdAndVersion idAndVersion;
	AsynchronousJobStatus status;
	TableUpdateTransactionRequest request;
	EntityType tableType;
	TableUpdateTransactionResponse responseBody;
	Long userId;
	UserInfo userInfo;
	RuntimeException translatedException;

	@Before
	public void before() throws RecoverableMessageException, TableUnavailableException{
		MockitoAnnotations.initMocks(this);
		worker = new TableTransactionWorker();
		ReflectionTestUtils.setField(worker, "asynchJobStatusManager", mockAsynchJobStatusManager);
		ReflectionTestUtils.setField(worker, "tableManagerSupport", mockTableManagerSupport);
		ReflectionTestUtils.setField(worker, "userManager", mockUserManager);
		ReflectionTestUtils.setField(worker, "tableTransactionManagerProvider", mockTransactionManagerProvider);
		ReflectionTestUtils.setField(worker, "tableExceptionTranslator", mockTableExceptionTranslator);

		userId = 987L;
		userInfo = new UserInfo(false);
		userInfo.setId(userId);
		
		jobId = "123";
		tableId = "syn123";
		idAndVersion = IdAndVersion.parse(tableId);
		tableType = EntityType.table;
		
		when(mockTransactionManagerProvider.getTransactionManagerForType(tableType)).thenReturn(mockTableTransactionManager);

		status = new AsynchronousJobStatus();
		status.setJobId(jobId);
		status.setStartedByUserId(userId);
		
		request = new TableUpdateTransactionRequest();
		request.setEntityId(tableId);
		status.setRequestBody(request);
		
		responseBody = new TableUpdateTransactionResponse();
		
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(jobId)).thenReturn(status);
		when(mockTableManagerSupport.getTableEntityType(idAndVersion)).thenReturn(tableType);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		
		// simulate some progress and return a result
		doAnswer(new Answer<TableUpdateTransactionResponse>(){

			@Override
			public TableUpdateTransactionResponse answer(
					InvocationOnMock invocation) throws Throwable {
				return responseBody;
			}}).when(mockTableTransactionManager).updateTableWithTransaction(any(ProgressCallback.class), any(UserInfo.class), any(TableUpdateTransactionRequest.class));
		
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				ProgressListener listener = (ProgressListener) invocation.getArguments()[0];
				listener.progressMade();
				listener.progressMade();
				listener.progressMade();
				return null;
			}
		}).when(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		
		doAnswer(invocation -> {
			Throwable exception = (Throwable) invocation.getArguments()[0];
			translatedException = new RuntimeException("translated",exception);
			return translatedException;
		}).when(mockTableExceptionTranslator).translateException(any(Throwable.class));

	}
	
	@Test
	public void testBasicRun() throws Exception{
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		// progress should be made three times
		// status should be made three times
		verify(mockAsynchJobStatusManager, times(3)).updateJobProgress(eq(jobId), anyLong(), anyLong(), anyString());
		// the job should be set complete
		verify(mockAsynchJobStatusManager).setComplete(jobId, responseBody);
		// the managers do the actual transaction work.
		verify(mockTableTransactionManager).updateTableWithTransaction(any(ProgressCallback.class), eq(userInfo), eq(request));
	}
	
	/** 
	 * The progress listener must be removed even if there is an exception.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProgressListenerRemovedException() throws Exception{
		IllegalStateException error = new IllegalStateException("an error");
		doThrow(error).when(mockAsynchJobStatusManager).setComplete(anyString(), any(AsynchronousResponseBody.class));
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		verify(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback).removeProgressListener(any(ProgressListener.class));
		// The error should be translated.
		verify(mockTableExceptionTranslator).translateException(error);
		// The translated exception should be set
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, translatedException);
	}
	
	@Test
	public void testNoManagerForType() throws RecoverableMessageException, Exception{
		// setup an unknown type
		when(mockTableManagerSupport.getTableEntityType(idAndVersion)).thenReturn(EntityType.project);
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		// job should fail.
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, translatedException);
	}
	
	@Test
	public void testNullRequestBody() throws RecoverableMessageException, Exception{
		status.setRequestBody(null);
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		// job should fail.
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, translatedException);
	}
	
	@Test
	public void testNullEntityId() throws RecoverableMessageException, Exception{
		request.setEntityId(null);
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		// job should fail.
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, translatedException);
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
		// the exception should be translated.
		verify(mockTableExceptionTranslator).translateException(exception);
		// The translated exception should be set
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, translatedException);
	}
}
