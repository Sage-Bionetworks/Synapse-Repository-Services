package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;

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

}
