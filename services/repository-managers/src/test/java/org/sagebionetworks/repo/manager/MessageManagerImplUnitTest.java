package org.sagebionetworks.repo.manager;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.simpleemail.model.SendEmailRequest;


public class MessageManagerImplUnitTest {
	private MessageManagerImpl messageManager;
	private UserManager userManager;
	private MessageDAO messageDAO;
	private UserGroupDAO userGroupDAO;
	private GroupMembersDAO groupMembersDao;
	private UserProfileManager userProfileManager;
	private NotificationEmailDAO notificationEmailDao;
	private PrincipalAliasDAO principalAliasDAO;
	private AuthorizationManager authorizationManager;
	private FileHandleDao fileDAO;
	private NodeDAO nodeDAO;
	private EntityPermissionsManager entityPermissionsManager;
	private SynapseEmailService sesClient;
	
	private static String MESSAGE_ID = "101";
	private static Long CREATOR_ID = 999L;
	private static Long RECIPIENT_ID = 888L;
	private MessageToUser mtu = null;
	private UserInfo creatorUserInfo = null;
	
	@Before
	public void setUp() throws Exception {
		messageDAO = Mockito.mock(MessageDAO.class);
		userGroupDAO = Mockito.mock(UserGroupDAO.class);
		groupMembersDao = Mockito.mock(GroupMembersDAO.class);
		userManager = Mockito.mock(UserManager.class);
		userProfileManager = Mockito.mock(UserProfileManager.class);
		notificationEmailDao = Mockito.mock(NotificationEmailDAO.class);
		principalAliasDAO = Mockito.mock(PrincipalAliasDAO.class);
		authorizationManager = Mockito.mock(AuthorizationManager.class);
		fileDAO = Mockito.mock(FileHandleDao.class);
		nodeDAO = Mockito.mock(NodeDAO.class);
		entityPermissionsManager = Mockito.mock(EntityPermissionsManager.class);
		sesClient = Mockito.mock(SynapseEmailService.class);
		
		messageManager = new MessageManagerImpl();
		ReflectionTestUtils.setField(messageManager, "messageDAO", messageDAO);
		ReflectionTestUtils.setField(messageManager, "userGroupDAO", userGroupDAO);
		ReflectionTestUtils.setField(messageManager, "groupMembersDAO", groupMembersDao);
		ReflectionTestUtils.setField(messageManager, "userManager", userManager);
		ReflectionTestUtils.setField(messageManager, "userProfileManager", userProfileManager);
		ReflectionTestUtils.setField(messageManager, "notificationEmailDao", notificationEmailDao);
		ReflectionTestUtils.setField(messageManager, "principalAliasDAO", principalAliasDAO);
		ReflectionTestUtils.setField(messageManager, "authorizationManager", authorizationManager);
		ReflectionTestUtils.setField(messageManager, "fileHandleDao", fileDAO);
		ReflectionTestUtils.setField(messageManager, "nodeDAO", nodeDAO);
		ReflectionTestUtils.setField(messageManager, "entityPermissionsManager", entityPermissionsManager);
		ReflectionTestUtils.setField(messageManager, "sesClient", sesClient);
		
		mtu = new MessageToUser();
		mtu.setCreatedBy(CREATOR_ID.toString());
		mtu.setRecipients(Collections.singleton(RECIPIENT_ID.toString()));
		mtu.setSubject("subject");
		when(messageDAO.getMessage(MESSAGE_ID)).thenReturn(mtu);
		when(principalAliasDAO.getUserName(CREATOR_ID)).thenReturn("foo");
		when(principalAliasDAO.getUserName(RECIPIENT_ID)).thenReturn("bar");
		
		creatorUserInfo = new UserInfo(false);
		creatorUserInfo.setId(CREATOR_ID);
		when(userManager.getUserInfo(CREATOR_ID)).thenReturn(creatorUserInfo);
		
		when(notificationEmailDao.getNotificationEmailForPrincipal(CREATOR_ID)).thenReturn("foo@sagebase.org");
	}

	@Test
	public void testSendDeliveryFailureEmail() {
		List<String> errors = new ArrayList<String>();
		messageManager.sendDeliveryFailureEmail(MESSAGE_ID, errors);
		ArgumentCaptor<SendEmailRequest> argument = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(sesClient).sendEmail(argument.capture());
		SendEmailRequest ser = argument.getValue();
		assertEquals("noreply@synapse.org", ser.getSource());
		assertEquals(1, ser.getDestination().getToAddresses().size());
		assertEquals("foo@sagebase.org", ser.getDestination().getToAddresses().get(0));
		assertTrue(ser.getMessage().getBody().getText().getData().indexOf("The following errors were experienced while delivering message")>=0);
	}

}
