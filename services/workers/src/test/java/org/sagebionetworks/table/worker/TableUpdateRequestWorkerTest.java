package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableUpdateRequestManager;
import org.sagebionetworks.repo.manager.table.TableUpdateRequestManagerProvider;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

@ExtendWith(MockitoExtension.class)
public class TableUpdateRequestWorkerTest {
	
	@Mock
	private TableManagerSupport mockTableManagerSupport;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private TableUpdateRequestManager mockTableUpdateRequestManager;
	@Mock
	private TableUpdateRequestManagerProvider mockTableUpdateRequestManagerProvider;
	@Mock
	private TableExceptionTranslator mockTableExceptionTranslator;
	
	@InjectMocks
	private TableUpdateRequestWorker worker;
	
	private String jobId;
	private String tableId;
	private IdAndVersion idAndVersion;
	private TableUpdateTransactionRequest request;
	private TableType tableType;
	private TableUpdateTransactionResponse responseBody;
	private UserInfo userInfo;
	private RuntimeException translatedException;

	@BeforeEach
	public void before() throws RecoverableMessageException, TableUnavailableException {
		userInfo = new UserInfo(false);
		userInfo.setId(987L);
		
		jobId = "123";
		tableId = "syn123";
		idAndVersion = IdAndVersion.parse(tableId);
		tableType = TableType.table;
	
		request = new TableUpdateTransactionRequest();
		request.setEntityId(tableId);
		
		responseBody = new TableUpdateTransactionResponse();
		translatedException = new RuntimeException("translated");		
	}
	
	@Test
	public void testBasicRun() throws Exception{
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockTableManagerSupport.getTableType(idAndVersion)).thenReturn(tableType);
		when(mockTableUpdateRequestManager.updateTableWithTransaction(any(), any(), any())).thenReturn(responseBody);
		makeProgress();
		// call under test
		TableUpdateTransactionResponse result = worker.run(jobId, userInfo, request, mockJobCallback);
		
		assertEquals(responseBody, result);
		// progress should be made three times
		// status should be made three times
		verify(mockJobCallback, times(3)).updateProgress(any(), any(), any());
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
		}).when(mockJobCallback).addProgressListener(any(ProgressListener.class));
	}
	
	/** 
	 * The progress listener must be removed even if there is an exception.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProgressListenerRemovedException() throws Exception{
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockTableManagerSupport.getTableType(idAndVersion)).thenReturn(tableType);
		when(mockTableExceptionTranslator.translateException(any())).thenReturn(translatedException);
		makeProgress();
		IllegalStateException error = new IllegalStateException("an error");
		
		doThrow(error).when(mockTableUpdateRequestManager).updateTableWithTransaction(any(), any(), any());
		
		// call under test
		RuntimeException result = assertThrows(RuntimeException.class, () -> {			
			worker.run(jobId, userInfo, request, mockJobCallback);
		});
		
		assertEquals(translatedException, result);
		
		verify(mockJobCallback).addProgressListener(any(ProgressListener.class));
		verify(mockJobCallback).removeProgressListener(any(ProgressListener.class));
		// The error should be translated.
		verify(mockTableExceptionTranslator).translateException(error);
	}
	
	@Test
	public void testNullRequestBody() throws RecoverableMessageException, Exception{
		request = null;
		
		when(mockTableExceptionTranslator.translateException(any())).thenReturn(translatedException);
		RuntimeException result = assertThrows(RuntimeException.class, () -> {
			// call under test
			worker.run(jobId, userInfo, request, mockJobCallback);
		});
		
		assertEquals(translatedException, result);
	}
	
	@Test
	public void testNullEntityId() throws RecoverableMessageException, Exception{
		request.setEntityId(null);
		// call under test
		when(mockTableExceptionTranslator.translateException(any())).thenReturn(translatedException);
		RuntimeException result = assertThrows(RuntimeException.class, () -> {
			// call under test
			worker.run(jobId, userInfo, request, mockJobCallback);
		});
		
		assertEquals(translatedException, result);
	}
		
	@Test
	public void testTableUnavailable() throws Exception{
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockTableManagerSupport.getTableType(idAndVersion)).thenReturn(tableType);
		makeProgress();
		when(mockTableUpdateRequestManager.updateTableWithTransaction(any(), any(), any())).thenThrow(new TableUnavailableException(null));
		
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(jobId, userInfo, request, mockJobCallback);
		});
		
		verify(mockJobCallback).updateProgress(TableUpdateRequestWorker.WAITING_FOR_TABLE_LOCK, 0L, 100L);
	}
	
	@Test
	public void testLockUnavilable() throws Exception{
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockTableManagerSupport.getTableType(idAndVersion)).thenReturn(tableType);
		makeProgress();
		when(mockTableUpdateRequestManager.updateTableWithTransaction(any(), any(), any())).thenThrow(new LockUnavilableException());
		
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(jobId, userInfo, request, mockJobCallback);

		});
		
		verify(mockJobCallback).updateProgress(TableUpdateRequestWorker.WAITING_FOR_TABLE_LOCK, 0L, 100L);
	}
	
	@Test
	public void testRecoverableMessageException() throws Exception{
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockTableManagerSupport.getTableType(idAndVersion)).thenReturn(tableType);
		makeProgress();
		when(mockTableUpdateRequestManager.updateTableWithTransaction(any(), any(), any())).thenThrow(new RecoverableMessageException("message"));
		
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(jobId, userInfo, request, mockJobCallback);
		});
		
		verify(mockJobCallback).updateProgress("message", 0L, 100L);
	}
	
	@Test
	public void testUnknownException() throws Exception{
		when(mockTableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType)).thenReturn(mockTableUpdateRequestManager);
		when(mockTableManagerSupport.getTableType(idAndVersion)).thenReturn(tableType);
		makeProgress();
		
		RuntimeException exception = new RuntimeException("message");
		
		when(mockTableUpdateRequestManager.updateTableWithTransaction(any(), any(), any())).thenThrow(exception);
		when(mockTableExceptionTranslator.translateException(any())).thenReturn(translatedException);
		
		RuntimeException result = assertThrows(RuntimeException.class, () -> {
			// call under test
			worker.run(jobId, userInfo, request, mockJobCallback);
		});
		
		assertEquals(translatedException, result);
		
		// the exception should be translated.
		verify(mockTableExceptionTranslator).translateException(exception);
	}
}
