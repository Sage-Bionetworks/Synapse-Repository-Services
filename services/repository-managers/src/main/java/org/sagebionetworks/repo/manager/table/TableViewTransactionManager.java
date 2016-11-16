package org.sagebionetworks.repo.manager.table;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SparseChangeSetDto;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Implementation of TableTransactionManager for TableViews.
 * 
 * @author John
 *
 */
public class TableViewTransactionManager implements TableTransactionManager, UploadRowProcessor {
	
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	TableViewManager tableViewManger;
	@Autowired
	TableUploadManager tableUploadManager;
	@Autowired
	StackConfiguration stackConfig;

	@WriteTransactionReadCommitted
	@Override
	public TableUpdateTransactionResponse updateTableWithTransaction(
			ProgressCallback<Void> progressCallback, UserInfo userInfo,
			TableUpdateTransactionRequest request)
			throws RecoverableMessageException, TableUnavailableException {
		ValidateArgument.required(progressCallback, "callback");
		ValidateArgument.required(userInfo, "userInfo");
		TableTransactionUtils.validateRequest(request);
		String tableId = request.getEntityId();
		// Validate the user has permission to edit the table.
		tableManagerSupport.validateTableWriteAccess(userInfo, tableId);
		
		TableUpdateTransactionResponse response = new TableUpdateTransactionResponse();
		List<TableUpdateResponse> results = new LinkedList<>();
		response.setResults(results);
		// process each type
		for(TableUpdateRequest change: request.getChanges()){
			// make progress for each request
			progressCallback.progressMade(null);
			TableUpdateResponse result = applyChange(progressCallback, userInfo, change);
			results.add(result);
		}
		return response;
	}
	
	/**
	 * Apply each change within a transaction.
	 * 
	 * @param change
	 * @return
	 */
	TableUpdateResponse applyChange(ProgressCallback<Void> progressCallback,
			UserInfo user, TableUpdateRequest change) {
		if (change instanceof TableSchemaChangeRequest) {
			return applySchemaChange(user, (TableSchemaChangeRequest) change);
		} else if (change instanceof AppendableRowSetRequest) {
			return applyRowChange(progressCallback, user, (AppendableRowSetRequest)change);
		}  else if (change instanceof UploadToTableRequest) {
			return applyRowChange(progressCallback, user, (UploadToTableRequest)change);
		} else {
			throw new IllegalArgumentException("Unsupported TableUpdateRequest: "+ change.getClass().getName());
		}
	}
	
	/**
	 * CSV upload to a view.
	 * @param progressCallback
	 * @param user
	 * @param change
	 * @return
	 */
	UploadToTableResult applyRowChange(ProgressCallback<Void> progressCallback, UserInfo user,
			UploadToTableRequest change) {
		return tableUploadManager.uploadCSV(progressCallback, user, change, this);
	}

	/**
	 * RowSet or PartialRowSet append to a view.
	 * 
	 * @param progressCallback
	 * @param user
	 * @param change
	 * @return
	 */
	RowReferenceSetResults applyRowChange(ProgressCallback<Void> progressCallback, UserInfo user,
			AppendableRowSetRequest change) {
		ValidateArgument.required(change, "AppendableRowSetRequest");
		ValidateArgument.required(change.getToAppend(), "AppendableRowSetRequest.toAppend");
		ValidateArgument.required(change.getEntityId(), "AppendableRowSetRequest.entityId");
		List<ColumnModel> currentSchema = tableManagerSupport.getColumnModelsForTable(change.getEntityId());
		if(change.getToAppend() instanceof PartialRowSet){
			return applyRowChagne(progressCallback, user, currentSchema, (PartialRowSet)change.getToAppend());
		}else if(change.getToAppend() instanceof RowSet){
			return applyRowChagne(progressCallback, user, currentSchema, (RowSet)change.getToAppend());
		}else{
			throw new IllegalArgumentException("Unknown AppendableRowSet type: "+change.getToAppend().getClass().getName());
		}
	}

