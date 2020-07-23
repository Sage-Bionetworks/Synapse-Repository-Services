package org.sagebionetworks.dataaccess.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBODataAccessNotification;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationDao;
import org.sagebionetworks.repo.model.dbo.feature.Feature;
import org.sagebionetworks.repo.model.dbo.feature.FeatureStatusDao;
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

	private static final long WORKER_TIMEOUT = 2 * 60 * 1000;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	
	@Autowired
	private AccessApprovalDAO accessApprovalDao;
	
	@Autowired
	private FeatureStatusDao featureStatusDao;
	
	@Autowired
	private DataAccessNotificationDao notificationDao;
	
	@Autowired
	private MessageDAO messageDao;
	
	private UserInfo user;
	private List<Long> accessRequirements;
	private List<Long> accessApprovals;
	
	@BeforeEach
	public void before() {
		featureStatusDao.clear();
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		// Enabled the features for testing, we need both the auto revocation and the notification workers running
		featureStatusDao.setFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION, true);
		featureStatusDao.setFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS, true);
		accessRequirements = new ArrayList<>();
		accessApprovals = new ArrayList<>();
	}
	
	@AfterEach
	public void after() {
		for (Long id : accessRequirements) {
			accessRequirementDao.delete(id.toString());
		}
		for (Long id : accessApprovals) {
			accessApprovalDao.delete(id.toString());
		}
		featureStatusDao.clear();
	}
	
	@Test
	public void testRun() throws Exception {
		
		// Will create an expired approval, one worker will expire it and another will act upon the change message to send the notification
		AccessApproval ap = newApproval(newAccessRequirement(), ApprovalState.APPROVED, Instant.now().minus(1, ChronoUnit.DAYS));
		
		TimeUtils.waitFor(WORKER_TIMEOUT, 1000L, () -> {
			Optional<DBODataAccessNotification> result = notificationDao.find(DataAccessNotificationType.REVOCATION, ap.getRequirementId(), user.getId());
			
			// Verify that the message to user was created for the user
			if (result.isPresent()) {
				Long messageId = result.get().getMessageId();

				MessageToUser message = messageDao.getMessage(messageId.toString());
			
				assertEquals(message.getCreatedBy(), BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId().toString());
				assertEquals(message.getRecipients(), Collections.singleton(user.getId().toString()));
				
			}
			
			return new Pair<>(result.isPresent(), null);
		});
		
	}
	
	private AccessRequirement newAccessRequirement() {
		AccessRequirement accessRequirement = new ManagedACTAccessRequirement();
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setCreatedBy(user.getId().toString());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(user.getId().toString());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setConcreteType(ManagedACTAccessRequirement.class.getName());
		
		accessRequirement = accessRequirementDao.create(accessRequirement);
		accessRequirements.add(accessRequirement.getId());
		return accessRequirement;
	}
	
	private AccessApproval newApproval(AccessRequirement accessRequirement, ApprovalState state, Instant expiresOn) {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setCreatedBy(user.getId().toString());
		accessApproval.setCreatedOn(new Date());
		accessApproval.setModifiedBy(user.getId().toString());
		accessApproval.setModifiedOn(new Date());
		accessApproval.setAccessorId(user.getId().toString());
		accessApproval.setRequirementId(accessRequirement.getId());
		accessApproval.setRequirementVersion(accessRequirement.getVersionNumber());
		accessApproval.setSubmitterId(user.getId().toString());
		accessApproval.setExpiredOn(expiresOn == null ? null : Date.from(expiresOn));
		accessApproval.setState(state);
		
		accessApproval = accessApprovalDao.create(accessApproval);
		accessApprovals.add(accessApproval.getId());
		
		return accessApproval;
	}
}
