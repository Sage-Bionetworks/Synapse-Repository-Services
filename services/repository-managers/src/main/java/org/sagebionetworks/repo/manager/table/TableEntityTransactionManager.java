package org.sagebionetworks.repo.manager.table;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransactionDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
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
	
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	TransactionTemplate readCommitedTransactionTemplate;
	@Autowired
	TableEntityManager tableEntityManager;
	@Autowired
	TableIndexConnectionFactory tableIndexConnectionFactory;
	@Autowired
	TableTransactionDao transactionDao;

	@Override
	public TableUpdateTransactionResponse updateTableWithTransaction(
			final ProgressCallback progressCallback, final UserInfo userInfo,
			final TableUpdateTransactionRequest request)
			throws RecoverableMessageException, TableUnavailableException {
		
		ValidateArgument.required(progressCallback, "callback");
		ValidateArgument.required(userInfo, "userInfo");
		TableTransactionUtils.validateRequest(request);
		String tableId = request.getEntityId();
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		// Validate the user has permission to edit the table before locking.
		tableManagerSupport.validateTableWriteAccess(userInfo, idAndVersion);
		try {
			return tableManagerSupport.tryRunWithTableExclusiveLock(progressCallback, idAndVersion, new ProgressingCallable<TableUpdateTransactionResponse>() {

				@Override
				public TableUpdateTransactionResponse call(ProgressCallback callback) throws Exception {
					return updateTableWithTransactionWithExclusiveLock(callback, userInfo, request);
				}
			});
		} catch (TableUnavailableException | RecoverableMessageException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
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
			final ProgressCallback callback, final UserInfo userInfo,
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
	void validateUpdateRequests(ProgressCallback callback,
			UserInfo userInfo, TableUpdateTransactionRequest request) {

		// Determine if a temporary table is needed to validate any of the requests.
		boolean isTemporaryTableNeeded = isTemporaryTableNeeded(callback, request);
		
		// setup a temporary table if needed.
		if(isTemporaryTableNeeded){
			String tableId = request.getEntityId();
			IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
			TableIndexManager indexManager = tableIndexConnectionFactory.connectToTableIndex(idAndVersion);
			indexManager.createTemporaryTableCopy(idAndVersion);
			try{
				// validate while the temp table exists.
				validateEachUpdateRequest(callback, userInfo, request, indexManager);
			}finally{
				indexManager.deleteTemporaryTableCopy(idAndVersion);
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
	void validateEachUpdateRequest(ProgressCallback callback,
			UserInfo userInfo, TableUpdateTransactionRequest request,
			TableIndexManager indexManager) {
		for(TableUpdateRequest change: request.getChanges()){
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
	boolean isTemporaryTableNeeded(ProgressCallback callback,
			TableUpdateTransactionRequest request) {
		for(TableUpdateRequest change: request.getChanges()){
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
			ProgressCallback callback, UserInfo userInfo,
			TableUpdateTransactionRequest request) {
		// Start a new table transaction and get a transaction number.
		long transactionId = transactionDao.startTransaction(request.getEntityId(), userInfo.getId());
		// execute each request
		List<TableUpdateResponse> results = new LinkedList<TableUpdateResponse>();
		TableUpdateTransactionResponse response = new TableUpdateTransactionResponse();
		response.setResults(results);
		if(request.getChanges() != null) {
			for(TableUpdateRequest change: request.getChanges()){
				TableUpdateResponse changeResponse = tableEntityManager.updateTable(callback, userInfo, change, transactionId);
				results.add(changeResponse);
			}
		}
		if (request.getCreateSnapshot() != null
				&& Boolean.TRUE.equals(request.getCreateSnapshot())) {
			Long snapshotVersion = tableEntityManager.createSnapshotAndBindToTransaction(userInfo, request.getEntityId(),
					request.getSnapshotOptions(), transactionId);
			response.setSnapshotVersionNumber(snapshotVersion);
		}
		return response;
	}

}
