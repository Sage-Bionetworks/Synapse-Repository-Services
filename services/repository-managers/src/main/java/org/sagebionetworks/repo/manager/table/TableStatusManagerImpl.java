package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.util.TimeoutUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class TableStatusManagerImpl implements TableStatusManager {
	
	public static final long TABLE_PROCESSING_TIMEOUT_MS = 1000*60*10; // 10 mins

	@Autowired
	TableStatusDAO tableStatusDAO;
	@Autowired
	TimeoutUtils timeoutUtils;
	@Autowired
	TransactionalMessenger transactionalMessenger;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	TableTruthManager tableTruthManager;
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableRowManager#getTableStatusOrCreateIfNotExists(java.lang.String)
	 */
	@WriteTransactionReadCommitted
	@Override
	public TableStatus getTableStatusOrCreateIfNotExists(String tableId) throws NotFoundException {
		try {
			TableStatus status = tableStatusDAO.getTableStatus(tableId);
			if(!TableState.AVAILABLE.equals(status.getState())){
				// Processing or Failed.
				// Is progress being made?
				if(timeoutUtils.hasExpired(TABLE_PROCESSING_TIMEOUT_MS, status.getChangedOn().getTime())){
					// progress has not been made so trigger another update
					return setTableToProcessingAndTriggerUpdate(tableId);
				}else{
					// progress has been made so just return the status
					return status;
				}
			}
			// Status is Available, is the index synchronized with the truth?
			if(isIndexSynchronizedWithTruth(tableId)){
				// Available and synchronized.
				return status;
			}else{
				// Available but not synchronized, so change the state to processing.
				return setTableToProcessingAndTriggerUpdate(tableId);
			}
			
		} catch (NotFoundException e) {
			// make sure the table exists
			if (!tableTruthManager.isTableAvailable(tableId)) {
				throw new NotFoundException("Table " + tableId + " not found");
			}
			return setTableToProcessingAndTriggerUpdate(tableId);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#setTableToProcessingAndTriggerUpdate(java.lang.String)
	 */
	@WriteTransactionReadCommitted
	@Override
	public TableStatus setTableToProcessingAndTriggerUpdate(String tableId) {
		// we get here, if the index for this table is not (yet?) being build. We need to kick off the
		// building of the index and report the table as unavailable
		String token = tableStatusDAO.resetTableStatusToProcessing(tableId);
		// lookup the table type.
		ObjectType tableType = tableTruthManager.getTableType(tableId);
		// notify all listeners.
		transactionalMessenger.sendMessageAfterCommit(tableId, tableType, token, ChangeType.UPDATE);
		// status should exist now
		return tableStatusDAO.getTableStatus(tableId);
	}

	@WriteTransactionReadCommitted
	@Override
	public void attemptToSetTableStatusToAvailable(String tableId,
			String resetToken, String tableChangeEtag) throws ConflictingUpdateException,
			NotFoundException {
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, resetToken, tableChangeEtag);
	}

	@WriteTransactionReadCommitted
	@Override
	public void attemptToSetTableStatusToFailed(String tableId,
			String resetToken, String errorMessage, String errorDetails)
			throws ConflictingUpdateException, NotFoundException {
		tableStatusDAO.attemptToSetTableStatusToFailed(tableId, resetToken, errorMessage, errorDetails);
	}

	@WriteTransactionReadCommitted
	@Override
	public void attemptToUpdateTableProgress(String tableId, String resetToken,
			String progressMessage, Long currentProgress, Long totalProgress)
			throws ConflictingUpdateException, NotFoundException {
		tableStatusDAO.attemptToUpdateTableProgress(tableId, resetToken, progressMessage, currentProgress, totalProgress);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#startTableProcessing(java.lang.String)
	 */
	@WriteTransactionReadCommitted
	@Override
	public String startTableProcessing(String tableId) {
		return tableStatusDAO.resetTableStatusToProcessing(tableId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#isIndexSynchronizedWithTruth(java.lang.String)
	 */
	@Override
	public boolean isIndexSynchronizedWithTruth(String tableId) {
		// MD5 of the table's schema
		String truthSchemaMD5Hex = tableTruthManager.getSchemaMD5Hex(tableId);
		// get the truth version
		long truthLastVersion = tableTruthManager.getTableVersion(tableId);
		// compare the truth with the index.
		return this.tableConnectionFactory.getConnection(tableId).doesIndexStateMatch(tableId, truthLastVersion, truthSchemaMD5Hex);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#isIndexWorkRequired(java.lang.String)
	 */
	@Override
	public boolean isIndexWorkRequired(String tableId) {
		// Does the table exist and not in the trash?
		if(!tableTruthManager.isTableAvailable(tableId)){
			return false;
		}
		// work is needed if the index is out-of-sych.
		if(!isIndexSynchronizedWithTruth(tableId)){
			return true;
		}
		// work is needed if the current state is processing.
		TableStatus status = tableStatusDAO.getTableStatus(tableId);
		return TableState.PROCESSING.equals(status.getState());
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#setTableDeleted(java.lang.String)
	 */
	@WriteTransactionReadCommitted
	@Override
	public void setTableDeleted(String deletedId) {
		ObjectType tableType = tableTruthManager.getTableType(deletedId);
		transactionalMessenger.sendMessageAfterCommit(deletedId, tableType, ChangeType.DELETE);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#validateTableIsAvailable(java.lang.String)
	 */
	@Override
	public TableStatus validateTableIsAvailable(String tableId) throws NotFoundException, TableUnavilableException, TableFailedException {
		final TableStatus status = getTableStatusOrCreateIfNotExists(tableId);
		switch(status.getState()){
		case AVAILABLE:
			return status;
		case PROCESSING:
			// When the table is not available, we communicate the current status of the
			// table in this exception.
			throw new TableUnavilableException(status);
		default:
		case PROCESSING_FAILED:
			// When the table is in a failed state, we communicate the current status of the
			// table in this exception.
			throw new TableFailedException(status);
		}
	}

}
