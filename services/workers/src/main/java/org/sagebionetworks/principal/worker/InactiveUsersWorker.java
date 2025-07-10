package org.sagebionetworks.principal.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.principal.UserStatusManager;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.springframework.stereotype.Service;

@Service
public class InactiveUsersWorker implements ProgressingRunner {
	
	private static final int MAX_USERS_TO_PROCESS = 500;
	private static final Logger LOG = LogManager.getLogger(InactiveUsersWorker.class);
		
	private UserStatusManager userStatusManager;
	
	public InactiveUsersWorker(UserStatusManager userStatusManager) {
		this.userStatusManager = userStatusManager;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		
		try {
			LOG.info("Disabling inactive users...");
			int disabledCount = 0;
			int disabledBatchCount;
			
			do {
				disabledBatchCount = userStatusManager.disableInactiveUsers(MAX_USERS_TO_PROCESS);
				disabledCount += disabledBatchCount;
			} while (disabledBatchCount > 0);
			
			LOG.info("Disabling inactive users...DONE (Disabled Count: {})", disabledCount);
		} catch (Throwable e) {
			LOG.error(e.getMessage(), e);
		}
		
	}

}
