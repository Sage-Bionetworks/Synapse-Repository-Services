package org.sagebionetworks.download.worker;

import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DownloadListQueryWorker implements AsyncJobRunner<DownloadListQueryRequest, DownloadListQueryResponse> {

	private DownloadListManager downloadListManager;

	@Autowired
	public DownloadListQueryWorker(DownloadListManager downloadListManager) {
		this.downloadListManager = downloadListManager;
	}
	
	@Override
	public Class<DownloadListQueryRequest> getRequestType() {
		return DownloadListQueryRequest.class;
	}
	
	@Override
	public Class<DownloadListQueryResponse> getResponseType() {
		return DownloadListQueryResponse.class;
	}
	
	@Override
	public DownloadListQueryResponse run(String jobId, UserInfo user, DownloadListQueryRequest request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		return downloadListManager.queryDownloadList(user, request);
	}

}