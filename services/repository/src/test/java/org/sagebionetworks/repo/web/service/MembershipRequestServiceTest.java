package org.sagebionetworks.repo.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.MembershipRequestManager;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;

@ExtendWith(MockitoExtension.class)
public class MembershipRequestServiceTest {
	@Mock
	private MembershipRequestManager mockMembershipRequestManager;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private NotificationManager mockNotificationManager;
	@InjectMocks
	private MembershipRequestServiceImpl membershipRequestService;
	
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

		// Call under test
		membershipRequestService.create(userId, mrs, acceptRequestEndpoint, notificationUnsubscribeEndpoint);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mockMembershipRequestManager).create(userInfo, mrs);
		verify(mockMembershipRequestManager).createMembershipRequestNotification(mrs,
				acceptRequestEndpoint, notificationUnsubscribeEndpoint);
		
		ArgumentCaptor<List> messageArg = ArgumentCaptor.forClass(List.class);
		
		verify(mockNotificationManager).sendNotifications(eq(userInfo), messageArg.capture());
		assertEquals(1, messageArg.getValue().size());		
		assertEquals(result, messageArg.getValue());		

	}

}
