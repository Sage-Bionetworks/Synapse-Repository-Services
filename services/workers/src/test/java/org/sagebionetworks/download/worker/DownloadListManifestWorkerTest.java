package org.sagebionetworks.download.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.download.DownloadListManifestRequest;
import org.sagebionetworks.repo.model.download.DownloadListManifestResponse;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class DownloadListManifestWorkerTest {

	@Mock
	private AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	private DownloadListManager mockDownloadListManger;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private ProgressCallback mockProgressCallback;
	@Mock
	private Message mockMessage;

	@InjectMocks
	DownloadListManifestWorker worker;

	String jobId;
	AsynchronousJobStatus jobStatus;
	Long startedByUserId;
	UserInfo user;
	DownloadListManifestRequest requestBody;
	DownloadListManifestResponse responseBody;

	@BeforeEach
	public void before() {
		jobId = "123";
		startedByUserId = 987L;
		boolean isAdmin = false;
		user = new UserInfo(isAdmin, startedByUserId);

		requestBody = new DownloadListManifestRequest();

		jobStatus = new AsynchronousJobStatus();
		jobStatus.setJobId(jobId);
		jobStatus.setRequestBody(requestBody);
		jobStatus.setStartedByUserId(startedByUserId);

		responseBody = new DownloadListManifestResponse();
		responseBody.setResultFileHandleId("123");
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(any())).thenReturn(jobStatus);
		when(mockUserManager.getUserInfo(startedByUserId)).thenReturn(user);
		when(mockDownloadListManger.createManifest(any(), any(), any())).thenReturn(responseBody);
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		verify(mockAsynchJobStatusManager).lookupJobStatus(jobId);
		verify(mockAsynchJobStatusManager).updateJobProgress(jobId, 0L, 100L, "Starting job...");
		verify(mockDownloadListManger).createManifest(mockProgressCallback, user, requestBody);
		verify(mockAsynchJobStatusManager).setComplete(jobId, responseBody);
		verify(mockAsynchJobStatusManager, never()).setJobFailed(any(), any());
	}

	@Test
	public void testRunFailed() throws RecoverableMessageException, Exception {
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(any())).thenReturn(jobStatus);
		when(mockUserManager.getUserInfo(startedByUserId)).thenReturn(user);
		UnauthorizedException exception = new UnauthorizedException("no");
		when(mockDownloadListManger.createManifest(any(), any(), any())).thenThrow(exception);
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		verify(mockDownloadListManger).createManifest(mockProgressCallback, user, requestBody);
		verify(mockAsynchJobStatusManager, never()).setComplete(any(), any());
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, exception);
	}

}
