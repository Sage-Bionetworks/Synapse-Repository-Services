package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.MembershipInvitationManager;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.util.Pair;

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
	public void testCreate() {
		Long userId = 111L;
		UserInfo userInfo = new UserInfo(false); 
		userInfo.setId(userId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		MessageToUser mtu = new MessageToUser();
		mtu.setRecipients(Collections.singleton("222"));
		String content = "foo";
		Pair<MessageToUser, String> result = new Pair<MessageToUser, String>(mtu, content);
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		when(mockMembershipInvitationManager.create(userInfo, mis)).thenReturn(mis);
		when(mockMembershipInvitationManager.createInvitationNotification(mis)).thenReturn(result);

		membershipInvitationService.create(userId, mis);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockMembershipInvitationManager).create(userInfo, mis);
		verify(mockMembershipInvitationManager).createInvitationNotification(mis);
		
		ArgumentCaptor<MessageToUser> mtuArg = ArgumentCaptor.forClass(MessageToUser.class);
		ArgumentCaptor<String> contentArg = ArgumentCaptor.forClass(String.class);
		verify(mockNotificationManager).
			sendNotification(eq(userInfo), mtuArg.capture(), contentArg.capture());
		assertEquals(mtu, mtuArg.getValue());
		assertEquals(content, contentArg.getValue());		
	}

}
