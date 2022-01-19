package org.sagebionetworks.download.worker;

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
import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.download.DownloadListPackageRequest;
import org.sagebionetworks.repo.model.download.DownloadListPackageResponse;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class DownloadListPackageWorkerTest {
	@Mock
	private DownloadListManager mockDownloadListManger;

	@InjectMocks
	private DownloadListPackageWorker worker;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private UserInfo user;
	@Mock
	private DownloadListPackageRequest requestBody;
	@Mock
	private DownloadListPackageResponse responseBody;

	private String jobId;

	@BeforeEach
	public void before() {
		jobId = "123";
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		when(mockDownloadListManger.packageFiles(any(), any(), any())).thenReturn(responseBody);
		// call under test
		DownloadListPackageResponse result = worker.run(jobId, user, requestBody, mockJobCallback);

		assertEquals(responseBody, result);

		verify(mockDownloadListManger).packageFiles(mockJobCallback, user, requestBody);
	}
}
