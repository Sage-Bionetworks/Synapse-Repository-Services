package org.sagebionetworks.download.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.download.DownloadListManifestRequest;
import org.sagebionetworks.repo.model.download.DownloadListManifestResponse;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class DownloadListManifestWorker implements AsyncJobRunner<DownloadListManifestRequest, DownloadListManifestResponse> {

	private DownloadListManager downloadListManager;

	@Autowired
	public DownloadListManifestWorker(DownloadListManager downloadListManager) {
		this.downloadListManager = downloadListManager;
	}
	
	@Override
	public Class<DownloadListManifestRequest> getRequestType() {
		return DownloadListManifestRequest.class;
	}
	
	@Override
	public Class<DownloadListManifestResponse> getResponseType() {
		return DownloadListManifestResponse.class;
	}

	@Override
	public DownloadListManifestResponse run(ProgressCallback progressCallback, String jobId, UserInfo user,
			DownloadListManifestRequest request, AsyncJobProgressCallback jobProgressCallback)
			throws RecoverableMessageException, Exception {
		return downloadListManager.createManifest(progressCallback, user, request);
	}

}