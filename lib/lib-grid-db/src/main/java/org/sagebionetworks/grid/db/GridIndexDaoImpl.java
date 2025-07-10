package org.sagebionetworks.grid.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.IndexNode;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, transactionManager = "gridTransactionManager")
public class GridIndexDaoImpl implements GridIndexDao {

	private static final Logger log = LogManager.getLogger(GridIndexDaoImpl.class);

	private final JdbcTemplate jdbcTempalte;
	private final NamedParameterJdbcTemplate namedTemplate;

	private static RowMapper<IndexNode> INDEX_NODE_MAPPER = (ResultSet rs, int rowNum) -> {
		return new IndexNode().setType(IndexType.valueOf(rs.getString("KIND"))).setId(
				new LogicalTimestamp().setReplicaId(rs.getLong("NODE_REP")).setSequenceNumber(rs.getLong("NODE_SEQ")));
	};

	private static RowMapper<ConstantNode> CONSTANT_NODE_MAPPER = (ResultSet rs, int rowNum) -> {
		return new ConstantNode().setId(
				new LogicalTimestamp().setReplicaId(rs.getLong("CON_REP")).setSequenceNumber(rs.getLong("CON_SEQ")))
				.setValueFromJson(rs.getString("CON_VAL"));
	};

	private static RowMapper<LogicalTimestamp> CLOCK_MAPPER = (ResultSet rs, int rowNum) -> {
		return new LogicalTimestamp().setReplicaId(rs.getLong("CLOCK_ID_REP"))
				.setSequenceNumber(rs.getLong("CLOCK_ID_SEQ"));
	};

	private static RowMapper<ObjectNode> OBJECT_NODE_MAPPER = (ResultSet rs, int rowNum) -> {
		return new ObjectNode().setId(
				new LogicalTimestamp().setReplicaId(rs.getLong("OBJ_REP")).setSequenceNumber(rs.getLong("OBJ_SEQ")))
				.setValueFromJson(rs.getString("OBJ_VAL"));
	};

	private static RowMapper<ValueNode> VALUE_NODE_MAPPER = (ResultSet rs, int rowNum) -> {
		return new ValueNode().setId(
				new LogicalTimestamp().setReplicaId(rs.getLong("VAL_REP")).setSequenceNumber(rs.getLong("VAL_SEQ")))
				.setValueFromJson(rs.getString("VAL_REF"));
	};

	private static RowMapper<VectorNode> VECTOR_NODE_MAPPER = (ResultSet rs, int rowNum) -> {
		return new VectorNode().setId(
				new LogicalTimestamp().setReplicaId(rs.getLong("VEC_REP")).setSequenceNumber(rs.getLong("VEC_SEQ")))
				.setValueFromJson(rs.getString("VEC_VAL"));
	};

	public GridIndexDaoImpl(JdbcTemplate gridDatabaseJdbcTempalte,
			NamedParameterJdbcTemplate gridDatabaseNamedParameterJdbcTempalte) {
		super();
		this.jdbcTempalte = gridDatabaseJdbcTempalte;
		this.namedTemplate = gridDatabaseNamedParameterJdbcTempalte;
		createTables(List.of("schema/Grid-Replica-ddl.sql", "schema/Grid-Clock-ddl.sql", "schema/Grid-Index-ddl.sql",
				"schema/Grid-Array-ddl.sql", "schema/Grid-Vector-ddl.sql", "schema/Grid-Object-ddl.sql",
				"schema/Grid-Constant-ddl.sql", "schema/Grid-Value-ddl.sql"));
	}

