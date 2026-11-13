package org.sagebionetworks.grid.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridUpdateJobRequest;
import org.sagebionetworks.repo.model.grid.GridUpdateJobResponse;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

@Service
public class GridUpdateWorker implements AsyncJobRunner<GridUpdateJobRequest, GridUpdateJobResponse> {

	private static final Logger log = LogManager.getLogger(GridUpdateWorker.class);

	private final GridManager gridManager;

	public GridUpdateWorker(GridManager gridManager) {
		super();
		this.gridManager = gridManager;
	}

	@Override
	public Class<GridUpdateJobRequest> getRequestType() {
		return GridUpdateJobRequest.class;
	}

	@Override
	public Class<GridUpdateJobResponse> getResponseType() {
		return GridUpdateJobResponse.class;
	}

	@Override
	public GridUpdateJobResponse run(String jobId, UserInfo user, GridUpdateJobRequest request,
			AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		try {
			GridConnectionInfo publishingConnection = gridManager.getOrCreateApiConnection(user,
					request.getSessionId(), request.getReplicaId());
			return gridManager.updateGrid(user, request, publishingConnection);
		} catch (RecoverableMessageException e) {
			log.warn("Recoverable message: " + e.getMessage());
			throw e;
		}
	}

}
