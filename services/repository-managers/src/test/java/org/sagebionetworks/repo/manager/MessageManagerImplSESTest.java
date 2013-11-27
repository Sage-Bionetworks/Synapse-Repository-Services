package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
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
 * Note: This test requires us to verify the sagebase.org domain with Amazon
 * Once complete, the test can be fleshed out for each of the 5 cases
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MessageManagerImplSESTest {
	
	private static final String SUCCESS_EMAIL = "success@simulator.amazonses.com";
	private static final String BOUNCE_EMAIL = "bounce@simulator.amazonses.com";
	private static final String OOTO_EMAIL = "ooto@simulator.amazonses.com";
	private static final String COMPLAINT_EMAIL = "complaint@simulator.amazonses.com";
	private static final String SUPPRESSION_EMAIL = "suppressionlist@simulator.amazonses.com";

	private MessageManager messageManager;

	private MessageDAO mockMessageDAO;
	private UserGroupDAO mockUserGroupDAO;
	private GroupMembersDAO mockGroupMembersDAO;
	private UserManager mockUserManager;
	private UserProfileDAO mockUserProfileDAO;
	private AuthorizationManager mockAuthorizationManager;
	private FileHandleManager mockFileHandleManager;

	@Autowired
	private AWSCredentials awsCredentials;
	private AmazonSimpleEmailServiceClient amazonSESClient;
	
	private MessageToUser mockMessageToUser;
	private UserInfo mockUserInfo;
	private UserGroup mockUserGroup;
	
	private final long mockUserId = -12345L;
	private final String mockUserIdString = "-12345";
	private final String mockRecipientId = "-67890";
	private Set<String> mockRecipients = new HashSet<String>() {
		private static final long serialVersionUID = 1L;
		{
			add(mockRecipientId);
		}
	};
	
	/**
	 * This is the one object that the tests will modify
	 */
	private UserProfile mockUserProfile;

	@Before
	public void setUp() throws Exception {
		mockMessageDAO = mock(MessageDAO.class);
		mockUserGroupDAO  = mock(UserGroupDAO.class);
		mockGroupMembersDAO = mock(GroupMembersDAO.class);
		mockUserManager = mock(UserManager.class);
		mockUserProfileDAO = mock(UserProfileDAO.class);
		mockAuthorizationManager = mock(AuthorizationManager.class);
		mockFileHandleManager = mock(FileHandleManager.class);
		
		// Use a working client
		amazonSESClient = new AmazonSimpleEmailServiceClient(awsCredentials);

		messageManager = new MessageManagerImpl(mockMessageDAO,
				mockUserGroupDAO, mockGroupMembersDAO, mockUserManager,
				mockUserProfileDAO, mockAuthorizationManager, amazonSESClient,
				mockFileHandleManager);
		
		// The end goal of this mocking is to pass a single recipient through the authorization 
		// and individual-ization checks within the MessageManager's sendMessage method.
		// The tests can then freely change the email of that recipient to one of Amazon's mailbox simulator emails.
		
		mockMessageToUser = new MessageToUser();
		mockUserInfo = new UserInfo(false);
		mockUserInfo.setIndividualGroup(new UserGroup());
		mockUserInfo.getIndividualGroup().setName("test@sagebase.org");
		mockMessageToUser.setCreatedBy(mockUserIdString);
		mockMessageToUser.setRecipients(mockRecipients);
		when(mockMessageDAO.getMessage(anyString())).thenReturn(mockMessageToUser);
		when(mockUserManager.getUserInfo(eq(mockUserId))).thenReturn(mockUserInfo);
		when(mockUserGroupDAO.findGroup(anyString(), eq(false))).thenReturn(new UserGroup());
		
		when(mockMessageDAO.hasMessageBeenSent(anyString())).thenReturn(false);
		
		mockUserGroup = new UserGroup();
		mockUserGroup.setIsIndividual(true);
		mockUserGroup.setId(mockRecipientId);
		when(mockUserGroupDAO.get(eq(mockRecipientId))).thenReturn(mockUserGroup);
		
		mockUserProfile = new UserProfile();
		mockUserProfile.setNotificationSettings(new Settings());
		when(mockUserProfileDAO.get(eq(mockRecipientId))).thenReturn(mockUserProfile);
		
		// Copied from the mocking that is done in the MessageManagerImplTest
		mockFileHandleManager = mock(FileHandleManager.class);
		URL url = MessageManagerImplSESTest.class.getClassLoader().getResource("images/notAnImage.txt");
		when(mockFileHandleManager.getRedirectURLForFileHandle(anyString())).thenReturn(url);
		messageManager.setFileHandleManager(mockFileHandleManager);
	}
	
	@Test
	public void testSuccess() throws Exception {
		mockUserGroup.setName(SUCCESS_EMAIL);
		List<String> errors = messageManager.processMessage("Blarg!");
		assertEquals(0, errors.size());
	}
	
	@Test
	public void testBounce() throws Exception {
		mockUserGroup.setName(BOUNCE_EMAIL);
		List<String> errors = messageManager.processMessage("Arrrr!");
		assertEquals(0, errors.size());
	}
	
	@Test
	public void testOutOfOffice() throws Exception {
		mockUserGroup.setName(OOTO_EMAIL);
		List<String> errors = messageManager.processMessage("Meh?!?");
		assertEquals(0, errors.size());
	}
	
	@Test
	public void testComplaint() throws Exception {
		mockUserGroup.setName(COMPLAINT_EMAIL);
		List<String> errors = messageManager.processMessage("Grrrr!");
		assertEquals(0, errors.size());
	}
	
	@Test
	public void testSuppressionList() throws Exception {
		mockUserGroup.setName(SUPPRESSION_EMAIL);
		List<String> errors = messageManager.processMessage("Oooof!");
		assertEquals(0, errors.size());
	}
}
