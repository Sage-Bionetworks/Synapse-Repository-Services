package org.sagebionetworks.repo.manager.grid.internal.replica.change;

public interface PatchBuilderPublisher {

	/**
	 * Send a set of intended changes to the patch builder.
	 * 
	 * @param changeSet
	 */
	void sendChangesToPatchBuilder(IntendedChangeSet changeSet);

}
