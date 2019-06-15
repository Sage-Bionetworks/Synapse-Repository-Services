package org.sagebionetworks.repo.manager.table;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityUpdateFailureCode;
import org.sagebionetworks.repo.model.table.EntityUpdateResult;
import org.sagebionetworks.repo.model.table.EntityUpdateResults;
import org.sagebionetworks.repo.model.table.PartialRowSet;
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
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
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

	@WriteTransaction
	@Override
	public TableUpdateTransactionResponse updateTableWithTransaction(
			ProgressCallback progressCallback, UserInfo userInfo,
			TableUpdateTransactionRequest request)
			throws RecoverableMessageException, TableUnavailableException {
		ValidateArgument.required(progressCallback, "callback");
		ValidateArgument.required(userInfo, "userInfo");
		TableTransactionUtils.validateRequest(request);
		String tableId = request.getEntityId();
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		// Validate the user has permission to edit the table.
		tableManagerSupport.validateTableWriteAccess(userInfo, idAndVersion);
		
		TableUpdateTransactionResponse response = new TableUpdateTransactionResponse();
		List<TableUpdateResponse> results = new LinkedList<>();
		response.setResults(results);
		// process each type
		for(TableUpdateRequest change: request.getChanges()){
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
	TableUpdateResponse applyChange(ProgressCallback progressCallback,
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
	TableUpdateResponse applyRowChange(ProgressCallback progressCallback, UserInfo user,
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
	TableUpdateResponse applyRowChange(ProgressCallback progressCallback, UserInfo user,
			AppendableRowSetRequest change) {
		ValidateArgument.required(change, "AppendableRowSetRequest");
		ValidateArgument.required(change.getToAppend(), "AppendableRowSetRequest.toAppend");
		ValidateArgument.required(change.getEntityId(), "AppendableRowSetRequest.entityId");
		IdAndVersion idAndVersion = IdAndVersion.parse(change.getEntityId());
		List<ColumnModel> currentSchema = tableManagerSupport.getColumnModelsForTable(idAndVersion);
		if(change.getToAppend() instanceof PartialRowSet){
			return applyPartialRowSet(progressCallback, user, currentSchema, (PartialRowSet)change.getToAppend());
		}else if(change.getToAppend() instanceof RowSet){
			return applyRowSet(progressCallback, user, currentSchema, (RowSet)change.getToAppend());
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
	 * @param rowSet
	 * @return
	 */
	TableUpdateResponse applyRowSet(
			ProgressCallback progressCallback, UserInfo user, List<ColumnModel> currentSchema,
			RowSet rowSet) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(currentSchema, "currentSchema");
		ValidateArgument.required(rowSet, "RowSet");
		ValidateArgument.required(rowSet.getTableId(), "RowSet.tableId");
		
		// Validate the request is under the max bytes per requested
		TableModelUtils.validateRequestSize(currentSchema, rowSet, stackConfig.getTableMaxBytesPerRequest());
		// convert
		SparseChangeSet sparseChangeSet = TableModelUtils.createSparseChangeSet(rowSet, currentSchema);
		SparseChangeSetDto dto = sparseChangeSet.writeToDto();
		//process all rows.
		return processRows(user, rowSet.getTableId(), currentSchema, dto.getRows().iterator(), null, progressCallback);
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
	TableUpdateResponse applyPartialRowSet(
			ProgressCallback progressCallback, UserInfo user, List<ColumnModel> currentSchema,
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
		return processRows(user, partialRowSet.getTableId(), currentSchema, dto.getRows().iterator(), null, progressCallback);
	}

	/**
	 * Apply a schema change request.
	 * 
	 * @param request
	 * @return
	 */
	TableSchemaChangeResponse applySchemaChange(UserInfo user, TableSchemaChangeRequest request){
		// view manager applies the change.
		List<ColumnModel> newSchema = tableViewManger.applySchemaChange(user, request.getEntityId(), request.getChanges(), request.getOrderedColumnIds());
		TableSchemaChangeResponse response = new TableSchemaChangeResponse();
		response.setSchema(newSchema);
		return response;
	}

	@Override
	public TableUpdateResponse processRows(UserInfo user, String tableId,
			List<ColumnModel> tableSchema, Iterator<SparseRowDto> rowStream,
			String updateEtag, ProgressCallback progressCallback) {
		List<EntityUpdateResult> results = new LinkedList<EntityUpdateResult>();
		// process all rows, each as a single transaction.
		while(rowStream.hasNext()){
			EntityUpdateResult result = processRow(user, tableSchema,
					rowStream.next(), progressCallback);
			results.add(result);
		}
		EntityUpdateResults response = new EntityUpdateResults();
		response.setUpdateResults(results);
		return response;
	}

	/**
	 * Process a row.
	 * 
	 * @param user
	 * @param tableSchema
	 * @param row
	 * @param progressCallback
	 * @return
	 */
	EntityUpdateResult processRow(UserInfo user,
			List<ColumnModel> tableSchema, SparseRowDto row,
			ProgressCallback progressCallback) {
		EntityUpdateResult result = new EntityUpdateResult();
		try {
			result.setEntityId(KeyFactory.keyToString(row.getRowId()));
			tableViewManger.updateEntityInView(user, tableSchema, row);
		} catch (NotFoundException e) {
			result.setFailureCode(EntityUpdateFailureCode.NOT_FOUND);
		}catch (ConflictingUpdateException e) {
			result.setFailureCode(EntityUpdateFailureCode.CONCURRENT_UPDATE);
		}catch (UnauthorizedException e) {
			result.setFailureCode(EntityUpdateFailureCode.UNAUTHORIZED);
		}catch (IllegalArgumentException e) {
			result.setFailureCode(EntityUpdateFailureCode.ILLEGAL_ARGUMENT);
			result.setFailureMessage(e.getMessage());
		}catch (Exception e) {
			result.setFailureCode(EntityUpdateFailureCode.UNKNOWN);
			result.setFailureMessage(e.getMessage());
		}
		return result;
	}

}
