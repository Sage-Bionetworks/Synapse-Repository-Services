package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_IS_VALID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SCHEMA_VALIDATION_RESULTS;

import java.sql.ResultSet;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EntitySchemaValidationResultDaoImpl implements EntitySchemaValidationResultDao {

	private static final String OFFSET = "offset";

	private static final String LIMIT = "limit";

	private static final String OBJECT_TYPE = "objectType";

	private static final String CHILD_IDS_TO_EXCLUDE = "childIdsToExclude";

	public static final String CONTAINER_ID = "containerId";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	SchemaValidationResultDao schemaValidationResultDao;

	@Autowired
	public EntitySchemaValidationResultDaoImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
			SchemaValidationResultDao schemaValidationResultDao) {
		super();
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.schemaValidationResultDao = schemaValidationResultDao;
	}

	@Override
	public ValidationResults getValidationResults(String entityId) {
		return schemaValidationResultDao.getValidationResults(entityId, ObjectType.entity);
	}

	@Override
	public ValidationSummaryStatistics getEntityValidationStatistics(String containerId, Set<Long> childIdsToExclude) {
		ValidateArgument.required(containerId, CONTAINER_ID);
		ValidateArgument.required(childIdsToExclude, CHILD_IDS_TO_EXCLUDE);
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue(CONTAINER_ID, KeyFactory.stringToKey(containerId));
		paramSource.addValue(CHILD_IDS_TO_EXCLUDE, childIdsToExclude);
		paramSource.addValue(OBJECT_TYPE, ObjectType.entity.name());
		String excludeChildfilter = "";
		if (!childIdsToExclude.isEmpty()) {
			excludeChildfilter = " AND N." + COL_NODE_ID + " NOT IN (:" + CHILD_IDS_TO_EXCLUDE + ")";
		}
		ValidationSummaryStatistics results = new ValidationSummaryStatistics();
		results.setContainerId(containerId);
		results.setGeneratedOn(new Date());
		// default all values to zero
		results.setNumberOfInvalidChildren(0L);
		results.setNumberOfValidChildren(0L);
		results.setNumberOfUnknownChildren(0L);
		String sql = "SELECT S." + COL_JSON_SCHEMA_VALIDATION_IS_VALID + ", COUNT(*) FROM " + TABLE_NODE
				+ " N LEFT JOIN " + TABLE_SCHEMA_VALIDATION_RESULTS + " S ON (N." + COL_NODE_ID + " = S."
				+ COL_JSON_SCHEMA_VALIDATION_OBJECT_ID + " AND S." + COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE + " = :"
				+ OBJECT_TYPE + ") WHERE N." + COL_NODE_PARENT_ID + " = :" + CONTAINER_ID + excludeChildfilter
				+ " GROUP BY S." + COL_JSON_SCHEMA_VALIDATION_IS_VALID;
		namedParameterJdbcTemplate.query(sql, paramSource, (ResultSet rs) -> {
			boolean unkown = false;
			boolean isValid = rs.getBoolean(1);
			if (rs.wasNull()) {
				// with the left join the null indicates nodes with no results.
				unkown = true;
			}
			long count = rs.getLong(2);
			if (unkown) {
				results.setNumberOfUnknownChildren(count);
			} else if (isValid) {
				results.setNumberOfValidChildren(count);
			} else {
				results.setNumberOfInvalidChildren(count);
			}
		});
		results.setTotalNumberOfChildren(results.getNumberOfInvalidChildren() + results.getNumberOfValidChildren()
				+ results.getNumberOfUnknownChildren());
		return results;
	}

	@Override
	public List<ValidationResults> getInvalidEntitySchemaValidationPage(String containerId, Set<Long> childIdsToExclude,
			long limit, long offset) {
		ValidateArgument.required(containerId, CONTAINER_ID);
		ValidateArgument.required(childIdsToExclude, CHILD_IDS_TO_EXCLUDE);

		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue(CONTAINER_ID, KeyFactory.stringToKey(containerId));
		paramSource.addValue(CHILD_IDS_TO_EXCLUDE, childIdsToExclude);
		paramSource.addValue(OBJECT_TYPE, ObjectType.entity.name());
		paramSource.addValue(LIMIT, limit);
		paramSource.addValue(OFFSET, offset);
		String excludeChildfilter = "";
		if (!childIdsToExclude.isEmpty()) {
			excludeChildfilter = " AND N." + COL_NODE_ID + " NOT IN (:" + CHILD_IDS_TO_EXCLUDE + ")";
		}
		String sql = "SELECT S.* FROM " + TABLE_NODE + " N JOIN " + TABLE_SCHEMA_VALIDATION_RESULTS + " S ON (N."
				+ COL_NODE_ID + " = S." + COL_JSON_SCHEMA_VALIDATION_OBJECT_ID + ") WHERE S."
				+ COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE + " = :" + OBJECT_TYPE + " AND N." + COL_NODE_PARENT_ID
				+ " = :" + CONTAINER_ID + " AND S." + COL_JSON_SCHEMA_VALIDATION_IS_VALID + " = FALSE"
				+ excludeChildfilter + " ORDER BY N." + COL_NODE_ID + " LIMIT :" + LIMIT + " OFFSET :" + OFFSET;

		return namedParameterJdbcTemplate.query(sql, paramSource,
				SchemaValidationResultDaoImpl.VALIDATION_RESULT_ROW_MAPPER);
	}

}
