package org.sagebionetworks.repo.manager.grid;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface PatchStore {
	
	/**
	 * Save a patch.
	 * 
	 * @param sessionId
	 * @param patchId
	 * @param body
	 * @return True if this is a new patch. False if an identical patch has been
	 *         saved before.
	 * @throws IllegalArgumentException If a patch with the same ID but a
	 *                                  different body has already been saved.
	 */
	boolean savePatch(String sessionId, LogicalTimestamp patchId, String body);
}
