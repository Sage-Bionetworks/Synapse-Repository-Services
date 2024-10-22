package org.sagebionetworks.tos.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.authentication.TermsOfServiceManager;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.springframework.stereotype.Service;

@Service
public class TermsOfServiceLatestVersionRefreshWorker implements ProgressingRunner {

	private static final Logger LOG = LogManager.getLogger(TermsOfServiceLatestVersionRefreshWorker.class);
	
	private TermsOfServiceManager manager;
	private WorkerLogger workerLogger;
	
	public TermsOfServiceLatestVersionRefreshWorker(TermsOfServiceManager manager, WorkerLogger workerLogger) {
		this.manager = manager;
		this.workerLogger = workerLogger;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		try {
			manager.refreshLatestVersion();
		} catch (Throwable e) {
			LOG.error("Failed to refresh terms of service version: ", e);
			workerLogger.logWorkerFailure(TermsOfServiceLatestVersionRefreshWorker.class.getName(), e, false);
		}
	}

}
