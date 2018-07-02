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
	public void setViewScopeAndType(Long viewId, Set<Long> containerIds, Long viewTypeMask);
	
	/**
	 * Find all views with a scope that intersects the given path.
	 * 
	 * @param path
	 * @return
	 */
	public Set<Long> findViewScopeIntersectionWithPath(Set<Long> path);
	
	/**
	 * Clear all data in the table.
	 */
	public void truncateAll();

	/**
	 * Get the scope for the given view.
	 * 
	 * @param viewId
	 * @return
	 */
	public Set<Long> getViewScope(Long viewId);

	/**
	 * Get the ViewType for the given table ID.
	 * @param tableId
	 * @return
	 */
	public Long getViewTypeMask(Long tableId);
	

}
