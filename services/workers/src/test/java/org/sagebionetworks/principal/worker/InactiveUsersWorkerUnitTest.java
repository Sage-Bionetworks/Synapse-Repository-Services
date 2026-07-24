package org.sagebionetworks.principal.worker;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.principal.UserStatusManager;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.util.progress.ProgressCallback;

@ExtendWith(MockitoExtension.class)
public class InactiveUsersWorkerUnitTest {

	@Mock
	private UserStatusManager mockUserStatusManager;

	@Mock
	private FeatureManager mockFeatureManager;

	@InjectMocks
	private InactiveUsersWorker worker;

	@Mock
	private ProgressCallback mockCallback;

	@Test
	public void testRun() throws Exception {

		when(mockFeatureManager.isFeatureEnabled(Feature.DISABLE_INACTIVE_USERS)).thenReturn(true);
		when(mockUserStatusManager.warnInactiveUsers(500)).thenReturn(500, 0);
		when(mockUserStatusManager.disableInactiveUsers(500)).thenReturn(500, 0);

		// call under test
		worker.run(mockCallback);

		verify(mockUserStatusManager, times(2)).warnInactiveUsers(500);
		verify(mockUserStatusManager, times(2)).disableInactiveUsers(500);
	}

	@Test
	public void testRunWithNoInactiveUsers() throws Exception {

		when(mockFeatureManager.isFeatureEnabled(Feature.DISABLE_INACTIVE_USERS)).thenReturn(true);
		when(mockUserStatusManager.warnInactiveUsers(500)).thenReturn(0);
		when(mockUserStatusManager.disableInactiveUsers(500)).thenReturn(0);

		// call under test
		worker.run(mockCallback);

		verify(mockUserStatusManager).warnInactiveUsers(500);
		verify(mockUserStatusManager).disableInactiveUsers(500);
	}


	@Test
	public void testRunWithFeatureDisabled() throws Exception {

		when(mockFeatureManager.isFeatureEnabled(Feature.DISABLE_INACTIVE_USERS)).thenReturn(false);

		// call under test
		worker.run(mockCallback);

		verifyNoMoreInteractions(mockUserStatusManager);
	}

}
