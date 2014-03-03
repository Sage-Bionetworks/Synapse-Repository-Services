package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;

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
	private UserProfileDAO mockUserProfileDAO;
	private AuthorizationManager mockAuthorizationManager;
	private FileHandleManager mockFileHandleManager;
	private NodeDAO mockNodeDAO;
	private EntityPermissionsManager mockEntityPermissionsManager;
	private FileHandleDao mockFileHandleDao;

	@Autowired
	private AWSCredentials awsCredentials;
	private AmazonSimpleEmailServiceClient amazonSESClient;
	
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
	private UserProfile mockUserProfile;
	
	private UserProfile mockSenderUserProfile;

	@Before
	public void setUp() throws Exception {
		mockMessageDAO = mock(MessageDAO.class);
		mockUserGroupDAO  = mock(UserGroupDAO.class);
		mockGroupMembersDAO = mock(GroupMembersDAO.class);
		mockUserManager = mock(UserManager.class);
		mockUserProfileDAO = mock(UserProfileDAO.class);
		mockAuthorizationManager = mock(AuthorizationManager.class);
		mockFileHandleManager = mock(FileHandleManager.class);
		mockNodeDAO = mock(NodeDAO.class);
		mockEntityPermissionsManager = mock(EntityPermissionsManager.class);
		mockFileHandleDao = mock(FileHandleDao.class);
		
		// Use a working client
		amazonSESClient = new AmazonSimpleEmailServiceClient(awsCredentials);

		messageManager = new MessageManagerImpl(mockMessageDAO,
				mockUserGroupDAO, mockGroupMembersDAO, mockUserManager,
				mockUserProfileDAO, mockAuthorizationManager, amazonSESClient,
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
		mockFileHandleManager = mock(FileHandleManager.class);
		URL urlPT = MessageManagerImplSESTest.class.getClassLoader().getResource("images/notAnImage.txt");
		when(mockFileHandleManager.getRedirectURLForFileHandle(FILE_HANDLE_ID_PLAIN_TEXT)).thenReturn(urlPT);
		URL urlHTML = MessageManagerImplSESTest.class.getClassLoader().getResource("images/notAnImage.html");
		when(mockFileHandleManager.getRedirectURLForFileHandle(FILE_HANDLE_ID_HTML)).thenReturn(urlHTML);
		messageManager.setFileHandleManager(mockFileHandleManager);

		// Proceed past this check
		when(mockMessageDAO.getMessageSent(anyString())).thenReturn(false);
		
		// Mocks expandRecipientSet(...)
		mockUserGroup = new UserGroup();
		mockUserGroup.setIsIndividual(true);
		mockUserGroup.setId(mockRecipientIdString);
		when(mockUserGroupDAO.get(eq(mockRecipientId))).thenReturn(mockUserGroup);
		
		// Mocks the getting of settings
		mockUserProfile = new UserProfile();
		mockUserProfile.setNotificationSettings(new Settings());
		when(mockUserProfileDAO.get(eq(mockRecipientIdString))).thenReturn(mockUserProfile);
		
		mockSenderUserProfile = new UserProfile();
		mockSenderUserProfile.setUserName("foo");
		when(mockUserProfileDAO.get(eq(mockUserIdString))).thenReturn(mockSenderUserProfile);

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
	}
	
	/**
	 * Use this test to visually check if messages are properly transmitted
	 */
	@Ignore
	@Test
	public void testPlainTextToDeveloper() throws Exception {
		List<String> errors = messageManager.processMessage(MESSAGE_ID_PLAIN_TEXT);
		assertEquals(errors.toString(), 0, errors.size());
	}
	
	/**
	 * Use this test to visually check if HTML messages are properly transmitted
	 */
	@Ignore
	@Test
	public void testHTMLToDeveloper() throws Exception {
		List<String> errors = messageManager.processMessage(MESSAGE_ID_HTML);
		assertEquals(errors.toString(), 0, errors.size());
	}
	
	@Test
	public void testSuccess() throws Exception {
		mockUserProfile.setEmails(new LinkedList<String>());
		mockUserProfile.getEmails().add(SUCCESS_EMAIL);
		List<String> errors = messageManager.processMessage(MESSAGE_ID_PLAIN_TEXT);
		assertEquals(errors.toString(), 0, errors.size());
	}
	
	@Test
	public void testBounce() throws Exception {
		mockUserProfile.setEmails(new LinkedList<String>());
		mockUserProfile.getEmails().add(BOUNCE_EMAIL);
		List<String> errors = messageManager.processMessage(MESSAGE_ID_PLAIN_TEXT);
		assertEquals(errors.toString(), 0, errors.size());
	}
	
	@Test
	public void testOutOfOffice() throws Exception {
		mockUserProfile.setEmails(new LinkedList<String>());
		mockUserProfile.getEmails().add(OOTO_EMAIL);
		List<String> errors = messageManager.processMessage(MESSAGE_ID_PLAIN_TEXT);
		assertEquals(errors.toString(), 0, errors.size());
	}
	
	@Test
	public void testComplaint() throws Exception {
		mockUserProfile.setEmails(new LinkedList<String>());
		mockUserProfile.getEmails().add(COMPLAINT_EMAIL);
		List<String> errors = messageManager.processMessage(MESSAGE_ID_PLAIN_TEXT);
		assertEquals(errors.toString(), 0, errors.size());
	}
	
	@Test
	public void testSuppressionList() throws Exception {
		mockUserProfile.setEmails(new LinkedList<String>());
		mockUserProfile.getEmails().add(SUPPRESSION_EMAIL);
		List<String> errors = messageManager.processMessage(MESSAGE_ID_PLAIN_TEXT);
		assertEquals(errors.toString(), 0, errors.size());
	}
}
