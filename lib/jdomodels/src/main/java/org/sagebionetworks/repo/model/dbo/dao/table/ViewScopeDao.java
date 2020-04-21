package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Set;

import org.sagebionetworks.repo.model.table.ViewScopeType;

/**
 * Tracks views and their scopes.
 * 
 *
 */
public interface ViewScopeDao {
	
	/**
	 * Associate the scope and type to the view with the given id
	 * 
	 * @param viewId
	 * @param containerIds
	 * @param scopeType
	 */
	void setViewScopeAndType(Long viewId, Set<Long> containerIds, ViewScopeType scopeType);

	/**
	 * Get the scope for the given view.
	 * 
	 * @param viewId
	 * @return
	 */
	Set<Long> getViewScope(Long viewId);
	
	/**
	 * Get the scope type for view with the given id, will include both the objectType and the type mask
	 * 
	 * @param viewId
	 * @return
	 */
	ViewScopeType getViewScopeType(Long viewId);
	
	/**
	 * Clear all data in the table.
	 */
	void truncateAll();

}