	/**
	 * Create all of the tables from the classpath.
	 * 
	 * @param tables
	 */
	private void createTables(List<String> tables) {
		tables.forEach(t -> {
			try (InputStream in = GridIndexDaoImpl.class.getClassLoader().getResourceAsStream(t)) {
				if (in == null) {
					throw new IllegalArgumentException("Cannot find file " + t + " on classpath.");
				}
				String ddl = IOUtils.toString(in, StandardCharsets.UTF_8);
				log.info("Running: {}", t);
				this.jdbcTempalte.update(ddl);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	Long validateReplica(String sessionId, Long replicaId) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		return GridUtils.gridSessionIdAsLong(sessionId);
	}

	@Transactional(readOnly = false)
	@Override
	public boolean createReplicaIfNotExists(String sessionIdString, Long replicaId) {
		Long sessionId = validateReplica(sessionIdString, replicaId);

		return jdbcTempalte.update(
				"INSERT IGNORE INTO GRID_REPLICA (SESSION_ID, REPLICA_ID, CREATED_ON) VALUES (?,?,NOW())", sessionId,
				replicaId) > 0;

	}

	@Transactional(readOnly = false)
	@Override
	public void deleteReplica(String sessionIdString, Long replicaId) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		jdbcTempalte.update("DELETE FROM GRID_REPLICA WHERE SESSION_ID = ? AND REPLICA_ID = ?", sessionId, replicaId);
	}

	@Override
	public Optional<Timestamp> getReplicaCreatedOn(String sessionIdString, Long replicaId) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		try {
			return Optional.of(jdbcTempalte.queryForObject(
					"SELECT CREATED_ON FROM GRID_REPLICA WHERE SESSION_ID = ? AND REPLICA_ID = ?", Timestamp.class,
					sessionId, replicaId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Transactional(readOnly = false)
	@Override
	public void saveIndex(String sessionIdString, Long replicaId, IndexType type, List<LogicalTimestamp> batch) {
		ValidateArgument.required(type, "type");
		Long sessionId = validateReplica(sessionIdString, replicaId);
		if (batch == null || batch.isEmpty()) {
			// Nothing to save, so we can return early.
			return;
		}
		SqlParameterSource[] batchArgs = batch.stream()
				.map(ts -> new MapSqlParameterSource().addValue("sessionId", sessionId).addValue("replicaId", replicaId)
						.addValue("nodeRep", ts.getReplicaId()).addValue("nodeSeq", ts.getSequenceNumber())
						.addValue("kind", type.name()))
				.toArray(SqlParameterSource[]::new);

		namedTemplate.batchUpdate("INSERT INTO GRID_REPLICA_INDEX (SESSION_ID, REPLICA_ID, NODE_REP, NODE_SEQ, KIND) "
				+ "VALUES (:sessionId, :replicaId, :nodeRep, :nodeSeq, :kind)", batchArgs);
	}

	@Override
	public List<IndexNode> getIndices(String sessionIdString, Long replicaId, List<LogicalTimestamp> ids) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList(); // Return an empty list if there's nothing to find.
		}

		MapSqlParameterSource params = createParameters(sessionId, replicaId, ids);

		return namedTemplate.query("SELECT NODE_REP, NODE_SEQ, KIND FROM GRID_REPLICA_INDEX "
				+ "WHERE SESSION_ID = :sessionId AND REPLICA_ID = :replicaId AND (NODE_REP, NODE_SEQ) IN (:ids)",
				params, INDEX_NODE_MAPPER);
	}

	@Transactional(readOnly = false)
	@Override
	public void setClock(String sessionIdString, Long replicaId, LogicalTimestamp clock) {
		ValidateArgument.required(clock, "clock");
		ValidateArgument.required(clock.getReplicaId(), "clock.replicaId");
		ValidateArgument.required(clock.getSequenceNumber(), "clock.sequenceNumber");
		Long sessionId = validateReplica(sessionIdString, replicaId);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("sessionId", sessionId);
		params.addValue("replicaId", replicaId);
		params.addValue("clockRep", clock.getReplicaId());
		params.addValue("clockSeq", clock.getSequenceNumber());

		namedTemplate.update(
				"INSERT INTO GRID_REPLICA_CLOCK (SESSION_ID, REPLICA_ID, CLOCK_ID_REP, CLOCK_ID_SEQ) VALUES"
						+ " (:sessionId,:replicaId,:clockRep,:clockSeq) ON DUPLICATE KEY UPDATE CLOCK_ID_SEQ = :clockSeq",
				params);
	}

	@Override
	public List<LogicalTimestamp> getClock(String sessionIdString, Long replicaId) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("sessionId", sessionId);
		params.addValue("replicaId", replicaId);
		return namedTemplate.query("SELECT CLOCK_ID_REP, CLOCK_ID_SEQ FROM GRID_REPLICA_CLOCK "
				+ "WHERE SESSION_ID = :sessionId AND REPLICA_ID = :replicaId " + "ORDER BY CLOCK_ID_REP, CLOCK_ID_SEQ",
				params, CLOCK_MAPPER);
	}

	@Override
	public Optional<Long> getClockSequenceNumber(String sessionIdString, Long replicaId, Long clockIdRep) {
		ValidateArgument.required(clockIdRep, "clockIdRep");
		Long sessionId = validateReplica(sessionIdString, replicaId);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("sessionId", sessionId);
		params.addValue("replicaId", replicaId);
		params.addValue("clockRep", clockIdRep);

		// query on empty exception return optional empty
		try {
			return Optional.of(namedTemplate.queryForObject(
					"SELECT CLOCK_ID_SEQ FROM GRID_REPLICA_CLOCK "
							+ "WHERE SESSION_ID = :sessionId AND REPLICA_ID = :replicaId AND CLOCK_ID_REP = :clockRep ",
					params, Long.class));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public List<ConstantNode> getConstants(String sessionIdString, Long replicaId, List<LogicalTimestamp> ids) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		MapSqlParameterSource params = createParameters(sessionId, replicaId, ids);

		return namedTemplate.query(
				"SELECT CON_REP, CON_SEQ, CON_VAL FROM GRID_REPLICA_CON "
						+ "WHERE SESSION_ID = :sessionId AND REPLICA_ID = :replicaId AND (CON_REP, CON_SEQ) IN (:ids)",
				params, CONSTANT_NODE_MAPPER);
	}

	MapSqlParameterSource createParameters(Long sessionId, Long replicaId, List<LogicalTimestamp> ids) {
		List<Object[]> idTuples = ids.stream().map(ts -> new Object[] { ts.getReplicaId(), ts.getSequenceNumber() })
				.collect(Collectors.toList());

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("sessionId", sessionId);
		params.addValue("replicaId", replicaId);
		params.addValue("ids", idTuples);
		return params;
	}

	@Transactional(readOnly = false)
	@Override
	public void saveNewConstants(String sessionIdString, Long replicaId, List<ConstantNode> batch) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		if (batch == null || batch.isEmpty()) {
			return;
		}
		SqlParameterSource[] batchArgs = batch.stream()
				.map(cn -> new MapSqlParameterSource().addValue("sessionId", sessionId).addValue("replicaId", replicaId)
						.addValue("conRep", cn.getId().getReplicaId())
						.addValue("conSeq", cn.getId().getSequenceNumber()).addValue("value", cn.getValueAsJson()))
				.toArray(SqlParameterSource[]::new);

		namedTemplate
				.batchUpdate("INSERT IGNORE INTO GRID_REPLICA_CON (SESSION_ID, REPLICA_ID, CON_REP, CON_SEQ, CON_VAL) "
						+ "VALUES (:sessionId, :replicaId, :conRep, :conSeq, :value)", batchArgs);

	}

	@Transactional(readOnly = false)
	@Override
	public void truncateAll() {
		jdbcTempalte.update("DELETE FROM GRID_REPLICA WHERE SESSION_ID > -1 AND REPLICA_ID > -1");
	}

	@Transactional(readOnly = false)
	@Override
	public void saveObjects(String sessionIdString, Long replicaId, List<ObjectNode> batch) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		if (batch == null || batch.isEmpty()) {
			return;
		}
		SqlParameterSource[] batchArgs = batch.stream()
				.map(o -> new MapSqlParameterSource().addValue("sessionId", sessionId).addValue("replicaId", replicaId)
						.addValue("objRep", o.getId().getReplicaId()).addValue("objSeq", o.getId().getSequenceNumber())
						.addValue("value", o.getValueAsJson()))
				.toArray(SqlParameterSource[]::new);
		namedTemplate.batchUpdate("INSERT INTO GRID_REPLICA_OBJ (SESSION_ID, REPLICA_ID, OBJ_REP, OBJ_SEQ, OBJ_VAL) "
				+ "VALUES (:sessionId, :replicaId, :objRep, :objSeq, :value) ON DUPLICATE KEY UPDATE OBJ_VAL = :value",
				batchArgs);

	}

	@Override
	public List<ObjectNode> getObjects(String sessionIdString, Long replicaId, List<LogicalTimestamp> ids) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		MapSqlParameterSource params = createParameters(sessionId, replicaId, ids);

		return namedTemplate.query(
				"SELECT OBJ_REP, OBJ_SEQ, OBJ_VAL FROM GRID_REPLICA_OBJ "
						+ "WHERE SESSION_ID = :sessionId AND REPLICA_ID = :replicaId AND (OBJ_REP, OBJ_SEQ) IN (:ids)",
				params, OBJECT_NODE_MAPPER);
	}

