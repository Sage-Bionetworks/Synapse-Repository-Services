package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
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
	
	/**
	 * Set the table's status to be PROCESSING, fire a table update and return the table's status.
	 * @param tableId
	 * @return
	 */
	public TableStatus setTableToProcessingAndTriggerUpdate(String tableId) {
		// we get here, if the index for this table is not (yet?) being build. We need to kick off the
		// building of the index and report the table as unavailable
		tableStatusDAO.resetTableStatusToProcessing(tableId);
		// notify all listeners.
		transactionalMessenger.sendMessageAfterCommit(tableId, ObjectType.TABLE, "", ChangeType.UPDATE);
		// status should exist now
		return tableStatusDAO.getTableStatus(tableId);
	}

	@Override
	public void attemptToSetTableStatusToAvailable(String tableId,
			String resetToken, String tableChangeEtag) throws ConflictingUpdateException,
			NotFoundException {
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, resetToken, tableChangeEtag);
	}

	@Override
	public void attemptToSetTableStatusToFailed(String tableId,
			String resetToken, String errorMessage, String errorDetails)
			throws ConflictingUpdateException, NotFoundException {
		tableStatusDAO.attemptToSetTableStatusToFailed(tableId, resetToken, errorMessage, errorDetails);
	}

	@Override
	public void attemptToUpdateTableProgress(String tableId, String resetToken,
			String progressMessage, Long currentProgress, Long totalProgress)
			throws ConflictingUpdateException, NotFoundException {
		tableStatusDAO.attemptToUpdateTableProgress(tableId, resetToken, progressMessage, currentProgress, totalProgress);
	}
	
	@Override
	public String startTableProcessing(String tableId) {
		return tableStatusDAO.resetTableStatusToProcessing(tableId);
	}


	@Override
	public boolean isIndexSynchronizedWithTruth(String tableId) {
		// Get the truth schema
		List<ColumnModel> truthSchema = tableTruthManager.getColumnModelsForObject(tableId);
		String truthSchemaMD5Hex = TableModelUtils.createSchemaMD5HexCM(truthSchema);
		// get the truth version
		long truthLastVersion = tableTruthManager.getTableVersion(tableId);
		// compare the truth with the index.
		return this.tableConnectionFactory.getConnection(tableId).doesIndexStateMatch(tableId, truthLastVersion, truthSchemaMD5Hex);
	}
	

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

	@Override
	public void setTableDeleted(String deletedId) {
		transactionalMessenger.sendMessageAfterCommit(deletedId, ObjectType.TABLE, ChangeType.DELETE);
	}

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
