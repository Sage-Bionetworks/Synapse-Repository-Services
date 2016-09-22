package org.sagebionetworks.repo.manager.table;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Implementation of TableTransactionManager for TableViews.
 * 
 * @author John
 *
 */
public class TableViewTransactionManager implements TableTransactionManager {
	
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	TableViewManager tableViewManger;

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
			TableUpdateResponse result = applyChange(userInfo, change);
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
	TableUpdateResponse applyChange(UserInfo user, TableUpdateRequest change){
		if(change instanceof TableSchemaChangeRequest){
			return applySchemaChange(user, (TableSchemaChangeRequest)change);
		}else{
			throw new IllegalArgumentException("Unsupported TableUpdateRequest: "+change.getClass().getName());
		}
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

}
