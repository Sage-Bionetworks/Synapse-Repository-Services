package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_CHANGE_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_PROGRESS_CURRENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_PROGRESS_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_PROGRESS_TOTAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_RESET_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_RUNTIME_MS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATUS;

import java.util.UUID;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableStatus;
import org.sagebionetworks.repo.model.dbo.persistence.table.TableStatusUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * A very basic DAO to tack table status.
 *
 */
public class TableStatusDAOImpl implements TableStatusDAO {
	
	/**
	 * Number used when the version number is null;
	 */
	public static final long NULL_VERSION = -1;
	
	private static final String SQL_UPDATE_TABLE_PROGRESS = "UPDATE " + TABLE_STATUS + " SET "
			+ COL_TABLE_STATUS_CHANGE_ON + " = ?, " + COL_TABLE_STATUS_PROGRESS_MESSAGE + " = ?, "
			+ COL_TABLE_STATUS_PROGRESS_CURRENT + " = ?, " + COL_TABLE_STATUS_PROGRESS_TOTAL + " = ?, "
			+ COL_TABLE_STATUS_RUNTIME_MS + " = ? WHERE " + COL_TABLE_STATUS_ID + " = ? AND " + COL_TABLE_STATUS_VERSION
			+ " = ?";
	private static final String CONFLICT_MESSAGE = "The passed reset-token was invalid. The table's status was reset after the passed reset-token was acquired.";
	
	private static final String SQL_SELECT_STATUS_FOR_UPDATE = "SELECT * FROM " + TABLE_STATUS + " WHERE "
			+ COL_TABLE_STATUS_ID + " = ? AND " + COL_TABLE_STATUS_VERSION + " = ?  FOR UPDATE";
	
	private static final String SQL_DELETE_ALL_STATE = "DELETE FROM " + TABLE_STATUS + " WHERE " + COL_TABLE_STATUS_ID
			+ " > -1";
	
	private static final String SQL_RESET_TO_PENDING = "INSERT INTO " + TABLE_STATUS + " (" + COL_TABLE_STATUS_ID + ", "
			+ COL_TABLE_STATUS_VERSION + ", " + COL_TABLE_STATUS_STATE + ", " + COL_TABLE_STATUS_RESET_TOKEN + ", "
			+ COL_TABLE_STATUS_STARTED_ON + ", " + COL_TABLE_STATUS_CHANGE_ON
			+ ") VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " + COL_TABLE_STATUS_STATE + " = ?, "
			+ COL_TABLE_STATUS_RESET_TOKEN + " = ?, " + COL_TABLE_STATUS_STARTED_ON + " = ?, "
			+ COL_TABLE_STATUS_CHANGE_ON + " = ?";
	
	private static final String SQL_DELETE_TABLE_STATUS = "DELETE FROM " + TABLE_STATUS + " WHERE "
			+ COL_TABLE_STATUS_ID + " = ? AND " + COL_TABLE_STATUS_VERSION + " = ?";

	TableMapping<DBOTableStatus> tableMapping = new DBOTableStatus().getTableMapping();
	
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;


	@Override
	public TableStatus getTableStatus(IdAndVersion idAndVersion) throws DatastoreException, NotFoundException {
		long version = validateAndGetVersion(idAndVersion);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("tableId", idAndVersion.getId());
		param.addValue("version", version);
		DBOTableStatus dbo =  basicDao.getObjectByPrimaryKey(DBOTableStatus.class, param);
		return TableStatusUtils.createDTOFromDBO(dbo);
	}
	
	@WriteTransaction
	@Override
	public String resetTableStatusToProcessing(IdAndVersion idAndVersion) {
		long version = validateAndGetVersion(idAndVersion);
		String state = TableState.PROCESSING.name();
		String resetToken = UUID.randomUUID().toString();
		long now = System.currentTimeMillis();
		// We are not unconditionally replacing this row.  Instead we are only setting the columns that we wish to change.
		jdbcTemplate.update(SQL_RESET_TO_PENDING, idAndVersion.getId(), version, state,resetToken, now, now, state, resetToken, now, now);
		return resetToken;
	}

