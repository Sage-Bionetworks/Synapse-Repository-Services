package org.sagebionetworks.grid.workers;

import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GridCreateWorker implements AsyncJobRunner<CreateGridRequest, CreateGridResponse> {

	private final GridManager gridManager;

	@Autowired
	public GridCreateWorker(GridManager gridManager) {
		super();
		this.gridManager = gridManager;
	}

	@Override
	public Class<CreateGridRequest> getRequestType() {
		return CreateGridRequest.class;
	}

	@Override
	public Class<CreateGridResponse> getResponseType() {
		return CreateGridResponse.class;
	}

	@Override
	public CreateGridResponse run(String jobId, UserInfo user, CreateGridRequest request,
			AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		return gridManager.createGrid(jobProgressCallback, user, request);
	}

}
