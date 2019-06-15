package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
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
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;

public class MembershipInvitationServiceTest {
	private MembershipInvitationServiceImpl membershipInvitationService;
	private MembershipInvitationManager mockMembershipInvitationManager;
	private UserManager mockUserManager;
	private NotificationManager mockNotificationManager;
	
	private static final Long USER_ID = 111L;
	private static final String ACCEPT_INVITATION_ENDPOINT = "acceptInvitationEndpoint:";
	private static final String NOTIFICATION_UNSUBSCRIBE_ENDPOINT = "notificationUnsubscribeEndpoint:";
	private UserInfo userInfo; 

	
	@Before
	public void before() throws Exception {
		mockMembershipInvitationManager = Mockito.mock(MembershipInvitationManager.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockNotificationManager = Mockito.mock(NotificationManager.class);

		userInfo = new UserInfo(false); 
		userInfo.setId(USER_ID);
		when(mockUserManager.getUserInfo(USER_ID)).thenReturn(userInfo);

		this.membershipInvitationService = new MembershipInvitationServiceImpl(
				mockMembershipInvitationManager,
				mockUserManager,
				mockNotificationManager);
	}

	@Test
	public void testCreate() {
		MessageToUser mtu = new MessageToUser();
		mtu.setRecipients(Collections.singleton("222"));
		String content = "foo";
		MessageToUserAndBody result = new MessageToUserAndBody(mtu, content, "text/plain");
		MembershipInvitation mis = new MembershipInvitation();
		mis.setInviteeId("1");
		when(mockMembershipInvitationManager.create(userInfo, mis)).thenReturn(mis);
		when(mockMembershipInvitationManager.createInvitationMessageToUser(
				mis, ACCEPT_INVITATION_ENDPOINT, NOTIFICATION_UNSUBSCRIBE_ENDPOINT)).thenReturn(result);

		// method under test
		membershipInvitationService.create(USER_ID, mis,
				ACCEPT_INVITATION_ENDPOINT,  NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		verify(mockUserManager).getUserInfo(USER_ID);
		verify(mockMembershipInvitationManager).create(userInfo, mis);
		verify(mockMembershipInvitationManager).createInvitationMessageToUser(
				mis, ACCEPT_INVITATION_ENDPOINT, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		ArgumentCaptor<List> messageArg = ArgumentCaptor.forClass(List.class);
		verify(mockNotificationManager).
			sendNotifications(eq(userInfo), messageArg.capture());
		assertEquals(1, messageArg.getValue().size());		
		assertEquals(result, messageArg.getValue().get(0));
	}
	
	@Test
	public void testBothEmailandId() throws Exception {
		MembershipInvitation mis = new MembershipInvitation();
		mis.setInviteeId("1");
		mis.setInviteeEmail("me@domain.com");
		when(mockMembershipInvitationManager.create(userInfo, mis)).thenReturn(mis);

		try {
			// method under test
			membershipInvitationService.create(USER_ID, mis,
					ACCEPT_INVITATION_ENDPOINT,  NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// as expected
		}

	}

	@Test
	public void testNeitherEmailNorId() throws Exception {
		MembershipInvitation mis = new MembershipInvitation();
		when(mockMembershipInvitationManager.create(userInfo, mis)).thenReturn(mis);

		try {
			// method under test
			membershipInvitationService.create(USER_ID, mis,
					ACCEPT_INVITATION_ENDPOINT,  NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// as expected
		}

	}

}
