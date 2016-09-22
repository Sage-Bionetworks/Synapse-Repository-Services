package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.RowBatchHandler;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ViewType;

/**
 * Business logic for materialized table views.
 *
 */
public interface TableViewManager {

	/**
	 * Set the schema and scope for a file view.
	 * @param userInfo
	 * @param schema
	 * @param scope
	 * @param viewId
	 */
	public void setViewSchemaAndScope(UserInfo userInfo, List<String> schema,
			List<String> scope, ViewType type, String viewId);
	
	 /**
	  * Find Views that contain the given Entity.
	  * 
	  * @param objectId
	  * @return
	  */
	public Set<Long> findViewsContainingEntity(String entityId);


	/**
	 * Get the view schema with the required columns including id, version, and benefactorId.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<ColumnModel> getViewSchemaWithRequiredColumns(String tableId);


	/**
	 * Apply the passed schema change to the passed view.
	 * 
	 * @param viewId 
	 * @param user 
	 * @param changes
	 * @return
	 */
	public List<ColumnModel> applySchemaChange(UserInfo user, String viewId, List<ColumnChange> changes);


}
