package org.sagebionetworks.doi.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.doi.DoiManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiRequest;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class DoiWorker implements AsyncJobRunner<DoiRequest, DoiResponse> {

	private DoiManager doiManager;

	@Autowired
	public DoiWorker(DoiManager doiManager) {
		this.doiManager = doiManager;
	}

	@Override
	public Class<DoiRequest> getRequestType() {
		return DoiRequest.class;
	}

	@Override
	public Class<DoiResponse> getResponseType() {
		return DoiResponse.class;
	}

	@Override
	public DoiResponse run(ProgressCallback progressCallback, String jobId, UserInfo user, DoiRequest request,
			AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		Doi doi = doiManager.createOrUpdateDoi(user, request.getDoi());
		DoiResponse response = new DoiResponse().setDoi(doi);
		return response;
	}
}