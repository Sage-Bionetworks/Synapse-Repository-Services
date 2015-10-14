package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.MembershipInvitationManager;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;

public class MembershipInvitationServiceTest {
	private MembershipInvitationServiceImpl membershipInvitationService;
	private MembershipInvitationManager mockMembershipInvitationManager;
	private UserManager mockUserManager;
	private NotificationManager mockNotificationManager;
	
	
	@Before
	public void before() throws Exception {
		mockMembershipInvitationManager = Mockito.mock(MembershipInvitationManager.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockNotificationManager = Mockito.mock(NotificationManager.class);

		this.membershipInvitationService = new MembershipInvitationServiceImpl(
				mockMembershipInvitationManager,
				mockUserManager,
				mockNotificationManager);
	}

	@Test
	public void testCreate() throws Exception {
		Long userId = 111L;
		UserInfo userInfo = new UserInfo(false); 
		userInfo.setId(userId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		MessageToUser mtu = new MessageToUser();
		mtu.setRecipients(Collections.singleton("222"));
		String content = "foo";
		MessageToUserAndBody result = new MessageToUserAndBody(mtu, content, "text/plain");
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		String acceptInvitationEndpoint = "acceptInvitationEndpoint:";
		String notificationUnsubscribeEndpoint = "notificationUnsubscribeEndpoint:";
		when(mockMembershipInvitationManager.create(userInfo, mis)).thenReturn(mis);
		when(mockMembershipInvitationManager.createInvitationNotification(
				mis, acceptInvitationEndpoint, notificationUnsubscribeEndpoint)).thenReturn(result);

		membershipInvitationService.create(userId, mis,
				acceptInvitationEndpoint,  notificationUnsubscribeEndpoint);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockMembershipInvitationManager).create(userInfo, mis);
		verify(mockMembershipInvitationManager).createInvitationNotification(
				mis, acceptInvitationEndpoint, notificationUnsubscribeEndpoint);
		
		ArgumentCaptor<List> messageArg = ArgumentCaptor.forClass(List.class);
		verify(mockNotificationManager).
			sendNotifications(eq(userInfo), messageArg.capture());
		assertEquals(1, messageArg.getValue().size());		
		assertEquals(result, messageArg.getValue().get(0));		
	}

}
