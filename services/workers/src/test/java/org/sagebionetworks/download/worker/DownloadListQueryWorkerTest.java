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
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class DownloadListQueryWorkerTest {
	@Mock
	private DownloadListManager mockDownloadListManger;

	@InjectMocks
	private DownloadListQueryWorker worker;
	
	@Mock
	private ProgressCallback mockProgressCallback;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private UserInfo user;
	@Mock
	private DownloadListQueryRequest requestBody;
	@Mock
	private DownloadListQueryResponse responseBody;
	
	private String jobId;

	@BeforeEach
	public void before() {
		jobId = "123";
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		when(mockDownloadListManger.queryDownloadList(any(), any())).thenReturn(responseBody);
		// call under test
		DownloadListQueryResponse result = worker.run(mockProgressCallback, jobId, user, requestBody, mockJobCallback);
		
		assertEquals(responseBody, result);

		verify(mockDownloadListManger).queryDownloadList(user, requestBody);
	}
}
