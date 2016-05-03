package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dbo.dao.table.FileEntityFields;
import org.sagebionetworks.repo.model.table.ColumnModel;

/**
 * Business logic for materialized table views.
 *
 */
public interface FileViewManager {

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
	 * Stream over all file data for the given view.  This is used to build the table index.
	 * 
	 * @param tableId
	 * @param handler
	 */
	public void streamOverAllFilesInView(String tableId, RowHandler handler);


	/**
	 * Get the ColumnModel for a given FileEntityField.
	 * 
	 * @param field
	 * @return
	 */
	public ColumnModel getColumModel(FileEntityFields field);
	
	/**
	 * Get the default ColumnModels for each primary filed of FileEntity.
	 * 
	 * @return
	 */
	public List<ColumnModel> getDefaultFileEntityColumns();

}
