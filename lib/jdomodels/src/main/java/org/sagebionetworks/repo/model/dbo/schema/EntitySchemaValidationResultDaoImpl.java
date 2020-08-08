package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_IS_VALID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SCHEMA_VALIDATION_RESULTS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EntitySchemaValidationResultDaoImpl implements EntitySchemaValidationResultDao {

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	SchemaValidationResultDao schemaValidationResultDao;

	@Override
	public ValidationResults getValidationResults(String entityId) {
		return schemaValidationResultDao.getValidationResults(entityId, ObjectType.entity);
	}

	@Override
	public ValidationSummaryStatistics getEntityValidationStatistics(String containerId, Set<Long> childIdsToExclude) {
		ValidateArgument.required(containerId, "containerId");
		ValidateArgument.required(childIdsToExclude, "childIdsToExclude");
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("containerId", KeyFactory.stringToKey(containerId));
		paramSource.addValue("childIdsToExclude", childIdsToExclude);
		paramSource.addValue("objectType", ObjectType.entity.name());
		String excludeChildfilter = "";
		if (!childIdsToExclude.isEmpty()) {
			excludeChildfilter = "AND N. NOT IN (:childIdsToExclude)";
		}
		String sql = "SELECT S."+COL_JSON_SCHEMA_VALIDATION_IS_VALID+", COUNT(*) FROM " + TABLE_NODE + " N LEFT JOIN " + TABLE_SCHEMA_VALIDATION_RESULTS + " S ON (N."
				+ COL_NODE_ID + " = S." + COL_JSON_SCHEMA_VALIDATION_OBJECT_ID + " AND S."
				+ COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE + " = :objectType) WHERE N." + COL_NODE_PARENT_ID
				+ " = :containerId "+excludeChildfilter+" GROUP BY S." + COL_JSON_SCHEMA_VALIDATION_IS_VALID;
		return namedParameterJdbcTemplate.queryForObject(sql, paramSource,
				new RowMapper<ValidationSummaryStatistics>() {

					@Override
					public ValidationSummaryStatistics mapRow(ResultSet rs, int rowNum) throws SQLException {
						ValidationSummaryStatistics stats = new ValidationSummaryStatistics();

						return stats;
					}
				});
	}

	@Override
	public List<ValidationResults> getInvalidEntitySchemaValidationPage(String containerId, Set<Long> childIdsToExclude,
			long limit, long offset) {
		ValidateArgument.required(containerId, "containerId");
		ValidateArgument.required(childIdsToExclude, "childIdsToExclude");
		return null;
	}

}
