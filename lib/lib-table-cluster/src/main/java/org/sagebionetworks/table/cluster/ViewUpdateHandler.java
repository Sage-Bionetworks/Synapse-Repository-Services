package org.sagebionetworks.table.cluster;

@FunctionalInterface
public interface ViewUpdateHandler {

	/**
	 * Handle a view update event.
	 * 
	 * @param viewId
	 */
	void handleViewUpdate(Long viewId);
}
