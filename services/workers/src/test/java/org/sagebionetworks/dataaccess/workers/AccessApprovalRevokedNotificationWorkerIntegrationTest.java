package org.sagebionetworks.dataaccess.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBODataAccessNotification;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationDao;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.model.dbo.feature.FeatureStatusDao;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
@ActiveProfiles("test-dataaccess-worker")
public class AccessApprovalRevokedNotificationWorkerIntegrationTest {

	private static final long WORKER_TIMEOUT = 3 * 60 * 1000;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private FeatureStatusDao featureStatusDao;
	
	@Autowired
	private DataAccessNotificationDao notificationDao;
	
	@Autowired
	private MessageDAO messageDao;
	
	@Autowired
	private DataAccessTestHelper testHelper;
	
	private UserInfo adminUser;
	private UserInfo user;
	private List<String> messages;
	
	@BeforeEach
	public void before() {
		notificationDao.clear();
		featureStatusDao.clear();
		testHelper.cleanUp();
		
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		NewUser newUser = new NewUser();
		newUser.setEmail(UUID.randomUUID().toString() + "@test.com");
		newUser.setUserName(UUID.randomUUID().toString());
		user = userManager.getUserInfo(userManager.createUser(newUser));
		
		// Enabled the features for testing, we need both the auto revocation and the notification workers running
		featureStatusDao.setFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION, true);
		featureStatusDao.setFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS, true);
		messages = new ArrayList<>();
	}
	
	@AfterEach
	public void after() {
		notificationDao.clear();
		featureStatusDao.clear();
		testHelper.cleanUp();
		messages.forEach(id -> messageDao.deleteMessage(id));
		userManager.deletePrincipal(adminUser, user.getId());
	}
	
	@Test
	public void testRunWithAutoRevocation() throws Exception {
		
		AccessRequirement ar = testHelper.newAccessRequirement(user);
		// Will create an expired approval, one worker will expire it and another will act upon the change message to send the notification
		AccessApproval ap = testHelper.newApproval(ar, user, ApprovalState.APPROVED, Instant.now().minus(1, ChronoUnit.DAYS));
		
		TimeUtils.waitFor(WORKER_TIMEOUT, 1000L, () -> {
			Optional<DBODataAccessNotification> result = notificationDao.find(DataAccessNotificationType.REVOCATION, ap.getRequirementId(), user.getId());
			
			if (!result.isPresent()) { 
				return new Pair<>(false, null); 
			}
			
			// Verify that the message to user was created for the user (the workers are setup to act as prod so that
			// the message is created and processed: no email is actually delivered)
			Long messageId = result.get().getMessageId();

			MessageToUser message = messageDao.getMessage(messageId.toString());
		
			messages.add(message.getId());
			
			assertEquals(message.getCreatedBy(), BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId().toString());
			assertEquals(message.getRecipients(), Collections.singleton(user.getId().toString()));
			
			// Wait till the message is processed
			return new Pair<>(messageDao.getMessageSent(message.getId()), null);
		});
		
	}
}
