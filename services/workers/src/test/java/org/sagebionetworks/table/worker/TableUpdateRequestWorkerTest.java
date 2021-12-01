package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableUpdateRequestManager;
import org.sagebionetworks.repo.manager.table.TableUpdateRequestManagerProvider;
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

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class TableUpdateRequestWorkerTest {
	
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
	TableUpdateRequestManager mockTableUpdateRequestManager;
	@Mock
	TableUpdateRequestManagerProvider mockTableUpdateRequestManagerProvider;
	@Mock
	TableExceptionTranslator mockTableExceptionTranslator;
	@InjectMocks
	TableUpdateRequestWorker worker;
	
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

	@BeforeEach
	public void before() throws RecoverableMessageException, TableUnavailableException{
		userId = 987L;
		userInfo = new UserInfo(false);
		userInfo.setId(userId);
		
		jobId = "123";
		tableId = "syn123";
		idAndVersion = IdAndVersion.parse(tableId);
		tableType = EntityType.table;

		status = new AsynchronousJobStatus();
		status.setJobId(jobId);
		status.setStartedByUserId(userId);
		
		request = new TableUpdateTransactionRequest();
		request.setEntityId(tableId);
		status.setRequestBody(request);
		
		responseBody = new TableUpdateTransactionResponse();
		
	}
	
	@Test
	public void testBasicRun() throws Exception{
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(jobId)).thenReturn(status);
		when(mockTableManagerSupport.getTableEntityType(idAndVersion)).thenReturn(tableType);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		when(mockTableUpdateRequestManager.updateTableWithTransaction(any(), any(), any())).thenReturn(responseBody);
		makeProgress();
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		// progress should be made three times
		// status should be made three times
		verify(mockAsynchJobStatusManager, times(3)).updateJobProgress(eq(jobId), anyLong(), anyLong(), anyString());
		// the job should be set complete
		verify(mockAsynchJobStatusManager).setComplete(jobId, responseBody);
		// the managers do the actual transaction work.
		verify(mockTableUpdateRequestManager).updateTableWithTransaction(any(ProgressCallback.class), eq(userInfo), eq(request));
	}
	
	private void makeProgress() {
		doAnswer(invocation -> {
			ProgressListener listener = (ProgressListener) invocation.getArguments()[0];
			listener.progressMade();
			listener.progressMade();
			listener.progressMade();
			return null;
		}).when(mockProgressCallback).addProgressListener(any(ProgressListener.class));
	}
	
	/** 
	 * The progress listener must be removed even if there is an exception.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProgressListenerRemovedException() throws Exception{
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(jobId)).thenReturn(status);
		when(mockTableManagerSupport.getTableEntityType(idAndVersion)).thenReturn(tableType);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		when(mockTableUpdateRequestManager.updateTableWithTransaction(any(), any(), any())).thenReturn(responseBody);
		makeProgress();
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
	public void testNoManagerForType() throws RecoverableMessageException, Exception {
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(jobId)).thenReturn(status);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
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
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(jobId)).thenReturn(status);
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		// job should fail.
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, translatedException);
	}
	
	@Test
	public void testNullEntityId() throws RecoverableMessageException, Exception{
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(jobId)).thenReturn(status);
		request.setEntityId(null);
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		// job should fail.
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, translatedException);
	}
	
	@Test
	public void testTableUnavailable() throws Exception{
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(jobId)).thenReturn(status);
		when(mockTableManagerSupport.getTableEntityType(idAndVersion)).thenReturn(tableType);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		makeProgress();
		when(mockTableUpdateRequestManager.updateTableWithTransaction(any(), any(), any())).thenThrow(new TableUnavailableException(null));
		
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockProgressCallback, mockMessage);
		});
		
		verify(mockAsynchJobStatusManager).updateJobProgress(jobId, 0L, 100L, TableUpdateRequestWorker.WAITING_FOR_TABLE_LOCK);
		// should not fail
		verify(mockAsynchJobStatusManager, never()).setJobFailed(anyString(), any(Throwable.class));
	}
	
	@Test
	public void testLockUnavilable() throws Exception{
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(jobId)).thenReturn(status);
		when(mockTableManagerSupport.getTableEntityType(idAndVersion)).thenReturn(tableType);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		makeProgress();
		when(mockTableUpdateRequestManager.updateTableWithTransaction(any(), any(), any())).thenThrow(new LockUnavilableException());
		
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockProgressCallback, mockMessage);
		});
		
		verify(mockAsynchJobStatusManager).updateJobProgress(jobId, 0L, 100L, TableUpdateRequestWorker.WAITING_FOR_TABLE_LOCK);
		// should not fail
		verify(mockAsynchJobStatusManager, never()).setJobFailed(anyString(), any(Throwable.class));
	}
	
	@Test
	public void testRecoverableMessageException() throws Exception{
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(jobId)).thenReturn(status);
		when(mockTableManagerSupport.getTableEntityType(idAndVersion)).thenReturn(tableType);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		makeProgress();
		when(mockTableUpdateRequestManager.updateTableWithTransaction(any(), any(), any())).thenThrow(new RecoverableMessageException("message"));
		
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockProgressCallback, mockMessage);
		});
		
		verify(mockAsynchJobStatusManager).updateJobProgress(jobId, 0L, 100L, "message");
		// should not fail
		verify(mockAsynchJobStatusManager, never()).setJobFailed(anyString(), any(Throwable.class));
	}
	
	@Test
	public void testUnknownException() throws Exception{
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(jobId)).thenReturn(status);
		when(mockTableManagerSupport.getTableEntityType(idAndVersion)).thenReturn(tableType);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		makeProgress();
		
		RuntimeException exception = new RuntimeException("message");
		
		when(mockTableUpdateRequestManager.updateTableWithTransaction(any(), any(), any())).thenThrow(exception);
		
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		// the exception should be translated.
		verify(mockTableExceptionTranslator).translateException(exception);
		// The translated exception should be set
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, translatedException);
	}
}
