package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;

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
	void setViewSchemaAndScope(UserInfo userInfo, List<String> schema,
			ViewScope scope, String viewId);


	/**
	 * Get the view schema with the required columns including id, version, and benefactorId.
	 * 
	 * @param tableId
	 * @return
	 */
	List<ColumnModel> getViewSchema(IdAndVersion idAndVersion);
	
	/**
	 * Get the column IDs for the given id and version pair.
	 * 
	 * @param idAndVersion
	 * @return
	 */
	List<String> getViewSchemaIds(IdAndVersion idAndVersion);


	/**
	 * Apply the passed schema change to the passed view.
	 * 
	 * @param viewId 
	 * @param user 
	 * @param changes
	 * @param orderedColumnIds 
	 * @return
	 */
	List<ColumnModel> applySchemaChange(UserInfo user, String viewId, List<ColumnChange> changes, List<String> orderedColumnIds);

	/**
	 * Update a single entity in a view using the passed row and schema.
	 * 
	 * @param user
	 * @param tableSchema
	 * @param row
	 */
	void updateRowInView(UserInfo user, List<ColumnModel> tableSchema, ViewObjectType objectType, SparseRowDto row);

	/**
	 * Create a snapshot of the given view.
	 * 
	 * @param userInfo
	 * @param tableId
	 * @param snapshotOptions
	 * @return
	 */
	long createSnapshot(UserInfo userInfo, String tableId, SnapshotRequest snapshotOptions);

	/**
	 * Delete the index associated with this view.
	 * @param idAndVersion
	 */
	void deleteViewIndex(IdAndVersion idAndVersion);

	/**
	 * Create or update the index for the given view.
	 * @param idAndVersion
	 * @param progressCallback
	 * @throws Exception
	 */
	void createOrUpdateViewIndex(IdAndVersion idAndVersion, ProgressCallback progressCallback) throws Exception;

}
