package org.sagebionetworks.auth.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenManager;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.springframework.stereotype.Service;

@Service
public class ExpiredAccessTokenWorker implements ProgressingRunner {
	
	private static final Logger LOG = LogManager.getLogger(ExpiredAccessTokenWorker.class);
	
	private OIDCTokenManager oidcTokenManager;
	private WorkerLogger workerLogger;
	
	public ExpiredAccessTokenWorker(OIDCTokenManager oidcTokenManager, WorkerLogger workerLogger) {
		this.oidcTokenManager = oidcTokenManager;
		this.workerLogger = workerLogger;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		try {
			LOG.info("Revoking expired access tokens...");
			int deletedCount = oidcTokenManager.revokeExpiredOIDCAccessTokens();
			LOG.info("Revoking expired access tokens...DONE (Deleted Count: {})", deletedCount);
		} catch (Throwable e) {
			LOG.error(e.getMessage(), e);
			boolean willRetry = false;
			// Sends a fail metric for cloud watch
			workerLogger.logWorkerFailure(ExpiredAccessTokenWorker.class.getName(), e, willRetry);
		}
	}


}
