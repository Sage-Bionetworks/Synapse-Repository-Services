package org.sagebionetworks.download.worker;

import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.download.DownloadListManifestRequest;
import org.sagebionetworks.repo.model.download.DownloadListManifestResponse;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
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
	public DownloadListManifestResponse run(String jobId, UserInfo user, DownloadListManifestRequest request,
			AsyncJobProgressCallback jobProgressCallback)
			throws RecoverableMessageException, Exception {
		return downloadListManager.createManifest(jobProgressCallback, user, request);
	}

}