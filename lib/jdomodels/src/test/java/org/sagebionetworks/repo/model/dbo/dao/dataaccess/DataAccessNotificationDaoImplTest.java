package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:jdomodels-test-context.xml")
public class DataAccessNotificationDaoImplTest {

	@Autowired
	private DataAccessNotificationDao notificationDao;

	@Autowired
	private AccessRequirementDAO accessRequirementDao;

	@Autowired
	private AccessApprovalDAO accessApprovalDao;

	@Autowired
	private UserGroupDAO userGroupDao;

	private UserGroup user;
	private UserGroup user2;

	@BeforeEach
	public void before() {
		user = new UserGroup();
		user.setIsIndividual(true);
		user.setCreationDate(new Date());
		user.setId(userGroupDao.create(user).toString());
		
		user2 = new UserGroup();
		user2.setIsIndividual(true);
		user2.setCreationDate(new Date());
		user2.setId(userGroupDao.create(user2).toString());

		notificationDao.clear();
		accessApprovalDao.clear();
		accessRequirementDao.clear();
	}

	@AfterEach
	public void after() {
		notificationDao.clear();
		accessApprovalDao.clear();
		accessRequirementDao.clear();
		userGroupDao.delete(user.getId());
		userGroupDao.delete(user2.getId());
	}

