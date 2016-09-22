package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class TableViewManagerImpl implements TableViewManager {
	
	public static final int MAX_CONTAINERS_PER_VIEW = 1000;
	public static final String MAX_CONTAINER_MESSAGE = "The provided view scope includes: %1$S containers which exceeds the maximum number of "+MAX_CONTAINERS_PER_VIEW;
	
	@Autowired
	ViewScopeDao viewScopeDao;
	@Autowired
	ColumnModelManager columModelManager;
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	ColumnModelDAO columnModelDao;
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableViewManager#setViewSchemaAndScope(org.sagebionetworks.repo.model.UserInfo, java.util.List, java.util.List, java.lang.String)
	 */
	@WriteTransactionReadCommitted
	@Override
	public void setViewSchemaAndScope(UserInfo userInfo, List<String> schema,
			List<String> scope, ViewType type, String viewIdString) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(type, "viewType");
		
		Long viewId = KeyFactory.stringToKey(viewIdString);
		Set<Long> scopeIds = null;
		if(scope != null){
			scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scope));
		}
		// validate the scope size
		int scopeContainerCount  = tableManagerSupport.getScopeContainerCount(scopeIds);
		if(scopeContainerCount > MAX_CONTAINERS_PER_VIEW){
			throw new IllegalArgumentException(String.format(MAX_CONTAINER_MESSAGE, scopeContainerCount));
		}
		
		// Define the scope of this view.
		viewScopeDao.setViewScopeAndType(viewId, scopeIds, type);
		// Define the schema of this view.
		columModelManager.bindColumnToObject(userInfo, schema, viewIdString);
		// trigger an update
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(viewIdString);
	}

	@Override
	public Set<Long> findViewsContainingEntity(String entityId) {
		Set<Long> entityPath = tableManagerSupport.getEntityPath(entityId);
		return viewScopeDao.findViewScopeIntersectionWithPath(entityPath);
	}

	@Override
	public List<ColumnModel> getViewSchemaWithRequiredColumns(String tableId) {
		List<ColumnModel> currentSchema = tableManagerSupport.getColumnModelsForTable(tableId);
		/* Set of required column names.
		 * Note: Using a list instead of a HashSet because the list only contains two elements.
		 */
		List<ColumnModel> requiredFields = tableManagerSupport.getColumnModels(
				EntityField.etag,
				EntityField.benefactorId
				);
		// remove the the columns that already exist
		for(ColumnModel cm: currentSchema){
			requiredFields.remove(cm);
		}
		// Add anything still missing
		for(ColumnModel required: requiredFields){
			currentSchema.add(required);
		}
		return currentSchema;
	}

	@WriteTransactionReadCommitted
	@Override
	public List<ColumnModel> applySchemaChange(UserInfo user, String viewId,
			List<ColumnChange> changes) {
		// first determine what the new Schema will be
		List<String> newSchemaIds = columModelManager.calculateNewSchemaIdsAndValidate(viewId, changes);
		columModelManager.bindColumnToObject(user, newSchemaIds, viewId);
		boolean keepOrder = true;
		List<ColumnModel> newSchema = columModelManager.getColumnModel(user, newSchemaIds, keepOrder);
		// trigger an update.
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(viewId);
		return newSchema;
	}

}
