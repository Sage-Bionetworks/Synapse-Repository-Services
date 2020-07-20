package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
	public void testRegisterNotification() {
		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;
		
		AccessRequirement accessRequirement = createManagedAR();
		AccessApproval approval = createApproval(accessRequirement);
		
		Long requirementId = accessRequirement.getId();
		Long accessApprovalId = approval.getId(); 
		Long recipientId = Long.valueOf(approval.getAccessorId());
		Long messageId = -1L;
		Instant sentOn = Instant.now();
		
		// Call under test
		notificationDao.registerNotification(type, requirementId, recipientId, accessApprovalId, messageId, sentOn);

		Optional<Instant> result = notificationDao.getSentOn(type, requirementId, recipientId);
		
		assertTrue(result.isPresent());
		assertEquals(sentOn, result.get());
	}
	
	@Test
	public void testRegisterNotificationWithExisting() {
		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;
		
		AccessRequirement accessRequirement = createManagedAR();
		AccessApproval approval = createApproval(accessRequirement);
		
		Long requirementId = accessRequirement.getId();
		Long accessApprovalId = approval.getId(); 
		Long recipientId = Long.valueOf(approval.getAccessorId());
		Long messageId = -1L;
		Instant sentOn = Instant.now();
		
		// Call under test
		notificationDao.registerNotification(type, requirementId, recipientId, accessApprovalId, messageId, sentOn);
		
		Optional<Instant> result = notificationDao.getSentOn(type, requirementId, recipientId);
		
		assertEquals(sentOn, result.get());

		String etag = notificationDao.getEtag(type, requirementId, recipientId).get();
		
		Instant sentOnUpdated = sentOn.plus(1, ChronoUnit.DAYS); 
		
		// Now call again
		notificationDao.registerNotification(type, requirementId, recipientId, accessApprovalId, messageId, sentOnUpdated);
		
		result = notificationDao.getSentOn(type, requirementId, recipientId);
		
		assertEquals(sentOnUpdated, result.get());
		
		// Verify etag change
		String updatedEtag = notificationDao.getEtag(type, requirementId, recipientId).get();
		
		assertNotEquals(etag, updatedEtag);
	}
	
	@Test
	public void testGetSentOn() {
		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;
		
		AccessRequirement accessRequirement = createManagedAR();
		AccessApproval approval = createApproval(accessRequirement);
		
		Long requirementId = accessRequirement.getId();
		Long accessApprovalId = approval.getId(); 
		Long recipientId = Long.valueOf(approval.getAccessorId());
		Long messageId = -1L;
		Instant sentOn = Instant.now();
		
		// Now register a notification
		notificationDao.registerNotification(type, requirementId, recipientId, accessApprovalId, messageId, sentOn);
		
		// Call under test
		Optional<Instant> result = notificationDao.getSentOn(type, requirementId, recipientId);
		
		assertTrue(result.isPresent());
		assertEquals(sentOn, result.get());
	}
	
	@Test
	public void testGetSentOnWithoutData() {
		DataAccessNotificationType type = DataAccessNotificationType.REVOCATION;
		
		AccessRequirement accessRequirement = createManagedAR();
		AccessApproval approval = createApproval(accessRequirement);
		
		Long requirementId = accessRequirement.getId();
		Long accessApprovalId = approval.getId(); 
		Long recipientId = Long.valueOf(approval.getAccessorId());
		Long messageId = -1L;
		Instant sentOn = Instant.now();
		
		// Call under test
		Optional<Instant> result = notificationDao.getSentOn(type, requirementId, recipientId);
		
		assertFalse(result.isPresent());
		
		// Now register a notification
		notificationDao.registerNotification(type, requirementId, recipientId, accessApprovalId, messageId, sentOn);
		
		// Call under test with a different requirement
		result = notificationDao.getSentOn(type, requirementId + 1, recipientId);
		
		assertFalse(result.isPresent());
		
		// Call under test with a different recipient
		result = notificationDao.getSentOn(type, requirementId, recipientId + 1);
		
		assertFalse(result.isPresent());
		
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