	/**
	 * Append a RowSet to the view.
	 * 
	 * @param progressCallback
	 * @param user
	 * @param currentSchema
	 * @param toAppend
	 * @return
	 */
	RowReferenceSetResults applyRowChagne(
			ProgressCallback<Void> progressCallback, UserInfo user, List<ColumnModel> currentSchema,
			RowSet toAppend) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(currentSchema, "currentSchema");
		ValidateArgument.required(toAppend, "RowSet");
		ValidateArgument.required(toAppend.getTableId(), "RowSet.tableId");
		
		// Validate the request is under the max bytes per requested
		TableModelUtils.validateRequestSize(currentSchema, toAppend, stackConfig.getTableMaxBytesPerRequest());
		// convert
		SparseChangeSet sparseChangeSet = TableModelUtils.createSparseChangeSet(toAppend, currentSchema);
		SparseChangeSetDto dto = sparseChangeSet.writeToDto();
		//process all rows.
		processRows(user, toAppend.getTableId(), currentSchema, dto.getRows().iterator(), null, progressCallback);
		
		RowReferenceSet refSet = new RowReferenceSet();
		refSet.setTableId(toAppend.getTableId());
		RowReferenceSetResults results = new RowReferenceSetResults();
		results.setRowReferenceSet(refSet);
		return results;
	}

	/**
	 * Append a PartialRowSet to the view.
	 * 
	 * @param progressCallback
	 * @param user
	 * @param currentSchema
	 * @param partialRowSet
	 * @return
	 */
	RowReferenceSetResults applyRowChagne(
			ProgressCallback<Void> progressCallback, UserInfo user, List<ColumnModel> currentSchema,
			PartialRowSet partialRowSet) {
		ValidateArgument.required("user", "UserInfo");
		ValidateArgument.required(currentSchema, "currentSchema");
		ValidateArgument.required(partialRowSet, "PartialRowSet");
		ValidateArgument.required(partialRowSet.getTableId(), "partialRowSet.tableId");
		
		// Validate the request is under the max bytes per requested
		TableModelUtils.validateRequestSize(partialRowSet, stackConfig.getTableMaxBytesPerRequest());
		TableModelUtils.validatePartialRowSet(partialRowSet, currentSchema);
		// convert
		SparseChangeSetDto dto = TableModelUtils.createSparseChangeSetFromPartialRowSet(null, partialRowSet);
		//process all rows.
		processRows(user, partialRowSet.getTableId(), currentSchema, dto.getRows().iterator(), null, progressCallback);
		
		RowReferenceSet refSet = new RowReferenceSet();
		refSet.setTableId(partialRowSet.getTableId());
		RowReferenceSetResults results = new RowReferenceSetResults();
		results.setRowReferenceSet(refSet);
		return results;
	}

	/**
	 * Apply a schema change request.
	 * 
	 * @param request
	 * @return
	 */
	TableSchemaChangeResponse applySchemaChange(UserInfo user, TableSchemaChangeRequest request){
		// view manager applies the change.
		List<ColumnModel> newSchema = tableViewManger.applySchemaChange(user, request.getEntityId(), request.getChanges());
		TableSchemaChangeResponse response = new TableSchemaChangeResponse();
		response.setSchema(newSchema);
		return response;
	}

	@Override
	public String processRows(UserInfo user, String tableId,
			List<ColumnModel> tableSchema, Iterator<SparseRowDto> rowStream,
			String updateEtag, ProgressCallback<Void> progressCallback) {
		RuntimeException firstException = null;
		// process all rows, each as a single transaction.
		while(rowStream.hasNext()){
			try {
				progressCallback.progressMade(null);
				SparseRowDto row = rowStream.next();
				tableViewManger.updateEntityInView(user, tableSchema, row);
			} catch (Exception e) {
				if(firstException != null){
					firstException = new RuntimeException(e);
				}
			}
		}
		// Throw the first exception that occurred.
		if(firstException != null){
			throw firstException;
		}
		return null;
	}

}
