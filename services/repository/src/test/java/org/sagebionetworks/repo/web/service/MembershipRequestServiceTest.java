package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.MembershipRequestManager;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.util.Pair;

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
		Pair<MessageToUser, String> result = new Pair<MessageToUser, String>(mtu, content);
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		when(mockMembershipRequestManager.create(userInfo, mrs)).thenReturn(mrs);
		when(mockMembershipRequestManager.createMembershipRequestNotification(mrs)).thenReturn(result);

		membershipRequestService.create(userId, mrs);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockMembershipRequestManager).create(userInfo, mrs);
		verify(mockMembershipRequestManager).createMembershipRequestNotification(mrs);
		
		ArgumentCaptor<MessageToUser> mtuArg = ArgumentCaptor.forClass(MessageToUser.class);
		ArgumentCaptor<String> contentArg = ArgumentCaptor.forClass(String.class);
		verify(mockNotificationManager).
			sendNotification(eq(userInfo), mtuArg.capture(), contentArg.capture());
		assertEquals(mtu, mtuArg.getValue());
		assertEquals(content, contentArg.getValue());		
	}

}
