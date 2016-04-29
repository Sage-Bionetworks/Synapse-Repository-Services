package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;

public class FileViewManagerImpl implements FileViewManager {
	

	@Autowired
	ViewScopeDao viewScopeDao;
	@Autowired
	ColumnModelManager columModelManager;
	@Autowired
	TableManagerSupport tableManagerSupport;
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableViewManager#setViewSchemaAndScope(org.sagebionetworks.repo.model.UserInfo, java.util.List, java.util.List, java.lang.String)
	 */
	@WriteTransactionReadCommitted
	@Override
	public void setViewSchemaAndScope(UserInfo userInfo, List<String> schema,
			List<String> scope, String viewIdString) {
		Long viewId = KeyFactory.stringToKey(viewIdString);
		Set<Long> scopeIds = null;
		if(scope != null){
			scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scope));
		}
		// Define the scope of this view.
		viewScopeDao.setViewScope(viewId, scopeIds);
		// Define the schema of this view.
		columModelManager.bindColumnToObject(userInfo, schema, viewIdString);
		// trigger an update
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(viewIdString);
	}

	@Override
	public void streamOverAllFilesInView(String tableIdString, RowHandler handler) {
		long tableId = KeyFactory.stringToKey(tableIdString);
		// Lookup the scope for this view
		Set<Long> allContainersInScope  = tableManagerSupport.getAllContainerIdsForViewScope(tableIdString);
		// Lookup the containers for this scope
		
		
	}


}
