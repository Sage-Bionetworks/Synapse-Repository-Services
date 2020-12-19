package org.sagebionetworks.repo.model.ar;

import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccessRestrictionStatusDaoImpl implements AccessRestrictionStatusDao {

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
	public List<SubjectStatus> getSubjectStatus(List<Long> subjectIds, RestrictableObjectType subjectType,
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
	public List<SubjectStatus> getEntityStatus(List<Long> entityIds, Long userId) {
		ValidateArgument.required(entityIds, "entityIds");
		ValidateArgument.required(userId, "userId");
		if (entityIds.isEmpty()) {
			return Collections.emptyList();
		}
		final Map<Long, SubjectStatus> statusMap = new LinkedHashMap<Long, SubjectStatus>(entityIds.size());
		for (Long entityId : entityIds) {
			SubjectStatus status = new SubjectStatus(entityId, userId);
			statusMap.put(entityId, status);
		}
		String sql = "WITH EI AS (SELECT N." + COL_NODE_ID + " AS ENTITY_ID, N." + COL_NODE_PARENT_ID + ", N."
				+ COL_NODE_TYPE + ", N." + COL_NODE_CREATED_BY + " FROM " + TABLE_NODE + " N WHERE N." + COL_NODE_ID
				+ " IN(:entityIds)), EAR AS (WITH RECURSIVE EAR (ENTITY_ID, PARENT_ID, REQUIREMENT_ID, DISTANCE) AS ("
				+ " SELECT EI.ENTITY_ID, EI.PARENT_ID, NAR." + COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID
				+ ", 1 FROM EI " + "LEFT JOIN " + TABLE_SUBJECT_ACCESS_REQUIREMENT + " NAR ON " + "(EI.ENTITY_ID = NAR."
				+ COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID + " AND NAR." + COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE
				+ " = '" + RestrictableObjectType.ENTITY.name() + "') " + "UNION ALL SELECT EAR.ENTITY_ID, N."
				+ COL_NODE_PARENT_ID + ", NAR." + COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID + ", EAR.DISTANCE+ 1"
				+ " FROM " + TABLE_NODE + " AS N JOIN EAR ON (N." + COL_NODE_ID + " = EAR.PARENT_ID)" + " LEFT JOIN "
				+ TABLE_SUBJECT_ACCESS_REQUIREMENT + " NAR ON (N." + COL_NODE_ID + " = NAR."
				+ COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID + " AND NAR." + COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE
				+ " = '" + RestrictableObjectType.ENTITY.name() + "') WHERE N." + COL_NODE_ID
				+ " IS NOT NULL AND DISTANCE < 100)"
				+ " SELECT distinct ENTITY_ID, REQUIREMENT_ID FROM EAR WHERE REQUIREMENT_ID"
				+ " IS NOT NULL ORDER BY ENTITY_ID, DISTANCE), APS AS ( SELECT EAR.*, " + "if(AA."
				+ COL_ACCESS_APPROVAL_STATE + " = 'APPROVED', TRUE, FALSE) AS APPROVED FROM EAR LEFT JOIN "
				+ TABLE_ACCESS_APPROVAL + " AA ON (EAR.REQUIREMENT_ID = AA." + COL_ACCESS_APPROVAL_REQUIREMENT_ID
				+ " AND AA." + COL_ACCESS_APPROVAL_ACCESSOR_ID + "" + " = :userId AND AA." + COL_ACCESS_APPROVAL_STATE
				+ " = 'APPROVED')) SELECT EI.ENTITY_ID, EI.NODE_TYPE,"
				+ " EI.CREATED_BY, APS.REQUIREMENT_ID, APS.APPROVED, AR." + COL_ACCESS_REQUIREMENT_CONCRETE_TYPE
				+ " AS REQUIREMENT_TYPE FROM EI LEFT JOIN APS ON (EI.ENTITY_ID = APS.ENTITY_ID) LEFT JOIN "
				+ TABLE_ACCESS_REQUIREMENT + " AR ON (APS.REQUIREMENT_ID = AR.ID)";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("entityIds", entityIds);
		params.addValue("userId", userId);
		namedJdbcTemplate.query(sql, params, (ResultSet rs) -> {
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
			String requirementType = rs.getString(REQUIREMENT_TYPE);
			// The user is automatically approved for any requirement on files they create.
			if (EntityType.file.equals(entityType) && userId.equals(createdBy)) {
				approved = true;
			}
			SubjectStatus status = statusMap.get(entityId);
			if (!approved) {
				status.setHasUnmet(true);
			}
			if (requirementId != null) {
				status.addRestrictionStatus(new UsersRequirementStatus().withRequirementId(requirementId)
						.withRequirementType(requirementType).withIsUnmet(!approved));
			}
		});
		return new ArrayList<SubjectStatus>(statusMap.values());
	}

	@Override
	public List<SubjectStatus> getNonEntityStatus(List<Long> subjectIds, RestrictableObjectType subjectType,
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
		final Map<Long, SubjectStatus> statusMap = new LinkedHashMap<Long, SubjectStatus>(subjectIds.size());
		for (Long subjectId : subjectIds) {
			SubjectStatus status = new SubjectStatus(subjectId, userId);
			statusMap.put(subjectId, status);
		}
		String sql = "SELECT NAR.*, if(AA." + COL_ACCESS_APPROVAL_STATE + " = 'APPROVED', TRUE, FALSE) AS APPROVED,"
				+ " AR." + COL_ACCESS_REQUIREMENT_CONCRETE_TYPE + " AS REQUIREMENT_TYPE FROM "
				+ TABLE_SUBJECT_ACCESS_REQUIREMENT + " NAR" + " LEFT JOIN " + TABLE_ACCESS_APPROVAL + " AA"
				+ " ON (NAR." + COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID + " = AA."
				+ COL_ACCESS_APPROVAL_REQUIREMENT_ID + " AND AA." + COL_ACCESS_APPROVAL_ACCESSOR_ID
				+ " = :userId AND AA." + COL_ACCESS_APPROVAL_STATE + " = 'APPROVED')" + " LEFT JOIN "
				+ TABLE_ACCESS_REQUIREMENT + " AR ON (NAR." + COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID
				+ " = AR.ID)" + " WHERE NAR." + COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID + " IN (:subjectIds) AND NAR."
				+ COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE + " = :subjectType;";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("subjectIds", subjectIds);
		params.addValue("subjectType", subjectType.name());
		params.addValue("userId", userId);
		namedJdbcTemplate.query(sql, params, (ResultSet rs) -> {
			Long subjectId = rs.getLong(SUBJECT_ID);
			Long requirementId = rs.getLong(REQUIREMENT_ID);
			if (rs.wasNull()) {
				requirementId = null;
			}
			Boolean approved = rs.getBoolean(APPROVED2);
			if (rs.wasNull()) {
				approved = null;
			}
			String requirementType = rs.getString(REQUIREMENT_TYPE);
			SubjectStatus status = statusMap.get(subjectId);
			if (!approved) {
				status.setHasUnmet(true);
			}
			if (requirementId != null) {
				status.addRestrictionStatus(new UsersRequirementStatus().withRequirementId(requirementId)
						.withRequirementType(requirementType).withIsUnmet(!approved));
			}
		});
		return new ArrayList<SubjectStatus>(statusMap.values());
	}

}
