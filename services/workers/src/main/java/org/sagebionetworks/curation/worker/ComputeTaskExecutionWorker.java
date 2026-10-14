package org.sagebionetworks.curation.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.curation.compute.ComputeTaskDispatcher;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.ComputeTaskExecutionRequest;
import org.sagebionetworks.repo.model.curation.ComputeTaskExecutionResponse;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

@Service
public class ComputeTaskExecutionWorker implements AsyncJobRunner<ComputeTaskExecutionRequest, ComputeTaskExecutionResponse> {

	private static final Logger log = LogManager.getLogger(ComputeTaskExecutionWorker.class);

	private final ComputeTaskDispatcher dispatcher;

	public ComputeTaskExecutionWorker(ComputeTaskDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	public Class<ComputeTaskExecutionRequest> getRequestType() {
		return ComputeTaskExecutionRequest.class;
	}

	@Override
	public Class<ComputeTaskExecutionResponse> getResponseType() {
		return ComputeTaskExecutionResponse.class;
	}

	@Override
	public ComputeTaskExecutionResponse run(String jobId, UserInfo user, ComputeTaskExecutionRequest request,
			AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		try {
			log.info("Dispatching compute task execution (taskId={}, jobId={})", request.getTaskId(), jobId);
			return dispatcher.dispatch(jobId, user, request, jobProgressCallback);
		} catch (RecoverableMessageException e) {
			log.error("Will retry.  Message: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Failed to execute compute task (taskId={}, jobId={}), message: {}",
					request.getTaskId(), jobId, e.getMessage(), e);
			throw e;
		}
	}
}
