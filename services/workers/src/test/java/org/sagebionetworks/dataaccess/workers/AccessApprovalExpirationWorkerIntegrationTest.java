package org.sagebionetworks.dataaccess.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
@ActiveProfiles("test-dataaccess-worker")
public class AccessApprovalExpirationWorkerIntegrationTest {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	
	@Autowired
	private AccessApprovalDAO accessApprovalDao;
	
	@Autowired
	private AccessApprovalExpirationWorker worker;
	
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
		// Approved and not expiring
		AccessApproval ap1 = newApproval(newAccessRequirement(), ApprovalState.APPROVED, null);
		// Already revoked and expired
		AccessApproval ap2 = newApproval(newAccessRequirement(), ApprovalState.REVOKED, Instant.now().minus(1, ChronoUnit.DAYS));
		// APPROVED by expired
		AccessApproval ap3 = newApproval(newAccessRequirement(), ApprovalState.APPROVED, Instant.now().minus(1, ChronoUnit.DAYS));
		// Already revoked and not expiring
		AccessApproval ap4 = newApproval(newAccessRequirement(), ApprovalState.REVOKED, null);
		// Approved and expiring in the future
		AccessApproval ap5 = newApproval(newAccessRequirement(), ApprovalState.APPROVED, Instant.now().plus(1, ChronoUnit.DAYS));
		
		// Simulates the run
		worker.run(null);
		
		// Should have not touched AP1 since it's not expiring 
		assertEquals(ap1, accessApprovalDao.get(ap1.getId().toString()));
		
		// Should have not touched AP2 since it's already revoked (but expired)
		assertEquals(ap2, accessApprovalDao.get(ap2.getId().toString()));
		
		AccessApproval ap3Updated = accessApprovalDao.get(ap3.getId().toString());

		// Should have revoked AP3 since it's approved but expired
		assertNotEquals(ap3, ap3Updated);
		assertEquals(ApprovalState.REVOKED, ap3Updated.getState());
		
		// Should have not touched AP4 since it's already revoked (and not expiring)
		assertEquals(ap4, accessApprovalDao.get(ap4.getId().toString()));
		
		// Should have not touched AP5 since it's it will expire in the future
		assertEquals(ap5, accessApprovalDao.get(ap5.getId().toString()));
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
