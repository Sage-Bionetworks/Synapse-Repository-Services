package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
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
import org.sagebionetworks.repo.manager.team.MembershipRequestManager;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;

public class MembershipRequestServiceTest {
	private MembershipRequestServiceImpl membershipRequestService;
	private MembershipRequestManager mockMembershipRequestManager;
	private UserManager mockUserManager;
	private NotificationManager mockNotificationManager;
	
	@Before
	public void before() throws Exception {
		mockMembershipRequestManager = Mockito.mock(MembershipRequestManager.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockNotificationManager = Mockito.mock(NotificationManager.class);

		this.membershipRequestService = new MembershipRequestServiceImpl(
				mockMembershipRequestManager,
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
		String acceptRequestEndpoint = "acceptRequestEndpoint:";
		String notificationUnsubscribeEndpoint = "notificationUnsubscribeEndpoint:";
		List<MessageToUserAndBody> result = Collections.singletonList(new MessageToUserAndBody(mtu, content, "text/plain"));
		MembershipRequest mrs = new MembershipRequest();
		when(mockMembershipRequestManager.create(userInfo, mrs)).thenReturn(mrs);
		when(mockMembershipRequestManager.createMembershipRequestNotification(mrs,
				acceptRequestEndpoint, notificationUnsubscribeEndpoint)).thenReturn(result);

		membershipRequestService.create(userId, mrs, acceptRequestEndpoint, notificationUnsubscribeEndpoint);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockMembershipRequestManager).create(userInfo, mrs);
		verify(mockMembershipRequestManager).createMembershipRequestNotification(mrs,
				acceptRequestEndpoint, notificationUnsubscribeEndpoint);
		
		ArgumentCaptor<List> messageArg = ArgumentCaptor.forClass(List.class);
		verify(mockNotificationManager).
			sendNotifications(eq(userInfo), messageArg.capture());
		assertEquals(1, messageArg.getValue().size());		
		assertEquals(result, messageArg.getValue());		

	}

}
