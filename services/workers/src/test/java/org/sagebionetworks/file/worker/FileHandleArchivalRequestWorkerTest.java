package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.FileHandleArchivalManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.util.progress.ProgressCallback;

@ExtendWith(MockitoExtension.class)
public class FileHandleArchivalRequestWorkerTest {
	
	@Mock
	private FileHandleArchivalManager mockManager;
		
	@InjectMocks
	private FileHandleArchivalRequestWorker worker;
	
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	
	@Mock
	private UserInfo mockUser;
	
	@Mock
	private FileHandleArchivalRequest mockRequest;
	
	@Mock
	private FileHandleArchivalResponse mockResponse;
	
	private String jobId = "jobId";
	
	@Test
	public void testRun() throws Exception {
				
		when(mockManager.processFileHandleArchivalRequest(any(), any())).thenReturn(mockResponse);
		
		// Call under test
		FileHandleArchivalResponse result = worker.run(jobId, mockUser, mockRequest, mockJobCallback);
		
		assertEquals(mockResponse, result);
		
		verify(mockManager).processFileHandleArchivalRequest(mockUser, mockRequest);
	}
}
