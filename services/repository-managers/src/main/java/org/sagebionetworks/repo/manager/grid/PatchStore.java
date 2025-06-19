package org.sagebionetworks.repo.manager.grid;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface PatchStore {
	
	/**
	 * Save a patch.
	 * 
	 * @param sessionId
	 * @param patchId
	 * @param body
	 * @return
	 */
	boolean savePatch(String sessionId, LogicalTimestamp patchId, String body);
}
