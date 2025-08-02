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
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.IndexNode;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Qualifier;
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

	private final String LIST_ARRAY_ORDER_SQL = loadStringFromClasspath("sql/ListArrayOrder.sql");
	private final String FIND_INSERT_LOCATION = loadStringFromClasspath("sql/FindInsertLocation.sql");

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

	private static RowMapper<ArrayNode> ARRAY_NODE_MAPPER = (ResultSet rs, int rowNum) -> {
		Boolean wasDeleted = rs.getBoolean("IS_DELETED");
		if (rs.wasNull()) {
			wasDeleted = null;
		}
		return new ArrayNode()
				.setArrayId(new LogicalTimestamp().setReplicaId(rs.getLong("ARR_REP"))
						.setSequenceNumber(rs.getLong("ARR_SEQ")))
				.setDataId(new LogicalTimestamp().setReplicaId(rs.getLong("DATA_REP"))
						.setSequenceNumber(rs.getLong("DATA_SEQ")))
				.setNodeId(new LogicalTimestamp().setReplicaId(rs.getLong("NODE_REP"))
						.setSequenceNumber(rs.getLong("NODE_SEQ")))
				.setReferenceNodeId(new LogicalTimestamp().setReplicaId(rs.getLong("REF_REP"))
						.setSequenceNumber(rs.getLong("REF_SEQ")))
				.setIsDeleted(wasDeleted);
	};

	private static RowMapper<MessageChain> MESSAGE_CHAIN_MAPPER = (ResultSet rs, int rowNum) -> {
		return new MessageChain().setSessionId(GridUtils.gridSessionIdAsString(rs.getLong("SESSION_ID")))
				.setReplicaId(rs.getLong("REPLICA_ID")).setId(rs.getInt("MESSAGE_ID"))
				.setMethod(rs.getString("METHOD_NAME")).setCreatedOn(rs.getTimestamp("CREATED_ON"));
	};

	public GridIndexDaoImpl(@Qualifier("gridDatabaseJdbcTemplate") JdbcTemplate gridDatabaseJdbcTemplate,
			@Qualifier("gridDatabaseNamedParameterJdbcTemplate") NamedParameterJdbcTemplate gridDatabaseNamedParameterJdbcTemplate) {
		super();
		this.jdbcTempalte = gridDatabaseJdbcTemplate;
		this.namedTemplate = gridDatabaseNamedParameterJdbcTemplate;
		createTables(List.of("schema/Grid-Replica-ddl.sql", "schema/Grid-Clock-ddl.sql", "schema/Grid-Index-ddl.sql",
				"schema/Grid-Array-ddl.sql", "schema/Grid-Vector-ddl.sql", "schema/Grid-Object-ddl.sql",
				"schema/Grid-Constant-ddl.sql", "schema/Grid-Value-ddl.sql", "schema/Grid-Message-ddl.sql"));
	}

	/**
	 * Create all of the tables from the classpath.
	 * 
	 * @param tables
	 */
	private void createTables(List<String> tables) {
		tables.forEach(t -> {
			String ddl = loadStringFromClasspath(t);
			log.info("Running: {}", t);
			this.jdbcTempalte.update(ddl);
		});
	}

	/**
	 * Helper to load a string from a file on the classpath.
	 * 
	 * @param name
	 * @return
	 */
	private static String loadStringFromClasspath(String name) {
		try (InputStream in = GridIndexDaoImpl.class.getClassLoader().getResourceAsStream(name)) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find file " + name + " on classpath.");
			}
			return IOUtils.toString(in, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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

	@Transactional(readOnly = false)
	@Override
	public void createArrayBatch(String sessionIdString, Long replicaId, List<LogicalTimestamp> arrayIds) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		if (arrayIds == null || arrayIds.isEmpty()) {
			return;
		}
		SqlParameterSource[] batchArgs = arrayIds.stream()
				.map(o -> new MapSqlParameterSource().addValue("sessionId", sessionId).addValue("replicaId", replicaId)
						.addValue("arrRep", o.getReplicaId()).addValue("arrSeq", o.getSequenceNumber()))
				.toArray(SqlParameterSource[]::new);
		namedTemplate.batchUpdate(
				"INSERT INTO GRID_REPLICA_ARR (SESSION_ID, REPLICA_ID, NODE_REP, NODE_SEQ, ARR_REP, ARR_SEQ)"
						+ "VALUES (:sessionId, :replicaId, :arrRep, :arrSeq, :arrRep, :arrSeq)",
				batchArgs);

	}

	@Transactional(readOnly = false)
	@Override
	public void insertIntoArray(String sessionIdString, Long replicaId, ArrayNode toInsert) {
		ValidateArgument.required(toInsert, "toInsert");
		ValidateArgument.required(toInsert.getDataId(), "toInsert.datatId");
		ValidateArgument.required(toInsert.getReferenceNodeId(), "toInsert.referenceNodeId");

		Long sessionId = validateReplica(sessionIdString, replicaId);
		MapSqlParameterSource params = createArrrayNodeParameter(sessionId, replicaId, toInsert);

		Optional<LogicalTimestamp> currentNodeId = getCurrentArrayNodeAtReference(params);
		if (currentNodeId.isPresent()) {
			params.addValue("currentNodeRep", currentNodeId.get().getReplicaId());
			params.addValue("currentNodeSeq", currentNodeId.get().getSequenceNumber());
			// clear the current node's reference
			namedTemplate
					.update("UPDATE GRID_REPLICA_ARR SET REF_REP = NULL, REF_SEQ = NULL WHERE SESSION_ID = :sessionId "
							+ "AND REPLICA_ID = :replicaId AND ARR_REP = :arrRep AND ARR_SEQ = :arrSeq "
							+ "AND NODE_REP = :currentNodeRep AND NODE_SEQ = :currentNodeSeq", params);
		}
		// insert the new node
		namedTemplate
				.update("INSERT INTO GRID_REPLICA_ARR (SESSION_ID, REPLICA_ID, NODE_REP, NODE_SEQ, ARR_REP, ARR_SEQ,"
						+ " DATA_REP, DATA_SEQ, REF_REP, REF_SEQ, IS_DELETED) "
						+ "VALUES (:sessionId, :replicaId, :nodeRep, :nodeSeq, :arrRep, :arrSeq,"
						+ " :dataRep, :dataSeq, :refRep, :refSeq, :isDeleted)", params);

		if (currentNodeId.isPresent()) {
			// set the current to point to the new node.
			namedTemplate.update(
					"UPDATE GRID_REPLICA_ARR SET REF_REP = :nodeRep, REF_SEQ = :nodeSeq WHERE SESSION_ID = :sessionId "
							+ "AND REPLICA_ID = :replicaId AND ARR_REP = :arrRep AND ARR_SEQ = :arrSeq "
							+ "AND NODE_REP = :currentNodeRep AND NODE_SEQ = :currentNodeSeq",
					params);
		}
	}

	/**
	 * Get the ID of the {@link ArrayNode} currently pointing to the provided
	 * reference.
	 * 
	 * @param params
	 * @return
	 */
	Optional<LogicalTimestamp> getCurrentArrayNodeAtReference(MapSqlParameterSource params) {
		try {
			return Optional.of(namedTemplate.queryForObject(
					"SELECT NODE_REP, NODE_SEQ FROM GRID_REPLICA_ARR WHERE SESSION_ID = :sessionId "
							+ "AND REPLICA_ID = :replicaId AND ARR_REP = :arrRep AND ARR_SEQ = :arrSeq "
							+ "AND REF_REP = :refRep AND REF_SEQ = :refSeq FOR UPDATE",
					params, (ResultSet rs, int rowNum) -> {
						return new LogicalTimestamp().setReplicaId(rs.getLong("NODE_REP"))
								.setSequenceNumber(rs.getLong("NODE_SEQ"));
					}));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	MapSqlParameterSource createArrrayNodeParameter(Long sessionId, Long replicaId, ArrayNode node) {
		return new MapSqlParameterSource().addValue("sessionId", sessionId).addValue("replicaId", replicaId)
				.addValue("nodeRep", node.getId().getReplicaId()).addValue("nodeSeq", node.getId().getSequenceNumber())
				.addValue("arrRep", node.getArrayId().getReplicaId())
				.addValue("arrSeq", node.getArrayId().getSequenceNumber())
				.addValue("dataRep", node.getDataId() != null ? node.getDataId().getReplicaId() : null)
				.addValue("dataSeq", node.getDataId() != null ? node.getDataId().getSequenceNumber() : null)
				.addValue("refRep", node.getReferenceNodeId() != null ? node.getReferenceNodeId().getReplicaId() : null)
				.addValue("refSeq",
						node.getReferenceNodeId() != null ? node.getReferenceNodeId().getSequenceNumber() : null)
				.addValue("isDeleted", node.getIsDeleted());
	}

	@Override
	public List<ArrayNode> getArrayNodesInOrder(String sessionIdString, Long replicaId, LogicalTimestamp arrayId,
			Long limit, Long offset) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("sessionId", sessionId);
		params.addValue("replicaId", replicaId);
		params.addValue("arrRep", arrayId.getReplicaId());
		params.addValue("arrSeq", arrayId.getSequenceNumber());
		params.addValue("limit", limit);
		params.addValue("offset", offset);

		return namedTemplate.query(LIST_ARRAY_ORDER_SQL, params, ARRAY_NODE_MAPPER);
	}

	@Override
	public Optional<LogicalTimestamp> findArrayInsertLocation(String sessionIdString, Long replicaId,
			ArrayNode toInsert) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		MapSqlParameterSource param = createArrrayNodeParameter(sessionId, replicaId, toInsert);
		try {
			/*
			 * This query will recursively walk the RGA starting at the new node's reference
			 * ID. The first node in the walk with a data value less than or equal to the
			 * new data's value will be returned. If there are no nodes that meet this
			 * condition, then the last node in the RGA will be returned.
			 */
			ArrayNode finalCursorNode = namedTemplate.queryForObject(FIND_INSERT_LOCATION, param, ARRAY_NODE_MAPPER);
			//
			int comp = finalCursorNode.getDataId().compareTo(toInsert.getDataId());
			if (comp == 0) {
				/*
				 * The data at the cursor position matches the new data, so the new node does
				 * not need to be inserted.
				 */
				return Optional.empty();
			}
			if (comp < 0) {
				/*
				 * The cursor node's data is less than the new data so the new node will be
				 * inserted at the cursor node's position with the cursor node referencing the
				 * new node.
				 */
				return Optional.of(finalCursorNode.getReferenceNodeId());
			}
			/*
			 * The cursor node's data is greater than the new data, but there are no more
			 * nodes in the RGA so the new node appended to the end of the RGA by
			 * referencing the cursor node.
			 */
			return Optional.of(finalCursorNode.getNodeId());
		} catch (EmptyResultDataAccessException e) {
			// no conflict
			return Optional.of(toInsert.getReferenceNodeId());
		}
	}

	@Transactional(readOnly = false)
	@Override
	public Integer createNextMessageId(String sessionIdString, Long replicaId, int maxValue) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		Integer current = jdbcTempalte.queryForObject(
				"SELECT LAST_MESSAGE_ID FROM GRID_REPLICA WHERE SESSION_ID = ? AND REPLICA_ID = ? FOR UPDATE",
				Integer.class, sessionId, replicaId);
		if (current >= maxValue) {
			current = -1;
		}
		Integer next = current + 1;
		jdbcTempalte.update("UPDATE GRID_REPLICA SET LAST_MESSAGE_ID = ? WHERE SESSION_ID = ? AND REPLICA_ID = ?", next,
				sessionId, replicaId);
		return next;
	}

	@Transactional(readOnly = false)
	@Override
	public MessageChain createMessageChain(MessageChain chain) {
		ValidateArgument.required(chain, "chain");
		ValidateArgument.required(chain.getId(), "chain.id");
		ValidateArgument.required(chain.getSessionId(), "chain.sessionId");
		ValidateArgument.required(chain.getReplicaId(), "chain.replicaId");
		ValidateArgument.required(chain.getMethod(), "chain.method");
		Long sessionId = validateReplica(chain.getSessionId(), chain.getReplicaId());
		jdbcTempalte.update(
				"INSERT INTO GRID_REPLICA_MESSAGE (SESSION_ID, REPLICA_ID, MESSAGE_ID, METHOD_NAME, CREATED_ON)"
						+ " VALUES (?,?,?,?,NOW()) ON DUPLICATE KEY UPDATE METHOD_NAME = ?, CREATED_ON = NOW()",
				sessionId, chain.getReplicaId(), chain.getId(), chain.getMethod(), chain.getMethod());
		return getMessageChain(chain.getSessionId(), chain.getReplicaId(), chain.getId()).get();
	}

	@Override
	public Optional<MessageChain> getMessageChain(String sessionIdString, Long replicaId, Integer chainId) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		ValidateArgument.required(chainId, "chainId");
		try {
			return Optional.of(jdbcTempalte.queryForObject(
					"SELECT * FROM GRID_REPLICA_MESSAGE WHERE SESSION_ID = ? AND REPLICA_ID = ? AND MESSAGE_ID = ?",
					MESSAGE_CHAIN_MAPPER, sessionId, replicaId, chainId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Transactional(readOnly = false)
	@Override
	public void deleteMessageChain(String sessionIdString, Long replicaId, Integer chainId) {
		Long sessionId = validateReplica(sessionIdString, replicaId);
		ValidateArgument.required(chainId, "chainId");
		jdbcTempalte.update(
				"DELETE FROM GRID_REPLICA_MESSAGE WHERE SESSION_ID = ? AND REPLICA_ID = ? AND MESSAGE_ID = ?",
				sessionId, replicaId, chainId);
	}

	@Override
	public Optional<ObjectNode> getRootObject(String gridSessionId, Long replicaId) {
		List<ValueNode> roots = getValues(gridSessionId, replicaId,
				List.of(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L)));
		if (roots.isEmpty()) {
			return Optional.empty();
		}
		List<ObjectNode> rootObjects = getObjects(gridSessionId, replicaId,
				roots.stream().map(v -> v.getValue()).collect(Collectors.toList()));
		if (rootObjects.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(rootObjects.get(0));
	}

	@Override
	public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) {
		return namedTemplate.query(sql, paramSource, rowMapper);
	}
}