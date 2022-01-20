package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.FileHandleArchivalManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandleRestoreRequest;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResponse;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResult;
import org.sagebionetworks.repo.model.file.FileHandleRestoreStatus;
import org.sagebionetworks.worker.AsyncJobProgressCallback;

@ExtendWith(MockitoExtension.class)
public class FileHandleRestoreRequestWorkerTest {

	@Mock
	private FileHandleArchivalManager mockManager;
	
	@InjectMocks
	private FileHandleRestoreRequestWorker worker;
	
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
		
	@Mock
	private UserInfo mockUser;
	
	@Mock
	private FileHandleRestoreRequest mockRequest;
		
	@Mock
	private FileHandleRestoreResult mockResult;
	
	private String jobId = "jobId";
		
	@Test
	public void testRun() throws Exception {
		
		when(mockRequest.getFileHandleIds()).thenReturn(Arrays.asList("1", "2"));
		when(mockManager.restoreFileHandle(any(), any())).thenReturn(mockResult, mockResult);
		
		FileHandleRestoreResponse expectedResponse = new FileHandleRestoreResponse()
				.setRestoreResults(Arrays.asList(mockResult, mockResult));
		
		// Call under test
		FileHandleRestoreResponse result = worker.run(jobId, mockUser, mockRequest, mockJobCallback);
		
		assertEquals(expectedResponse, result);
		
		verify(mockManager).restoreFileHandle(mockUser, "1");
		verify(mockManager).restoreFileHandle(mockUser, "2");
	}
	
	@Test
	public void testRunWithNullList() throws Exception {
		
		when(mockRequest.getFileHandleIds()).thenReturn(null);
		
		IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			worker.run(jobId, mockUser, mockRequest, mockJobCallback);
		});
		
		assertEquals("The fileHandleIds list is required and must not be empty.", result.getMessage());
		
		verifyZeroInteractions(mockManager);
	}
	
	@Test
	public void testRunWithEmptyList() throws Exception {
		
		when(mockRequest.getFileHandleIds()).thenReturn(Collections.emptyList());
					
		IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			worker.run(jobId, mockUser, mockRequest, mockJobCallback);
		});
		
		assertEquals("The fileHandleIds list is required and must not be empty.", result.getMessage());
		
		verifyZeroInteractions(mockManager);
	}
	
	@Test
	public void testRunWithExceedBatchSize() throws Exception {
		
		when(mockRequest.getFileHandleIds()).thenReturn(
				IntStream.range(0, FileHandleRestoreRequestWorker.MAX_BATCH_SIZE + 1).boxed().map(i -> i.toString()).collect(Collectors.toList())
		);
					
		IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			worker.run(jobId, mockUser, mockRequest, mockJobCallback);
		});
		
		assertEquals("The number of file handles exceed the maximum allowed (Was: 1001, Max:1000).", result.getMessage());
		
		verifyZeroInteractions(mockManager);
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
		FileHandleRestoreResponse result = worker.run(jobId, mockUser, mockRequest, mockJobCallback);
		
		assertEquals(expectedResponse, result);
		
		verify(mockManager).restoreFileHandle(mockUser, "1");
		verify(mockManager).restoreFileHandle(mockUser, "2");
	}

}
