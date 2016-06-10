package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.RowBatchHandler;
import org.sagebionetworks.repo.model.table.ColumnModel;

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
			List<String> scope, String viewId);
	
	
	/**
	 * Stream over all entity data of the given type for the given view in batches.  This is used to build the table index.
	 * @param tableId
	 * @param type
	 * @param currentSchema
	 * @param rowsPerBatch
	 * @param rowBatchHandler
	 * @return
	 */
	public Long streamOverAllEntitiesInViewAsBatch(String tableId, EntityType type,
			List<ColumnModel> currentSchema, int rowsPerBatch, RowBatchHandler rowBatchHandler);
	
	
	/**
	 * Get the schema of a FileView.  This schema will include any columns of the view plus the benefactor column.
	 * 
	 * @param viewId
	 * @return
	 */
	 List<ColumnModel> getViewSchemaWithBenefactor(String viewId);

	 /**
	  * Find Views that contain the given Entity.
	  * 
	  * @param objectId
	  * @return
	  */
	public Set<Long> findViewsContainingEntity(String entityId);


}
