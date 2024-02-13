package org.sagebionetworks.repo.model.dbo.dao.table;

import org.sagebionetworks.repo.model.table.ViewScopeType;

/**
 * Tracks views and their scopes.
 * 
 *
 */
public interface ViewScopeTypeDao {
	
	/**
	 * Associate the scope and type to the view with the given id
	 * 
	 * @param viewId
	 * @param containerIds
	 * @param scopeType
	 */
	void setViewScopeType(Long viewId, ViewScopeType scopeType);
	
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
