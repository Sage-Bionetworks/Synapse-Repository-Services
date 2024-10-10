package org.sagebionetworks.tos.workers;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.authentication.TermsOfServiceManager;
import org.sagebionetworks.util.progress.ProgressCallback;

@ExtendWith(MockitoExtension.class)
public class TermsOfServiceLatestVersionRefreshWorkerTest {

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

}
