package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.dbo.persistence.table.DBODefiningSqlObject.DEFAULT_VERSION;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.entity.IdAndVersionBuilder;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.collect.ImmutableMap;

@Repository
public class DefiningSqlDependencyDaoImpl implements DefiningSqlDependencyDao {

	private static final RowMapper<IdAndVersion> ID_AND_VERSION_MAPPER = (rs, i) -> {

		Long id = rs.getLong(1);
		Long version = rs.getLong(2);

		IdAndVersionBuilder builder = IdAndVersion.newBuilder().setId(id);

		if (!DEFAULT_VERSION.equals(version)) {
			builder.setVersion(version);
		}

		return builder.build();
	};

	private final NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	public DefiningSqlDependencyDaoImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
		this.jdbcTemplate = namedJdbcTemplate;
	}

	@Override
	@WriteTransaction
	public void addSourceTables(IdAndVersion objectId, String objectType, Set<IdAndVersion> sourceTableIds) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.requiredNotBlank(objectType, "objectType");
		ValidateArgument.required(sourceTableIds, "sourceTableIds");

		// Make sure to insert the owner id row or update its etag for migration purposes
		jdbcTemplate.getJdbcTemplate().update(
				"INSERT INTO DEFINING_SQL_OBJECT VALUES(?, UUID()) ON DUPLICATE KEY UPDATE ETAG = UUID()",
				objectId.getId());

		if (sourceTableIds.isEmpty()) {
			return;
		}

		List<IdAndVersion> batch = new ArrayList<>(sourceTableIds);

		jdbcTemplate.getJdbcTemplate().batchUpdate(
				"INSERT IGNORE INTO DEFINING_SQL_DEPENDENCY VALUES(?,?,?,?,?)",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						IdAndVersion sourceTableId = batch.get(i);
						int index = 1;
						ps.setLong(index++, objectId.getId());
						ps.setLong(index++, objectId.getVersion().orElse(DEFAULT_VERSION));
						ps.setString(index++, objectType);
						ps.setLong(index++, sourceTableId.getId());
						ps.setLong(index, sourceTableId.getVersion().orElse(DEFAULT_VERSION));
					}

					@Override
					public int getBatchSize() {
						return batch.size();
					}
				});
	}

	@Override
	@WriteTransaction
	public void deleteSourceTables(IdAndVersion objectId, Set<IdAndVersion> sourceTableIds) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(sourceTableIds, "sourceTableIds");

		if (sourceTableIds.isEmpty()) {
			return;
		}

		List<Long[]> targetIdsParam = sourceTableIds.stream()
				.map(id -> new Long[] { id.getId(), id.getVersion().orElse(DEFAULT_VERSION) })
				.collect(Collectors.toList());

		Map<String, ?> params = ImmutableMap.of("objectId", objectId.getId(), "objectVersion",
				objectId.getVersion().orElse(DEFAULT_VERSION), "idAndVersionList", targetIdsParam);

		jdbcTemplate.update(
				"DELETE FROM DEFINING_SQL_DEPENDENCY WHERE OBJECT_ID = :objectId AND OBJECT_VERSION = :objectVersion"
						+ " AND (SOURCE_TABLE_ID, SOURCE_TABLE_VERSION) IN (:idAndVersionList)",
				params);
	}

	@Override
	public Set<IdAndVersion> getSourceTables(IdAndVersion objectId) {
		ValidateArgument.required(objectId, "objectId");

		List<IdAndVersion> sourceTableIds = jdbcTemplate.getJdbcTemplate().query(
				"SELECT SOURCE_TABLE_ID, SOURCE_TABLE_VERSION FROM DEFINING_SQL_DEPENDENCY WHERE OBJECT_ID = ?"
						+ " AND OBJECT_VERSION = ?",
				ID_AND_VERSION_MAPPER, objectId.getId(), objectId.getVersion().orElse(DEFAULT_VERSION));

		return new HashSet<>(sourceTableIds);
	}

	@Override
	@WriteTransaction
	public void setSourceTable(IdAndVersion objectId, String objectType, IdAndVersion sourceTableId) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.requiredNotBlank(objectType, "objectType");
		ValidateArgument.required(sourceTableId, "sourceTableId");

		deleteObject(objectId);
		addSourceTables(objectId, objectType, Set.of(sourceTableId));
	}

	@Override
	public Optional<IdAndVersion> getSourceTable(IdAndVersion objectId) {
		ValidateArgument.required(objectId, "objectId");

		List<IdAndVersion> sources = new ArrayList<>(getSourceTables(objectId));

		if (sources.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(sources.get(0));
	}

	@Override
	@WriteTransaction
	public void deleteObject(IdAndVersion objectId) {
		ValidateArgument.required(objectId, "objectId");

		jdbcTemplate.getJdbcTemplate().update(
				"DELETE FROM DEFINING_SQL_DEPENDENCY WHERE OBJECT_ID = ? AND OBJECT_VERSION = ?", objectId.getId(),
				objectId.getVersion().orElse(DEFAULT_VERSION));
	}

	@Override
	public List<DependentObject> getDependentsPage(IdAndVersion sourceTableId, long limit, long offset) {
		ValidateArgument.required(sourceTableId, "sourceTableId");

		return jdbcTemplate.getJdbcTemplate().query(
				"SELECT OBJECT_ID, OBJECT_VERSION, OBJECT_TYPE FROM DEFINING_SQL_DEPENDENCY WHERE SOURCE_TABLE_ID = ?"
						+ " AND SOURCE_TABLE_VERSION = ? ORDER BY OBJECT_TYPE, OBJECT_ID, OBJECT_VERSION"
						+ " LIMIT ? OFFSET ?",
				(rs, i) -> new DependentObject(ID_AND_VERSION_MAPPER.mapRow(rs, i), rs.getString("OBJECT_TYPE")),
				sourceTableId.getId(), sourceTableId.getVersion().orElse(DEFAULT_VERSION), limit, offset);
	}

}
