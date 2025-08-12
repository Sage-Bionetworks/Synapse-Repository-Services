package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;

public interface PatchPublisher {

	/**
	 * Publish the provided patch. Note: This method will not return until the
	 * provided patch has been accepted.
	 * 
	 * @param connection
	 * @param patchBody
	 * @throws InterruptedException
	 */
	void publishPatch(GridConnectionInfo connection, JSONArray patchBody);

}
