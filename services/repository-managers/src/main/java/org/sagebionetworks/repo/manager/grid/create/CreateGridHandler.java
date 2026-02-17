package org.sagebionetworks.repo.manager.grid.create;

import org.sagebionetworks.repo.manager.grid.PatchStore;
import org.sagebionetworks.repo.manager.grid.SnapshotStore;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;

public interface CreateGridHandler {

	/**
	 * Can this handler create the grid session?
	 * 
	 * @param request
	 * @return
	 */
	boolean canCreate(CreateGridRequest request);

	/**
	 * Create a grid session from the provided request.
	 * 
	 * @param callback
	 * @param user
	 * @param request
	 * @return
	 */
	CreateGridHandlerResult createGrid(AsyncJobProgressCallback callback, UserInfo user, CreateGridRequest request,
			SnapshotStore snapshotStore);

}
