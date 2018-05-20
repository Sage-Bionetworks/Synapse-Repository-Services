package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.auth.AWSCredentials;

/**
 * Checks how the message manager handles sending emails to Amazon SES
 * 
 * Mocks out all non SES-classes
 * 
 * Note: This test, or something similar, may be used to test the automation of bound/complaint message processing
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MessageManagerImplSESTest {
	
	private static final String SUCCESS_EMAIL = "success@simulator.amazonses.com";
	private static final String BOUNCE_EMAIL = "bounce@simulator.amazonses.com";
	private static final String OOTO_EMAIL = "ooto@simulator.amazonses.com";
	private static final String COMPLAINT_EMAIL = "complaint@simulator.amazonses.com";
	private static final String SUPPRESSION_EMAIL = "suppressionlist@simulator.amazonses.com";
	
	private static final String MESSAGE_ID_PLAIN_TEXT = "101";
	private static final String MESSAGE_ID_HTML = "202";

	private static final String FILE_HANDLE_ID_PLAIN_TEXT = "10101";
	private static final String FILE_HANDLE_ID_HTML = "20202";

	private MessageManager messageManager;

	private MessageDAO mockMessageDAO;
	private UserGroupDAO mockUserGroupDAO;
	private GroupMembersDAO mockGroupMembersDAO;
	private UserManager mockUserManager;
	private UserProfileManager mockUserProfileManager;
	private NotificationEmailDAO mockNotificationEmailDao;
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	private AuthorizationManager mockAuthorizationManager;
	private FileHandleManager mockFileHandleManager;
	private NodeDAO mockNodeDAO;
	private EntityPermissionsManager mockEntityPermissionsManager;
	private FileHandleDao mockFileHandleDao;
	private ProgressCallback mockProgressCallback;
	
	@Autowired
	private SynapseEmailService synapseEmailService;
	
	private MessageToUser mockMessageToUser;
	private UserInfo mockUserInfo;
	private UserGroup mockUserGroup;
	
	private final long mockUserId = -12345L;
	private final String mockUserIdString = "-12345";
	private final Long mockRecipientId = new Long(-67890);
	private final String mockRecipientIdString = mockRecipientId.toString();
	private Set<String> mockRecipients = new HashSet<String>() {
		private static final long serialVersionUID = 1L;
		{
			add(mockRecipientIdString);
		}
	};
	
	/**
	 * This is the one object that the tests will modify
	 */
	private PrincipalAlias mockRecipientPrincipalAlias;

	@Before
	public void setUp() throws Exception {
		mockMessageDAO = mock(MessageDAO.class);
		mockUserGroupDAO  = mock(UserGroupDAO.class);
		mockGroupMembersDAO = mock(GroupMembersDAO.class);
		mockUserManager = mock(UserManager.class);
		mockUserProfileManager = mock(UserProfileManager.class);
		mockNotificationEmailDao = mock(NotificationEmailDAO.class);
		mockPrincipalAliasDAO = mock(PrincipalAliasDAO.class);
		mockAuthorizationManager = mock(AuthorizationManager.class);
		mockFileHandleManager = mock(FileHandleManager.class);
		mockNodeDAO = mock(NodeDAO.class);
		mockEntityPermissionsManager = mock(EntityPermissionsManager.class);
		mockFileHandleDao = mock(FileHandleDao.class);
		
		messageManager = new MessageManagerImpl(mockMessageDAO,
				mockUserGroupDAO, mockGroupMembersDAO, mockUserManager,
				mockUserProfileManager, mockNotificationEmailDao, mockPrincipalAliasDAO, 
				mockAuthorizationManager, synapseEmailService,
				mockFileHandleManager, mockNodeDAO, mockEntityPermissionsManager,
				mockFileHandleDao);
		
		// The end goal of this mocking is to pass a single recipient through the authorization 
		// and individual-ization checks within the MessageManager's sendMessage method.
		// The tests can then freely change the email of that recipient to one of Amazon's mailbox simulator emails.
		
		// Mocks the first line of processMessage(String, boolean)
		mockMessageToUser = new MessageToUser();
		mockMessageToUser.setCreatedBy(mockUserIdString);
		mockMessageToUser.setRecipients(mockRecipients);
		mockMessageToUser.setFileHandleId(FILE_HANDLE_ID_PLAIN_TEXT);
		when(mockMessageDAO.getMessage(MESSAGE_ID_PLAIN_TEXT)).thenReturn(mockMessageToUser);
		
		MessageToUser mockHtmlMessageToUser = new MessageToUser();
		mockHtmlMessageToUser.setCreatedBy(mockUserIdString);
		mockHtmlMessageToUser.setRecipients(mockRecipients);
		mockHtmlMessageToUser.setFileHandleId(FILE_HANDLE_ID_HTML);
		when(mockMessageDAO.getMessage(MESSAGE_ID_HTML)).thenReturn(mockHtmlMessageToUser);
		
		// Mocks downloadEmailContent(...)
		String urlPT = MessageManagerImplSESTest.class.getClassLoader().getResource("images/notAnImage.txt").toExternalForm();
		when(mockFileHandleManager.getRedirectURLForFileHandle(FILE_HANDLE_ID_PLAIN_TEXT)).thenReturn(urlPT);
		String urlHTML = MessageManagerImplSESTest.class.getClassLoader().getResource("images/notAnImage.html").toExternalForm();
		when(mockFileHandleManager.getRedirectURLForFileHandle(FILE_HANDLE_ID_HTML)).thenReturn(urlHTML);
		when(mockFileHandleManager.downloadFileToString(anyString())).thenReturn("my dog has fleas");

		// Proceed past this check
		when(mockMessageDAO.getMessageSent(anyString())).thenReturn(false);
		
		// Mocks expandRecipientSet(...)
		mockUserGroup = new UserGroup();
		mockUserGroup.setIsIndividual(true);
		mockUserGroup.setId(mockRecipientIdString);
		when(mockUserGroupDAO.get(eq(mockRecipientId))).thenReturn(mockUserGroup);
		
		// Mocks the getting of settings
		UserProfile mockUserProfile = new UserProfile();
		mockUserProfile.setNotificationSettings(new Settings());
		when(mockUserProfileManager.getUserProfile(eq(mockRecipientIdString))).thenReturn(mockUserProfile);
		
		mockRecipientPrincipalAlias = new PrincipalAlias();
		mockRecipientPrincipalAlias.setType(AliasType.USER_EMAIL);

		UserProfile mockSenderUserProfile = new UserProfile();
		mockSenderUserProfile.setUserName("foo");
		when(mockUserProfileManager.getUserProfile(eq(mockUserIdString))).thenReturn(mockSenderUserProfile);

		PrincipalAlias senderPrincipalAlias = new PrincipalAlias();
		senderPrincipalAlias.setType(AliasType.USER_EMAIL);
		senderPrincipalAlias.setAlias("foo@bar.com");
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(mockUserId))
		.thenReturn(senderPrincipalAlias.getAlias());
		
		// Mocks the username supplied to SES
		mockUserInfo = new UserInfo(false, mockUserId);
		when(mockUserManager.getUserInfo(eq(mockUserId))).thenReturn(mockUserInfo);
		
		S3FileHandle plainTextFileHandle = new S3FileHandle();
		plainTextFileHandle.setId(FILE_HANDLE_ID_PLAIN_TEXT);
		plainTextFileHandle.setContentType("text/plain; charset=utf-8");
		when(mockFileHandleDao.get(FILE_HANDLE_ID_PLAIN_TEXT)).thenReturn(plainTextFileHandle);
		
		S3FileHandle htmlFileHandle = new S3FileHandle();
		htmlFileHandle.setId(FILE_HANDLE_ID_HTML);
		htmlFileHandle.setContentType("text/html; charset=utf-8");
		when(mockFileHandleDao.get(FILE_HANDLE_ID_HTML)).thenReturn(htmlFileHandle);
		
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
	}
	
	/**
	 * Use this test to visually check if messages are properly transmitted
	 */
	@Ignore
	@Test
	public void testPlainTextToDeveloper() throws Exception {
		List<String> errors = messageManager.processMessage(MESSAGE_ID_PLAIN_TEXT, mockProgressCallback);
		assertEquals(errors.toString(), 0, errors.size());
	}
	
	/**
	 * Use this test to visually check if HTML messages are properly transmitted
	 */
	@Ignore
	@Test
	public void testHTMLToDeveloper() throws Exception {
		List<String> errors = messageManager.processMessage(MESSAGE_ID_HTML, mockProgressCallback);
		assertEquals(errors.toString(), 0, errors.size());
	}
	
	@Test
	public void testSuccess() throws Exception {
		mockRecipientPrincipalAlias.setAlias(SUCCESS_EMAIL);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(mockRecipientId))
		.thenReturn(mockRecipientPrincipalAlias.getAlias());
		List<String> errors = messageManager.processMessage(MESSAGE_ID_PLAIN_TEXT, mockProgressCallback);
		assertEquals(errors.toString(), 0, errors.size());
	}
	
	@Test
	public void testBounce() throws Exception {
		mockRecipientPrincipalAlias.setAlias(BOUNCE_EMAIL);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(mockRecipientId))
		.thenReturn(mockRecipientPrincipalAlias.getAlias());
		List<String> errors = messageManager.processMessage(MESSAGE_ID_PLAIN_TEXT, mockProgressCallback);
		assertEquals(errors.toString(), 0, errors.size());
	}
	
	@Test
	public void testOutOfOffice() throws Exception {
		mockRecipientPrincipalAlias.setAlias(OOTO_EMAIL);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(mockRecipientId))
		.thenReturn(mockRecipientPrincipalAlias.getAlias());
		List<String> errors = messageManager.processMessage(MESSAGE_ID_PLAIN_TEXT, mockProgressCallback);
		assertEquals(errors.toString(), 0, errors.size());
	}
	
	@Test
	public void testComplaint() throws Exception {
		mockRecipientPrincipalAlias.setAlias(COMPLAINT_EMAIL);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(mockRecipientId))
		.thenReturn(mockRecipientPrincipalAlias.getAlias());
		List<String> errors = messageManager.processMessage(MESSAGE_ID_PLAIN_TEXT, mockProgressCallback);
		assertEquals(errors.toString(), 0, errors.size());
	}
	
	@Test
	public void testSuppressionList() throws Exception {
		mockRecipientPrincipalAlias.setAlias(SUPPRESSION_EMAIL);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(mockRecipientId))
		.thenReturn(mockRecipientPrincipalAlias.getAlias());
		List<String> errors = messageManager.processMessage(MESSAGE_ID_PLAIN_TEXT, mockProgressCallback);
		assertEquals(errors.toString(), 0, errors.size());
	}
}
