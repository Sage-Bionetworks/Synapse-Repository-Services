package org.sagebionetworks.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class AsyncJobProgressRunnerAdapterTest {

	@Mock
	private AsynchJobStatusManager mockJobManager;
	
	@Mock
	private UserManager mockUserManager;
	
	@Mock
	private AsynchronousRequestBody mockRequest;
	
	@Mock
	private AsynchronousResponseBody mockResponse;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private AsyncJobRunner<AsynchronousRequestBody, AsynchronousResponseBody> mockRunner;
	
	@InjectMocks
	private AsyncJobRunnerAdapter<AsynchronousRequestBody, AsynchronousResponseBody> adapter;
	
	@Mock
	private Message mockMessage;
	
	@Mock
	private UserInfo mockUser;
	
	@Mock
	private AsynchronousJobStatus mockStatus;

	@Captor
	private ArgumentCaptor<AsyncJobProgressCallback> captorJobProgressCallback;
	
	private String jobId;
	private Long userId;
	
	@BeforeEach
	public void before() throws Exception {
		// This will be invoked by spring
		adapter.configure(mockJobManager, mockUserManager);
		
		jobId = "123";
		userId = 456L;
		
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockJobManager.lookupJobStatus(any())).thenReturn(mockStatus);
		when(mockStatus.getRequestBody()).thenReturn(mockRequest);
		when(mockStatus.getStartedByUserId()).thenReturn(userId);
		when(mockUserManager.getUserInfo(any())).thenReturn(mockUser);
		when(mockRunner.run(any(), any(), any(), any(), any())).thenReturn(mockResponse);
		when(mockRunner.getRequestType()).thenReturn(AsynchronousRequestBody.class);
	}
	
	@Test
	public void testRunSuccessful() throws RecoverableMessageException, Exception {
		
		// Call under test
		adapter.run(mockCallback, mockMessage);
		
		verify(mockJobManager).lookupJobStatus(jobId);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockRunner).run(eq(mockCallback), eq(jobId), eq(mockUser), eq(mockRequest), captorJobProgressCallback.capture());
		verify(mockJobManager).setComplete(jobId, mockResponse);
		verifyNoMoreInteractions(mockJobManager);
	}
	
	@Test
	public void testRunSuccessfulWithProgressCallback() throws RecoverableMessageException, Exception {
		
		// Call under test
		adapter.run(mockCallback, mockMessage);
		
		verify(mockJobManager).lookupJobStatus(jobId);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockRunner).run(eq(mockCallback), eq(jobId), eq(mockUser), eq(mockRequest), captorJobProgressCallback.capture());
		verify(mockJobManager).setComplete(jobId, mockResponse);
		AsyncJobProgressCallback callback = captorJobProgressCallback.getValue();
		// Emulates a worker invoking the callback
		callback.updateProgress("Simulated progress", 0L, 100L);
		verify(mockJobManager).updateJobProgress(jobId, 0L, 100L, "Simulated progress");
		verifyNoMoreInteractions(mockJobManager);
	}
	
	@Test
	public void testRunFailed() throws RecoverableMessageException, Exception {
		
		RuntimeException ex = new RuntimeException("failed");
		
		when(mockRunner.run(any(), any(), any(), any(), any())).thenThrow(ex);
		
		// Call under test
		adapter.run(mockCallback, mockMessage);
		
		verify(mockJobManager).lookupJobStatus(jobId);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockRunner).run(eq(mockCallback), eq(jobId), eq(mockUser), eq(mockRequest), captorJobProgressCallback.capture());
		verify(mockJobManager).setJobFailed(jobId, ex);
		verifyNoMoreInteractions(mockJobManager);
	}
	
	@Test
	public void testRunRecoverable() throws RecoverableMessageException, Exception {
		
		RecoverableMessageException ex = new RecoverableMessageException("retry");
		
		when(mockRunner.run(any(), any(), any(), any(), any())).thenThrow(ex);
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			adapter.run(mockCallback, mockMessage);
		});
		
		assertEquals(ex, result);
		
		verify(mockJobManager).lookupJobStatus(jobId);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockRunner).run(eq(mockCallback), eq(jobId), eq(mockUser), eq(mockRequest), captorJobProgressCallback.capture());
		verifyNoMoreInteractions(mockJobManager);
	}

}
