package org.sagebionetworks.dataaccess.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalNotificationManager;
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
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class AccessApprovalReminderNotificationWorkerIntegrationTest {
	
	private static final long WORKER_TIMEOUT = 3 * 60 * 1000;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private FeatureStatusDao featureStatusDao;
	
	@Autowired
	private DataAccessNotificationDao notificationDao;

	@Autowired
	private DataAccessTestHelper testHelper;
	
	@Autowired
	private MessageDAO messageDao;
	
	private UserInfo admin;
	private UserInfo user;
	
	private List<String> messages;
	
	@BeforeEach
	public void before() {
		messages = new ArrayList<>();
		
		notificationDao.truncateAll();
		testHelper.cleanUp();
		featureStatusDao.clear();
		
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		NewUser newUser = new NewUser();
		newUser.setEmail(UUID.randomUUID().toString() + "@test.com");
		newUser.setUserName(UUID.randomUUID().toString());
		user = userManager.getUserInfo(userManager.createUser(newUser));
		
		// Enabled the feature for testing
		featureStatusDao.setFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS, true);
	}
	
	@AfterEach
	public void after() {

		notificationDao.truncateAll();
		testHelper.cleanUp();
		featureStatusDao.clear();
		messages.forEach(messageDao::deleteMessage);
		userManager.deletePrincipal(admin, user.getId());
	}
	
	@Test
	public void testProcessReminders() throws Exception {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessRequirement ar = testHelper.newAccessRequirement(admin);
		
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		
		Instant expiresOn = today.plus(notificationType.getReminderPeriod())
				.atStartOfDay()
				// Give some time within the day to avoid passing the processing period
				.plus(1, ChronoUnit.HOURS)
				.toInstant(ZoneOffset.UTC);
		
		// Creates an approval that expires exactly the reminder period days from today
		AccessApproval approval = testHelper.newApproval(ar, user, ApprovalState.APPROVED, expiresOn);
		
		TimeUtils.waitFor(WORKER_TIMEOUT, 1000L, () -> {
			Optional<DBODataAccessNotification> result = notificationDao.find(notificationType, ar.getId(), user.getId());
			
			result.ifPresent(notification -> {
				assertEquals(ar.getId(), notification.getRequirementId());
				assertEquals(approval.getId(), notification.getAccessApprovalId());
				assertEquals(user.getId(), notification.getRecipientId());
				// Makes sure that it didn't actually send a message
				assertEquals(AccessApprovalNotificationManager.NO_MESSAGE_TO_USER, notification.getMessageId());
			});
			
			return new Pair<>(result.isPresent(), null);
		});
	}
	
}
