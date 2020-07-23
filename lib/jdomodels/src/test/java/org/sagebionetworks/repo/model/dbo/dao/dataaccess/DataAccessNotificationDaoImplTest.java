package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;
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

	private List<Long> requirements;
	private List<Long> approvals;

	@BeforeEach
	public void before() {
		requirements = new ArrayList<>();
		approvals = new ArrayList<>();

		user = new UserGroup();
		user.setIsIndividual(true);
		user.setCreationDate(new Date());
		user.setId(userGroupDao.create(user).toString());

		notificationDao.clear();
	}

	@AfterEach
	public void after() {

		for (Long approval : approvals) {
			accessApprovalDao.delete(approval.toString());
		}

		for (Long requirement : requirements) {
			accessRequirementDao.delete(requirement.toString());
		}

		userGroupDao.delete(user.getId());
		notificationDao.clear();
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
		DBODataAccessNotification anotherNotification = newNotification(DataAccessNotificationType.RENEWAL_REMINDER_1,
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

	private DBODataAccessNotification newNotification(DataAccessNotificationType type, AccessRequirement requirement,
			AccessApproval approval, Long messageId) {
		return newNotification(type, requirement.getId(), Long.valueOf(approval.getAccessorId()), approval.getId(),
				messageId);
	}

	private DBODataAccessNotification newNotification(DataAccessNotificationType type, Long requirement, Long recipient,
			Long approval, Long messageId) {
		DBODataAccessNotification notification = new DBODataAccessNotification();
		notification.setNotificationType(type == null ? null : type.name());
		notification.setRequirementId(requirement);
		notification.setRecipientId(recipient);
		notification.setSentOn(Timestamp.from(Instant.now()));
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

		accessRequirement = accessRequirementDao.create(accessRequirement);

		requirements.add(accessRequirement.getId());

		return accessRequirement;
	}

	private AccessApproval createApproval(AccessRequirement accessRequirement) {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setCreatedBy(user.getId());
		accessApproval.setCreatedOn(new Date());
		accessApproval.setModifiedBy(user.getId());
		accessApproval.setModifiedOn(new Date());
		accessApproval.setAccessorId(user.getId());
		accessApproval.setRequirementId(accessRequirement.getId());
		accessApproval.setRequirementVersion(accessRequirement.getVersionNumber());
		accessApproval.setSubmitterId(user.getId());
		accessApproval.setState(ApprovalState.APPROVED);

		accessApproval = accessApprovalDao.create(accessApproval);

		approvals.add(accessApproval.getId());

		return accessApproval;
	}
}
