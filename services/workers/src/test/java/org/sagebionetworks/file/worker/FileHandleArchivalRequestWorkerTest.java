package org.sagebionetworks.file.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleArchivalManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class FileHandleArchivalRequestWorkerTest {
	
	@Mock
	private FileHandleArchivalManager mockManager;
	
	@Mock
	private UserManager mockUserManager;
	
	@Mock
	private AsynchJobStatusManager mockJobManager;
	
	@InjectMocks
	private FileHandleArchivalRequestWorker worker;

	@Mock
	private ProgressCallback mockProgressCallback;
	
	@Mock
	private Message mockMessage;
	
	@Mock
	private AsynchronousJobStatus mockJobStatus;
	
	@Mock
	private UserInfo mockUser;
	
	@Mock
	private FileHandleArchivalRequest mockRequest;
	
	@Mock
	private FileHandleArchivalResponse mockResponse;
	
	private String jobId = "jobId";
	
	@BeforeEach
	public void setup() {
		when(mockMessage.getBody()).thenReturn(jobId);
		
		when(mockJobStatus.getJobId()).thenReturn(jobId);
		when(mockJobStatus.getRequestBody()).thenReturn(mockRequest);
		when(mockJobStatus.getStartedByUserId()).thenReturn(123L);
		
		when(mockJobManager.lookupJobStatus(anyString())).thenReturn(mockJobStatus);
		when(mockUserManager.getUserInfo(any())).thenReturn(mockUser);
	}
	
	@Test
	public void testRun() throws Exception {
				
		when(mockManager.processFileHandleArchivalRequest(any(), any())).thenReturn(mockResponse);
		
		// Call under test
		worker.run(mockProgressCallback, mockMessage);
		
		verify(mockJobManager).lookupJobStatus(jobId);
		verify(mockJobManager).updateJobProgress(jobId, 0L, 100L, "Starting job...");
		verify(mockUserManager).getUserInfo(123L);
		verify(mockManager).processFileHandleArchivalRequest(mockUser, mockRequest);
		verify(mockJobManager).setComplete(jobId, mockResponse);
	}
	
	@Test
	public void testRunWithFailure() throws Exception {
		
		RuntimeException ex = new RuntimeException("Something went wrong");
		
		when(mockManager.processFileHandleArchivalRequest(any(), any())).thenThrow(ex);
		
		// Call under test
		worker.run(mockProgressCallback, mockMessage);
		
		verify(mockJobManager).lookupJobStatus(jobId);
		verify(mockJobManager).updateJobProgress(jobId, 0L, 100L, "Starting job...");
		verify(mockUserManager).getUserInfo(123L);
		verify(mockManager).processFileHandleArchivalRequest(mockUser, mockRequest);
		verify(mockJobManager).setJobFailed(jobId, ex);
	}

}
