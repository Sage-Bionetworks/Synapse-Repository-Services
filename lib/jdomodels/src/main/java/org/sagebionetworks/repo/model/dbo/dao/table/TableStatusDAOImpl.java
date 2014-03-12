package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_ERROR_DETAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_ERROR_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_PROGRESS_CURRENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_PROGRESS_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_RESET_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_RUNTIME_MS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATUS;

import java.util.UUID;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableStatus;
import org.sagebionetworks.repo.model.dbo.persistence.table.TableStatusUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A very basic DAO to tack table status.
 * 
 * @author John
 *
 */
public class TableStatusDAOImpl implements TableStatusDAO {
	
	private static final String SQL_UPDATE_TABLE_PROGRESS = "UPDATE "+TABLE_STATUS+" SET "+COL_TABLE_STATUS_CHANGE_ON+" = ?, "+COL_TABLE_STATUS_PROGRESS_MESSAGE+" = ?, "+COL_TABLE_STATUS_PROGRESS_CURRENT+" = ?, "+COL_TABLE_STATUS_PROGRESS_TOTAL+" = ?, "+COL_TABLE_STATUS_RUNTIME_MS+" = ? WHERE "+COL_TABLE_STATUS_ID+" = ?";
	private static final String CONFLICT_MESSAGE = "The passed reset-token was invalid. The table's status was reset after the passed reset-token was acquired.";
	private static final String SQL_UPDATE_END_STATE = "UPDATE "+TABLE_STATUS+" SET "+COL_TABLE_STATUS_STATE+" = ?, "+COL_TABLE_STATUS_CHANGE_ON+" = ?, "+COL_TABLE_STATUS_PROGRESS_MESSAGE+" = ?, "+COL_TABLE_STATUS_PROGRESS_CURRENT+" = ?, "+COL_TABLE_STATUS_ERROR_MESSAGE+" = ?, "+COL_TABLE_STATUS_ERROR_DETAILS+" = ?, "+COL_TABLE_STATUS_RUNTIME_MS+" = ?, "+COL_TABLE_LAST_TABLE_CHANGE_ETAG+" = ? WHERE "+COL_TABLE_STATUS_ID+" = ?";
	private static final String SQL_SELECT_STATUS_FOR_UPDATE = "SELECT * FROM "+TABLE_STATUS+" WHERE "+COL_TABLE_STATUS_ID+" = ? FOR UPDATE";
	private static final String SQL_DELETE_ALL_STATE = "DELETE FROM "+TABLE_STATUS+" WHERE "+COL_TABLE_STATUS_ID+" > -1";
	private static final String SQL_RESET_TO_PENDING = "INSERT INTO "+TABLE_STATUS+" ("+COL_TABLE_STATUS_ID+", "+COL_TABLE_STATUS_STATE+", "+COL_TABLE_STATUS_RESET_TOKEN+", "+COL_TABLE_STATUS_STARTED_ON+", "+COL_TABLE_STATUS_CHANGE_ON+") VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE "+COL_TABLE_STATUS_STATE+" = ?, "+COL_TABLE_STATUS_RESET_TOKEN+" = ?, "+COL_TABLE_STATUS_STARTED_ON+" = ?, "+COL_TABLE_STATUS_CHANGE_ON+" = ?";

	TableMapping<DBOTableStatus> tableMapping = new DBOTableStatus().getTableMapping();
	
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	TransactionalMessenger transactionalMessanger;
	
