package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Set;

/**
 * Tracks views and their scopes.
 * 
 *
 */
public interface ViewScopeDao {
	
	/**
	 * Set the containers that define the scope of a view.
	 * 
	 * @param viewId
	 * @param containerIds
	 */
	void setViewScopeAndType(Long viewId, Set<Long> containerIds, Long viewTypeMask);
	
	/**
	 * Clear all data in the table.
	 */
	void truncateAll();

	/**
	 * Get the scope for the given view.
	 * 
	 * @param viewId
	 * @return
	 */
	Set<Long> getViewScope(Long viewId);

	/**
	 * Get the ViewType for the given table ID.
	 * @param tableId
	 * @return
	 */
	Long getViewTypeMask(Long tableId);
	

}
