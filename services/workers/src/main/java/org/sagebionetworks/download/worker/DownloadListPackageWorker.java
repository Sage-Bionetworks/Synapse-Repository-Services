package org.sagebionetworks.download.worker;

import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.download.DownloadListPackageRequest;
import org.sagebionetworks.repo.model.download.DownloadListPackageResponse;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class DownloadListPackageWorker implements AsyncJobRunner<DownloadListPackageRequest, DownloadListPackageResponse> {

	private DownloadListManager downloadListManager;

	@Autowired
	public DownloadListPackageWorker(DownloadListManager downloadListManager) {
		this.downloadListManager = downloadListManager;
	}

	@Override
	public Class<DownloadListPackageRequest> getRequestType() {
		return DownloadListPackageRequest.class;
	}
	
	@Override
	public Class<DownloadListPackageResponse> getResponseType() {
		return DownloadListPackageResponse.class;
	}

	@Override
	public DownloadListPackageResponse run(String jobId, UserInfo user, DownloadListPackageRequest request,
			AsyncJobProgressCallback jobProgressCallback)
			throws RecoverableMessageException, Exception {
		return downloadListManager.packageFiles(jobProgressCallback, user, request);
	}


}
