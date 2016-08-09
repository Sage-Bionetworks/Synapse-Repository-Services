package org.sagebionetworks.repo.manager.table;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class TableEntityTransactionManager implements TableTransactionManager {
	
	private static final int EXCLUSIVE_LOCK_TIMEOUT_MS = 5*1000;
	
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	TransactionTemplate readCommitedTransactionTemplate;
	@Autowired
	TableEntityManager tableEntityManager;
	@Autowired
	TableIndexConnectionFactory tableIndexConnectionFactory;

	@Override
	public TableUpdateTransactionResponse updateTableWithTransaction(
			final ProgressCallback<Void> progressCallback, final UserInfo userInfo,
			final TableUpdateTransactionRequest request)
			throws RecoverableMessageException, TableUnavailableException {
		
		ValidateArgument.required(progressCallback, "callback");
		ValidateArgument.required(userInfo, "userInfo");
		validateRequest(request);
		String tableId = request.getEntityId();
		// Validate the user has permission to edit the table before locking.
		tableManagerSupport.validateTableWriteAccess(userInfo, tableId);
		try {
			return tableManagerSupport.tryRunWithTableExclusiveLock(progressCallback, tableId, EXCLUSIVE_LOCK_TIMEOUT_MS, new ProgressingCallable<TableUpdateTransactionResponse, Void>() {

				@Override
				public TableUpdateTransactionResponse call(ProgressCallback<Void> callback) throws Exception {
					return updateTableWithTransactionWithExclusiveLock(callback, userInfo, request);
				}
			});
		}catch (TableUnavailableException e) {
			throw e;
		}catch (LockUnavilableException e) {
			throw e;
		}catch (RecoverableMessageException e) {
			throw e;
		}catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Validate a request.
	 * 
	 * @param request
	 */
	public static void validateRequest(TableUpdateTransactionRequest request){
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getEntityId(), "request.entityId");
		ValidateArgument.required(request.getChanges(), "request.changes");
		if(request.getChanges().isEmpty()){
			throw new IllegalArgumentException("Must include be at least one change.");
		}
		String tableId = request.getEntityId();
		for(TableUpdateRequest change: request.getChanges()){
			if(change.getEntityId() == null){
				change.setEntityId(tableId);
			}
			if(!tableId.equals(change.getEntityId())){
				throw new IllegalArgumentException("EntityId of TableUpdateRequest does not match the requested transaction entityId");
			}
		}
	}
	
	/**
	 * This method should only be called from while holding the lock on the table.
	 * @param callback
	 * @param userInfo
	 * @param request
	 * @return
	 */
	TableUpdateTransactionResponse updateTableWithTransactionWithExclusiveLock(
			final ProgressCallback<Void> callback, final UserInfo userInfo,
			final TableUpdateTransactionRequest request) {
		/*
		 * Request validation can take a long time and may not involve the primary database.
		 * Therefore the primary transaction is not started until after validation succeeds.
		 * A transaction template is used to allow for finer control of the transaction boundary.
		 */
		validateUpdateRequests(callback, userInfo, request);
		// the update is valid so the primary transaction can be started.
		return readCommitedTransactionTemplate.execute(new TransactionCallback<TableUpdateTransactionResponse>() {

			@Override
			public TableUpdateTransactionResponse doInTransaction(
					TransactionStatus status) {
				return doIntransactionUpdateTable(status, callback, userInfo, request);
			}
		} );
	}


	/**
	 * Validate the passed update request.
	 * @param callback
	 * @param userInfo
	 * @param request
	 */
	void validateUpdateRequests(ProgressCallback<Void> callback,
			UserInfo userInfo, TableUpdateTransactionRequest request) {

		// Determine if a temporary table is needed to validate any of the requests.
		boolean isTemporaryTableNeeded = isTemporaryTableNeeded(callback, request);
		
		// setup a temporary table if needed.
		if(isTemporaryTableNeeded){
			TableIndexManager indexManager = tableIndexConnectionFactory.connectToTableIndex(request.getEntityId());
			indexManager.createTemporaryTableCopy(callback);
			try{
				// validate while the temp table exists.
				validateEachUpdateRequest(callback, userInfo, request, indexManager);
			}finally{
				indexManager.deleteTemporaryTableCopy(callback);
			}
		}else{
			// we do not need a temporary copy to validate this request.
			validateEachUpdateRequest(callback, userInfo, request, null);
		}
	}


	/**
	 * Validate each update request.
	 * @param callback
	 * @param userInfo
	 * @param request
	 * @param indexManager
	 */
	void validateEachUpdateRequest(ProgressCallback<Void> callback,
			UserInfo userInfo, TableUpdateTransactionRequest request,
			TableIndexManager indexManager) {
		for(TableUpdateRequest change: request.getChanges()){
			// progress before each check.
			callback.progressMade(null);
			tableEntityManager.validateUpdateRequest(callback, userInfo, change, indexManager);
		}
	}

	/**
	 * Is a temporary table needed to validate any of the changes for the given request.
	 * 
	 * @param callback
	 * @param request
	 * @return
	 */
	boolean isTemporaryTableNeeded(ProgressCallback<Void> callback,
			TableUpdateTransactionRequest request) {
		for(TableUpdateRequest change: request.getChanges()){
			// progress before each check.
			callback.progressMade(null);
			boolean tempNeeded = tableEntityManager.isTemporaryTableNeededToValidate(change);
			if(tempNeeded){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Called after the update has been validated and from within a transaction.
	 * 
	 * @param status
	 * @param callback
	 * @param userInfo
	 * @param request
	 * @return
	 */
	TableUpdateTransactionResponse doIntransactionUpdateTable(TransactionStatus status,
			ProgressCallback<Void> callback, UserInfo userInfo,
			TableUpdateTransactionRequest request) {
		// execute each request
		List<TableUpdateResponse> results = new LinkedList<TableUpdateResponse>();
		TableUpdateTransactionResponse response = new TableUpdateTransactionResponse();
		response.setResults(results);
		for(TableUpdateRequest change: request.getChanges()){
			callback.progressMade(null);
			TableUpdateResponse changeResponse = tableEntityManager.updateTable(callback, userInfo, change);
			results.add(changeResponse);
		}
		return response;
	}

}
