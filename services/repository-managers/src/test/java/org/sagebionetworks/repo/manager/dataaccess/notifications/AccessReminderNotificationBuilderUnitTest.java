package org.sagebionetworks.repo.manager.dataaccess.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationType;

@ExtendWith(MockitoExtension.class)
public class AccessReminderNotificationBuilderUnitTest {
	
	@Mock
	private UserProfileManager mockProfileManager;
	
	@Mock
	private VelocityEngine mockVelocityEngine;
	
	@InjectMocks
	private AccessReminderNotificationBuilder builder;
	
	@Mock
	private ManagedACTAccessRequirement mockRequirement;

	@Mock
	private AccessApproval mockApproval;
	
	@Mock
	private UserInfo mockRecipient;
	
	@Mock
	private UserProfile mockUserProfile;
	
	@Mock
	private Template mockTemplate;
	
	@Test
	public void testSupportedTypes() {
		// We are explicit here so that the test will break if a new reminder type is added
		List<DataAccessNotificationType> expected = Arrays.asList(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, DataAccessNotificationType.SECOND_RENEWAL_REMINDER);
		List<DataAccessNotificationType> result = builder.supportedTypes();
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetMimeType() {
		assertEquals("text/html", builder.getMimeType());
	}
	
	@Test
	public void testBuildSubjectWithoutDescription() {
		
		String expected = "Data Access Renewal Reminder";
		
		// Call under test
		String result = builder.buildSubject(mockRequirement, mockApproval, mockRecipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildSubjectWithDescription() {
		
		String description = "Some Dataset";
		String expected = description + " Access Renewal Reminder";
		
		when(mockRequirement.getDescription()).thenReturn(description);
		
		// Call under test
		String result = builder.buildSubject(mockRequirement, mockApproval, mockRecipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildMessageBody() {
		
		Long requirementId = 1L;
		String description = "Some Description";
		Long recipientId = 2L;
		String userName = "Synapse User";
		Date expiredOn = Date.from(LocalDate.of(2020, 7, 27).atStartOfDay(ZoneOffset.UTC).toInstant());
		
		when(mockRecipient.getId()).thenReturn(recipientId);
		when(mockUserProfile.getUserName()).thenReturn(userName);
		
		when(mockProfileManager.getUserProfile(any())).thenReturn(mockUserProfile);
		when(mockRequirement.getId()).thenReturn(requirementId);
		when(mockRequirement.getDescription()).thenReturn(description);
		when(mockApproval.getExpiredOn()).thenReturn(expiredOn);
		when(mockVelocityEngine.getTemplate(any(), any())).thenReturn(mockTemplate);
		doNothing().when(mockTemplate).merge(any(), any());
		
		// Call under test
		builder.buildMessageBody(mockRequirement, mockApproval, mockRecipient);
		
		verify(mockProfileManager).getUserProfile(recipientId.toString());
		verify(mockUserProfile).getUserName();
		verify(mockApproval).getExpiredOn();
		verify(mockRequirement).getId();
		verify(mockRequirement).getDescription();
		verify(mockRequirement).getIsDUCRequired();
		verify(mockRequirement).getIsIRBApprovalRequired();
		verify(mockVelocityEngine).getTemplate(AccessReminderNotificationBuilder.TEMPLATE_FILE, StandardCharsets.UTF_8.name());

		ArgumentCaptor<VelocityContext> contextCaptor = ArgumentCaptor.forClass(VelocityContext.class);
		
		verify(mockTemplate).merge(contextCaptor.capture(), any());
		
		VelocityContext context = contextCaptor.getValue();
		
		assertEquals(userName, context.get(AccessReminderNotificationBuilder.PARAM_DISPLAY_NAME));
		assertEquals(requirementId, context.get(AccessReminderNotificationBuilder.PARAM_REQUIREMENT_ID));
		assertEquals(description, context.get(AccessReminderNotificationBuilder.PARAM_REQUIREMENT_DESCRIPTION));
		assertEquals("July 27, 2020", context.get(AccessReminderNotificationBuilder.PARAM_RENEWAL_DATE));
		assertEquals(false, context.get(AccessReminderNotificationBuilder.PARAM_DUC_REQUIRED));
		assertEquals(false, context.get(AccessReminderNotificationBuilder.PARAM_IRB_APPROVAL_REQUIRED));
	}

	@Test
	public void testGetFormattedDate() {
		Date date = Date.from(LocalDate.of(2020, 7, 4).atStartOfDay(ZoneOffset.UTC).toInstant());
		String expected = "July 4, 2020";
	
		assertEquals(expected, AccessReminderNotificationBuilder.getFormattedDate(date));
	}
	
	@Test
	public void testGetFormattedDateWithNoDate() {
		Date date = null;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			AccessReminderNotificationBuilder.getFormattedDate(date);
		}).getMessage();
		
		assertEquals("date is required.", errorMessage);
	}
	
	
}
