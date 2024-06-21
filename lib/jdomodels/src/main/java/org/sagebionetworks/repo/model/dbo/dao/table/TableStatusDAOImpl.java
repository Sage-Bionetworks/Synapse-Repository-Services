package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_LAST_TABLE_CHANGE_ETAG;
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

import java.sql.ResultSet;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.IdVersionTableType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableStatus;
import org.sagebionetworks.repo.model.dbo.persistence.table.TableStatusUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.entity.IdAndVersionBuilder;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableStatusChangeEvent;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * A very basic DAO to tack table status.
 *
 */
@Repository
public class TableStatusDAOImpl implements TableStatusDAO {
	
	public static final Long TRASH_FOLDER_ID = Long.parseLong(
			StackConfigurationSingleton.singleton().getTrashFolderEntityId());

	public static final String TABLES_WITH_MISSING_STATUS = DDLUtilsImpl
			.loadSQLFromClasspath("sql/GetTablesWithMissingStatus.sql");
	
	private static final String SELECT_STATUS_TEMPLATE = "SELECT %1$s FROM " + TABLE_STATUS + " WHERE "
			+ COL_TABLE_STATUS_ID + " = ? AND " + COL_TABLE_STATUS_VERSION + " = ?";

	public static final int MAX_ERROR_MESSAGE_CHARS = 1000;

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

	private static final String SQL_DELETE_TABLE_STATUS_FOR_VERSION = "DELETE FROM " + TABLE_STATUS + " WHERE "
			+ COL_TABLE_STATUS_ID + " = ? AND " + COL_TABLE_STATUS_VERSION + " = ?";

	private static final String SQL_DELETE_TABLE_STATUS = "DELETE FROM " + TABLE_STATUS + " WHERE "
			+ COL_TABLE_STATUS_ID + " = ?";

	TableMapping<DBOTableStatus> tableMapping = new DBOTableStatus().getTableMapping();

	private DBOBasicDao basicDao;

	private JdbcTemplate jdbcTemplate;
		
