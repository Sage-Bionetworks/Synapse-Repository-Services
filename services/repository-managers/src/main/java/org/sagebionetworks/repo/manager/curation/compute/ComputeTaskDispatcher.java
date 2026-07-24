package org.sagebionetworks.repo.manager.curation.compute;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.ComputeTaskExecutionRequest;
import org.sagebionetworks.repo.model.curation.ComputeTaskExecutionResponse;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;

/**
 * Dispatches compute task execution requests to appropriate sub-workers,
 * managing the task state machine transitions (NOT_STARTED -> EXECUTING -> IN_REVIEW).
 */
public interface ComputeTaskDispatcher {

	/**
	 * Dispatch the given request to the appropriate sub-worker based on the task's execution details type.
	 *
	 * @param jobId The async job ID
	 * @param user The user who started the job
	 * @param request The execution request containing the task ID
	 * @param callback Progress callback
	 * @return The execution response with updated details
	 */
	ComputeTaskExecutionResponse dispatch(String jobId, UserInfo user, ComputeTaskExecutionRequest request,
			AsyncJobProgressCallback callback) throws Exception;
}
