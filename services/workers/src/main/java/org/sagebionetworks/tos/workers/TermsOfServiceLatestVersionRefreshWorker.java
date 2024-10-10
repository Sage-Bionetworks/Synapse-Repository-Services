package org.sagebionetworks.tos.workers;

import org.sagebionetworks.repo.manager.authentication.TermsOfServiceManager;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.springframework.stereotype.Service;

@Service
public class TermsOfServiceLatestVersionRefreshWorker implements ProgressingRunner {

	private TermsOfServiceManager manager;
	
	public TermsOfServiceLatestVersionRefreshWorker(TermsOfServiceManager manager) {
		this.manager = manager;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		manager.refreshLatestVersion();
	}

}
