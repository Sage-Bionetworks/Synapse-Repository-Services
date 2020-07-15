package org.sagebionetworks.dataaccess.workers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A worker that checks for expired access approvals and update their status to REVOKED
 * 
 * @author Marco Marasca
 */
public class AccessApprovalExpirationWorker implements ProgressingRunner {
	
	private static final Logger LOG = LogManager.getLogger(AccessApprovalExpirationWorker.class);
	
	protected static final int BATCH_SIZE = 100;

	// We look only 60 days backward
	protected static final int CUT_OFF_DAYS = 60;
	
	// Max number of items to process in one run
	protected static final int MAX_RUN_SIZE = BATCH_SIZE * 10;
	
	private AccessApprovalManager accessApprovalManager;
	private StackStatusDao stackStatusDao;
	private UserManager userManager;

	@Autowired
	public AccessApprovalExpirationWorker(
			final AccessApprovalManager accessApprovalManager, 
			final StackStatusDao stackStatusDao,
			final UserManager userManager) {
		this.accessApprovalManager = accessApprovalManager;
		this.stackStatusDao = stackStatusDao;
		this.userManager = userManager;
	}
	
	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		
		int count = 0;
		long startTime = System.currentTimeMillis();

		try {
			
			UserInfo admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
			
			Instant expiredAfter = Instant.now().minus(CUT_OFF_DAYS, ChronoUnit.DAYS);
			
			while (count <= MAX_RUN_SIZE) {
				if (!stackStatusDao.isStackReadWrite()) {
					LOG.warn("The stack switched from READ/WRITE mode, ending execution.");
					logProgress(count, startTime);
					return;
				}
				
				int revokedCount = accessApprovalManager.revokeExpiredApprovals(admin, expiredAfter, BATCH_SIZE);
				
				count += revokedCount;
				
				if (revokedCount < BATCH_SIZE) {
					// Finished working for now
					break;
				}
			}
			
			logProgress(count, startTime);
		} catch (Throwable e) {
			LOG.error(e.getMessage(),  e);
			logProgress(count, startTime);
		}
		
	}
	
	private void logProgress(int count, long startTime) {
		LOG.info("Sucessfully processed {} access approvals (Time: {} ms).", count, System.currentTimeMillis() - startTime);
	}

}
