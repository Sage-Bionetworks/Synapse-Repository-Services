package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.sagebionetworks.repo.manager.file.FileHandleArchivalManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.repo.model.file.FileHandleRestoreRequest;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResponse;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResult;
import org.sagebionetworks.repo.model.file.FileHandleRestoreStatus;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class FileHandleRestoreRequestWorkerTest {

	@Mock
	private FileHandleArchivalManager mockManager;
	
	@Mock
	private UserManager mockUserManager;
	
	@Mock
	private AsynchJobStatusManager mockJobManager;
	
	@InjectMocks
	private FileHandleRestoreRequestWorker worker;

	@Mock
	private ProgressCallback mockProgressCallback;
	
	@Mock
	private Message mockMessage;
	
	@Mock
	private AsynchronousJobStatus mockJobStatus;
	
	@Mock
	private UserInfo mockUser;
	
	@Mock
	private FileHandleRestoreRequest mockRequest;
		
	@Mock
	private FileHandleRestoreResult mockResult;
	
	@Captor
	private ArgumentCaptor<Throwable> exCaptor;
	
	private Long userId = 123L;
	private String jobId = "jobId";
	
	@BeforeEach
	public void setup() {
		when(mockMessage.getBody()).thenReturn(jobId);
		
		when(mockJobStatus.getJobId()).thenReturn(jobId);
		when(mockJobStatus.getRequestBody()).thenReturn(mockRequest);
		when(mockJobStatus.getStartedByUserId()).thenReturn(userId);
		
		when(mockJobManager.lookupJobStatus(anyString())).thenReturn(mockJobStatus);
		when(mockUserManager.getUserInfo(any())).thenReturn(mockUser);
	}
	
	@Test
	public void testRun() throws Exception {
		
		when(mockRequest.getFileHandleIds()).thenReturn(Arrays.asList("1", "2"));
		when(mockManager.restoreFileHandle(any(), any())).thenReturn(mockResult, mockResult);
		
		FileHandleRestoreResponse expectedResponse = new FileHandleRestoreResponse()
				.setRestoreResults(Arrays.asList(mockResult, mockResult));
		
		// Call under test
		worker.run(mockProgressCallback, mockMessage);
		
		verify(mockJobManager).lookupJobStatus(jobId);
		verify(mockJobManager).updateJobProgress(jobId, 0L, 100L, "Starting job...");
		verify(mockUserManager).getUserInfo(userId);
		verify(mockManager).restoreFileHandle(mockUser, "1");
		verify(mockManager).restoreFileHandle(mockUser, "2");
		verify(mockJobManager).setComplete(jobId, expectedResponse);
	}
	
	@Test
	public void testRunWithNullList() throws Exception {
		
		when(mockRequest.getFileHandleIds()).thenReturn(null);
					
		// Call under test
		worker.run(mockProgressCallback, mockMessage);
		
		verify(mockJobManager).lookupJobStatus(jobId);
		verify(mockJobManager).updateJobProgress(jobId, 0L, 100L, "Starting job...");
		verify(mockUserManager).getUserInfo(userId);
		verifyZeroInteractions(mockManager);
		
		verify(mockJobManager).setJobFailed(eq(jobId), exCaptor.capture());
		
		assertEquals(IllegalArgumentException.class, exCaptor.getValue().getClass());
		assertEquals("The fileHandleIds list is required and must not be empty.", exCaptor.getValue().getMessage());
	}
	
	@Test
	public void testRunWithEmptyList() throws Exception {
		
		when(mockRequest.getFileHandleIds()).thenReturn(Collections.emptyList());
					
		// Call under test
		worker.run(mockProgressCallback, mockMessage);
		
		verify(mockJobManager).lookupJobStatus(jobId);
		verify(mockJobManager).updateJobProgress(jobId, 0L, 100L, "Starting job...");
		verify(mockUserManager).getUserInfo(userId);
		verifyZeroInteractions(mockManager);
		
		verify(mockJobManager).setJobFailed(eq(jobId), exCaptor.capture());
		
		assertEquals(IllegalArgumentException.class, exCaptor.getValue().getClass());
		assertEquals("The fileHandleIds list is required and must not be empty.", exCaptor.getValue().getMessage());
	}
	
	@Test
	public void testRunWithExceedBatchSize() throws Exception {
		
		when(mockRequest.getFileHandleIds()).thenReturn(
				IntStream.range(0, FileHandleRestoreRequestWorker.MAX_BATCH_SIZE + 1).boxed().map(i -> i.toString()).collect(Collectors.toList())
		);
					
		// Call under test
		worker.run(mockProgressCallback, mockMessage);
		
		verify(mockJobManager).lookupJobStatus(jobId);
		verify(mockJobManager).updateJobProgress(jobId, 0L, 100L, "Starting job...");
		verify(mockUserManager).getUserInfo(userId);
		verifyZeroInteractions(mockManager);
		
		verify(mockJobManager).setJobFailed(eq(jobId), exCaptor.capture());
		
		assertEquals(IllegalArgumentException.class, exCaptor.getValue().getClass());
		assertEquals("The number of file handles exceed the maximum allowed (Was: 1001, Max:1000).", exCaptor.getValue().getMessage());
	}
	
	@Test
	public void testRunWithRestoreException() throws Exception {
		
		RuntimeException ex = new RuntimeException("Something went wrong");
		
		when(mockRequest.getFileHandleIds()).thenReturn(Arrays.asList("1", "2"));
		
		when(mockManager.restoreFileHandle(any(), any())).thenReturn(mockResult);
		when(mockManager.restoreFileHandle(mockUser, "1")).thenThrow(ex);
		
		FileHandleRestoreResponse expectedResponse = new FileHandleRestoreResponse()
				.setRestoreResults(Arrays.asList(
						new FileHandleRestoreResult()
							.setFileHandleId("1")
							.setStatus(FileHandleRestoreStatus.FAILED)
							.setStatusMessage("Something went wrong"),
						mockResult
					)
				);
		
		// Call under test
		worker.run(mockProgressCallback, mockMessage);
		
		verify(mockJobManager).lookupJobStatus(jobId);
		verify(mockJobManager).updateJobProgress(jobId, 0L, 100L, "Starting job...");
		verify(mockUserManager).getUserInfo(userId);
		verify(mockManager).restoreFileHandle(mockUser, "1");
		verify(mockManager).restoreFileHandle(mockUser, "2");
		verify(mockJobManager).setComplete(jobId, expectedResponse);
	}

}
