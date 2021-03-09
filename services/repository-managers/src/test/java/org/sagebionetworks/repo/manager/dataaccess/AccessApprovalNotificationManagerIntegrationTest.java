package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotification;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationResponse;
import org.sagebionetworks.repo.model.dataaccess.NotificationType;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBODataAccessNotification;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationDao;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.model.dbo.feature.FeatureStatusDao;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AccessApprovalNotificationManagerIntegrationTest {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private FeatureStatusDao featureStatusDao;
	
	@Autowired
	private AccessApprovalDAO accessApprovalDao;
	
	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	
	@Autowired
	private DataAccessNotificationDao notificationDao;
	
	@Autowired
	private AccessApprovalNotificationManager manager;
	
	private UserInfo adminUser;
	private UserInfo submitter;
	private UserInfo accessor;
	
	private List<Long> users;
	
	@BeforeEach
	public void before() {
		users = new ArrayList<>();
		notificationDao.truncateAll();
		accessApprovalDao.clear();
		accessRequirementDao.clear();
		featureStatusDao.clear();
		featureStatusDao.setFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS, true);
		
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		submitter = createUser("submitter");
		accessor = createUser("accessor");
	
		users.addAll(Arrays.asList(submitter.getId(), accessor.getId()));
	}
	
	@AfterEach
	public void after() {
		notificationDao.truncateAll();
		accessApprovalDao.clear();
		accessRequirementDao.clear();
		featureStatusDao.clear();
		
		users.forEach( id -> {
			userManager.deletePrincipal(adminUser, id);
		});
	}
	
	@Test
	public void processAccessApprovalChangeWithNonExistingAccessApproval() throws RecoverableMessageException {
		ChangeMessage message = changeMessage(-1L);
		
		assertThrows(NotFoundException.class, () -> {			
			manager.processAccessApprovalChange(message);
		});
	}
	
	@Test
	public void processAccessApprovalChange() throws RecoverableMessageException {
		AccessRequirement requirement = createManagedAR();
		
		AccessApproval approval = createApproval(requirement, submitter, submitter, ApprovalState.REVOKED, null);
		
		ChangeMessage message = changeMessage(approval.getId());
		
		// Call under test
		manager.processAccessApprovalChange(message);
		
		Optional<DBODataAccessNotification> result = notificationDao.findForUpdate(DataAccessNotificationType.REVOCATION, requirement.getId(), submitter.getId());
		
		assertTrue(result.isPresent());
	}
	
	@Test
	public void processAccessApprovalChangeReprocess() throws RecoverableMessageException {
		AccessRequirement requirement = createManagedAR();
		
		AccessApproval approval = createApproval(requirement, submitter, submitter, ApprovalState.REVOKED, null);
		
		ChangeMessage message = changeMessage(approval.getId());
		
		// Call under test
		manager.processAccessApprovalChange(message);
		
		Optional<DBODataAccessNotification> result = notificationDao.findForUpdate(DataAccessNotificationType.REVOCATION, requirement.getId(), submitter.getId());
		
		assertTrue(result.isPresent());
	
		DBODataAccessNotification notification = result.get();
		
		// Duplicate message
		manager.processAccessApprovalChange(message);
		
		result = notificationDao.findForUpdate(DataAccessNotificationType.REVOCATION, requirement.getId(), submitter.getId());

		// Make sure the notification didn't change
		assertEquals(notification, result.get());
	}
	
	@Test
	public void processAccessApprovalChangeWithDifferentAccessors() throws RecoverableMessageException {
		AccessRequirement requirement = createManagedAR();
		
		AccessApproval ap1 = createApproval(requirement, submitter, submitter, ApprovalState.REVOKED, null);
		AccessApproval ap2 = createApproval(requirement, submitter, accessor, ApprovalState.REVOKED, null);
		
		ChangeMessage message1 = changeMessage(ap1.getId());
		ChangeMessage message2 = changeMessage(ap2.getId());
		
		// Call under test
		manager.processAccessApprovalChange(message1);
		manager.processAccessApprovalChange(message2);
		
		Optional<DBODataAccessNotification> result1 = notificationDao.findForUpdate(DataAccessNotificationType.REVOCATION, requirement.getId(), submitter.getId());
		
		assertTrue(result1.isPresent());
		
		Optional<DBODataAccessNotification> result2 = notificationDao.findForUpdate(DataAccessNotificationType.REVOCATION, requirement.getId(), accessor.getId());
		
		assertTrue(result2.isPresent());
	
	}
	
	@Test
	public void processAccessApprovalChangeWithMultipleRevocations() throws RecoverableMessageException {
		AccessRequirement requirement = createManagedAR();
		
		// Simulates two revoked approvals for the same accessor but different submitters
		AccessApproval ap1 = createApproval(requirement, submitter, accessor, ApprovalState.REVOKED, null);
		AccessApproval ap2 = createApproval(requirement, accessor, accessor, ApprovalState.REVOKED, null);
		
		ChangeMessage message1 = changeMessage(ap1.getId());
		ChangeMessage message2 = changeMessage(ap2.getId());
		
		// Call under test
		manager.processAccessApprovalChange(message1);
		manager.processAccessApprovalChange(message2);
		
		Optional<DBODataAccessNotification> result = notificationDao.findForUpdate(DataAccessNotificationType.REVOCATION, requirement.getId(), accessor.getId());
		
		assertTrue(result.isPresent());
		
		// The notification is sent for the first processed approval only 
		assertEquals(ap1.getId(), result.get().getAccessApprovalId());
		
	}
	
	@Test
	public void testProcessAccessApprovalReminder() throws RecoverableMessageException {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		
		Instant expireOn = LocalDate.now(ZoneOffset.UTC)
				.plus(notificationType.getReminderPeriod())
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		
		AccessRequirement requirement = createManagedAR();
		AccessApproval approval = createApproval(requirement, submitter, submitter, ApprovalState.APPROVED, expireOn);
		
		// Call under test
		manager.processAccessApproval(notificationType, approval.getId());
		
		Optional<DBODataAccessNotification> result = notificationDao.findForUpdate(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, requirement.getId(), submitter.getId());
	
		assertTrue(result.isPresent());
	}
	
	@Test
	public void testProcessAccessApprovalReminderReprocess() throws RecoverableMessageException {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		
		Instant expireOn = LocalDate.now(ZoneOffset.UTC)
				.plus(notificationType.getReminderPeriod())
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		
		AccessRequirement requirement = createManagedAR();
		AccessApproval approval = createApproval(requirement, submitter, submitter, ApprovalState.APPROVED, expireOn);
		
		manager.processAccessApproval(notificationType, approval.getId());
		
		Optional<DBODataAccessNotification> result = notificationDao.findForUpdate(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, requirement.getId(), submitter.getId());
	
		assertTrue(result.isPresent());
		
		DBODataAccessNotification expectedNotification = result.get();
		
		// Call under test
		manager.processAccessApproval(notificationType, approval.getId());
		
		// Check that the notification wasn't updated
		result = notificationDao.findForUpdate(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, requirement.getId(), submitter.getId());
		
		assertEquals(expectedNotification, result.get());
	}
	
	@Test
	public void testProcessAccessApprovalReminderDifferentRequirements() throws RecoverableMessageException {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		
		Instant expireOn = LocalDate.now(ZoneOffset.UTC)
				.plus(notificationType.getReminderPeriod())
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		
		AccessRequirement requirement = createManagedAR();
		AccessRequirement requirement2 = createManagedAR();
		
		AccessApproval ap1 = createApproval(requirement, submitter, submitter, ApprovalState.APPROVED, expireOn);
		AccessApproval ap2 = createApproval(requirement2, submitter, submitter, ApprovalState.APPROVED, expireOn);
		
		// Call under test
		manager.processAccessApproval(notificationType, ap1.getId());
		manager.processAccessApproval(notificationType, ap2.getId());
		
		Optional<DBODataAccessNotification> result = notificationDao.findForUpdate(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, requirement.getId(), submitter.getId());
	
		assertTrue(result.isPresent());
		
		result = notificationDao.findForUpdate(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, requirement2.getId(), submitter.getId());
		
		assertTrue(result.isPresent());
	}
	
	@Test
	public void testProcessAccessApprovalReminderWithFutureApproval() throws RecoverableMessageException {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		
		Instant expireOn = LocalDate.now(ZoneOffset.UTC)
				.plus(notificationType.getReminderPeriod())
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		
		AccessRequirement requirement = createManagedAR();
		
		AccessApproval ap1 = createApproval(requirement, submitter, submitter, ApprovalState.APPROVED, expireOn);
		
		// Simulates a new requirement version
		requirement.setVersionNumber(requirement.getVersionNumber() + 1);
		
		// Expires in the future (but the same day)
		Instant futureExpireOn = expireOn.plus(12, ChronoUnit.HOURS);
		
		AccessApproval ap2 = createApproval(requirement, submitter, submitter, ApprovalState.APPROVED, futureExpireOn);
		
		// Call under test
		manager.processAccessApproval(notificationType, ap1.getId());
		
		Optional<DBODataAccessNotification> result = notificationDao.findForUpdate(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, requirement.getId(), submitter.getId());
		
		assertFalse(result.isPresent());
		
		// We now process also the second one
		manager.processAccessApproval(notificationType, ap2.getId());
		
		result = notificationDao.findForUpdate(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, requirement.getId(), submitter.getId());
	
		assertTrue(result.isPresent());
		
		DBODataAccessNotification expectedNotification = result.get();
		
		// If we re-process out of order the first one a new notification should not be sent
		manager.processAccessApproval(notificationType, ap2.getId());
		
		result = notificationDao.findForUpdate(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, requirement.getId(), submitter.getId());
		
		assertTrue(result.isPresent());
		
		assertEquals(expectedNotification, result.get());
		
	}
	
	@Test
	public void testProcessAccessApprovalReminderWithDifferentReminders() throws RecoverableMessageException {
		
		Instant expireOn = LocalDate.now(ZoneOffset.UTC)
				.plus(DataAccessNotificationType.FIRST_RENEWAL_REMINDER.getReminderPeriod())
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		
		AccessRequirement requirement = createManagedAR();
		
		AccessApproval ap1 = createApproval(requirement, submitter, submitter, ApprovalState.APPROVED, expireOn);
		
		// Call under test
		manager.processAccessApproval(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, ap1.getId());		
		manager.processAccessApproval(DataAccessNotificationType.SECOND_RENEWAL_REMINDER, ap1.getId());
		
		Optional<DBODataAccessNotification> result = notificationDao.findForUpdate(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, requirement.getId(),
				submitter.getId());

		assertTrue(result.isPresent());
		
		assertFalse(notificationDao.findForUpdate(DataAccessNotificationType.SECOND_RENEWAL_REMINDER, requirement.getId(),
				submitter.getId()).isPresent());
		
		DBODataAccessNotification expected = result.get();
		
		// Emulates passing of time
		
		expireOn = LocalDate.now(ZoneOffset.UTC)
				.plus(DataAccessNotificationType.SECOND_RENEWAL_REMINDER.getReminderPeriod())
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		
		ap1.setExpiredOn(Date.from(expireOn));
		
		accessApprovalDao.createOrUpdateBatch(Arrays.asList(ap1));
		
		// Re-process both
		manager.processAccessApproval(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, ap1.getId());
		manager.processAccessApproval(DataAccessNotificationType.SECOND_RENEWAL_REMINDER, ap1.getId());
		
		// The first should not be re-processed at this time
		assertEquals(expected, notificationDao.findForUpdate(DataAccessNotificationType.FIRST_RENEWAL_REMINDER, requirement.getId(),
				submitter.getId()).get());
		
		assertTrue(notificationDao.findForUpdate(DataAccessNotificationType.SECOND_RENEWAL_REMINDER, requirement.getId(),
				submitter.getId()).isPresent());

	}
	
	@Test
	public void testListNotificationRequest() throws RecoverableMessageException {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		
		Instant expireOn = LocalDate.now(ZoneOffset.UTC)
				.plus(notificationType.getReminderPeriod())
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		
		AccessRequirement requirement = createManagedAR();
		
		AccessApproval ap1 = createApproval(requirement, submitter, submitter, ApprovalState.APPROVED, expireOn);
		
		// Process the approval so that the notification is created
		manager.processAccessApproval(notificationType, ap1.getId());
		
		
		AccessApprovalNotificationRequest request = new AccessApprovalNotificationRequest();
		
		request.setRequirementId(requirement.getId());
		request.setRecipientIds(Arrays.asList(submitter.getId()));
		
		AccessApprovalNotification expected = new AccessApprovalNotification();
		
		expected.setNotificationType(NotificationType.valueOf(notificationType.name()));
		expected.setRequirementId(requirement.getId());
		expected.setRecipientId(submitter.getId());
		
		// Call under test
		AccessApprovalNotificationResponse response = manager.listNotificationsRequest(adminUser, request);
		
		assertEquals(1, response.getResults().size());
		
		AccessApprovalNotification result = response.getResults().iterator().next();
		
		expected.setSentOn(result.getSentOn());
		
		assertEquals(expected, result);
		
	}
	
	private ChangeMessage changeMessage(Long id) {
		ChangeMessage message = new ChangeMessage();
		
		message.setChangeNumber(12345L);
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectType(ObjectType.ACCESS_APPROVAL);
		message.setTimestamp(new Date());
		message.setObjectId(id.toString());
		
		return message;
	}
	
	private UserInfo createUser(String prefix) {
		NewUser newUser = new NewUser();
		newUser.setEmail(UUID.randomUUID().toString() + "@test.com");
		newUser.setUserName(prefix + "_" + UUID.randomUUID().toString());
		return userManager.getUserInfo(userManager.createUser(newUser));
	}
	
	private AccessRequirement createManagedAR() {
		AccessRequirement accessRequirement = new ManagedACTAccessRequirement();
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setCreatedBy(adminUser.getId().toString());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(adminUser.getId().toString());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setConcreteType(ManagedACTAccessRequirement.class.getName());
		
		return accessRequirementDao.create(accessRequirement);
	}
	
	private AccessApproval createApproval(AccessRequirement accessRequirement, UserInfo submitter, UserInfo accessor, ApprovalState state, Instant expiresOn) {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setCreatedBy(adminUser.getId().toString());
		accessApproval.setCreatedOn(new Date());
		accessApproval.setModifiedBy(adminUser.getId().toString());
		accessApproval.setModifiedOn(new Date());
		accessApproval.setAccessorId(accessor.getId().toString());
		accessApproval.setRequirementId(accessRequirement.getId());
		accessApproval.setRequirementVersion(accessRequirement.getVersionNumber());
		accessApproval.setSubmitterId(submitter.getId().toString());
		accessApproval.setExpiredOn(expiresOn == null ? null : Date.from(expiresOn));
		accessApproval.setState(state);
		
		return accessApprovalDao.create(accessApproval);
	}
}
