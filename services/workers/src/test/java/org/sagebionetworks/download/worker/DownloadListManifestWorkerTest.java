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
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.download.DownloadListManifestRequest;
import org.sagebionetworks.repo.model.download.DownloadListManifestResponse;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class DownloadListManifestWorkerTest {

	@Mock
	private DownloadListManager mockDownloadListManger;

	@InjectMocks
	DownloadListManifestWorker worker;

	@Mock
	private ProgressCallback mockProgressCallback;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private UserInfo user;
	@Mock
	private DownloadListManifestRequest requestBody;
	@Mock
	private DownloadListManifestResponse responseBody;

	private String jobId;

	@BeforeEach
	public void before() {
		jobId = "123";
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		when(mockDownloadListManger.createManifest(any(), any(), any())).thenReturn(responseBody);
		// call under test
		DownloadListManifestResponse result = worker.run(mockProgressCallback, jobId, user, requestBody, mockJobCallback);

		assertEquals(responseBody, result);

		verify(mockDownloadListManger).createManifest(mockProgressCallback, user, requestBody);
	}

}
