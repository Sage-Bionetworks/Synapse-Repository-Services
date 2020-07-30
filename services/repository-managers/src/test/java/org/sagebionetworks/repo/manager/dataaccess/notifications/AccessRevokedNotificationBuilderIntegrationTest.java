package org.sagebionetworks.repo.manager.dataaccess.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(value = {SpringExtension.class, MockitoExtension.class})
@ContextConfiguration("classpath:test-context.xml")
public class AccessRevokedNotificationBuilderIntegrationTest {
	
	@Autowired
	private VelocityEngine velocity;
	
	private AccessRevokedNotificationBuilder builder;
	
	// We mock all this as we only want to test the final template output
	@Mock
	private UserProfileManager profileManager;
	@Mock
	private UserProfile mockProfile;
	@Mock
	private UserProfile mockSubmitterProfile;
	@Mock
	private ManagedACTAccessRequirement accessRequirement;
	@Mock
	private AccessApproval approval;
	@Mock
	private UserInfo recipient;
	
	@BeforeEach
	public void before() {
		builder = new AccessRevokedNotificationBuilder(profileManager, velocity);
	}
	
	@Test
	public void testBuildMessageBodyWithDescription() throws IOException {
		
		Long recipientId = 4567L;
		Long requirementId = 1234L;
		String requirementDescription = "Some Dataset";
		String firstName = "First";
		String lastName = "Second";
		
		when(recipient.getId()).thenReturn(recipientId);
		when(approval.getSubmitterId()).thenReturn(recipientId.toString());
		when(profileManager.getUserProfile(any())).thenReturn(mockProfile);
		when(mockProfile.getFirstName()).thenReturn(firstName);
		when(mockProfile.getLastName()).thenReturn(lastName);
		when(accessRequirement.getId()).thenReturn(requirementId);
		when(accessRequirement.getDescription()).thenReturn(requirementDescription);
		
		String expected = TestUtils.loadFromClasspath("message/AccessApprovalRevokedNotificationWithDescription.html");

		// Call under test
		String result = builder.buildMessageBody(accessRequirement, approval, recipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildMessageBodyWithoutDescription() throws IOException {
		
		Long recipientId = 4567L;
		Long requirementId = 1234L;
		String requirementDescription = "";
		String firstName = "First";
		String lastName = "Second";
		
		when(recipient.getId()).thenReturn(recipientId);
		when(approval.getSubmitterId()).thenReturn(recipientId.toString());
		when(profileManager.getUserProfile(any())).thenReturn(mockProfile);
		when(mockProfile.getFirstName()).thenReturn(firstName);
		when(mockProfile.getLastName()).thenReturn(lastName);
		when(accessRequirement.getId()).thenReturn(requirementId);
		when(accessRequirement.getDescription()).thenReturn(requirementDescription);
		
		String expected = TestUtils.loadFromClasspath("message/AccessApprovalRevokedNotificationWithoutDescription.html");

		// Call under test
		String result = builder.buildMessageBody(accessRequirement, approval, recipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildMessageBodyWithoutFirstLastName() throws IOException {
		
		Long recipientId = 4567L;
		Long requirementId = 1234L;
		String requirementDescription = "Some Dataset";
		String firstName = "";
		String lastName = null;
		String userName = "Synapse User";
		
		when(recipient.getId()).thenReturn(recipientId);
		when(approval.getSubmitterId()).thenReturn(recipientId.toString());
		when(profileManager.getUserProfile(any())).thenReturn(mockProfile);
		when(mockProfile.getFirstName()).thenReturn(firstName);
		when(mockProfile.getLastName()).thenReturn(lastName);
		when(mockProfile.getUserName()).thenReturn(userName);
		when(accessRequirement.getId()).thenReturn(requirementId);
		when(accessRequirement.getDescription()).thenReturn(requirementDescription);
		
		String expected = TestUtils.loadFromClasspath("message/AccessApprovalRevokedNotificationWithoutFirstLast.html");
		
		// Call under test
		String result = builder.buildMessageBody(accessRequirement, approval, recipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildMessageBodyForNonSubmitter() throws IOException {
		
		Long recipientId = 4567L;
		Long submitterId = 7684L;
		Long requirementId = 1234L;
		String requirementDescription = "Some Dataset";
		String userName = "Synapse User";
		String submitterUserName = "Submitter User";
		
		when(recipient.getId()).thenReturn(recipientId);
		when(approval.getSubmitterId()).thenReturn(submitterId.toString());
		when(profileManager.getUserProfile(recipientId.toString())).thenReturn(mockProfile);
		when(profileManager.getUserProfile(submitterId.toString())).thenReturn(mockSubmitterProfile);
		when(mockProfile.getUserName()).thenReturn(userName);
		when(mockSubmitterProfile.getUserName()).thenReturn(submitterUserName);
		when(accessRequirement.getId()).thenReturn(requirementId);
		when(accessRequirement.getDescription()).thenReturn(requirementDescription);
		
		String expected = TestUtils.loadFromClasspath("message/AccessApprovalRevokedNotificationForNonSubmitter.html");
		
		// Call under test
		String result = builder.buildMessageBody(accessRequirement, approval, recipient);
		
		assertEquals(expected, result);
	}
	
}
