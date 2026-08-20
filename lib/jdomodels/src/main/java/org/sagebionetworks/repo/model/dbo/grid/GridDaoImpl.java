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
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_SIZE_BYTES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_CREATE_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_CREATE_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_IS_AGENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_REPLICA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_AUTH_MODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_REP_ID_CLIENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_REP_ID_SERVICE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_SOURCE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_SOURCE_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAPSHOT_CLOCK_TABLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAPSHOT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAPSHOT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAPSHOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAPSHOT_S3_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAPSHOT_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.json.JSONArray;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
import org.sagebionetworks.repo.model.grid.AuthorizationMode;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridConstants;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridReplicaInfo;
import org.sagebionetworks.repo.model.grid.GridReplicaType;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
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
		String authModeStr = rs.getString(COL_GRID_SESSION_AUTH_MODE);
		AuthorizationMode authMode = authModeStr == null ? null : AuthorizationMode.valueOf(authModeStr);
		return new GridSession().setSessionId(rs.getString(COL_GRID_SESSION_SESSION_ID))
				.setStartedOn(rs.getTimestamp(COL_GRID_SESSION_CREATED_ON))
				.setOwnerPrincipalId(rs.getString(COL_GRID_SESSION_OWNER))
				.setStartedBy(rs.getString(COL_GRID_SESSION_CREATED_BY)).setEtag(rs.getString(COL_GRID_SESSION_ETAG))
				.setModifiedOn(rs.getTimestamp(COL_GRID_SESSION_MODIFIED_ON))
				.setLastReplicaIdClient(rs.getLong(COL_GRID_SESSION_REP_ID_CLIENT))
				.setLastReplicaIdService(rs.getLong(COL_GRID_SESSION_REP_ID_SERVICE)).setSourceEntityId(sourceId)
				.setSourceEntityVersionNumber(rs.getObject(COL_GRID_SESSION_SOURCE_VERSION, Long.class))
				.setGridJsonSchema$Id(rs.getString(COL_GRID_SESSION_SCHEMA_ID))
				.setAuthorizationMode(authMode);
	};

	private final RowMapper<GridReplica> REPLICA_MAPPER = (ResultSet rs, int rowNum) -> {
		return new GridReplica().setReplicaId(rs.getLong(COL_GRID_REPLICA_REPLICA_ID))
				.setCreatedBy(rs.getString(COL_GRID_REPLICA_CREATE_BY))
				.setCreatedOn(rs.getTime(COL_GRID_REPLICA_CREATE_ON))
				.setGridSessionId(rs.getString(COL_GRID_REPLICA_SESSION_ID))
				.setIsAgentReplica(rs.getBoolean(COL_GRID_REPLICA_IS_AGENT));
	};

	private final RowMapper<GridReplicaInfo> REPLICA_INFO_MAPPER = (ResultSet rs, int rowNum) -> {
		long replicaId = rs.getLong(COL_GRID_REPLICA_REPLICA_ID);
		boolean isAgent = rs.getBoolean(COL_GRID_REPLICA_IS_AGENT);
		GridReplicaType type;
		if (isAgent) {
			type = GridReplicaType.AGENT;
		} else if (GridConstants.isUserReplica(replicaId)) {
			type = GridReplicaType.USER;
		} else {
			type = GridReplicaType.SERVICE;
		}
		return new GridReplicaInfo()
				.setReplicaId(replicaId)
				.setCreatedBy(rs.getString(COL_GRID_REPLICA_CREATE_BY))
				.setIsConnected(rs.getBoolean("IS_CONNECTED"))
				.setReplicaType(type);
	};

	private final RowMapper<GridConnectionInfo> CONNECTION_MAPPER = (ResultSet rs, int rowNum) -> {
		return new GridConnectionInfo().setConnectionId(rs.getString(COL_GRID_CON_CONNECTION_ID))
				.setCreatedBy(rs.getLong(COL_GRID_CON_CREATED_BY))
				.setCreatedOn(rs.getTimestamp(COL_GRID_CON_CREATED_ON))
				.setSessionId(rs.getString(COL_GRID_CON_SESSION_ID)).setReplicaId(rs.getLong(COL_GRID_CON_REPLICA_ID))
				.setSource(EventSource.valueOf(rs.getString(COL_GRID_CON_SOURCE)));
	};

	private final RowMapper<PatchInfo> PATCH_INFO_MAPPER = (ResultSet rs, int rowNum) -> {
		return new PatchInfo().setSessionId(rs.getString(COL_GRID_PAT_SESSION_ID))
				.setPatchId(new LogicalTimestamp().setReplicaId(rs.getLong(COL_GRID_PAT_PATCH_ID_REP))
						.setSequenceNumber(rs.getLong(COL_GRID_PAT_PATCH_ID_SEQ)))
				.setCreatedOn(rs.getTimestamp(COL_GRID_PAT_CREATED_ON))
				.setExpiresOn(rs.getTimestamp(COL_GRID_PAT_EXPIRES_ON)).setS3Key(rs.getString(COL_GRID_PAT_S3_KEY))
				.setSizeBytes(rs.getObject(COL_GRID_PAT_SIZE_BYTES, Long.class));
	};

	private final RowMapper<GridSnapshot> SNAPSHOT_INFO_MAPPER = (ResultSet rs, int rowNum) -> {
		return new GridSnapshot()
				.setId(rs.getLong(COL_GRID_SNAPSHOT_ID))
				.setSessionId(rs.getString(COL_GRID_SNAPSHOT_SESSION_ID))
				.setClockTable(ClockTable.fromJsonArray(new JSONArray(rs.getString(COL_GRID_SNAPSHOT_CLOCK_TABLE))))
				.setCreatedOn(rs.getTimestamp(COL_GRID_SNAPSHOT_CREATED_ON))
				.setCreatedBy(rs.getLong(COL_GRID_SNAPSHOT_CREATED_BY))
				.setS3Key(rs.getString(COL_GRID_SNAPSHOT_S3_KEY));
	};

	private final RowMapper<LogicalTimestamp> TIMESTAMP_MAPPER = (ResultSet rs, int rowNum) -> {
		return new LogicalTimestamp().setReplicaId(rs.getLong(COL_GRID_PAT_PATCH_ID_REP))
				.setSequenceNumber(rs.getLong(COL_GRID_PAT_PATCH_ID_SEQ));
	};
	
	private final RowMapper<GridSource> GRID_SOURCE_MAPPER = (ResultSet rs, int rowNum) -> {
		return new GridSource(rs.getLong(COL_GRID_SESSION_SOURCE_ID),EntityType.valueOf(rs.getString(COL_NODE_TYPE)));
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
		long ownerId = create.getOwner() != null? create.getOwner(): create.getUserId();
		String authMode = create.getAuthorizationMode() == null ? null : create.getAuthorizationMode().name();
		Object[] args = { id, create.getUserId(), sessionId, repIdClient, repIdService, sourceId,
				create.getSourceVersion(), create.getSchemaId(), ownerId, authMode };
		int[] argTypes = { Types.BIGINT, Types.BIGINT, Types.VARCHAR, Types.BIGINT,
				Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.VARCHAR,
				Types.BIGINT, Types.VARCHAR };
		jdbcTemplate.update(
				"INSERT INTO GRID_SESSION (ID, ETAG, CREATED_BY, CREATED_ON, MODIFIED_ON, SESSION_ID, REP_ID_CLIENT, REP_ID_SERVICE, SOURCE_ID, SOURCE_VERSION, SCHEMA_ID, OWNER_ID, AUTHORIZATION_MODE)"
						+ " VALUES(?,UUID(),?,NOW(3),NOW(3),?,?,?,?,?,?,?,?)",
				args, argTypes);
		return getGridSession(sessionId).get();
	}

	@Override
	public Optional<Long> getGridSessionOwner(String gridSessionId) {
		ValidateArgument.required(gridSessionId, "gridSessionId");
		try {
			return Optional.of(jdbcTemplate.queryForObject(
					"SELECT OWNER_ID" + "  FROM GRID_SESSION WHERE SESSION_ID = ?", Long.class, gridSessionId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<GridSession> getGridSession(String gridSessionId) {
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
		switch (source.getRequestOrigin()) {
		case SERVICE:
			// decrement service.
			set = "REP_ID_SERVICE = REP_ID_SERVICE-1";
			select = "REP_ID_SERVICE";
			break;

		case USER:
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
	public Optional<Long> getReplicaCreatedBy(String sessionId, Long replicaId) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		try {
			return Optional.of(jdbcTemplate.queryForObject(
					"SELECT CREATED_BY FROM GRID_REPLICA WHERE SESSION_ID = ? AND REPLICA_ID = ?",
					Long.class, sessionId, replicaId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<GridReplicaInfo> getReplicaInfo(String sessionId, Long replicaId) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		try {
			return Optional.of(jdbcTemplate.queryForObject(
					"SELECT r.REPLICA_ID, r.CREATED_BY, r.IS_AGENT, (c.REPLICA_ID IS NOT NULL) AS IS_CONNECTED"
							+ " FROM GRID_REPLICA r"
							+ " LEFT JOIN GRID_CONNECTION c ON r.SESSION_ID = c.SESSION_ID AND r.REPLICA_ID = c.REPLICA_ID"
							+ " WHERE r.SESSION_ID = ? AND r.REPLICA_ID = ?",
					REPLICA_INFO_MAPPER, sessionId, replicaId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public List<GridReplicaInfo> listReplicas(String sessionId, long limit, long offset) {
		ValidateArgument.required(sessionId, "sessionId");
		return jdbcTemplate.query(
				"SELECT r.REPLICA_ID, r.CREATED_BY, r.IS_AGENT, (c.REPLICA_ID IS NOT NULL) AS IS_CONNECTED"
						+ " FROM GRID_REPLICA r"
						+ " LEFT JOIN GRID_CONNECTION c ON r.SESSION_ID = c.SESSION_ID AND r.REPLICA_ID = c.REPLICA_ID"
						+ " WHERE r.SESSION_ID = ?"
						+ " ORDER BY r.REPLICA_ID ASC"
						+ " LIMIT ? OFFSET ?",
				REPLICA_INFO_MAPPER,
				sessionId, limit, offset);
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
		Long id = idGenerator.generateNewId(IdType.GRID_CONNECTION_ID);

		jdbcTemplate.update(
				"INSERT INTO GRID_CONNECTION (ID, CONNECTION_ID, SESSION_ID, REPLICA_ID, CREATED_BY, CREATED_ON, SOURCE)"
						+ " VALUES (?,?,?,?,?,NOW(),?) ON DUPLICATE KEY UPDATE CONNECTION_ID = ?, CREATED_ON = NOW()",
				id, connection.getConnectionId(), connection.getSessionId(), connection.getReplicaId(),
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
    public Optional<GridConnectionInfo> getSingletonConnection(String sessionId, EventSource source) {
        ValidateArgument.required(sessionId, "sessionId");
        ValidateArgument.required(source, "source");
        if(!source.isSingleton()) {
        	return Optional.empty();
        }
        // This will be the largest replica ID that is within the bounds of our internal ID space
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM GRID_CONNECTION WHERE SESSION_ID = ? AND SOURCE = ? ORDER BY REPLICA_ID DESC LIMIT 1",
                    CONNECTION_MAPPER, sessionId, source.name()));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<GridConnectionInfo> getUserConnection(String sessionId, Long userId, EventSource source) {
        ValidateArgument.required(sessionId, "sessionId");
        ValidateArgument.required(userId, "userId");
        ValidateArgument.required(source, "source");
        return jdbcTemplate.query(
                "SELECT * FROM GRID_CONNECTION WHERE SESSION_ID = ? AND CREATED_BY = ? AND SOURCE = ?"
                        + " ORDER BY REPLICA_ID DESC LIMIT 1",
                CONNECTION_MAPPER, sessionId, userId, source.name()).stream().findFirst();
    }
    
	@Override
	public Optional<GridConnectionInfo> getConnection(String sessionId, Long replicaId) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		try {
			return Optional.of(
					jdbcTemplate.queryForObject("SELECT * FROM GRID_CONNECTION WHERE SESSION_ID = ? AND REPLICA_ID = ?",
							CONNECTION_MAPPER, sessionId, replicaId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public void removeConnection(String connectionId) {
		ValidateArgument.required(connectionId, "connectionId");
		jdbcTemplate.update("DELETE FROM GRID_CONNECTION WHERE CONNECTION_ID = ?", connectionId);

	}

	@WriteTransaction
	@Override
	public boolean savePatch(String sessionId, LogicalTimestamp patchId, String s3Key, long sizeBytes) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(patchId, "patchId");
		ValidateArgument.required(s3Key, "s3Key");

		Long id = idGenerator.generateNewId(IdType.GRID_SESSION_ID);
		return jdbcTemplate.update(
				"INSERT IGNORE INTO GRID_PATCH "
						+ "(ID, SESSION_ID, PATCH_ID_REP, PATCH_ID_SEQ, CREATED_ON, EXPIRES_ON, S3_KEY, SIZE_BYTES)"
						+ " VALUES (?,?,?,?,NOW(),NULL,?,?)",
				id, sessionId, patchId.getReplicaId(), patchId.getSequenceNumber(), s3Key, sizeBytes) > 0;
	}

	@WriteTransaction
	@Override
	public boolean saveSnapshot(String sessionId, ClockTable clockTable, String s3Key, Long createdByPrincipalId) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(clockTable, "clockTable");
		ValidateArgument.required(s3Key, "s3Key");

		Long id = idGenerator.generateNewId(IdType.GRID_SNAPSHOT_ID);
		return jdbcTemplate.update(
				"INSERT INTO GRID_SNAPSHOT "
						+ "(ID, SESSION_ID, CLOCK_TABLE, CREATED_ON, CREATED_BY, S3_KEY)"
						+ " VALUES (?,?,?,NOW(),?,?)",
				id, sessionId, clockTable.toJsonArray().toString(), createdByPrincipalId, s3Key) > 0;
	}

	@Override
	public Optional<GridSnapshot> getLatestSnapshot(String sessionId) {
		ValidateArgument.required(sessionId, "sessionId");

		try {
			return Optional.of(jdbcTemplate.queryForObject(
					"SELECT * FROM GRID_SNAPSHOT WHERE SESSION_ID = ? ORDER BY CREATED_ON DESC LIMIT 1",
					SNAPSHOT_INFO_MAPPER, sessionId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
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
	public List<PatchInfo> listMissingPatchInfoForClock(String sessionId, List<LogicalTimestamp> clock, long limit) {
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
		sql += " LIMIT ?;";
		return jdbcTemplate.query(sql, PATCH_INFO_MAPPER, sessionId, limit);
	}

	@Override
	public int countMissingPatchesForClock(String sessionId, List<LogicalTimestamp> clock) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(clock, "clock");
		if (clock.isEmpty()) {
			clock = List.of(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L));
		}
		StringJoiner rows = new StringJoiner(",");
		clock.forEach(id -> {
			rows.add(String.format("ROW(%d,%d)", id.getReplicaId(), id.getSequenceNumber()));
		});
		String sql = "SELECT COUNT(*) FROM ("  +
				String.format(LIST_MISSING_PATCHES, rows) +
				") as mp;";
		return jdbcTemplate.queryForObject(sql, Integer.class, sessionId);
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

	@Override
	public List<String> listAllSessionIds(long limit, long offset) {
		return jdbcTemplate.queryForList("SELECT SESSION_ID FROM GRID_SESSION ORDER BY ID ASC LIMIT ? OFFSET ?",
				String.class, limit, offset);
	}

	@Override
	public Optional<GridSource> getSessionSource(String sessionId) {
		ValidateArgument.required(sessionId, "sessionId");
		try {
			return Optional.of(jdbcTemplate.queryForObject(
					"SELECT G.SOURCE_ID, N.NODE_TYPE FROM GRID_SESSION G JOIN NODE N ON (G.SOURCE_ID = N.ID) WHERE G.SOURCE_ID IS NOT NULL AND G.SESSION_ID = ? ",
					GRID_SOURCE_MAPPER, sessionId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<AuthorizationMode> getAuthorizationMode(String sessionId) {
		ValidateArgument.required(sessionId, "sessionId");
		try {
			String value = jdbcTemplate.queryForObject(
					"SELECT AUTHORIZATION_MODE FROM GRID_SESSION WHERE SESSION_ID = ?", String.class, sessionId);
			return Optional.ofNullable(value == null ? null : AuthorizationMode.valueOf(value));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@WriteTransaction
	@Override
	public void updateSessionBenefactorIds(String sessionId, Set<Long> benefactorIds) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(benefactorIds, "benefactorIds");
		String json = new JSONArray(benefactorIds).toString();
		jdbcTemplate.update(
				"UPDATE GRID_SESSION SET ETAG = UUID(), MODIFIED_ON = NOW(3), BENEFACTOR_IDS = ? WHERE SESSION_ID = ?",
				json, sessionId);
	}

	@WriteTransaction
	@Override
	public void updateSourceEntityVersion(String sessionId, Long sourceVersion) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(sourceVersion, "sourceVersion");
		jdbcTemplate.update(
				"UPDATE GRID_SESSION SET ETAG = UUID(), MODIFIED_ON = NOW(3), SOURCE_VERSION = ? WHERE SESSION_ID = ?",
				sourceVersion, sessionId);
	}

	@WriteTransaction
	@Override
	public void updateSessionSchemaId(String sessionId, String schemaId) {
		ValidateArgument.required(sessionId, "sessionId");
		jdbcTemplate.update(
				"UPDATE GRID_SESSION SET ETAG = UUID(), MODIFIED_ON = NOW(3), SCHEMA_ID = ? WHERE SESSION_ID = ?",
				schemaId, sessionId);
	}

	@Override
	public Set<Long> getSessionBenefactorIds(String sessionId) {
		ValidateArgument.required(sessionId, "sessionId");
		try {
			String json = jdbcTemplate.queryForObject(
					"SELECT BENEFACTOR_IDS FROM GRID_SESSION WHERE SESSION_ID = ?", String.class, sessionId);
			if (json == null) {
				return Collections.emptySet();
			}
			JSONArray arr = new JSONArray(json);
			Set<Long> result = new HashSet<>();
			for (int i = 0; i < arr.length(); i++) {
				result.add(arr.getLong(i));
			}
			return result;
		} catch (EmptyResultDataAccessException e) {
			return Collections.emptySet();
		}
	}

}
