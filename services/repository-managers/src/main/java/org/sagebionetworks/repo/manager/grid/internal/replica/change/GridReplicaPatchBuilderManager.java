package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.io.IOException;
import java.util.Optional;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface GridReplicaPatchBuilderManager {

	/**
	 * Build a new patch (or patches) for the provided change set.
	 * @throws IOException 
	 */
	void buildPatch(IntendedChangeSet changeSet) throws IOException;


    /**
     * Retrieve the current clock for this session. This method references the patch database and will return an empty optional
     * if there are outstanding patches to apply.
     * @param sessionId
     * @param replicaId
     * @return
     */
    Optional<LogicalTimestamp> getCurrentClockIfAllPatchesApplied(String sessionId, Long replicaId);
}
