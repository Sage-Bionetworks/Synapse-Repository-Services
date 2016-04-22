package org.sagebionetworks.repo.manager.table;

import java.util.Set;

public interface TableViewTruthManager {
	
	/**
	 * Calculate a Cyclic Redundancy Check (CRC) of a TableView.
	 * The CRC is calculated as SUM(CRC23(CONCAT(ID, '-', ETAG)))
	 * given the ID and ETAG of each entity within the view's scope.
	 * 
	 * Warning this call is not cheap.
	 * 
	 * @param table
	 * @return
	 */
	public Long calculateTableViewCRC(String table);
	
	/**
	 * Get the set of container ids (Projects and Folders) for a view's scope.
	 * The resulting set will include the scope containers plus all folders
	 * contained within each scope.
	 * 
	 * All FileEntities within the the given view will have a parentId from the
	 * returned set.
	 * 
	 * @param viewId
	 * @return
	 */
	public Set<Long> getAllContainerIdsForViewScope(String viewId);

}
