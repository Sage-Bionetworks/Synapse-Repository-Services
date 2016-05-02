package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.RowHandler;

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

}
