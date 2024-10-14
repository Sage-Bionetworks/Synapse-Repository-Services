package org.sagebionetworks.tos.workers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.authentication.TermsOfServiceManager;
import org.sagebionetworks.repo.util.github.GithubApiException;
import org.sagebionetworks.util.progress.ProgressCallback;

@ExtendWith(MockitoExtension.class)
public class TermsOfServiceLatestVersionRefreshWorkerTest {

	@Mock
	private WorkerLogger mockLogger;
	
	@Mock
	private TermsOfServiceManager mockManager;
	
	@InjectMocks
	private TermsOfServiceLatestVersionRefreshWorker worker;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Test
	public void testRun() throws Exception {
		
		worker.run(mockCallback);
		
		verify(mockManager).refreshLatestVersion();
	}

	@Test
	public void testRunWithException() throws Exception {
		GithubApiException ex = new GithubApiException("failed request");
		
		when(mockManager.refreshLatestVersion()).thenThrow(ex);

		worker.run(mockCallback);
		
		verify(mockLogger).logWorkerFailure(TermsOfServiceLatestVersionRefreshWorker.class.getName(), ex, false);
	}
	
}