	@WriteTransaction
	@Override
	public void attemptToSetTableStatusToFailed(IdAndVersion tableIdString,
			String resetToken, String errorMessage, String errorDetails)
			throws ConflictingUpdateException, NotFoundException {
		if(tableIdString == null) throw new IllegalArgumentException("TableId cannot be null");
		attemptToSetTableEndState(tableIdString, resetToken, TableState.PROCESSING_FAILED, null, errorMessage, errorDetails, null);
	}

	
	@WriteTransaction
	@Override
	public void attemptToSetTableStatusToAvailable(IdAndVersion tableIdString,
			String resetToken, String tableChangeEtag) throws ConflictingUpdateException, NotFoundException {
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
	private void attemptToSetTableEndState(IdAndVersion idAndVersion,
			String resetToken, TableState state, String progressMessage, String errorMessage, String errorDetails, String tableChangeEtag) throws NotFoundException, ConflictingUpdateException{
		// This method cannot be used to reset to processing
		if(TableState.PROCESSING.equals(state)) {
			throw new IllegalArgumentException("This method cannot be used to change the state to PROCESSING because it does not change the reset-token");
		}
		DBOTableStatus current = selectResetTokenForUpdate(idAndVersion);
		if(!current.getResetToken().equals(resetToken)) {
			throw new ConflictingUpdateException(CONFLICT_MESSAGE);
		}
		// With no conflict make the changes
		long now = System.currentTimeMillis();
		// Calculate the total runtime
		long runtimeMS = now - current.getStartedOn();
		// Set the progress current to be the same as the progress total.
		Long progressCurrent = current.getProgressTotal();
		byte[] errorDetailsBytes = TableStatusUtils.createErrorDetails(errorDetails);
		current.setState(state.name());
		current.setChangedOn(now);
		current.setProgressCurrent(progressCurrent);
		current.setErrorMessage(errorMessage);
		current.setErrorDetails(errorDetailsBytes);
		current.setTotalRunTimeMS(runtimeMS);
		current.setLastTableChangeEtag(tableChangeEtag);
		basicDao.update(current);
 	}
	
	/**
	 * Select the current reset token FOR UPDATE
	 * @param tableId
	 * @return
	 * @throws NotFoundException
	 */
	private DBOTableStatus selectResetTokenForUpdate(IdAndVersion idAndVersion) throws NotFoundException{
		try {
			long version = validateAndGetVersion(idAndVersion);
			return jdbcTemplate.queryForObject(SQL_SELECT_STATUS_FOR_UPDATE, tableMapping, idAndVersion.getId(), version);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Table status does not exist for: "+idAndVersion.toString());
		}
	}


	@WriteTransaction
	@Override
	public void clearAllTableState() {
		jdbcTemplate.update(SQL_DELETE_ALL_STATE);
	}

	@Override
	public void attemptToUpdateTableProgress(IdAndVersion idAndVersion, String resetToken,
			String progressMessage, Long currentProgress, Long totalProgress)
			throws ConflictingUpdateException, NotFoundException {
		// Ensure the reset-token matches.
		DBOTableStatus current = selectResetTokenForUpdate(idAndVersion);
		if(!current.getResetToken().equals(resetToken)) {
			throw new ConflictingUpdateException(CONFLICT_MESSAGE);
		}
		// With no conflict make the changes
		long now = System.currentTimeMillis();
		// Calculate the total runtime
		long runtimeMS = now - current.getStartedOn();
		jdbcTemplate.update(SQL_UPDATE_TABLE_PROGRESS, now, progressMessage, currentProgress, totalProgress, runtimeMS, current.getTableId(), current.getVersion());
	}

	@Override
	public void deleteTableStatus(IdAndVersion idAndVersion) {
		long version = validateAndGetVersion(idAndVersion);
		jdbcTemplate.update(SQL_DELETE_TABLE_STATUS, idAndVersion.getId(), version);
	}
	
	/**
	 * Validate the passed IdAndVersion and extract a non-null version.
	 * 
	 * @param idAndVersion
	 * @return
	 */
	public static long validateAndGetVersion(IdAndVersion idAndVersion) {
		if(idAndVersion == null) {
			throw new IllegalArgumentException("IdAndVersion cannot be null");
		}
		if(idAndVersion.getId() == null) {
			throw new IllegalArgumentException("IdAndVersion.id cannot be null");
		}
		return idAndVersion.getVersion().orElse(NULL_VERSION);
	}

}
