package org.sagebionetworks.repo.model.dbo.grid;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_CON_CONNECTION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_CON_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_CON_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_CON_REPLICA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_CON_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_CON_SOURCE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_PATCH_ID_REP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_PATCH_ID_SEQ;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_S3_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_CREATE_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_CREATE_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_IS_AGENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_REPLICA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_REP_ID_CLIENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_REP_ID_SERVICE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_SOURCE_ID;

import java.sql.ResultSet;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridConstants;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.PatchInfo;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class GridDaoImpl implements GridDao {

	private final IdGenerator idGenerator;
	private final JdbcTemplate jdbcTemplate;

	public static final String LIST_MISSING_PATCHES = DDLUtilsImpl
			.loadSQLFromClasspath("sql/grid/ListMissingPatches.sql");

	private final RowMapper<GridSession> SESSION_MAPPER = (ResultSet rs, int rowNum) -> {
		long sourceIdLong = rs.getLong(COL_GRID_SESSION_SOURCE_ID);
		String sourceId = rs.wasNull() ? null : KeyFactory.keyToString(sourceIdLong);
		return new GridSession().setSessionId(rs.getString(COL_GRID_SESSION_SESSION_ID))
				.setStartedOn(rs.getTimestamp(COL_GRID_SESSION_CREATED_ON))
				.setStartedBy(rs.getString(COL_GRID_SESSION_CREATED_BY)).setEtag(rs.getString(COL_GRID_SESSION_ETAG))
				.setModifiedOn(rs.getTimestamp(COL_GRID_SESSION_MODIFIED_ON))
				.setLastReplicaIdClient(rs.getLong(COL_GRID_SESSION_REP_ID_CLIENT))
				.setLastReplicaIdService(rs.getLong(COL_GRID_SESSION_REP_ID_SERVICE)).setSourceEntityId(sourceId)
				.setGridJsonSchema$Id(rs.getString(COL_GRID_SESSION_SCHEMA_ID));
	};

	private final RowMapper<GridReplica> REPLICA_MAPPER = (ResultSet rs, int rowNum) -> {
		return new GridReplica().setReplicaId(rs.getLong(COL_GRID_REPLICA_REPLICA_ID))
				.setCreatedBy(rs.getString(COL_GRID_REPLICA_CREATE_BY))
				.setCreatedOn(rs.getTime(COL_GRID_REPLICA_CREATE_ON))
				.setGridSessionId(rs.getString(COL_GRID_REPLICA_SESSION_ID))
				.setIsAgentReplica(rs.getBoolean(COL_GRID_REPLICA_IS_AGENT));
	};

	private final RowMapper<GridConnectionInfo> CONNECTION_MAPPER = (ResultSet rs, int rowNum) -> {
		return new GridConnectionInfo().setConnectionId(rs.getString(COL_GRID_CON_CONNECTION_ID))
				.setCreatedBy(rs.getLong(COL_GRID_CON_CREATED_BY))
				.setCreatedOn(rs.getTimestamp(COL_GRID_CON_CREATED_ON))
				.setSessionId(rs.getString(COL_GRID_CON_SESSION_ID)).setReplicaId(rs.getLong(COL_GRID_CON_REPLICA_ID))
				.setSource(EventSource.valueOf(rs.getString(COL_GRID_CON_SOURCE)));
	};

	private final RowMapper<PatchInfo> PATCH_INFO_MAPPER = (ResultSet rs, int rowNum) -> {
		return new PatchInfo().setSesisonId(rs.getString(COL_GRID_PAT_SESSION_ID))
				.setPatchId(new LogicalTimestamp().setReplicaId(rs.getLong(COL_GRID_PAT_PATCH_ID_REP))
						.setSequenceNumber(rs.getLong(COL_GRID_PAT_PATCH_ID_SEQ)))
				.setCreatedOn(rs.getTimestamp(COL_GRID_PAT_CREATED_ON))
				.setExpiresOn(rs.getTimestamp(COL_GRID_PAT_EXPIRES_ON)).setS3Key(rs.getString(COL_GRID_PAT_S3_KEY));
	};

	private final RowMapper<LogicalTimestamp> TIMESTAMP_MAPPER = (ResultSet rs, int rowNum) -> {
		return new LogicalTimestamp().setReplicaId(rs.getLong(COL_GRID_PAT_PATCH_ID_REP))
				.setSequenceNumber(rs.getLong(COL_GRID_PAT_PATCH_ID_SEQ));
	};

	public GridDaoImpl(IdGenerator idGenerator, JdbcTemplate jdbcTemplate) {
		super();
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
	}

	@WriteTransaction
	@Override
	public GridSession createGridSession(CreateGridSession create) {
		ValidateArgument.required(create, "create");
		ValidateArgument.required(create.getUserId(), "create.userId");
		Long id = idGenerator.generateNewId(IdType.GRID_SESSION_ID);
		String sessionId = GridUtils.gridSessionIdAsString(id);
		long repIdClient = GridConstants.START_REPLICA_ID_CLIENT;
		long repIdService = GridConstants.START_REPLICA_ID_SERVICE;
		Long sourceId = create.getSourceId() == null ? null : KeyFactory.stringToKey(create.getSourceId());
		Object[] args = { id, create.getUserId(), sessionId, repIdClient, repIdService, sourceId,
				create.getSchemaId() };
		int[] argTypes = { java.sql.Types.BIGINT, java.sql.Types.BIGINT, java.sql.Types.VARCHAR, java.sql.Types.BIGINT,
				java.sql.Types.BIGINT, java.sql.Types.BIGINT, java.sql.Types.VARCHAR };
		jdbcTemplate.update(
				"INSERT INTO GRID_SESSION (ID, ETAG, CREATED_BY, CREATED_ON, MODIFIED_ON, SESSION_ID, REP_ID_CLIENT, REP_ID_SERVICE, SOURCE_ID, SCHEMA_ID)"
						+ " VALUES(?,UUID(),?,NOw(),NOW(),?,?,?,?,?)",
				args, argTypes);
		return geGridSession(sessionId).get();
	}

	@Override
	public Optional<Long> getGridSessionStartedBy(String gridSessionId) {
		ValidateArgument.required(gridSessionId, "gridSessionId");
		try {
			return Optional.of(jdbcTemplate.queryForObject(
					"SELECT CREATED_BY" + "  FROM GRID_SESSION WHERE SESSION_ID = ?", Long.class, gridSessionId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<GridSession> geGridSession(String gridSessionId) {
		ValidateArgument.required(gridSessionId, "gridSessionId");
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM GRID_SESSION WHERE SESSION_ID = ?",
					SESSION_MAPPER, gridSessionId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@WriteTransaction
	@Override
	public GridReplica createReplica(Long userId, String gridSessionId, boolean isAgent, EventSource source) {
		ValidateArgument.required(userId, "userId");

		Long replicaId = getNextReplicaSequence(gridSessionId, source);
		Long id = idGenerator.generateNewId(IdType.GRID_REPLICA_ID);
		jdbcTemplate.update("INSERT INTO GRID_REPLICA (ID, REPLICA_ID, CREATED_BY, CREATED_ON, SESSION_ID, IS_AGENT)"
				+ " VALUES(?,?,?,NOW(),?,?)", id, replicaId, userId, gridSessionId, isAgent);

		return getGridReplica(gridSessionId, replicaId).get();
	}

	/**
	 * Generate a new replica number for a session.
	 * 
	 * @param gridSessionId
	 * @param source
	 * @return
	 */
	Long getNextReplicaSequence(String gridSessionId, EventSource source) {
		ValidateArgument.required(gridSessionId, "gridSessionId");
		ValidateArgument.required(source, "source");
		String set = null;
		String select = null;
		switch (source) {
		case INTERNAL:
			// decrement service.
			set = "REP_ID_SERVICE = REP_ID_SERVICE-1";
			select = "REP_ID_SERVICE";
			break;

		case WEBSOCKET:
			// increment client.
			set = "REP_ID_CLIENT = REP_ID_CLIENT+1";
			select = "REP_ID_CLIENT";
			break;
		default:
			throw new IllegalArgumentException("Unknown eventSource: " + source);
		}

		String updateSql = String
				.format("UPDATE GRID_SESSION SET %s, ETAG=UUID(), MODIFIED_ON = NOW() WHERE SESSION_ID = ?", set);
		jdbcTemplate.update(updateSql, gridSessionId);
		String selectSql = String.format("SELECT %s FROM GRID_SESSION WHERE SESSION_ID = ?", select);
		return jdbcTemplate.queryForObject(selectSql, Long.class, gridSessionId);
	}

	@Override
	public Optional<GridReplica> getGridReplica(String sessionId, Long replicaId) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		try {
			return Optional.of(
					jdbcTemplate.queryForObject("SELECT * FROM GRID_REPLICA WHERE SESSION_ID = ? AND REPLICA_ID = ?",
							REPLICA_MAPPER, sessionId, replicaId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<Long> getReplicaCreatedBy(String sessionId, Long replicaId, boolean isAgentReplica) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		try {
			return Optional.of(jdbcTemplate.queryForObject(
					"SELECT CREATED_BY FROM GRID_REPLICA WHERE SESSION_ID = ? AND REPLICA_ID = ? AND IS_AGENT = ?",
					Long.class, sessionId, replicaId, isAgentReplica));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM GRID_SESSION WHERE ID > -1");

	}

	@WriteTransaction
	@Override
	public void createConnection(GridConnectionInfo connection) {
		ValidateArgument.required(connection, "connection");
		ValidateArgument.required(connection.getConnectionId(), "connection.connectionId");
		ValidateArgument.required(connection.getSessionId(), "connection.sessionId");
		ValidateArgument.required(connection.getReplicaId(), "connection.replicaId");
		ValidateArgument.required(connection.getCreatedBy(), "connection.createdBy");
		ValidateArgument.required(connection.getSource(), "connection.source");

		jdbcTemplate.update(
				"INSERT INTO GRID_CONNECTION (CONNECTION_ID, SESSION_ID, REPLICA_ID, CREATED_BY, CREATED_ON, SOURCE)"
						+ " VALUES (?,?,?,?,NOW(),?) ON DUPLICATE KEY UPDATE CONNECTION_ID = ?, CREATED_ON = NOW()",
				connection.getConnectionId(), connection.getSessionId(), connection.getReplicaId(),
				connection.getCreatedBy(), connection.getSource().name(), connection.getConnectionId());
	}

	@Override
	public Optional<GridConnectionInfo> getConnection(String connectionId) {
		ValidateArgument.required(connectionId, "connectionId");
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM GRID_CONNECTION WHERE CONNECTION_ID = ?",
					CONNECTION_MAPPER, connectionId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public List<GridConnectionInfo> listConnections(String sessionId) {
		ValidateArgument.required(sessionId, "sessionId");
		return jdbcTemplate.query("SELECT * FROM GRID_CONNECTION WHERE SESSION_ID = ? ORDER BY REPLICA_ID ASC",
				CONNECTION_MAPPER, sessionId);
	}

	@Override
	public void removeConnection(String connectionId) {
		ValidateArgument.required(connectionId, "connectionId");
		jdbcTemplate.update("DELETE FROM GRID_CONNECTION WHERE CONNECTION_ID = ?", connectionId);

	}

	@WriteTransaction
	@Override
	public boolean savePatch(String sessionId, LogicalTimestamp patchId, String s3Key, Duration expires) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(patchId, "patchId");
		ValidateArgument.required(s3Key, "s3Key");
		ValidateArgument.required(expires, "expires");

		Long id = idGenerator.generateNewId(IdType.GRID_SESSION_ID);
		return jdbcTemplate.update(
				"INSERT IGNORE INTO GRID_PATCH "
						+ "(ID, SESSION_ID, PATCH_ID_REP, PATCH_ID_SEQ, CREATED_ON, EXPIRES_ON, S3_KEY)"
						+ " VALUES (?,?,?,?,NOW(),NOW() + INTERVAL ? SECOND,?)",
				id, sessionId, patchId.getReplicaId(), patchId.getSequenceNumber(), expires.getSeconds(), s3Key) > 0;
	}

	@Override
	public Optional<PatchInfo> getPatchInfo(String sessionId, LogicalTimestamp patchId) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(patchId, "patchId");
		try {
			return Optional.of(jdbcTemplate.queryForObject(
					"SELECT * FROM GRID_PATCH WHERE SESSION_ID = ? AND PATCH_ID_REP = ? AND PATCH_ID_SEQ = ?",
					PATCH_INFO_MAPPER, sessionId, patchId.getReplicaId(), patchId.getSequenceNumber()));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public List<LogicalTimestamp> listMissingPatchIdsForClock(String sessionId, List<LogicalTimestamp> clock,
			long limit) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(clock, "clock");
		if (clock.isEmpty()) {
			clock = List.of(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L));
		}
		StringJoiner rows = new StringJoiner(",");
		clock.forEach(id -> {
			rows.add(String.format("ROW(%d,%d)", id.getReplicaId(), id.getSequenceNumber()));
		});
		String sql = String.format(LIST_MISSING_PATCHES, rows.toString());
		return jdbcTemplate.query(sql, TIMESTAMP_MAPPER, sessionId, limit);
	}

	@Override
	public List<GridSession> listActiveGridSession(Long userId, String sourceIdString, Long limit, Long offset) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(sourceIdString, "sourceId");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		return jdbcTemplate.query(
				"SELECT * FROM GRID_SESSION WHERE CREATED_BY = ? AND SOURCE_ID = ? ORDER BY MODIFIED_ON DESC LIMIT ? OFFSET ?",
				SESSION_MAPPER, userId, KeyFactory.stringToKey(sourceIdString), limit, offset);
	}

	@Override
	public List<GridSession> listActiveGridSession(Long userId, Long limit, Long offset) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		return jdbcTemplate.query(
				"SELECT * FROM GRID_SESSION WHERE CREATED_BY = ? ORDER BY MODIFIED_ON DESC LIMIT ? OFFSET ?",
				SESSION_MAPPER, userId, limit, offset);
	}

	@WriteTransaction
	@Override
	public void deleteGridSession(String sessionId) {
		ValidateArgument.required(sessionId, "sessionId");
		jdbcTemplate.update("DELETE FROM GRID_SESSION WHERE SESSION_ID = ?", sessionId);
	}

}
