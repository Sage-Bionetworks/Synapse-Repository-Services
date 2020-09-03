package org.sagebionetworks.dataaccess.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalNotificationManager;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.model.feature.Feature;

@ExtendWith(MockitoExtension.class)
public class AccessApprovalReminderNotificationWorkerUnitTest {

	@Mock
	private FeatureManager mockFeatureManager;
	
	@Mock
	private AccessApprovalNotificationManager mockNotificationManager;
	
	@Mock
	private LoggerProvider mockLoggerProvider;
	
	@InjectMocks
	private AccessApprovalReminderNotificationWorker worker;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private Logger mockLogger;
	
	@BeforeEach
	public void before() {
		when(mockLoggerProvider.getLogger(any())).thenReturn(mockLogger);
		worker.configureLogger(mockLoggerProvider);
	}
	
	@AfterEach
	public void after() {
		verify(mockLoggerProvider).getLogger(AccessApprovalReminderNotificationWorker.class.getName());
	}
	
	@Test
	public void testRunWithFeatureDisabled() throws Exception {
		
		boolean featureEnabled = false;
		
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		verifyZeroInteractions(mockNotificationManager);
		verifyZeroInteractions(mockLogger);
	}
	
	@Test
	public void testRun() throws Exception {
		
		boolean featureEnabled = true;
		
		List<Long> approvals = Arrays.asList(1L, 2L);
		
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		when(mockNotificationManager.listSubmitterApprovalsForUnsentReminder(any(), anyInt())).thenReturn(approvals);
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		
		for (DataAccessNotificationType type : DataAccessNotificationType.values()) {
			if (!type.isReminder()) {
				continue;
			}
			
			verify(mockNotificationManager).listSubmitterApprovalsForUnsentReminder(type, AccessApprovalReminderNotificationWorker.BATCH_SIZE);
			
			for (Long id : approvals) {
				verify(mockNotificationManager).processAccessApproval(type, id);
			}
			
			verify(mockLogger).info(eq("Sucessfully processed {} reminders (Type: {}, Errored: {}, Time: {} ms)."), eq(approvals.size()), eq(type), eq(0), anyLong());
		}
	}
	
	@Test
	public void testRunWithFailingApproval() throws Exception {
		
		boolean featureEnabled = true;
		
		List<Long> approvals = Arrays.asList(1L, 2L);
		
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		when(mockNotificationManager.listSubmitterApprovalsForUnsentReminder(any(), anyInt())).thenReturn(approvals);
		
		IllegalArgumentException ex = new IllegalArgumentException("Some error");
		DataAccessNotificationType failingType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		Long failingApproval = 1L;
		
		doNothing().when(mockNotificationManager).processAccessApproval(any(), anyLong());
		doThrow(ex).when(mockNotificationManager).processAccessApproval(failingType, failingApproval);
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		
		for (DataAccessNotificationType type : DataAccessNotificationType.values()) {
			if (!type.isReminder()) {
				continue;
			}
			
			verify(mockNotificationManager).listSubmitterApprovalsForUnsentReminder(type, AccessApprovalReminderNotificationWorker.BATCH_SIZE);
			
			for (Long id : approvals) {
				verify(mockNotificationManager).processAccessApproval(type, id);
			}
			
			if (failingType.equals(type)) {
				verify(mockLogger).error(ex.getMessage(), ex);
				verify(mockLogger).info(eq("Sucessfully processed {} reminders (Type: {}, Errored: {}, Time: {} ms)."), eq(approvals.size() - 1), eq(type), eq(1), anyLong());
			} else {
				verify(mockLogger).info(eq("Sucessfully processed {} reminders (Type: {}, Errored: {}, Time: {} ms)."), eq(approvals.size()), eq(type), eq(0), anyLong());
			}			
		}
	}
	
}
