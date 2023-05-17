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
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class AddToDownloadListWorkerTest {

	@Mock
	private DownloadListManager mockDownloadListManger;

	@InjectMocks
	private AddToDownloadListWorker worker;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private UserInfo user;
	@Mock
	private AddToDownloadListRequest requestBody;
	@Mock
	private AddToDownloadListResponse responseBody;

	private String jobId;

	@BeforeEach
	public void before() {
		jobId = "123";
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		when(mockDownloadListManger.addToDownloadList(any(), any(), any())).thenReturn(responseBody);
		// call under test
		AddToDownloadListResponse result = worker.run(jobId, user, requestBody, mockJobCallback);

		assertEquals(responseBody, result);

		verify(mockDownloadListManger).addToDownloadList(mockJobCallback, user, requestBody);
	}
}
