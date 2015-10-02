package org.sagebionetworks.repo.web.service;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.CloudMailInManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;

public class MessageServiceImplTest {
	
	private MessageServiceImpl service;
	private MessageManager messageManager;
	private UserManager userManager;
	private CloudMailInManager cloudMailInManager;
	private NotificationManager notificationManager;
	
	@Before
	public void setUp() throws Exception {
		messageManager = Mockito.mock(MessageManager.class);
		userManager = Mockito.mock(UserManager.class);
		cloudMailInManager = Mockito.mock(CloudMailInManager.class);
		notificationManager = Mockito.mock(NotificationManager.class);
		service = new MessageServiceImpl(messageManager, userManager, cloudMailInManager, notificationManager);
	}

	@Test
	public void testCreateCloudMailInMessage() {
		Message message = new Message();
		String notificationUnsubscribeEndpoint = "https://www.synapse.org/#foo:";
		MessageToUserAndBody mtub = new MessageToUserAndBody();
		MessageToUser mtu = new MessageToUser();
		Long creator = 101L;
		mtu.setCreatedBy(creator.toString());
		mtub.setMetadata(mtu);
		when(cloudMailInManager.convertMessage(message, notificationUnsubscribeEndpoint)).thenReturn(Collections.singletonList(mtub));
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(creator);
		when(userManager.getUserInfo(creator)).thenReturn(userInfo);
		service.create(message, notificationUnsubscribeEndpoint);
		verify(notificationManager).sendNotifications(eq(userInfo), eq(Collections.singletonList(mtub)));
	}

}
