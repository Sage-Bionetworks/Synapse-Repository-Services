package org.sagebionetworks.grid.db;

import org.sagebionetworks.repo.model.grid.patch.Patch;

public interface GridIndexManager {

	/**
	 * Apply the patch in a transaction.
	 * 
	 * @param patch
	 */
	void applyPatch(String sessionId, Long replicaId, Patch patch);

}
