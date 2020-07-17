package org.sagebionetworks.dataaccess.workers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;

@ExtendWith(MockitoExtension.class)
public class AccessApprovalExpirationWorkerTest {

	@Mock
	private AccessApprovalManager mockAccessApprovalManager;
	
	@Mock
	private UserManager mockUserManager;

	@InjectMocks
	private AccessApprovalExpirationWorker worker;

	@Mock
	private ProgressCallback mockCallback;

	@Mock
	private UserInfo mockUser;

	@Test
	public void testRun() throws Exception {

		int processedApprovals = 10;

		when(mockUserManager.getUserInfo(anyLong())).thenReturn(mockUser);
		when(mockAccessApprovalManager.revokeExpiredApprovals(any(), any(), anyInt())).thenReturn(processedApprovals);

		// A bit of room if the test runs too fast
		long resolutionMs = 1000;
		
		Instant past = Instant.now().minus(AccessApprovalExpirationWorker.CUT_OFF_DAYS, ChronoUnit.DAYS).minusMillis(resolutionMs);
		
		// Call under test
		worker.run(mockCallback);

		verify(mockUserManager).getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		ArgumentCaptor<Instant> expireCaptor = ArgumentCaptor.forClass(Instant.class);

		verify(mockAccessApprovalManager).revokeExpiredApprovals(same(mockUser), expireCaptor.capture(),
				eq(AccessApprovalExpirationWorker.BATCH_SIZE));
		
		assertTrue(expireCaptor.getValue().isAfter(past));
		assertTrue(expireCaptor.getValue().isBefore(Instant.now().plusMillis(resolutionMs).minus(AccessApprovalExpirationWorker.CUT_OFF_DAYS, ChronoUnit.DAYS)));
	}

}