	@Transactional(readOnly = false)
	@Override
	public void saveValues(String sessionIdString, Long replicaId, List<ValueNode> batch) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		if (batch == null || batch.isEmpty()) {
			return;
		}
		SqlParameterSource[] batchArgs = batch.stream()
				.map(o -> new MapSqlParameterSource().addValue("sessionId", sessionId).addValue("replicaId", replicaId)
						.addValue("valRep", o.getId().getReplicaId()).addValue("valSeq", o.getId().getSequenceNumber())
						.addValue("ref", o.getValueAsJson()))
				.toArray(SqlParameterSource[]::new);
		namedTemplate.batchUpdate("INSERT INTO GRID_REPLICA_VAL (SESSION_ID, REPLICA_ID, VAL_REP, VAL_SEQ, VAL_REF) "
				+ "VALUES (:sessionId, :replicaId, :valRep, :valSeq, :ref) ON DUPLICATE KEY UPDATE VAL_REF = :ref",
				batchArgs);

	}

	@Override
	public List<ValueNode> getValues(String sessionIdString, Long replicaId, List<LogicalTimestamp> ids) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		MapSqlParameterSource params = createParameters(sessionId, replicaId, ids);

		return namedTemplate.query(
				"SELECT VAL_REP, VAL_SEQ, VAL_REF FROM GRID_REPLICA_VAL "
						+ "WHERE SESSION_ID = :sessionId AND REPLICA_ID = :replicaId AND (VAL_REP, VAL_SEQ) IN (:ids)",
				params, VALUE_NODE_MAPPER);
	}

	@Transactional(readOnly = false)
	@Override
	public void saveVectors(String sessionIdString, Long replicaId, List<VectorNode> batch) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		if (batch == null || batch.isEmpty()) {
			return;
		}
		SqlParameterSource[] batchArgs = batch.stream()
				.map(o -> new MapSqlParameterSource().addValue("sessionId", sessionId).addValue("replicaId", replicaId)
						.addValue("vecRep", o.getId().getReplicaId()).addValue("vecSeq", o.getId().getSequenceNumber())
						.addValue("vecVal", o.getValueAsJson()))
				.toArray(SqlParameterSource[]::new);
		namedTemplate.batchUpdate("INSERT INTO GRID_REPLICA_VEC (SESSION_ID, REPLICA_ID, VEC_REP, VEC_SEQ, VEC_VAL) "
				+ "VALUES (:sessionId, :replicaId, :vecRep, :vecSeq, :vecVal) ON DUPLICATE KEY UPDATE VEC_VAL = :vecVal",
				batchArgs);
	}

	@Override
	public List<VectorNode> getVectors(String sessionIdString, Long replicaId, List<LogicalTimestamp> ids) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		MapSqlParameterSource params = createParameters(sessionId, replicaId, ids);

		return namedTemplate.query(
				"SELECT VEC_REP, VEC_SEQ, VEC_VAL FROM GRID_REPLICA_VEC "
						+ "WHERE SESSION_ID = :sessionId AND REPLICA_ID = :replicaId AND (VEC_REP, VEC_SEQ) IN (:ids)",
				params, VECTOR_NODE_MAPPER);
	}
}