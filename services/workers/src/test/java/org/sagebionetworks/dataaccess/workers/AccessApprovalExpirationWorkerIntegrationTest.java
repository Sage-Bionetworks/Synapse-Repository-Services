package org.sagebionetworks.dataaccess.workers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.feature.FeatureStatusDao;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class AccessApprovalExpirationWorkerIntegrationTest {
	
	private static final long WORKER_TIMEOUT = 60 * 1000;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private FeatureStatusDao featureStatusDao;
	
	@Autowired
	private DataAccessTestHelper testHelper;
	
	private UserInfo user;
	
	@BeforeEach
	public void before() {
		testHelper.cleanUp();
		featureStatusDao.clear();
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		// Enabled the feature for testing
		featureStatusDao.setFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION, true);
	}
	
	@AfterEach
	public void after() {
		featureStatusDao.clear();
		testHelper.cleanUp();
	}
	
	@Test
	public void testRun() throws Exception {
		
		AccessRequirement ar = testHelper.newAccessRequirement(user);
		
		AccessApproval ap = testHelper.newApproval(ar, user, ApprovalState.APPROVED, Instant.now().minus(1, ChronoUnit.DAYS));
		
		TimeUtils.waitFor(WORKER_TIMEOUT, 1000L, () -> {
			AccessApproval updated = testHelper.getApproval(ap.getId());
			return new Pair<>(ApprovalState.REVOKED.equals(updated.getState()), updated);
		});
		
	}
	
	
}