	private TransactionalMessenger messenger;
	
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	public TableStatusDAOImpl(DBOBasicDao basicDao, JdbcTemplate jdbcTemplate, TransactionalMessenger messenger) {
		this.basicDao = basicDao;
		this.jdbcTemplate = jdbcTemplate;
		this.messenger = messenger;
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	@Override
	public TableStatus getTableStatus(IdAndVersion idAndVersion) throws DatastoreException, NotFoundException {
		long version = validateAndGetVersion(idAndVersion);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("tableId", idAndVersion.getId());
		param.addValue("version", version);
		DBOTableStatus dbo = basicDao.getObjectByPrimaryKey(DBOTableStatus.class, param)
				.orElseThrow(() -> new NotFoundException(
						String.format("Table status for '%s' does not exist", idAndVersion.toString())));
		return TableStatusUtils.createDTOFromDBO(dbo);
	}
	
	@WriteTransaction
	@Override
	public String resetTableStatusToProcessing(IdAndVersion idAndVersion, boolean resetToken) {
		long version = validateAndGetVersion(idAndVersion);
		String state = TableState.PROCESSING.name();		
		String token = UUID.randomUUID().toString();
		
		if (!resetToken) {
			token = getTableStatusToken(idAndVersion).orElse(token);
		}
		
		long now = System.currentTimeMillis();
		
		// We are not unconditionally replacing this row. Instead we are only setting
		// the columns that we wish to change.
		
		String sql = "INSERT INTO " + TABLE_STATUS + " (" + COL_TABLE_STATUS_ID + ", "
				+ COL_TABLE_STATUS_VERSION + ", " + COL_TABLE_STATUS_STATE + ", " + COL_TABLE_STATUS_RESET_TOKEN + ", "
				+ COL_TABLE_STATUS_STARTED_ON + ", " + COL_TABLE_STATUS_CHANGE_ON
				+ ") VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " + COL_TABLE_STATUS_STATE + " = ?, "
				+ COL_TABLE_STATUS_RESET_TOKEN + " = ?, " + COL_TABLE_STATUS_CHANGE_ON + " = ?";
		
		jdbcTemplate.update(sql, idAndVersion.getId(), version, state, token, now, now, state, token, now);
		
		sendTableStatusEvent(idAndVersion, TableState.PROCESSING);
				
		return token;
	}

	@WriteTransaction
	@Override
	public void attemptToSetTableStatusToFailed(IdAndVersion tableIdString, String errorMessage, String errorDetails)
			throws InvalidStatusTokenException, NotFoundException {
		if (tableIdString == null)
			throw new IllegalArgumentException("TableId cannot be null");
		String resetToken = null;
		attemptToSetTableEndState(tableIdString, resetToken, TableState.PROCESSING_FAILED, null, errorMessage,
				errorDetails, null);
	}

	@WriteTransaction
	@Override
	public void attemptToSetTableStatusToAvailable(IdAndVersion tableIdString, String resetToken,
			String tableChangeEtag) throws InvalidStatusTokenException, NotFoundException {
		ValidateArgument.required(resetToken, "resetToken");
		attemptToSetTableEndState(tableIdString, resetToken, TableState.AVAILABLE, null, null, null, tableChangeEtag);
	}
	
	/**
	 * Private method to attempt to set the end (or final) state on a table.
	 * 
	 * @param idAndVersion
	 * @param resetToken
	 * @param state
	 * @param progressMessage
	 * @param errorMessage
	 * @param errorDetails
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException Thrown if the passed reset-token does not
	 *                                    match the current reset-token, indicating
	 *                                    it have been reset since the processing
	 *                                    started.
	 */
	private void attemptToSetTableEndState(IdAndVersion idAndVersion, String resetToken, TableState state,
			String progressMessage, String errorMessage, String errorDetails, String tableChangeEtag)
			throws NotFoundException, InvalidStatusTokenException {
		// This method cannot be used to reset to processing
		if (TableState.PROCESSING.equals(state)) {
			throw new IllegalArgumentException(
					"This method cannot be used to change the state to PROCESSING because it does not change the reset-token");
		}
		
		DBOTableStatus current = selectResetTokenForUpdate(idAndVersion).orElseGet(() -> {
			// If we failed the processing for any reason we should be able to unconditionally set the table as failed
			if (TableState.PROCESSING_FAILED.equals(state)) {
				DBOTableStatus failed = new DBOTableStatus();
				failed.setTableId(idAndVersion.getId());
				failed.setVersion(validateAndGetVersion(idAndVersion));
				failed.setStartedOn(System.currentTimeMillis());
				return failed;
			}
			
			throw new NotFoundException("Table status does not exist for: " + idAndVersion.toString());
		});
		
		if (resetToken != null && !current.getResetToken().equals(resetToken)) {
			throw new InvalidStatusTokenException(CONFLICT_MESSAGE);
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
		current.setErrorMessage(StringUtils.abbreviate(errorMessage, MAX_ERROR_MESSAGE_CHARS));
		current.setErrorDetails(errorDetailsBytes);
		current.setTotalRunTimeMS(runtimeMS);
		current.setLastTableChangeEtag(tableChangeEtag);
		current.setResetToken(UUID.randomUUID().toString());
		
		basicDao.createOrUpdate(current);
		
		sendTableStatusEvent(idAndVersion, state);
	}

	/**
	 * Select the current reset token FOR UPDATE
	 * 
	 * @param idAndVersion
	 * @return
	 * @throws NotFoundException
	 */
	Optional<DBOTableStatus> selectResetTokenForUpdate(IdAndVersion idAndVersion) throws NotFoundException {
		try {
			long version = validateAndGetVersion(idAndVersion);
			DBOTableStatus status = jdbcTemplate.queryForObject(SQL_SELECT_STATUS_FOR_UPDATE, tableMapping, idAndVersion.getId(), version);
			return Optional.of(status);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@WriteTransaction
	@Override
	public void clearAllTableState() {
		jdbcTemplate.update(SQL_DELETE_ALL_STATE);
	}

	@Override
	public void attemptToUpdateTableProgress(IdAndVersion idAndVersion, String resetToken, String progressMessage,
			Long currentProgress, Long totalProgress) throws NotFoundException {
		// Ensure the reset-token matches.
		DBOTableStatus current = selectResetTokenForUpdate(idAndVersion).orElseThrow(() -> new NotFoundException("Table status does not exist for: " + idAndVersion.toString()));
		if (!current.getResetToken().equals(resetToken)) {
			throw new InvalidStatusTokenException(CONFLICT_MESSAGE);
		}
		// With no conflict make the changes
		long now = System.currentTimeMillis();
		// Calculate the total runtime
		long runtimeMS = now - current.getStartedOn();
		jdbcTemplate.update(SQL_UPDATE_TABLE_PROGRESS, now, progressMessage, currentProgress, totalProgress, runtimeMS,
				current.getTableId(), current.getVersion());
	}

	@Override
	public void deleteTableStatus(IdAndVersion idAndVersion) {
		long version = validateAndGetVersion(idAndVersion);
		jdbcTemplate.update(SQL_DELETE_TABLE_STATUS_FOR_VERSION, idAndVersion.getId(), version);
	}

	/**
	 * Validate the passed IdAndVersion and extract a non-null version.
	 * 
	 * @param idAndVersion
	 * @return
	 */
	public static long validateAndGetVersion(IdAndVersion idAndVersion) {
		if (idAndVersion == null) {
			throw new IllegalArgumentException("IdAndVersion cannot be null");
		}
		if (idAndVersion.getId() == null) {
			throw new IllegalArgumentException("IdAndVersion.id cannot be null");
		}
		return idAndVersion.getVersion().orElse(NULL_VERSION);
	}

	@Override
	public Optional<TableState> getTableStatusState(IdAndVersion tableId) {
		long version = validateAndGetVersion(tableId);
		try {
			return Optional.of(jdbcTemplate.queryForObject(
					String.format(SELECT_STATUS_TEMPLATE, COL_TABLE_STATUS_STATE), (ResultSet rs, int rowNum) -> {
						return TableState.valueOf(rs.getString(COL_TABLE_STATUS_STATE));
					}, tableId.getId(), version));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
	
	Optional<String> getTableStatusToken(IdAndVersion tableId) {
		long version = validateAndGetVersion(tableId);
		try {
			return Optional.of(jdbcTemplate.queryForObject(
					String.format(SELECT_STATUS_TEMPLATE, COL_TABLE_STATUS_RESET_TOKEN), String.class, tableId.getId(), version));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<Date> getLastChangedOn(IdAndVersion tableId) {
		long version = validateAndGetVersion(tableId);
		try {
			Long changedOn = jdbcTemplate.queryForObject(
					String.format(SELECT_STATUS_TEMPLATE, COL_TABLE_STATUS_CHANGE_ON), Long.class, tableId.getId(),
					version);
			return Optional.of(new Date(changedOn));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@NewWriteTransaction
	@Override
	public boolean updateChangedOnIfAvailable(IdAndVersion tableId) {
		long version = validateAndGetVersion(tableId);
		long now = System.currentTimeMillis();
		int count = jdbcTemplate.update("UPDATE " + TABLE_STATUS + " SET " + COL_TABLE_STATUS_CHANGE_ON + " = ? WHERE "
				+ COL_TABLE_STATUS_ID + " = ?" + " AND " + COL_TABLE_STATUS_VERSION + " = ? AND "
				+ COL_TABLE_STATUS_STATE + " = '" + TableState.AVAILABLE.name() + "'", now, tableId.getId(), version);
		
		boolean updated = count > 0;
		
		if(updated) {
			sendTableStatusEvent(tableId, TableState.AVAILABLE);
		}
		
		return updated;
	}

	@Override
	public Optional<String> getLastChangeEtag(IdAndVersion tableId) {
		long version = validateAndGetVersion(tableId);
		try {
			String sql = String.format(SELECT_STATUS_TEMPLATE, COL_TABLE_LAST_TABLE_CHANGE_ETAG);
			String etag = jdbcTemplate.queryForObject(sql, String.class, tableId.getId(), version);
			return Optional.ofNullable(etag);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	private void sendTableStatusEvent(IdAndVersion idAndVersion, TableState state) {
		TableStatusChangeEvent event = new TableStatusChangeEvent()
				.setObjectType(ObjectType.TABLE_STATUS_EVENT)
				.setState(state)
				.setObjectId(idAndVersion.getId().toString())
				.setObjectVersion(idAndVersion.getVersion().orElse(null))
				.setTimestamp(Date.from(Instant.now()));
		
		messenger.publishMessageAfterCommit(event);
	}
	
	@Override
	public List<IdVersionTableType> getAllTablesAndViewsWithMissingStatus(long limit) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("tableTypes", Stream.of(TableType.values()).map(t -> t.name()).collect(Collectors.toList()));
		params.addValue("trashId", TRASH_FOLDER_ID);
		params.addValue("limit", limit);
		return namedParameterJdbcTemplate.query(TABLES_WITH_MISSING_STATUS, params, (ResultSet rs, int rowNum) -> {
			Long id = rs.getLong("ID");
			Long version = rs.getLong("VERSION");
			if (version == -1L) {
				version = null;
			}
			TableType type = TableType.valueOf(rs.getString("TYPE"));
			return new IdVersionTableType(new IdAndVersionBuilder().setId(id).setVersion(version).build(), type);
		});
	}

	@Override
	public void deleteTableStatusForAllVersions(IdAndVersion idAndVersion) {
		jdbcTemplate.update(SQL_DELETE_TABLE_STATUS, idAndVersion.getId());
	}
}
