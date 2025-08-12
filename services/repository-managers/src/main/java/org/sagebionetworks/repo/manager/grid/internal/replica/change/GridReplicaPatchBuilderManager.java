package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.io.IOException;

public interface GridReplicaPatchBuilderManager {

	/**
	 * Build a new patch (or patches) for the provided change set.
	 * @throws IOException 
	 */
	void buildPatch(IntendedChangeSet changeSet) throws IOException;

}
