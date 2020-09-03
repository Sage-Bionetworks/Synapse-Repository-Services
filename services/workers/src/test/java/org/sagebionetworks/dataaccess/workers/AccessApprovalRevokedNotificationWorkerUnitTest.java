package org.sagebionetworks.dataaccess.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalNotificationManager;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.message.ChangeMessage;


@ExtendWith(MockitoExtension.class)
public class AccessApprovalRevokedNotificationWorkerUnitTest {

	@Mock
	private AccessApprovalNotificationManager mockNotificationManager;

	@Mock
	private FeatureManager mockFeatureManager;
	
	@InjectMocks
	private AccessApprovalRevokedNotificationWorker worker;

	@Mock
	private ChangeMessage mockChange;
	
	@Mock
	private ProgressCallback mockCallback;

	@Test
	public void testRunWithDisabledFeature() throws Exception {
		
		boolean featureEnabled = false;
		
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		
		
		// Call under test
		worker.run(mockCallback, mockChange);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		verifyZeroInteractions(mockNotificationManager);
	}
	
	@Test
	public void testRunWithEnabledFeature() throws Exception {
		
		boolean featureEnabled = true;
		
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		
		
		// Call under test
		worker.run(mockCallback, mockChange);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		verify(mockNotificationManager).processAccessApprovalChange(mockChange);
	}
}
