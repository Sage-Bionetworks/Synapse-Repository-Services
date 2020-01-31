package org.sagebionetworks;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.TableFailedException;

public interface AsynchronousJobWorkerHelper {

	/**
	 * Start and wait for a job of the given type.
	 * 
	 * @param user User to start the job.
	 * @param request The job request body.
	 * @param maxWaitMS The maximum amount of time to wait for the job (MS).
	 * @param responseClass The type of the response.
	 * @return
	 * @throws InterruptedException
	 */
	<R extends AsynchronousRequestBody, T extends AsynchronousResponseBody> T startAndWaitForJob(UserInfo user,
			R request, long maxWaitMS, Class<? extends T> responseClass) throws InterruptedException;

	/**
	 * Wait for the given entity to appear in the given view.
	 * @param user
	 * @param tableId
	 * @param entityId
	 * @param maxWaitMS
	 * @return
	 * @throws InterruptedException
	 */
	EntityDTO waitForEntityReplication(UserInfo user, String tableId, String entityId, long maxWaitMS)
			throws InterruptedException;

	/**
	 * Create a view with the default columns for the type.
	 * 
	 * @param user
	 * @param name
	 * @param parentId
	 * @param scope
	 * @param viewTypeMask
	 * @return
	 */
	EntityView createView(UserInfo user, String name, String parentId, List<String> scope, long viewTypeMask);

	/**
	 * If the view available and up-to-date.
	 * @param tableId
	 * @return
	 * @throws TableFailedException
	 */
	Optional<Boolean> isViewAvailableAndUpToDate(IdAndVersion tableId) throws TableFailedException;

	/**
	 * Wait for the given view to be up-to-date.
	 * @param viewId
	 * @param maxWaitMS
	 * @throws InterruptedException
	 * @throws AsynchJobFailedException
	 * @throws TableFailedException
	 */
	void waitForViewToBeUpToDate(IdAndVersion viewId, long maxWaitMS)
			throws InterruptedException, AsynchJobFailedException, TableFailedException;
}
