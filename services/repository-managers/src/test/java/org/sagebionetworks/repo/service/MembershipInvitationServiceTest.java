package org.sagebionetworks.repo.service;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.MembershipInvitationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.service.MembershipInvitationServiceImpl;

@ExtendWith(MockitoExtension.class)
public class MembershipInvitationServiceTest {
	@Mock
	private MembershipInvitationManager mockMembershipInvitationManager;
	@Mock
	private UserManager mockUserManager;
	
	@InjectMocks
	private MembershipInvitationServiceImpl membershipInvitationService;
	
	private static final Long USER_ID = 111L;
	private static final String ACCEPT_INVITATION_ENDPOINT = "acceptInvitationEndpoint:";
	private static final String NOTIFICATION_UNSUBSCRIBE_ENDPOINT = "notificationUnsubscribeEndpoint:";
	private UserInfo userInfo; 

	
	@BeforeEach
	public void before() throws Exception {
		userInfo = new UserInfo(false); 
		userInfo.setId(USER_ID);
		when(mockUserManager.getUserInfo(USER_ID)).thenReturn(userInfo);
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
		doNothing().when(mockMembershipInvitationManager).sendInvitationEmailToSynapseUser(userInfo, mis, ACCEPT_INVITATION_ENDPOINT, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);

		// method under test
		membershipInvitationService.create(USER_ID, mis,
				ACCEPT_INVITATION_ENDPOINT,  NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		verify(mockUserManager).getUserInfo(USER_ID);
		verify(mockMembershipInvitationManager).create(userInfo, mis);
		verify(mockMembershipInvitationManager).sendInvitationEmailToSynapseUser(userInfo, mis, ACCEPT_INVITATION_ENDPOINT, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
	}
	
	@Test
	public void testBothEmailandId() throws Exception {
		MembershipInvitation mis = new MembershipInvitation();
		mis.setInviteeId("1");
		mis.setInviteeEmail("me@domain.com");
		when(mockMembershipInvitationManager.create(userInfo, mis)).thenReturn(mis);

		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// method under test
			membershipInvitationService.create(USER_ID, mis, ACCEPT_INVITATION_ENDPOINT,  NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		});

	}

	@Test
	public void testNeitherEmailNorId() throws Exception {
		MembershipInvitation mis = new MembershipInvitation();
		when(mockMembershipInvitationManager.create(userInfo, mis)).thenReturn(mis);

		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// method under test
			membershipInvitationService.create(USER_ID, mis, ACCEPT_INVITATION_ENDPOINT,  NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		});

	}
	
	@Test
	public void testInviteByEmailCertified() throws Exception {
		
		userInfo.setGroups(Collections.singleton(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		
		MembershipInvitation mis = new MembershipInvitation();
		mis.setInviteeEmail("me@domain.com");
		
		when(mockMembershipInvitationManager.create(userInfo, mis)).thenReturn(mis);
		doNothing().when(mockMembershipInvitationManager).sendInvitationEmailToEmail(userInfo, mis, ACCEPT_INVITATION_ENDPOINT);

		// method under test
		membershipInvitationService.create(USER_ID, mis, ACCEPT_INVITATION_ENDPOINT,  NOTIFICATION_UNSUBSCRIBE_ENDPOINT);

		verify(mockMembershipInvitationManager).create(userInfo, mis);
		verify(mockMembershipInvitationManager).sendInvitationEmailToEmail(userInfo, mis, ACCEPT_INVITATION_ENDPOINT);
	}

}
