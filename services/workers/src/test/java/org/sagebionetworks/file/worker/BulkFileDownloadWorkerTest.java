package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.FileHandlePackageManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class BulkFileDownloadWorkerTest {

	@Mock
	private FileHandlePackageManager mockBulkDownloadManager;
	@InjectMocks
	private BulkFileDownloadWorker worker;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private BulkFileDownloadRequest mockRequest;
	@Mock
	private BulkFileDownloadResponse mockResponse;
	
	private String jobId;
	private UserInfo user;

	@BeforeEach
	public void before() throws JSONObjectAdapterException {
		user = new UserInfo(false, 777L);
		jobId = "9999";
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		when(mockBulkDownloadManager.buildZip(any(), any())).thenReturn(mockResponse);
		
		// call under test
		BulkFileDownloadResponse result = worker.run(jobId, user, mockRequest, mockJobCallback);
		
		assertEquals(mockResponse, result);
		
		verify(mockBulkDownloadManager).buildZip(user, mockRequest);
	}
}
