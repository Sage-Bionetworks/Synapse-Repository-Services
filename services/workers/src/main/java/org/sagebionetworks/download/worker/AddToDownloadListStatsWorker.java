package org.sagebionetworks.download.worker;

import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.download.AddToDownloadListStatsRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListStatsResponse;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AddToDownloadListStatsWorker implements AsyncJobRunner<AddToDownloadListStatsRequest, AddToDownloadListStatsResponse> {

	private DownloadListManager downloadListManager;

	@Autowired
	public AddToDownloadListStatsWorker(DownloadListManager downloadListManager) {
		this.downloadListManager = downloadListManager;
	}
	
	@Override
	public Class<AddToDownloadListStatsRequest> getRequestType() {
		return AddToDownloadListStatsRequest.class;
	}
	
	@Override
	public Class<AddToDownloadListStatsResponse> getResponseType() {
		return AddToDownloadListStatsResponse.class;
	}

	@Override
	public AddToDownloadListStatsResponse run(String jobId, UserInfo user, AddToDownloadListStatsRequest request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		return downloadListManager.getAddToDownloadListStats(jobProgressCallback, user, request);
	}

}
