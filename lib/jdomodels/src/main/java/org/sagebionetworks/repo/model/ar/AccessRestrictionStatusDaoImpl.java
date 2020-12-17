package org.sagebionetworks.repo.model.ar;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccessRestrictionStatusDaoImpl implements AccessRestrictionStatusDao {

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Override
	public List<SubjectStatus> getSubjectStatus(List<Long> subjectIds, RestrictableObjectType subjectType,
			Long userId) {
		ValidateArgument.required(subjectType, "subjectType");
		if (RestrictableObjectType.ENTITY.equals(subjectType)) {
			return getEntityStatus(subjectIds, userId);
		} else {
			getNonEntityStatus(subjectIds, userId);
		}
		return null;
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
		String sql = "WITH EI AS (SELECT N.ID AS ENTITY_ID, N.PARENT_ID, N.NODE_TYPE,"
				+ " N.CREATED_BY FROM JDONODE N WHERE N.ID IN(:entityIds)), "
				+ "EAR AS (WITH RECURSIVE EAR (ENTITY_ID, PARENT_ID, REQUIREMENT_ID, DISTANCE) AS ("
				+ " SELECT EI.ENTITY_ID, EI.PARENT_ID, NAR.REQUIREMENT_ID, 1 FROM EI "
				+ "LEFT JOIN NODE_ACCESS_REQUIREMENT NAR ON "
				+ "(EI.ENTITY_ID = NAR.SUBJECT_ID AND NAR.SUBJECT_TYPE = 'ENTITY') "
				+ "UNION ALL SELECT EAR.ENTITY_ID, N.PARENT_ID, NAR.REQUIREMENT_ID, EAR.DISTANCE+ 1"
				+ " FROM JDONODE AS N JOIN EAR ON (N.ID = EAR.PARENT_ID)"
				+ " LEFT JOIN NODE_ACCESS_REQUIREMENT NAR ON (N.ID = NAR.SUBJECT_ID"
				+ " AND NAR.SUBJECT_TYPE = 'ENTITY') WHERE N.ID IS NOT NULL AND DISTANCE < 100)"
				+ " SELECT distinct ENTITY_ID, REQUIREMENT_ID FROM EAR WHERE REQUIREMENT_ID"
				+ " IS NOT NULL ORDER BY ENTITY_ID, DISTANCE), APS AS ( SELECT EAR.*, "
				+ "if(AA.STATE = 'APPROVED', TRUE, FALSE) AS APPROVED FROM EAR LEFT JOIN"
				+ " ACCESS_APPROVAL AA ON (EAR.REQUIREMENT_ID = AA.REQUIREMENT_ID AND AA.ACCESSOR_ID"
				+ " = :userId AND AA.STATE = 'APPROVED')) SELECT EI.ENTITY_ID, EI.NODE_TYPE,"
				+ " EI.CREATED_BY, APS.REQUIREMENT_ID, APS.APPROVED, AR.CONCRETE_TYPE AS "
				+ "REQUIREMENT_TYPE FROM EI LEFT JOIN APS ON (EI.ENTITY_ID = APS.ENTITY_ID) "
				+ "LEFT JOIN ACCESS_REQUIREMENT AR ON (APS.REQUIREMENT_ID = AR.ID)";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("entityIds", entityIds);
		params.addValue("userId", userId);
		namedJdbcTemplate.query(sql, params, (ResultSet rs) -> {
			Long entityId = rs.getLong("ENTITY_ID");
			EntityType entityType = EntityType.valueOf(rs.getString("NODE_TYPE"));
			Long createdBy = rs.getLong("CREATED_BY");
			Long requirementId = rs.getLong("REQUIREMENT_ID");
			if (rs.wasNull()) {
				requirementId = null;
			}
			Boolean approved = rs.getBoolean("APPROVED");
			if (rs.wasNull()) {
				approved = null;
			}
			String requirementType = rs.getString("REQUIREMENT_TYPE");
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
	public List<SubjectStatus> getNonEntityStatus(List<Long> subjectIds, Long userId) {
		ValidateArgument.required(subjectIds, "subjectIds");
		ValidateArgument.required(userId, "userId");
		if (subjectIds.isEmpty()) {
			return Collections.emptyList();
		}

		return null;
	}

}
