package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;

public class TableViewManagerImpl implements TableViewManager {
	
	private static final String ETAG = "";
	@Autowired
	TableStatusDAO tableStatusDAO;
	@Autowired
	ViewScopeDao viewScopeDao;
	@Autowired
	ColumnModelManager columModelManager;
	@Autowired
	TransactionalMessenger transactionalMessenger;

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableViewManager#setViewSchemaAndScope(org.sagebionetworks.repo.model.UserInfo, java.util.List, java.util.List, java.lang.String)
	 */
	@WriteTransactionReadCommitted
	@Override
	public void setViewSchemaAndScope(UserInfo userInfo, List<String> schema,
			List<String> scope, String viewIdString) {
		Long viewId = KeyFactory.stringToKey(viewIdString);
		Set<Long> scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scope));
		// Define the scope of this view.
		viewScopeDao.setViewScope(viewId, scopeIds);
		// Define the schema of this view.
		columModelManager.bindColumnToObject(userInfo, schema, viewIdString);
		// Set the status of the view to processing.
		tableStatusDAO.resetTableStatusToProcessing(viewIdString);
		// Notify all listeners of the change.
		transactionalMessenger.sendMessageAfterCommit(viewIdString, ObjectType.FILE_VIEW, ETAG,ChangeType.UPDATE, userInfo.getId());
	}


}
