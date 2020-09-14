package org.sagebionetworks.repo.manager.dataaccess.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
public class AccessRevokedNotificationBuilderUnitTest {

	@Mock
	private UserProfileManager mockProfileManager;
	
	@Mock
	private VelocityEngine mockVelocityEngine;
	
	@InjectMocks
	private AccessRevokedNotificationBuilder builder;
	
	@Mock
	private ManagedACTAccessRequirement mockRequirement;

	@Mock
	private AccessApproval mockApproval;
	
	@Mock
	private UserInfo mockRecipient;
	
	@Mock
	private UserProfile mockUserProfile;
	
	@Mock
	private UserProfile mockSubmitterUserProfile;
	
	@Mock
	private Template mockTemplate;
	
	@Test
	public void testSupportedTypes() {
		List<DataAccessNotificationType> expected = Arrays.asList(DataAccessNotificationType.REVOCATION);
		List<DataAccessNotificationType> result = builder.supportedTypes();
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetMimeType() {
		assertEquals("text/html", builder.getMimeType());
	}
	
	@Test
	public void testBuildSubjectWithoutDescription() {
		
		String expected = "Data Access Revoked";
		
		// Call under test
		String result = builder.buildSubject(mockRequirement, mockApproval, mockRecipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildSubjectWithDescription() {
		
		String description = "Some Dataset";
		String expected = description + " Access Revoked";
		
		when(mockRequirement.getDescription()).thenReturn(description);
		
		// Call under test
		String result = builder.buildSubject(mockRequirement, mockApproval, mockRecipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildMessageBodyForSubmitter() {
		
		Long requirementId = 1L;
		String description = "Some Description";
		Long recipientId = 2L;
		Long submitterId = recipientId;
		String firstName = "First";
		String lastName = "Last";
		
		when(mockRecipient.getId()).thenReturn(recipientId);
		when(mockApproval.getSubmitterId()).thenReturn(submitterId.toString());
		when(mockUserProfile.getFirstName()).thenReturn(firstName);
		when(mockUserProfile.getLastName()).thenReturn(lastName);
		
		when(mockProfileManager.getUserProfile(recipientId.toString())).thenReturn(mockUserProfile);
		when(mockRequirement.getId()).thenReturn(requirementId);
		when(mockRequirement.getDescription()).thenReturn(description);
		when(mockVelocityEngine.getTemplate(any(), any())).thenReturn(mockTemplate);
		doNothing().when(mockTemplate).merge(any(), any());
		
		// Call under test
		builder.buildMessageBody(mockRequirement, mockApproval, mockRecipient);
		
		verify(mockProfileManager).getUserProfile(recipientId.toString());
		verify(mockUserProfile).getFirstName();
		verify(mockUserProfile).getLastName();
		verify(mockUserProfile, never()).getUserName();
		verify(mockRequirement).getId();
		verify(mockRequirement).getDescription();
		verify(mockVelocityEngine).getTemplate(AccessRevokedNotificationBuilder.TEMPLATE_FILE, StandardCharsets.UTF_8.name());

		ArgumentCaptor<VelocityContext> contextCaptor = ArgumentCaptor.forClass(VelocityContext.class);
		
		verify(mockTemplate).merge(contextCaptor.capture(), any());
		
		VelocityContext context = contextCaptor.getValue();
		
		assertEquals(firstName + " " + lastName, context.get(AccessRevokedNotificationBuilder.PARAM_DISPLAY_NAME));
		assertEquals(requirementId, context.get(AccessRevokedNotificationBuilder.PARAM_REQUIREMENT_ID));
		assertEquals(description, context.get(AccessRevokedNotificationBuilder.PARAM_REQUIREMENT_DESCRIPTION));
		assertNull(context.get(AccessRevokedNotificationBuilder.PARAM_SUBMITTER_DISPLAY_NAME));
	}
	
	@Test
	public void testBuildMessageBodyForNonSubmitter() {
		
		Long requirementId = 1L;
		String description = "Some Description";
		Long recipientId = 2L;
		Long submitterId = 3L;
		
		String firstName = "First";
		String lastName = "Last";
		
		String submitterUserName = "Submitter";
		
		when(mockRecipient.getId()).thenReturn(recipientId);
		when(mockApproval.getSubmitterId()).thenReturn(submitterId.toString());
		when(mockUserProfile.getFirstName()).thenReturn(firstName);
		when(mockUserProfile.getLastName()).thenReturn(lastName);
		when(mockSubmitterUserProfile.getUserName()).thenReturn(submitterUserName);
		
		when(mockProfileManager.getUserProfile(submitterId.toString())).thenReturn(mockSubmitterUserProfile);
		when(mockProfileManager.getUserProfile(recipientId.toString())).thenReturn(mockUserProfile);
		
		when(mockRequirement.getId()).thenReturn(requirementId);
		when(mockRequirement.getDescription()).thenReturn(description);
		when(mockVelocityEngine.getTemplate(any(), any())).thenReturn(mockTemplate);
		doNothing().when(mockTemplate).merge(any(), any());
		
		// Call under test
		builder.buildMessageBody(mockRequirement, mockApproval, mockRecipient);
		
		verify(mockProfileManager).getUserProfile(recipientId.toString());
		verify(mockProfileManager).getUserProfile(submitterId.toString());
		verify(mockUserProfile).getFirstName();
		verify(mockUserProfile).getLastName();
		verify(mockUserProfile, never()).getUserName();
		verify(mockSubmitterUserProfile).getUserName();
		verify(mockRequirement).getId();
		verify(mockRequirement).getDescription();
		verify(mockVelocityEngine).getTemplate(AccessRevokedNotificationBuilder.TEMPLATE_FILE, StandardCharsets.UTF_8.name());

		ArgumentCaptor<VelocityContext> contextCaptor = ArgumentCaptor.forClass(VelocityContext.class);
		
		verify(mockTemplate).merge(contextCaptor.capture(), any());
		
		VelocityContext context = contextCaptor.getValue();
		
		assertEquals(firstName + " " + lastName, context.get(AccessRevokedNotificationBuilder.PARAM_DISPLAY_NAME));
		assertEquals(requirementId, context.get(AccessRevokedNotificationBuilder.PARAM_REQUIREMENT_ID));
		assertEquals(description, context.get(AccessRevokedNotificationBuilder.PARAM_REQUIREMENT_DESCRIPTION));
		assertEquals(submitterUserName, context.get(AccessRevokedNotificationBuilder.PARAM_SUBMITTER_DISPLAY_NAME));
	}
	
}
