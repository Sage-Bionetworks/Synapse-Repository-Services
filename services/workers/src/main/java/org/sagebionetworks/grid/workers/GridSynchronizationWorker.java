package org.sagebionetworks.grid.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.grid.synch.GridSynchronizationManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.SynchronizeGridRequest;
import org.sagebionetworks.repo.model.grid.SynchronizeGridResponse;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

@Service
public class GridSynchronizationWorker implements AsyncJobRunner<SynchronizeGridRequest, SynchronizeGridResponse> {
	
	private static final Logger log = LogManager.getLogger(GridSynchronizationWorker.class);

	private final GridSynchronizationManager manager;

	public GridSynchronizationWorker(GridSynchronizationManager manager) {
		super();
		this.manager = manager;
	}

	@Override
	public Class<SynchronizeGridRequest> getRequestType() {
		return SynchronizeGridRequest.class;
	}

	@Override
	public Class<SynchronizeGridResponse> getResponseType() {
		return SynchronizeGridResponse.class;
	}

	@Override
	public SynchronizeGridResponse run(String jobId, UserInfo user, SynchronizeGridRequest request,
			AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		return manager.synchronizeCopyWithSource(jobProgressCallback, user, request);
	}

}
