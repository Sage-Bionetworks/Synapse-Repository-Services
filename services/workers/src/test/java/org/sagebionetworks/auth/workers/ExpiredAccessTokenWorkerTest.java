package org.sagebionetworks.auth.workers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenManager;
import org.sagebionetworks.util.progress.ProgressCallback;

@ExtendWith(MockitoExtension.class)
public class ExpiredAccessTokenWorkerTest {
	
	@Mock
	private OIDCTokenManager mockOidcTokenManager;
	
	@Mock
	private WorkerLogger mockLogger;
	
	@InjectMocks
	private ExpiredAccessTokenWorker worker;
	
	@Mock
	private ProgressCallback mockCallback;

	@Test
	public void testRun() throws Exception {
		doReturn(1_000, 1_000, 0).when(mockOidcTokenManager).revokeExpiredOIDCAccessTokens();
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockOidcTokenManager, times(3)).revokeExpiredOIDCAccessTokens();
	}
	
	@Test
	public void testRunWithError() throws Exception {
		RuntimeException ex = new RuntimeException("Nope");
		
		doThrow(ex).when(mockOidcTokenManager).revokeExpiredOIDCAccessTokens();
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockOidcTokenManager).revokeExpiredOIDCAccessTokens();
		verify(mockLogger).logWorkerFailure(ExpiredAccessTokenWorker.class.getName(), ex, false);
	}

}
