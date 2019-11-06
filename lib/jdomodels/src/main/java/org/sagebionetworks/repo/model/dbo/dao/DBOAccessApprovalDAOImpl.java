package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_EXPIRED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_SUBMITTER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_APPROVAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SUBJECT_ACCESS_REQUIREMENT;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroup;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.Sets;

public class DBOAccessApprovalDAOImpl implements AccessApprovalDAO {
	public static final String LIMIT_PARAM = "LIMIT";
	public static final String OFFSET_PARAM = "OFFSET";
	public static final long DEFAULT_NOT_EXPIRED = 0L;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final String SELECT_ACCESS_APPROVALS_FOR_SUBJECTS =
			"SELECT *"
			+ " FROM "+TABLE_ACCESS_APPROVAL
				+ " JOIN "+TABLE_SUBJECT_ACCESS_REQUIREMENT
				+ " ON "+TABLE_ACCESS_APPROVAL+"."+COL_ACCESS_APPROVAL_REQUIREMENT_ID
				+ " = "+TABLE_SUBJECT_ACCESS_REQUIREMENT+"."+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID
			+ " WHERE "+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+" in (:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+") "
			+ " AND "+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+" = :"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE
			+ " LIMIT :"+LIMIT_PARAM
			+ " OFFSET :"+OFFSET_PARAM;

	private static final String SELECT_BY_PRIMARY_KEY= 
			"SELECT *"
			+" FROM "+TABLE_ACCESS_APPROVAL
			+" WHERE "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+" = :"+COL_ACCESS_APPROVAL_REQUIREMENT_ID
			+" AND "+COL_ACCESS_APPROVAL_REQUIREMENT_VERSION+" = :"+COL_ACCESS_APPROVAL_REQUIREMENT_VERSION
			+" AND "+COL_ACCESS_APPROVAL_SUBMITTER_ID+" = :"+COL_ACCESS_APPROVAL_SUBMITTER_ID
			+" AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" = :"+COL_ACCESS_APPROVAL_ACCESSOR_ID;

	private static final String SELECT_MET_ACCESS_REQUIREMENT_COUNT =
			"SELECT COUNT(DISTINCT "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+")"
			+ " FROM "+TABLE_ACCESS_APPROVAL
			+ " WHERE "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+" IN (:"+COL_ACCESS_APPROVAL_REQUIREMENT_ID+")"
			+ " AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" = :"+COL_ACCESS_APPROVAL_ACCESSOR_ID
			+ " AND "+COL_ACCESS_APPROVAL_STATE+" = '"+ApprovalState.APPROVED.name()+"'";

	private static final String REVOKE = "UPDATE "+TABLE_ACCESS_APPROVAL
			+" SET "+COL_ACCESS_APPROVAL_ETAG+" = :"+COL_ACCESS_APPROVAL_ETAG+", "
					+COL_ACCESS_APPROVAL_MODIFIED_BY+" = :"+COL_ACCESS_APPROVAL_MODIFIED_BY+", "
					+COL_ACCESS_APPROVAL_MODIFIED_ON+" = :"+COL_ACCESS_APPROVAL_MODIFIED_ON+", "
					+COL_ACCESS_APPROVAL_STATE+" = '"+ApprovalState.REVOKED.name()+"' "
			+ " WHERE "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+" = :"+COL_ACCESS_APPROVAL_REQUIREMENT_ID
			+ " AND "+COL_ACCESS_APPROVAL_STATE+" = '"+ApprovalState.APPROVED.name()+"' ";

	private static final String REVOKE_ACCESSOR = REVOKE
			+ " AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" = :"+COL_ACCESS_APPROVAL_ACCESSOR_ID;

	private static final String REVOKE_GROUP = REVOKE
			+ " AND "+COL_ACCESS_APPROVAL_SUBMITTER_ID+" = :"+COL_ACCESS_APPROVAL_SUBMITTER_ID;

	private static final String SQL_REVOKE_BY_SUBMITTER = REVOKE
			+ " AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" = :"+COL_ACCESS_APPROVAL_ACCESSOR_ID
			+ " AND "+COL_ACCESS_APPROVAL_SUBMITTER_ID+" = :"+COL_ACCESS_APPROVAL_SUBMITTER_ID;

	private static final String SQL_CREATE_OR_UPDATE = "INSERT INTO "
			+TABLE_ACCESS_APPROVAL+"("
			+COL_ACCESS_APPROVAL_ID+", "
			+COL_ACCESS_APPROVAL_ETAG+", "
			+COL_ACCESS_APPROVAL_CREATED_BY+", "
			+COL_ACCESS_APPROVAL_CREATED_ON+", "
			+COL_ACCESS_APPROVAL_MODIFIED_BY+", "
			+COL_ACCESS_APPROVAL_MODIFIED_ON+", "
			+COL_ACCESS_APPROVAL_EXPIRED_ON+", "
			+COL_ACCESS_APPROVAL_REQUIREMENT_ID+", "
			+COL_ACCESS_APPROVAL_REQUIREMENT_VERSION+", "
			+COL_ACCESS_APPROVAL_SUBMITTER_ID+", "
			+COL_ACCESS_APPROVAL_ACCESSOR_ID+", "
			+COL_ACCESS_APPROVAL_STATE
			+") VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
			+COL_ACCESS_APPROVAL_ETAG+" = ?, "
			+COL_ACCESS_APPROVAL_MODIFIED_BY+" = ?, "
			+COL_ACCESS_APPROVAL_MODIFIED_ON+" = ?, "
			+COL_ACCESS_APPROVAL_EXPIRED_ON+" = ?, "
			+COL_ACCESS_APPROVAL_STATE+" = ? ";

	private static final String SELECT_ACCESSORS = 
				"SELECT DISTINCT "+COL_ACCESS_APPROVAL_ACCESSOR_ID
			+" FROM "+TABLE_ACCESS_APPROVAL
			+" WHERE "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+" = :"+COL_ACCESS_APPROVAL_REQUIREMENT_ID
			+" AND "+COL_ACCESS_APPROVAL_SUBMITTER_ID+" = :"+COL_ACCESS_APPROVAL_SUBMITTER_ID
			+" AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" IN (:"+COL_ACCESS_APPROVAL_ACCESSOR_ID+")";

	private static final String SELECT_ACTIVE_APPROVALS = 
			"SELECT *"
			+ " FROM "+TABLE_ACCESS_APPROVAL
			+ " WHERE "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+" = :"+COL_ACCESS_APPROVAL_REQUIREMENT_ID
			+ " AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" = :"+COL_ACCESS_APPROVAL_ACCESSOR_ID
			+ " AND "+COL_ACCESS_APPROVAL_STATE+" = '"+ApprovalState.APPROVED.name()+"'";

	private static final String SELECT_REQUIREMENTS_WITH_APPROVAL = 
			"SELECT DISTINCT "+COL_ACCESS_APPROVAL_REQUIREMENT_ID
			+ " FROM "+TABLE_ACCESS_APPROVAL
			+ " WHERE "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+" IN (:"+COL_ACCESS_APPROVAL_REQUIREMENT_ID+")"
			+ " AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" = :"+COL_ACCESS_APPROVAL_ACCESSOR_ID
			+ " AND "+COL_ACCESS_APPROVAL_STATE+" = '"+ApprovalState.APPROVED.name()+"'";

	private static final String SEPARATOR = ",";
	private static final String ACCESSOR_LIST = "ACCESSOR_LIST";

	private static final String SELECT_ACCESSOR_GROUP_PREFIX = "SELECT "
				+ COL_ACCESS_APPROVAL_REQUIREMENT_ID+", "
				+ COL_ACCESS_APPROVAL_SUBMITTER_ID+", "
				+ COL_ACCESS_APPROVAL_EXPIRED_ON+", "
				+ "GROUP_CONCAT(DISTINCT "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" SEPARATOR '"+SEPARATOR+"') AS "+ACCESSOR_LIST
			+ " FROM "+TABLE_ACCESS_APPROVAL
			+ " WHERE "+COL_ACCESS_APPROVAL_STATE+" = '"+ApprovalState.APPROVED.name()+"'";
	private static final String REQUIREMENT_ID_COND =
			" AND "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+" = :"+COL_ACCESS_APPROVAL_REQUIREMENT_ID;
	private static final String SUBMITTER_ID_COND =
			" AND "+COL_ACCESS_APPROVAL_SUBMITTER_ID+" = :"+COL_ACCESS_APPROVAL_SUBMITTER_ID;
	private static final String EXPIRED_ON_COND =
			" AND "+COL_ACCESS_APPROVAL_EXPIRED_ON+" <> "+DEFAULT_NOT_EXPIRED
			+" AND "+COL_ACCESS_APPROVAL_EXPIRED_ON+" <= :"+COL_ACCESS_APPROVAL_EXPIRED_ON;
	private static final String SELECT_ACCESSOR_GROUP_POSTFIX = " GROUP BY "
				+ COL_ACCESS_APPROVAL_REQUIREMENT_ID+", "
				+ COL_ACCESS_APPROVAL_SUBMITTER_ID+", "
				+ COL_ACCESS_APPROVAL_EXPIRED_ON
			+ " ORDER BY "+COL_ACCESS_APPROVAL_EXPIRED_ON
			+ " LIMIT :"+LIMIT_PARAM
			+ " OFFSET :"+OFFSET_PARAM;

	private static final RowMapper<DBOAccessApproval> rowMapper = (new DBOAccessApproval()).getTableMapping();

	@WriteTransaction
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOAccessApproval.class, param);
	}

	@WriteTransaction
	@Override
	public AccessApproval create(AccessApproval dto) throws DatastoreException {
		createOrUpdateBatch(Arrays.asList((AccessApproval)dto));
		return getByPrimaryKey(dto.getRequirementId(), dto.getRequirementVersion(), dto.getSubmitterId(), dto.getAccessorId());
	}

	@Override
	public AccessApproval getByPrimaryKey(Long requirementId, Long requirementVersion, String submitterId, String accessorId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, requirementId);
		param.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_VERSION, requirementVersion);
		param.addValue(COL_ACCESS_APPROVAL_SUBMITTER_ID, submitterId);
		param.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, accessorId);
		try {
			DBOAccessApproval dbo =  namedJdbcTemplate.queryForObject(SELECT_BY_PRIMARY_KEY, param, rowMapper);
			return AccessApprovalUtils.copyDboToDto(dbo);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@Override
	public AccessApproval get(String id) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ID.toLowerCase(), id);
		DBOAccessApproval dbo = basicDao.getObjectByPrimaryKey(DBOAccessApproval.class, param);
		AccessApproval dto = AccessApprovalUtils.copyDboToDto(dbo);
		return dto;
	}

	@WriteTransaction
	@Override
	public void revokeAll(String accessRequirementId, String accessorId, String revokedBy) {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(accessorId, "accessorId");
		ValidateArgument.required(revokedBy, "revokedBy");
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementId);
		params.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, accessorId);
		params.addValue(COL_ACCESS_APPROVAL_ETAG, UUID.randomUUID().toString());
		params.addValue(COL_ACCESS_APPROVAL_MODIFIED_BY, revokedBy);
		params.addValue(COL_ACCESS_APPROVAL_MODIFIED_ON, System.currentTimeMillis());
		namedJdbcTemplate.update(REVOKE_ACCESSOR, params);
	}

	@WriteTransaction
	@Override
	public void revokeGroup(String accessRequirementId, String submitterId, String revokedBy) {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(submitterId, "submitterId");
		ValidateArgument.required(revokedBy, "revokedBy");
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementId);
		params.addValue(COL_ACCESS_APPROVAL_SUBMITTER_ID, submitterId);
		params.addValue(COL_ACCESS_APPROVAL_ETAG, UUID.randomUUID().toString());
		params.addValue(COL_ACCESS_APPROVAL_MODIFIED_BY, revokedBy);
		params.addValue(COL_ACCESS_APPROVAL_MODIFIED_ON, System.currentTimeMillis());
		namedJdbcTemplate.update(REVOKE_GROUP, params);
	}

	@WriteTransaction
	@Override
	public void revokeBySubmitter(String accessRequirementId, String submitterId, List<String> accessors, String revokedBy) {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(submitterId, "submitterId");
		ValidateArgument.required(revokedBy, "revokedBy");
		ValidateArgument.required(accessors, "accessors");
		if (accessors.isEmpty()) {
			return;
		}
		MapSqlParameterSource[] params = new MapSqlParameterSource[accessors.size()];
		int i = 0;
		for (String accessor : accessors) {
			params[i] = new MapSqlParameterSource();
			params[i].addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementId);
			params[i].addValue(COL_ACCESS_APPROVAL_SUBMITTER_ID, submitterId);
			params[i].addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, accessor);
			params[i].addValue(COL_ACCESS_APPROVAL_ETAG, UUID.randomUUID().toString());
			params[i].addValue(COL_ACCESS_APPROVAL_MODIFIED_BY, revokedBy);
			params[i].addValue(COL_ACCESS_APPROVAL_MODIFIED_ON, System.currentTimeMillis());
			i++;
		}
		namedJdbcTemplate.batchUpdate(SQL_REVOKE_BY_SUBMITTER, params);
	}

	@Override
	public Boolean hasUnmetAccessRequirement(Set<String> requirementIdSet, String userId) {
		if (requirementIdSet.isEmpty()) {
			return false;
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, requirementIdSet);
		params.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, userId);
		return requirementIdSet.size() > namedJdbcTemplate.queryForObject(SELECT_MET_ACCESS_REQUIREMENT_COUNT, params, Integer.class);
	}

	@WriteTransaction
	@Override
	public void createOrUpdateBatch(List<AccessApproval> dtos) {
		final List<DBOAccessApproval> dbos = AccessApprovalUtils.copyDtosToDbos(dtos, true/*for creation*/, idGenerator);
		final List<String> ids = new LinkedList<String>();
		jdbcTemplate.batchUpdate(SQL_CREATE_OR_UPDATE, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				// create fields
				ps.setLong(1, dbos.get(i).getId());
				ps.setString(2, dbos.get(i).geteTag());
				ps.setLong(3, dbos.get(i).getCreatedBy());
				ps.setLong(4, dbos.get(i).getCreatedOn());
				ps.setLong(5, dbos.get(i).getModifiedBy());
				ps.setLong(6, dbos.get(i).getModifiedOn());
				ps.setLong(7, dbos.get(i).getExpiredOn());
				ps.setLong(8, dbos.get(i).getRequirementId());
				ps.setLong(9, dbos.get(i).getRequirementVersion());
				ps.setLong(10, dbos.get(i).getSubmitterId());
				ps.setLong(11, dbos.get(i).getAccessorId());
				ps.setString(12, dbos.get(i).getState());
				// update fields
				ps.setString(13, dbos.get(i).geteTag());
				ps.setLong(14, dbos.get(i).getModifiedBy());
				ps.setLong(15, dbos.get(i).getModifiedOn());
				ps.setLong(16, dbos.get(i).getExpiredOn());
				ps.setString(17, dbos.get(i).getState());
				ids.add(dbos.get(i).getId().toString());
			}

			@Override
			public int getBatchSize() {
				return dbos.size();
			}
		});
	}

	@Deprecated
	@Override
	public List<AccessApproval> getAccessApprovalsForSubjects(List<String> subjectIdList, RestrictableObjectType type, long limit, long offset) {
		List<AccessApproval> dtos = new ArrayList<AccessApproval>();
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, KeyFactory.stringToKey(subjectIdList));
		params.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, type.name());
		params.addValue(LIMIT_PARAM, limit);
		params.addValue(OFFSET_PARAM, offset);
		List<DBOAccessApproval> dbos = namedJdbcTemplate.query(SELECT_ACCESS_APPROVALS_FOR_SUBJECTS, params, rowMapper);
		for (DBOAccessApproval dbo : dbos) {
			AccessApproval dto = AccessApprovalUtils.copyDboToDto(dbo);
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public List<AccessApproval> getActiveApprovalsForUser(String accessRequirementId, String userId) {
		List<AccessApproval> dtos = new ArrayList<AccessApproval>();
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementId);
		params.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, userId);
		List<DBOAccessApproval> dbos = namedJdbcTemplate.query(SELECT_ACTIVE_APPROVALS, params, rowMapper);
		for (DBOAccessApproval dbo : dbos) {
			dtos.add(AccessApprovalUtils.copyDboToDto(dbo));
		}
		return dtos;
	}

	@Override
	public boolean hasApprovalsSubmittedBy(Set<String> accessorIds, String submitterId, String accessRequirementId) {
		if (accessorIds.isEmpty()) {
			return true;
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementId);
		params.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, accessorIds);
		params.addValue(COL_ACCESS_APPROVAL_SUBMITTER_ID, submitterId);
		List<String> approvedUsers = namedJdbcTemplate.queryForList(SELECT_ACCESSORS, params, String.class);
		return approvedUsers.containsAll(accessorIds);
	}

	@Override
	public List<AccessorGroup> listAccessorGroup(String accessRequirementId, String submitterId, Date expireBefore,
			long limit, long offset) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementId);
		params.addValue(COL_ACCESS_APPROVAL_SUBMITTER_ID, submitterId);
		if (expireBefore != null) {
			params.addValue(COL_ACCESS_APPROVAL_EXPIRED_ON, expireBefore.getTime());
		}
		params.addValue(LIMIT_PARAM, limit);
		params.addValue(OFFSET_PARAM, offset);
		String query = buildAccessorGroupQuery(accessRequirementId, submitterId, expireBefore);
		return namedJdbcTemplate.query(query, params, new RowMapper<AccessorGroup>(){

			@Override
			public AccessorGroup mapRow(ResultSet rs, int rowNum) throws SQLException {
				AccessorGroup group = new AccessorGroup();
				group.setAccessRequirementId(rs.getString(COL_ACCESS_APPROVAL_REQUIREMENT_ID));
				group.setSubmitterId(rs.getString(COL_ACCESS_APPROVAL_SUBMITTER_ID));
				group.setAccessorIds(convertToList(rs.getString(ACCESSOR_LIST)));
				group.setExpiredOn(new Date(rs.getLong(COL_ACCESS_APPROVAL_EXPIRED_ON)));
				return group;
			}
		});
	}

	public static List<String> convertToList(String accessorList) {
		if (accessorList == null) {
			return new LinkedList<String>();
		}
		String[] accessors = accessorList.split(SEPARATOR);
		return Arrays.asList(accessors);
	}

	public static String buildAccessorGroupQuery(String accessRequirementId, String submitterId, Date expireBefore) {
		String query = SELECT_ACCESSOR_GROUP_PREFIX;
		if (accessRequirementId != null) {
			query+= REQUIREMENT_ID_COND;
		}
		if (submitterId != null) {
			query+= SUBMITTER_ID_COND;
		}
		if (expireBefore != null) {
			query+= EXPIRED_ON_COND;
		}
		return query+=SELECT_ACCESSOR_GROUP_POSTFIX;
	}

	@Override
	public Set<String> getRequirementsUserHasApprovals(String userId, List<String> accessRequirementIds) {
		if (accessRequirementIds.isEmpty()) {
			return new HashSet<String>();
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementIds);
		params.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, userId);
		return Sets.newHashSet(namedJdbcTemplate.queryForList(SELECT_REQUIREMENTS_WITH_APPROVAL, params, String.class));
	}
}
