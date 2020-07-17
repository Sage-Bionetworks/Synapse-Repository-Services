package org.sagebionetworks.dataaccess.workers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
@ActiveProfiles("test-dataaccess-worker")
public class AccessApprovalExpirationWorkerIntegrationTest {
	
	private static final long WORKER_TIMEOUT = 60 * 1000;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	
	@Autowired
	private AccessApprovalDAO accessApprovalDao;
	
	private UserInfo user;
	private List<Long> accessRequirements;
	private List<Long> accessApprovals;
	
	@BeforeEach
	public void before() {
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
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
	}
	
	@Test
	public void testRun() throws Exception {
		
		AccessApproval ap = newApproval(newAccessRequirement(), ApprovalState.APPROVED, Instant.now().minus(1, ChronoUnit.DAYS));
		
		TimeUtils.waitFor(WORKER_TIMEOUT, 1000L, () -> {
			AccessApproval updated = accessApprovalDao.get(ap.getId().toString());
			return new Pair<>(ApprovalState.REVOKED.equals(updated.getState()), updated);
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
