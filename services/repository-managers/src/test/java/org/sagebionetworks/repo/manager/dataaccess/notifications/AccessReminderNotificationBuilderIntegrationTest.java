package org.sagebionetworks.repo.manager.dataaccess.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

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
public class AccessReminderNotificationBuilderIntegrationTest {
	
	@Autowired
	private VelocityEngine velocity;
	
	private AccessReminderNotificationBuilder builder;
	
	// We mock all this as we only want to test the final template output
	@Mock
	private UserProfileManager profileManager;
	@Mock
	private UserProfile mockProfile;
	@Mock
	private ManagedACTAccessRequirement accessRequirement;
	@Mock
	private AccessApproval approval;
	@Mock
	private UserInfo recipient;
	
	@BeforeEach
	public void before() {
		builder = new AccessReminderNotificationBuilder(profileManager, velocity);
	}
	
	@Test
	public void testBuildMessageBodyWithDescription() throws IOException {
		
		Long recipientId = 4567L;
		Long requirementId = 1234L;
		String requirementDescription = "Some Dataset";
		String firstName = "First";
		String lastName = "Second";
		Date epxpiresOn = Date.from(LocalDate.of(2020, 7, 27).atStartOfDay(ZoneOffset.UTC).toInstant());
		
		when(recipient.getId()).thenReturn(recipientId);
		when(profileManager.getUserProfile(any())).thenReturn(mockProfile);
		when(mockProfile.getFirstName()).thenReturn(firstName);
		when(mockProfile.getLastName()).thenReturn(lastName);
		when(approval.getExpiredOn()).thenReturn(epxpiresOn);
		when(accessRequirement.getId()).thenReturn(requirementId);
		when(accessRequirement.getDescription()).thenReturn(requirementDescription);
		
		String expected = TestUtils.loadFromClasspath("message/AccessApprovalReminderNotificationWithDescription.html");

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
		Date epxpiresOn = Date.from(LocalDate.of(2020, 7, 27).atStartOfDay(ZoneOffset.UTC).toInstant());
		
		when(recipient.getId()).thenReturn(recipientId);
		when(profileManager.getUserProfile(any())).thenReturn(mockProfile);
		when(mockProfile.getFirstName()).thenReturn(firstName);
		when(mockProfile.getLastName()).thenReturn(lastName);
		when(approval.getExpiredOn()).thenReturn(epxpiresOn);
		when(accessRequirement.getId()).thenReturn(requirementId);
		when(accessRequirement.getDescription()).thenReturn(requirementDescription);
		
		String expected = TestUtils.loadFromClasspath("message/AccessApprovalReminderNotificationWithoutDescription.html");

		// Call under test
		String result = builder.buildMessageBody(accessRequirement, approval, recipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildMessageBodyWithIRBApprovalRequired() throws IOException {
		
		Long recipientId = 4567L;
		Long requirementId = 1234L;
		String requirementDescription = "Some Dataset";
		String userName = "Synapse User";
		Date epxpiresOn = Date.from(LocalDate.of(2020, 7, 27).atStartOfDay(ZoneOffset.UTC).toInstant());
		
		when(recipient.getId()).thenReturn(recipientId);
		when(profileManager.getUserProfile(any())).thenReturn(mockProfile);
		when(mockProfile.getUserName()).thenReturn(userName);
		when(approval.getExpiredOn()).thenReturn(epxpiresOn);
		when(accessRequirement.getId()).thenReturn(requirementId);
		when(accessRequirement.getDescription()).thenReturn(requirementDescription);
		when(accessRequirement.getIsIRBApprovalRequired()).thenReturn(true);
		
		String expected = TestUtils.loadFromClasspath("message/AccessApprovalReminderNotificationWithIRB.html");
		
		// Call under test
		String result = builder.buildMessageBody(accessRequirement, approval, recipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildMessageBodyWithDUCRequired() throws IOException {
		
		Long recipientId = 4567L;
		Long requirementId = 1234L;
		String requirementDescription = "Some Dataset";
		String userName = "Synapse User";
		Date expiresOn = Date.from(LocalDate.of(2020, 7, 27).atStartOfDay(ZoneOffset.UTC).toInstant());
		
		when(recipient.getId()).thenReturn(recipientId);
		when(profileManager.getUserProfile(any())).thenReturn(mockProfile);
		when(mockProfile.getUserName()).thenReturn(userName);
		when(approval.getExpiredOn()).thenReturn(expiresOn);
		when(accessRequirement.getId()).thenReturn(requirementId);
		when(accessRequirement.getDescription()).thenReturn(requirementDescription);
		when(accessRequirement.getIsDUCRequired()).thenReturn(true);
		
		String expected = TestUtils.loadFromClasspath("message/AccessApprovalReminderNotificationWithDUC.html");
		
		// Call under test
		String result = builder.buildMessageBody(accessRequirement, approval, recipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildMessageBodyWithDUCAndIRBApprovalRequired() throws IOException {
		
		Long recipientId = 4567L;
		Long requirementId = 1234L;
		String requirementDescription = "Some Dataset";
		String userName = "Synapse User";
		Date epxpiresOn = Date.from(LocalDate.of(2020, 7, 27).atStartOfDay(ZoneOffset.UTC).toInstant());
		
		when(recipient.getId()).thenReturn(recipientId);
		when(profileManager.getUserProfile(any())).thenReturn(mockProfile);
		when(mockProfile.getUserName()).thenReturn(userName);
		when(approval.getExpiredOn()).thenReturn(epxpiresOn);
		when(accessRequirement.getId()).thenReturn(requirementId);
		when(accessRequirement.getDescription()).thenReturn(requirementDescription);
		when(accessRequirement.getIsDUCRequired()).thenReturn(true);
		when(accessRequirement.getIsIRBApprovalRequired()).thenReturn(true);
		
		String expected = TestUtils.loadFromClasspath("message/AccessApprovalReminderNotificationWithIRBandDUC.html");
		
		// Call under test
		String result = builder.buildMessageBody(accessRequirement, approval, recipient);
		
		assertEquals(expected, result);
	}
	
}
