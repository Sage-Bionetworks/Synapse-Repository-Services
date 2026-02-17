package org.sagebionetworks.repo.manager.grid.synch.core;

public interface CopyItem {
	/**
	 * Determines if this item was modified by the user. Used during synchronization
	 * to decide whether changes should be pushed to the source or pulled from the
	 * source.
	 * 
	 * @return true if the user made changes to this item, false otherwise
	 */
	boolean wasChangedByUser();
}
