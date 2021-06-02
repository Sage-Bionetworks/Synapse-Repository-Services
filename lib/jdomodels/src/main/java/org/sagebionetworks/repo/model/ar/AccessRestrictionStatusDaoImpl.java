package org.sagebionetworks.repo.model.ar;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccessRestrictionStatusDaoImpl implements AccessRestrictionStatusDao {
	
	public static final String GET_ENTITY_ACCESS_RESTRICTIONS_SQL = DDLUtilsImpl
			.loadSQLFromClasspath("sql/GetEntityAccessRestrictions.sql");
	public static final String GET_NON_ENTITY_ACCESS_RESTRICTIONS_SQL = DDLUtilsImpl
			.loadSQLFromClasspath("sql/GetNonEntityAccessRestrictions.sql");

	private static final String CREATED_BY = "CREATED_BY";
	private static final String NODE_TYPE = "NODE_TYPE";
	private static final String ENTITY_ID = "ENTITY_ID";
	private static final String REQUIREMENT_TYPE = "REQUIREMENT_TYPE";
	private static final String APPROVED2 = "APPROVED";
	private static final String REQUIREMENT_ID = "REQUIREMENT_ID";
	private static final String SUBJECT_ID = "SUBJECT_ID";
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Override
	public List<UsersRestrictionStatus> getSubjectStatus(List<Long> subjectIds, RestrictableObjectType subjectType,
			Long userId) {
		ValidateArgument.required(subjectIds, "subjectIds");
		ValidateArgument.required(subjectType, "subjectType");
		ValidateArgument.required(userId, "userId");
		if (RestrictableObjectType.ENTITY.equals(subjectType)) {
			return getEntityStatus(subjectIds, userId);
		} else {
			return getNonEntityStatus(subjectIds, subjectType, userId);
		}
	}
	
	@Override
	public Map<Long, UsersRestrictionStatus> getEntityStatusAsMap(List<Long> entityIds, Long userId) {
		ValidateArgument.required(entityIds, "entityIds");
		ValidateArgument.required(userId, "userId");
		if (entityIds.isEmpty()) {
			return Collections.emptyMap();
		}
		final Map<Long, UsersRestrictionStatus> statusMap = new LinkedHashMap<Long, UsersRestrictionStatus>(entityIds.size());
		for (Long entityId : entityIds) {
			UsersRestrictionStatus status = new UsersRestrictionStatus(entityId, userId);
			statusMap.put(entityId, status);
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("entityIds", entityIds);
		params.addValue("userId", userId);
		params.addValue("depth", NodeConstants.MAX_PATH_DEPTH_PLUS_ONE);
		namedJdbcTemplate.query(GET_ENTITY_ACCESS_RESTRICTIONS_SQL, params, (ResultSet rs) -> {
			Long entityId = rs.getLong(ENTITY_ID);
			EntityType entityType = EntityType.valueOf(rs.getString(NODE_TYPE));
			Long createdBy = rs.getLong(CREATED_BY);
			Long requirementId = rs.getLong(REQUIREMENT_ID);
			if (rs.wasNull()) {
				requirementId = null;
			}
			Boolean approved = rs.getBoolean(APPROVED2);
			if (rs.wasNull()) {
				approved = null;
			}
			String requirementTypeString = rs.getString(REQUIREMENT_TYPE);
			AccessRequirementType requirementType = null;
			if(requirementTypeString != null) {
				requirementType = AccessRequirementType.lookupClassName(requirementTypeString);
			}
			// The user is automatically approved for any requirement on files they create.
			if (EntityType.file.equals(entityType) && userId.equals(createdBy)) {
				approved = true;
			}
			UsersRestrictionStatus status = statusMap.get(entityId);
			if (approved != null && !approved) {
				status.setHasUnmet(true);
			}
			if (requirementId != null) {
				status.addRestrictionStatus(new UsersRequirementStatus().withRequirementId(requirementId)
						.withRequirementType(requirementType).withIsUnmet(!approved));
			}
		});
		return statusMap;
	}

	@Override
	public List<UsersRestrictionStatus> getEntityStatus(List<Long> entityIds, Long userId) {
		Map<Long, UsersRestrictionStatus> statusMap = getEntityStatusAsMap(entityIds, userId);
		return new ArrayList<UsersRestrictionStatus>(statusMap.values());
	}

	@Override
	public List<UsersRestrictionStatus> getNonEntityStatus(List<Long> subjectIds, RestrictableObjectType subjectType,
			Long userId) {
		ValidateArgument.required(subjectIds, "subjectIds");
		ValidateArgument.required(subjectType, "subjectType");
		ValidateArgument.required(userId, "userId");
		if (RestrictableObjectType.ENTITY.equals(subjectType)) {
			throw new IllegalArgumentException("This method can only be used for non-entity subject types.");
		}
		if (subjectIds.isEmpty()) {
			return Collections.emptyList();
		}
		final Map<Long, UsersRestrictionStatus> statusMap = new LinkedHashMap<Long, UsersRestrictionStatus>(subjectIds.size());
		for (Long subjectId : subjectIds) {
			UsersRestrictionStatus status = new UsersRestrictionStatus(subjectId, userId);
			statusMap.put(subjectId, status);
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("subjectIds", subjectIds);
		params.addValue("subjectType", subjectType.name());
		params.addValue("userId", userId);
		namedJdbcTemplate.query(GET_NON_ENTITY_ACCESS_RESTRICTIONS_SQL, params, (ResultSet rs) -> {
			Long subjectId = rs.getLong(SUBJECT_ID);
			Long requirementId = rs.getLong(REQUIREMENT_ID);
			if (rs.wasNull()) {
				requirementId = null;
			}
			Boolean approved = rs.getBoolean(APPROVED2);
			if (rs.wasNull()) {
				approved = null;
			}
			String requirementTypeString = rs.getString(REQUIREMENT_TYPE);
			AccessRequirementType requirementType = null;
			if(requirementTypeString != null) {
				requirementType = AccessRequirementType.lookupClassName(requirementTypeString);
			}
			UsersRestrictionStatus status = statusMap.get(subjectId);
			if (!approved) {
				status.setHasUnmet(true);
			}
			if (requirementId != null) {
				status.addRestrictionStatus(new UsersRequirementStatus().withRequirementId(requirementId)
						.withRequirementType(requirementType).withIsUnmet(!approved));
			}
		});
		return new ArrayList<UsersRestrictionStatus>(statusMap.values());
	}

}