	@Test
	public void testCreateWithId() {
		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;

		AccessRequirement accessRequirement = createManagedAR();
		AccessApproval approval = createApproval(accessRequirement);

		DBODataAccessNotification notification = newNotification(type, accessRequirement, approval, -1L);

		notification.setId(1L);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			notificationDao.create(notification);
		}).getMessage();

		assertEquals("The id must be unassigned.", message);
	}

	@Test
	public void testCreateWithNoNotification() {

		DBODataAccessNotification notification = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			notificationDao.create(notification);
		}).getMessage();

		assertEquals("The notification is required.", message);
	}

	@Test
	public void testCreate() {
		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;

		AccessRequirement accessRequirement = createManagedAR();
		AccessApproval approval = createApproval(accessRequirement);

		DBODataAccessNotification notification = newNotification(type, accessRequirement, approval, -1L);

		// Call under test
		DBODataAccessNotification result = notificationDao.create(notification);

		notification.setId(result.getId());
		notification.setEtag(result.getEtag());

		assertEquals(notification, result);
	}

	@Test
	public void testCreateWithExisting() {
		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;

		AccessRequirement accessRequirement = createManagedAR();
		AccessApproval approval = createApproval(accessRequirement);

		DBODataAccessNotification notification = newNotification(type, accessRequirement, approval, -1L);

		DBODataAccessNotification result = notificationDao.create(notification);

		assertNotNull(result);

		notification.setId(null);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			notificationDao.create(notification);
		});

		assertTrue(ex.getCause() instanceof DuplicateKeyException);
	}

	@Test
	public void testUpdate() {
		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;

		AccessRequirement accessRequirement = createManagedAR();
		AccessApproval approval = createApproval(accessRequirement);

		DBODataAccessNotification notification = newNotification(type, accessRequirement, approval, -1L);

		DBODataAccessNotification result = notificationDao.create(notification);

		assertNotNull(result);

		String etag = result.getEtag();

		notification.setMessageId(10L);

		// Call under test
		notificationDao.update(result.getId(), notification);

		assertNotEquals(etag, notification.getEtag());
		assertEquals(10L, notification.getMessageId());
	}

	@Test
	public void testUpdateWithNoId() {
		DataAccessNotificationType type = null;

		AccessRequirement accessRequirement = createManagedAR();
		AccessApproval approval = createApproval(accessRequirement);

		Long id = null;
		DBODataAccessNotification notification = newNotification(type, accessRequirement, approval, -1L);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			notificationDao.update(id, notification);
		}).getMessage();

		assertEquals("The id is required.", message);
	}

	@Test
	public void testUpdateWithNoNotification() {
		Long id = 1L;
		DBODataAccessNotification notification = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			notificationDao.update(id, notification);
		}).getMessage();

		assertEquals("The notification is required.", message);
	}

	@Test
	public void testFindForUpdate() {
		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;

		AccessRequirement accessRequirement = createManagedAR();
		AccessApproval approval = createApproval(accessRequirement);

		DBODataAccessNotification notification = newNotification(type, accessRequirement, approval, -1L);
		DBODataAccessNotification anotherNotification = newNotification(DataAccessNotificationType.FIRST_RENEWAL_REMINDER,
				accessRequirement, approval, -1L);

		// Call under test
		Optional<DBODataAccessNotification> result = notificationDao.findForUpdate(type, accessRequirement.getId(),
				notification.getRecipientId());

		assertFalse(result.isPresent());

		notificationDao.create(notification);
		notificationDao.create(anotherNotification);

		// Call under test
		result = notificationDao.findForUpdate(type, accessRequirement.getId(), notification.getRecipientId());

		assertTrue(result.isPresent());

		assertEquals(notification, result.get());
	}

	@Test
	public void testValidateForStorageWithNoId() {

		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;
		
		Long id = null;
		Long requirement = 1L;
		Long recipient = 2L;
		Long approval = 3L;
		Long message = 4L;

		DBODataAccessNotification notification = newNotification(type, requirement, recipient, approval, message);
		
		notification.setId(id);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			DataAccessNotificationDaoImpl.validateForStorage(notification);
		}).getMessage();
		
		assertEquals("The id is required.", errorMessage);
	}
	
	@Test
	public void testValidateForStorageWithNoRequirement() {

		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;
		
		Long id = 1L;
		Long requirement = null;
		Long recipient = 2L;
		Long approval = 3L;
		Long message = 4L;

		DBODataAccessNotification notification = newNotification(type, requirement, recipient, approval, message);
		
		notification.setId(id);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			DataAccessNotificationDaoImpl.validateForStorage(notification);
		}).getMessage();
		
		assertEquals("The requirement id is required.", errorMessage);
	}
	
	@Test
	public void testValidateForStorageWithNoRecipient() {

		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;
		
		Long id = 1L;
		Long requirement = 2L;
		Long recipient = null;
		Long approval = 3L;
		Long message = 4L;

		DBODataAccessNotification notification = newNotification(type, requirement, recipient, approval, message);
		
		notification.setId(id);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			DataAccessNotificationDaoImpl.validateForStorage(notification);
		}).getMessage();
		
		assertEquals("The recipient id is required.", errorMessage);
	}
	
	@Test
	public void testValidateForStorageWithNoApproval() {

		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;
		
		Long id = 1L;
		Long requirement = 2L;
		Long recipient = 2L;
		Long approval = null;
		Long message = 4L;

		DBODataAccessNotification notification = newNotification(type, requirement, recipient, approval, message);
		
		notification.setId(id);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			DataAccessNotificationDaoImpl.validateForStorage(notification);
		}).getMessage();
		
		assertEquals("The approval id is required.", errorMessage);
	}
	
	@Test
	public void testValidateForStorageWithNoMessage() {

		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;
		
		Long id = 1L;
		Long requirement = 2L;
		Long recipient = 2L;
		Long approval = 3L;
		Long message = null;

		DBODataAccessNotification notification = newNotification(type, requirement, recipient, approval, message);
		
		notification.setId(id);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			DataAccessNotificationDaoImpl.validateForStorage(notification);
		}).getMessage();
		
		assertEquals("The message id is required.", errorMessage);
	}
	
	@Test
	public void testValidateForStorageWithNoType() {

		DataAccessNotificationType type = null;
		
		Long id = 1L;
		Long requirement = 2L;
		Long recipient = 2L;
		Long approval = 3L;
		Long message = 4L;

		DBODataAccessNotification notification = newNotification(type, requirement, recipient, approval, message);
		
		notification.setId(id);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			DataAccessNotificationDaoImpl.validateForStorage(notification);
		}).getMessage();
		
		assertEquals("The notification type is required.", errorMessage);
	}
	
	@Test
	public void testValidateForStorageWithNoSentOn() {

		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;
		
		Long id = 1L;
		Long requirement = 2L;
		Long recipient = 2L;
		Long approval = 3L;
		Long message = 4L;

		DBODataAccessNotification notification = newNotification(type, requirement, recipient, approval, message);
		
		notification.setSentOn(null);
		notification.setId(id);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			DataAccessNotificationDaoImpl.validateForStorage(notification);
		}).getMessage();
		
		assertEquals("The sent on is required.", errorMessage);
	}
	
	@Test
	public void testListSubmitterApprovalsForUnSentReminder() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessRequirement requirement = createManagedAR();
		
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate expirationDate = today.plus(notificationType.getReminderPeriod());

		// The approval is expiring according to the reminder period
		AccessApproval approval = createApproval(requirement, expirationDate.atStartOfDay(ZoneOffset.UTC).toInstant());
		
		int limit = 100;
		
		List<Long> expected = Arrays.asList(approval.getId());
		
		// Call under test
		List<Long> result = notificationDao.listSubmmiterApprovalsForUnSentReminder(notificationType, today, limit);
		
		// Now simulates a notification sent today
		notificationDao.create(newNotification(notificationType, requirement, approval, today.atStartOfDay(ZoneOffset.UTC).toInstant(), -1L));
		
		expected = Collections.emptyList();
		result = notificationDao.listSubmmiterApprovalsForUnSentReminder(notificationType, today, limit);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testListSubmitterApprovalsForUnSentReminderWithExisting() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessRequirement requirement = createManagedAR();
		
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate expirationDate = today.plus(notificationType.getReminderPeriod());

		// The approval is expiring according to the reminder period
		AccessApproval approval = createApproval(requirement, expirationDate.atStartOfDay(ZoneOffset.UTC).toInstant());
		
		Instant sentOn = today.atStartOfDay(ZoneOffset.UTC).toInstant();
		
		// Notification sent today
		notificationDao.create(newNotification(notificationType, requirement, approval, sentOn, -1L));
		
		int limit = 100;
		
		List<Long> expected = Collections.emptyList();
		
		// Call under test
		List<Long> result = notificationDao.listSubmmiterApprovalsForUnSentReminder(notificationType, today, limit);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testListSubmitterApprovalsForUnSentReminderWithPastExisting() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessRequirement requirement = createManagedAR();
		
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate oneMonthAgo = today.minusMonths(1);
		LocalDate expirationDate = today.plus(notificationType.getReminderPeriod());

		// The approval is expiring according to the reminder period
		AccessApproval approval = createApproval(requirement, expirationDate.atStartOfDay(ZoneOffset.UTC).toInstant());
		
		Instant sentOn = oneMonthAgo.atStartOfDay(ZoneOffset.UTC).toInstant();
		
		// Last notification sent a month ago
		notificationDao.create(newNotification(notificationType, requirement, approval, sentOn, -1L));
		
		int limit = 100;
		
		List<Long> expected = Arrays.asList(approval.getId());
		
		// Call under test
		List<Long> result = notificationDao.listSubmmiterApprovalsForUnSentReminder(notificationType, today, limit);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testListSubmitterApprovalsForUnSentReminderWithDifferentType() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessRequirement requirement = createManagedAR();
		
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate expirationDate = today.plus(notificationType.getReminderPeriod());

		// The approval is expiring according to the reminder period
		AccessApproval approval = createApproval(requirement, expirationDate.atStartOfDay(ZoneOffset.UTC).toInstant());
		
		Instant sentOn = today.atStartOfDay(ZoneOffset.UTC).toInstant();
		
		// Last processed notification was a revocation
		notificationDao.create(newNotification(DataAccessNotificationType.REVOCATION, requirement, approval, sentOn, -1L));
		
		int limit = 100;
		
		List<Long> expected = Arrays.asList(approval.getId());
		
		// Call under test
		List<Long> result = notificationDao.listSubmmiterApprovalsForUnSentReminder(notificationType, today, limit);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testListSubmitterApprovalsForUnSentReminderWithDifferentSubmitters() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessRequirement requirement = createManagedAR();
		
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate expirationDate = today.plus(notificationType.getReminderPeriod());

		// The approval is expiring according to the reminder period
		AccessApproval ap1 = createApproval(requirement, expirationDate.atStartOfDay(ZoneOffset.UTC).toInstant());
		// An approval for the same requirement, but different submitter
		AccessApproval ap2 = createApproval(requirement, user2, expirationDate.atStartOfDay(ZoneOffset.UTC).toInstant());
		
		int limit = 100;
		
		List<Long> expected = Arrays.asList(ap1.getId(), ap2.getId());
		
		// Call under test
		List<Long> result = notificationDao.listSubmmiterApprovalsForUnSentReminder(notificationType, today, limit);
		
		Collections.sort(expected);
		Collections.sort(result);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testListSubmitterApprovalsForUnSentReminderWithDifferentRequirements() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessRequirement requirement = createManagedAR();
		AccessRequirement requirement2 = createManagedAR();
		
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate expirationDate = today.plus(notificationType.getReminderPeriod());

		// The approval is expiring according to the reminder period
		AccessApproval ap1 = createApproval(requirement, expirationDate.atStartOfDay(ZoneOffset.UTC).toInstant());
		// An approval for another requirement
		AccessApproval ap2 = createApproval(requirement2, expirationDate.atStartOfDay(ZoneOffset.UTC).toInstant());
		
		int limit = 100;
		
		List<Long> expected = Arrays.asList(ap1.getId(), ap2.getId());
		
		// Call under test
		List<Long> result = notificationDao.listSubmmiterApprovalsForUnSentReminder(notificationType, today, limit);
		
		Collections.sort(expected);
		Collections.sort(result);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testListSubmitterApprovalsForUnSentReminderWithMultipleApprovals() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessRequirement requirement = createManagedAR();
		
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate expirationDate = today.plus(notificationType.getReminderPeriod());

		// The approval is expiring according to the reminder period
		createApproval(requirement, expirationDate.atStartOfDay(ZoneOffset.UTC).toInstant());
		// An approval for the same requirement (but different version) that expires a little later in the day
		AccessApproval ap2 = createApproval(requirement, expirationDate.atStartOfDay(ZoneOffset.UTC).plusHours(5).toInstant());
		
		int limit = 100;
		
		List<Long> expected = Arrays.asList(ap2.getId());
		
		// Call under test
		List<Long> result = notificationDao.listSubmmiterApprovalsForUnSentReminder(notificationType, today, limit);
		
		assertEquals(expected, result);
	}

	private DBODataAccessNotification newNotification(DataAccessNotificationType type, AccessRequirement requirement,
			AccessApproval approval, Long messageId) {
		return newNotification(type, requirement.getId(), Long.valueOf(approval.getAccessorId()), approval.getId(),
				messageId);
	}
	
	private DBODataAccessNotification newNotification(DataAccessNotificationType type, AccessRequirement requirement,
			AccessApproval approval, Instant sentOn, Long messageId) {
		return newNotification(type, requirement.getId(), Long.valueOf(approval.getAccessorId()), sentOn, approval.getId(),
				messageId);
	}

	private DBODataAccessNotification newNotification(DataAccessNotificationType type, Long requirement, Long recipient,
			Long approval, Long messageId) {
		return newNotification(type, requirement, recipient, Instant.now(), approval, messageId);
	}
	
	private DBODataAccessNotification newNotification(DataAccessNotificationType type, Long requirement, Long recipient,
			Instant sentOn,
			Long approval, Long messageId) {
		DBODataAccessNotification notification = new DBODataAccessNotification();
		notification.setNotificationType(type == null ? null : type.name());
		notification.setRequirementId(requirement);
		notification.setRecipientId(recipient);
		notification.setSentOn(Timestamp.from(sentOn));
		notification.setAccessApprovalId(approval);
		notification.setMessageId(messageId);
		return notification;
	}

	private AccessRequirement createManagedAR() {
		AccessRequirement accessRequirement = new ManagedACTAccessRequirement();
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setCreatedBy(user.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(user.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setConcreteType(ManagedACTAccessRequirement.class.getName());

		return accessRequirementDao.create(accessRequirement);
	}
	
	private AccessApproval createApproval(AccessRequirement accessRequirement) {
		return createApproval(accessRequirement, null);
	}

	private AccessApproval createApproval(AccessRequirement accessRequirement, Instant expiresOn) {
		return createApproval(accessRequirement, user, expiresOn);
	}
	
	private AccessApproval createApproval(AccessRequirement accessRequirement, UserGroup submitter, Instant expiresOn) {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setCreatedBy(submitter.getId());
		accessApproval.setCreatedOn(new Date());
		accessApproval.setModifiedBy(submitter.getId());
		accessApproval.setModifiedOn(new Date());
		accessApproval.setAccessorId(submitter.getId());
		accessApproval.setExpiredOn(expiresOn == null ? null : Date.from(expiresOn));
		accessApproval.setRequirementId(accessRequirement.getId());
		accessApproval.setRequirementVersion(accessRequirement.getVersionNumber());
		accessApproval.setSubmitterId(submitter.getId());
		accessApproval.setState(ApprovalState.APPROVED);

		return accessApprovalDao.create(accessApproval);
	}
}
