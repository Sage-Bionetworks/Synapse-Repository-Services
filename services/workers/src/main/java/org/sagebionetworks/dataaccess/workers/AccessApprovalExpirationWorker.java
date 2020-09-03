package org.sagebionetworks.dataaccess.workers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalManager;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.feature.Feature;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A worker that checks for expired access approvals and update their status to REVOKED
 * 
 * @author Marco Marasca
 */
public class AccessApprovalExpirationWorker implements ProgressingRunner {
	
	private static final Logger LOG = LogManager.getLogger(AccessApprovalExpirationWorker.class);
	
	protected static final int BATCH_SIZE = 100;

	// We look only 7 days backward
	protected static final int CUT_OFF_DAYS = 7;
	
	private AccessApprovalManager accessApprovalManager;
	private UserManager userManager;
	private FeatureManager featureManager;

	@Autowired
	public AccessApprovalExpirationWorker(
			final AccessApprovalManager accessApprovalManager,
			final UserManager userManager,
			final FeatureManager featureTestingManager) {
		this.accessApprovalManager = accessApprovalManager;
		this.userManager = userManager;
		this.featureManager = featureTestingManager;
	}
	
	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		
		// Feature not yet enabled
		if (!featureManager.isFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION)) {
			return;
		}
		
		long startTime = System.currentTimeMillis();

		try {
			
			UserInfo admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
			
			Instant expiredAfter = Instant.now().minus(CUT_OFF_DAYS, ChronoUnit.DAYS);
				
			int revokedCount = accessApprovalManager.revokeExpiredApprovals(admin, expiredAfter, BATCH_SIZE);
			
			LOG.info("Sucessfully processed {} access approvals (Time: {} ms).", revokedCount, System.currentTimeMillis() - startTime);
		} catch (Throwable e) {
			LOG.error(e.getMessage(),  e);
		}
		
	}

}
