package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowBatchHandler;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dbo.dao.table.FileEntityFields;
import org.sagebionetworks.repo.model.dbo.dao.table.TableViewDao;
import org.sagebionetworks.repo.model.dbo.dao.table.FileViewUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;

public class TableViewManagerImpl implements TableViewManager {
	
	@Autowired
	ViewScopeDao viewScopeDao;
	@Autowired
	ColumnModelManager columModelManager;
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	ColumnModelDAO columnModelDao;
	@Autowired
	TableViewDao fileViewDao;
	
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
	public  List<ColumnModel> getViewSchemaWithBenefactor(String viewId){
		final List<ColumnModel> currentSchema = tableManagerSupport.getColumnModelsForTable(viewId);
		// we need to ensure the benefactor column in included in all tables for the authorization filter.
		if(!FileViewUtils.containsBenefactor(currentSchema)){
			currentSchema.add(tableManagerSupport.getColumModel(FileEntityFields.benefactorId));
		}
		return currentSchema;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.FileViewManager#streamOverAllFilesInViewAsBatch(java.lang.String, java.util.List, org.sagebionetworks.repo.model.dao.table.RowBatchHandler, int)
	 */
	@Override
	public Long streamOverAllEntitiesInViewAsBatch(String tableId, EntityType type,
			List<ColumnModel> currentSchema, final int rowsPerBatch, final RowBatchHandler rowBatchHandler) {
		
		// Get the containers for this view.
		Set<Long> allContainersInScope  = tableManagerSupport.getAllContainerIdsForViewScope(tableId);
		
		// Count the number of expected rows for progress.
		final long totalProgress = fileViewDao.countAllEntitiesInView(allContainersInScope, type);
		
		// This CRC represents the current state of the view.
		Long viewCRC = fileViewDao.calculateCRCForAllEntitiesWithinContainers(allContainersInScope, type);
		// This will contain the batch of rows.
		final List<Row> batchRows = new LinkedList<Row>();
		// copy all FileEntity data to the table
		fileViewDao.streamOverEntities(allContainersInScope, type, currentSchema, new RowHandler() {
			
			long currentProgresss = 0;
			
			@Override
			public void nextRow(Row row) {
				currentProgresss++;
				batchRows.add(row);
				// is the batch read?
				if(batchRows.size() > rowsPerBatch){
					// send the batch
					rowBatchHandler.nextBatch(new LinkedList<Row>(batchRows), currentProgresss, totalProgress);
					batchRows.clear();
				}
			}
		});
		
		// If there are any rows remaining send them as a batch
		if(!batchRows.isEmpty()){
			// send the last batch with progress done.
			rowBatchHandler.nextBatch(new LinkedList<Row>(batchRows), totalProgress, totalProgress);
		}
		return viewCRC;
	}

}
