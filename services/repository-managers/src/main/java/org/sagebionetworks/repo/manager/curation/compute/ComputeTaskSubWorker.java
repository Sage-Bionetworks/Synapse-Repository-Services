package org.sagebionetworks.repo.manager.curation.compute;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.execution.ExecutableTaskExecutionDetails;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;

/**
 * Interface for type-specific sub-workers that execute curation task computations.
 * Each implementation handles a specific concrete type of {@link ExecutableTaskExecutionDetails}.
 *
 * @param <T> The concrete execution details type this sub-worker handles.
 */
public interface ComputeTaskSubWorker<T extends ExecutableTaskExecutionDetails> {

	/**
	 * @return The class of ExecutableTaskExecutionDetails this sub-worker handles.
	 */
	Class<T> getExecutionDetailsType();

	/**
	 * Execute the computation for the given task.
	 *
	 * @param user The user who initiated the execution
	 * @param task The curation task to execute
	 * @param executionDetails The typed execution details
	 * @param callback Progress callback for the async job
	 * @return The updated execution details after successful execution
	 */
	T execute(UserInfo user, CurationTask task, T executionDetails, AsyncJobProgressCallback callback) throws Exception;
}
