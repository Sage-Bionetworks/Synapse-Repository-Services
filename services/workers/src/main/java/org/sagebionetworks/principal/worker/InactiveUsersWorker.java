package org.sagebionetworks.principal.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.principal.UserStatusManager;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.springframework.stereotype.Service;

@Service
public class InactiveUsersWorker implements ProgressingRunner {
	
	private static final int MAX_USERS_TO_PROCESS = 500;
	private static final Logger LOG = LogManager.getLogger(InactiveUsersWorker.class);
		
	private UserStatusManager userStatusManager;
	private FeatureManager featureManager;
	
	public InactiveUsersWorker(UserStatusManager userStatusManager, FeatureManager featureManager) {
		this.userStatusManager = userStatusManager;
		this.featureManager = featureManager;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {

		try {

			if (!featureManager.isFeatureEnabled(Feature.DISABLE_INACTIVE_USERS)) {
				LOG.warn("Disabling inactive user feature disabled, will not run.");
				return;
			}

			LOG.info("Warning inactive users...");
			int warnedCount = 0;
			int warnedBatchCount;
			do {
				warnedBatchCount = userStatusManager.warnInactiveUsers(MAX_USERS_TO_PROCESS);
				warnedCount += warnedBatchCount;
			} while (warnedBatchCount > 0);
			LOG.info("Warning inactive users...DONE (Warned Count: {})", warnedCount);

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
