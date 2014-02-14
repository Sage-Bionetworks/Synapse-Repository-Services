package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_RESET_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATUS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableStatus;
import org.sagebionetworks.repo.model.dbo.persistence.table.TableStatusUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
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
	
	private static final String SQL_SELECT_STATUS_FOR_UPDATE = "SELECT * FROM "+TABLE_STATUS+" WHERE "+COL_TABLE_STATUS_ID+" = ? FOR UPDATE";

	private static final String SQL_DELETE_ALL_STATE = "DELETE FROM "+TABLE_STATUS+" WHERE "+COL_TABLE_STATUS_ID+" > -1";

	private static final String SQL_RESET_TO_PENDING = "INSERT INTO "+TABLE_STATUS+" ("+COL_TABLE_STATUS_ID+", "+COL_TABLE_STATUS_STATE+", "+COL_TABLE_STATUS_RESET_TOKEN+", "+COL_TABLE_STATUS_STARTED_ON+", "+COL_TABLE_STATUS_CHANGE_ON+") VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE "+COL_TABLE_STATUS_STATE+" = ?, "+COL_TABLE_STATUS_RESET_TOKEN+" = ?, "+COL_TABLE_STATUS_STARTED_ON+" = ?, "+COL_TABLE_STATUS_CHANGE_ON+" = ?";

	TableMapping<DBOTableStatus> tableMapping = new DBOTableStatus().getTableMapping();
	
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
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
		return resetToken;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void attemptToSetTableStatusToAvailable(String tableIdString,
			String resetToken) throws ConflictingUpdateException, NotFoundException {
		attemptToSetTableEndState(tableIdString, resetToken, TableState.AVAILABLE, null, null, null);
	}
	
	private void attemptToSetTableEndState(String tableIdString,
			String resetToken, TableState state, String progressMessage, String errorMessage, String errorDetails) throws NotFoundException{
		// This method cannot be used to reset to processing
		if(TableState.PROCESSING.equals(state)) throw new IllegalArgumentException("This method cannot be used to change the state to PROCESSING because it does not change the reset-token");
		if(tableIdString == null) throw new IllegalArgumentException("TableId cannot be null");
		Long tableId = KeyFactory.stringToKey(tableIdString);
		DBOTableStatus current = selectResetTokenForUpdate(tableId);
		if(!current.getResetToken().equals(resetToken)) throw new ConflictingUpdateException("The passed reset-token was invalid. The table's was reset after the passed reset-token was acquired.");
		
		
		// With no conflict make the changes
		long now = System.currentTimeMillis();
		// Calculate the total runtime
		long runtimeMS = now - current.getStartedOn();
		// Set the progress current to be the same as the progress total.
		Long progressCurrent = current.getProgresssTotal();
		byte[] errorDetailsBytes = TableStatusUtils.createErrorDetails(errorDetails);
		simpleJdbcTemplate.update("UPDATE "+TABLE_STATUS+" SET "+COL_TABLE_STATUS_STATE+" = ?, "+COL_TABLE_STATUS_CHANGE_ON+" = ?, "+COL_TABLE_STATUS_PROGRESS_MESSAGE+" = ?, "+COL_TABLE_STATUS_PROGRESS_CURRENT+" = ?, "+COL_TABLE_STATUS_ERROR_MESSAGE+" = ?, "+COL_TABLE_STATUS_ERROR_DETAILS+" = ?, "+COL_TABLE_STATUS_RUNTIME_MS+" = ? WHERE "+COL_TABLE_STATUS_ID+" = ?", state.name(), now, progressMessage, progressCurrent, errorMessage, errorDetailsBytes, runtimeMS, tableId);
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
	public void attemptToSetTableStatusToFailed(String tableIdString,
			String resetToken, String errorMessage, Throwable errorDetails)
			throws ConflictingUpdateException {
		if(tableIdString == null) throw new IllegalArgumentException("TableId cannot be null");
		Long tableId = KeyFactory.stringToKey(tableIdString);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void clearAllTableState() {
		simpleJdbcTemplate.update(SQL_DELETE_ALL_STATE);
	}
	
	

}
