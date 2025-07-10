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
import org.sagebionetworks.repo.manager.principal.UserStatusManager;
import org.sagebionetworks.util.progress.ProgressCallback;

@ExtendWith(MockitoExtension.class)
public class InactiveUsersWorkerUnitTest {

	@Mock
	private UserStatusManager mockUserStatusManager;
	
	@InjectMocks
	private InactiveUsersWorker worker;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Test
	public void testRun() throws Exception {
		
		when(mockUserStatusManager.disableInactiveUsers(500)).thenReturn(500, 0);
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockUserStatusManager, times(2)).disableInactiveUsers(500);
	}
	
	@Test
	public void testRunWithNoInactiveUsers() throws Exception {
		
		when(mockUserStatusManager.disableInactiveUsers(500)).thenReturn(0);
		
		// Call under test
		worker.run(mockCallback);
		
		verifyNoMoreInteractions(mockUserStatusManager);
	}

}
