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
	 * Get the ViewType for the given table ID.
	 * @param tableId
	 * @return
	 */
	Long getViewTypeMask(Long tableId);
	
	/**
	 * Clear all data in the table.
	 */
	void truncateAll();

}