	@Override
	public TableStatus getTableStatus(String tableIdString) throws DatastoreException, NotFoundException {
		if(tableIdString == null) throw new IllegalArgumentException("TableId cannot be null");
		Long tableId = KeyFactory.stringToKey(tableIdString);
		SqlParameterSource param = new MapSqlParameterSource("tableId", tableId);
		DBOTableStatus dbo =  basicDao.getObjectByPrimaryKey(DBOTableStatus.class, param);
		return TableStatusUtils.createDTOFromDBO(dbo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String resetTableStatusToProcessing(String tableIdString) {
		if(tableIdString == null) throw new IllegalArgumentException("TableId cannot be null");
		Long tableId = KeyFactory.stringToKey(tableIdString);
		String state = TableState.PROCESSING.name();
		String resetToken = UUID.randomUUID().toString();
		long now = System.currentTimeMillis();
		// We are not unconditionally replacing this row.  Instead we are only setting the columns that we wish to change.
		simpleJdbcTemplate.update(SQL_RESET_TO_PENDING, tableId, state,resetToken, now, now, state, resetToken, now, now);
		// Fire a change event
		transactionalMessanger.sendMessageAfterCommit(tableId.toString(), ObjectType.TABLE, resetToken, ChangeType.UPDATE);
		return resetToken;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void attemptToSetTableStatusToFailed(String tableIdString,
			String resetToken, String errorMessage, String errorDetails)
			throws ConflictingUpdateException, NotFoundException {
		if(tableIdString == null) throw new IllegalArgumentException("TableId cannot be null");
		attemptToSetTableEndState(tableIdString, resetToken, TableState.PROCESSING_FAILED, null, errorMessage, errorDetails, null);
	}

	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void attemptToSetTableStatusToAvailable(String tableIdString,
			String resetToken, String tableChangeEtag) throws ConflictingUpdateException, NotFoundException {
		if(tableChangeEtag == null) throw new IllegalArgumentException("tableChangeEtag cannot be null when setting to available.");
		attemptToSetTableEndState(tableIdString, resetToken, TableState.AVAILABLE, null, null, null, tableChangeEtag);
	}
	
	/**
	 * Private method to attempt to set the end (or final) state on a table.
	 * 
	 * @param tableIdString
	 * @param resetToken
	 * @param state
	 * @param progressMessage
	 * @param errorMessage
	 * @param errorDetails
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException Thrown if the passed reset-token does not match the current reset-token, indicating it have been reset since the processing started.
	 */
	private void attemptToSetTableEndState(String tableIdString,
			String resetToken, TableState state, String progressMessage, String errorMessage, String errorDetails, String tableChangeEtag) throws NotFoundException, ConflictingUpdateException{
		// This method cannot be used to reset to processing
		if(TableState.PROCESSING.equals(state)) throw new IllegalArgumentException("This method cannot be used to change the state to PROCESSING because it does not change the reset-token");
		if(tableIdString == null) throw new IllegalArgumentException("TableId cannot be null");
		Long tableId = KeyFactory.stringToKey(tableIdString);
		DBOTableStatus current = selectResetTokenForUpdate(tableId);
		if(!current.getResetToken().equals(resetToken)) throw new ConflictingUpdateException(CONFLICT_MESSAGE);
		// With no conflict make the changes
		long now = System.currentTimeMillis();
		// Calculate the total runtime
		long runtimeMS = now - current.getStartedOn();
		// Set the progress current to be the same as the progress total.
		Long progressCurrent = current.getProgresssTotal();
		byte[] errorDetailsBytes = TableStatusUtils.createErrorDetails(errorDetails);
		simpleJdbcTemplate.update(SQL_UPDATE_END_STATE, state.name(), now, progressMessage, progressCurrent, errorMessage, errorDetailsBytes, runtimeMS,tableChangeEtag, tableId);
 	}
	
	/**
	 * Select the current reset token FOR UPDATE
	 * @param tableId
	 * @return
	 * @throws NotFoundException
	 */
	private DBOTableStatus selectResetTokenForUpdate(Long tableId) throws NotFoundException{
		try {
			return simpleJdbcTemplate.queryForObject(SQL_SELECT_STATUS_FOR_UPDATE, tableMapping, tableId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Table status does not exist for: "+tableId);
		}
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void clearAllTableState() {
		simpleJdbcTemplate.update(SQL_DELETE_ALL_STATE);
	}

	@Override
	public void attemptToUpdateTableProgress(String tableIdString, String resetToken,
			String progressMessage, Long currentProgress, Long totalProgress)
			throws ConflictingUpdateException, NotFoundException {
		if(tableIdString == null) throw new IllegalArgumentException("TableId cannot be null");
		Long tableId = KeyFactory.stringToKey(tableIdString);
		// Ensure the reset-token matches.
		DBOTableStatus current = selectResetTokenForUpdate(tableId);
		if(!current.getResetToken().equals(resetToken)) throw new ConflictingUpdateException(CONFLICT_MESSAGE);
		// With no conflict make the changes
		long now = System.currentTimeMillis();
		// Calculate the total runtime
		long runtimeMS = now - current.getStartedOn();
		simpleJdbcTemplate.update(SQL_UPDATE_TABLE_PROGRESS, now, progressMessage, currentProgress, totalProgress, runtimeMS, tableId);
	}

}
