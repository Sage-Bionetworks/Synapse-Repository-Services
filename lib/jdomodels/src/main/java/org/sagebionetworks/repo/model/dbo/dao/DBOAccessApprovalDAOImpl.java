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
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_APPROVAL;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroup;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;
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
import org.springframework.stereotype.Repository;

import com.google.common.collect.Sets;

@Repository
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
	
	private static final String SQL_SELECT_APPROVED_IDS = "SELECT " + COL_ACCESS_APPROVAL_ID 
			+ " FROM " + TABLE_ACCESS_APPROVAL
			+ " WHERE " + COL_ACCESS_APPROVAL_STATE + " = '" + ApprovalState.APPROVED.name() + "'";
	
	// Note that the query automatically excludes the default value for non expiring approvals since their value is 0
	private static final String SQL_SELECT_EXPIRED_APPROVALS = SQL_SELECT_APPROVED_IDS
			+ " AND " + COL_ACCESS_APPROVAL_EXPIRED_ON + " BETWEEN ? AND ?"
			+ " ORDER BY " + COL_ACCESS_APPROVAL_EXPIRED_ON
			+ " LIMIT ?";
	
	private static final String SQL_SELECT_APPROVALS_FOR_SUBMITTER_COUNT =  "SELECT COUNT(" + COL_ACCESS_APPROVAL_ID + ")" 
			+ " FROM " + TABLE_ACCESS_APPROVAL
			+ " WHERE " + COL_ACCESS_APPROVAL_REQUIREMENT_ID + " = ?"
			+ " AND " + COL_ACCESS_APPROVAL_SUBMITTER_ID + " = ?"
			+ " AND " + COL_ACCESS_APPROVAL_STATE + " = '" + ApprovalState.APPROVED.name() + "'"
			// Only the submitter approvals
			+ " AND " + COL_ACCESS_APPROVAL_SUBMITTER_ID + " = " + COL_ACCESS_APPROVAL_ACCESSOR_ID
			+ " AND (" + COL_ACCESS_APPROVAL_EXPIRED_ON + " > ? OR " + COL_ACCESS_APPROVAL_EXPIRED_ON + " = " + DEFAULT_NOT_EXPIRED + ")";
	
	private static final String SQL_SELECT_APPROVALS_FOR_ACCESSOR_COUNT =  "SELECT COUNT(" + COL_ACCESS_APPROVAL_ID + ")" 
			+ " FROM " + TABLE_ACCESS_APPROVAL
			+ " WHERE " + COL_ACCESS_APPROVAL_REQUIREMENT_ID + " = ?"
			+ " AND " + COL_ACCESS_APPROVAL_ACCESSOR_ID + " = ?"
			+ " AND " + COL_ACCESS_APPROVAL_STATE + " = '" + ApprovalState.APPROVED.name() + "'";
	
	private static final String SQL_SELECT_APPROVALS_BY_ACCESSOR = SQL_SELECT_APPROVED_IDS
			+ " AND " + COL_ACCESS_APPROVAL_REQUIREMENT_ID + " = ?"
			+ " AND " + COL_ACCESS_APPROVAL_ACCESSOR_ID + " = ?";
	
	private static final String SQL_SELECT_APPROVALS_BY_SUBMITTER = SQL_SELECT_APPROVED_IDS
			+ " AND " + COL_ACCESS_APPROVAL_REQUIREMENT_ID + " = ?"
			+ " AND " + COL_ACCESS_APPROVAL_SUBMITTER_ID + " = ?";
	
	private static final String SQL_SELECT_APPROVALS_BY_SUBMITTER_AND_ACCESSORS = SQL_SELECT_APPROVED_IDS
			+ " AND " + COL_ACCESS_APPROVAL_REQUIREMENT_ID + " = :" + COL_ACCESS_APPROVAL_REQUIREMENT_ID
			+ " AND " + COL_ACCESS_APPROVAL_SUBMITTER_ID + " = :" + COL_ACCESS_APPROVAL_SUBMITTER_ID
			+ " AND " + COL_ACCESS_APPROVAL_ACCESSOR_ID + " IN (:" + COL_ACCESS_APPROVAL_ACCESSOR_ID + ")";
			
	private static final String REVOKE_BY_ID = "UPDATE " + TABLE_ACCESS_APPROVAL + " SET " 
			+ COL_ACCESS_APPROVAL_ETAG + " = UUID(), " 
			+ COL_ACCESS_APPROVAL_MODIFIED_BY + " = ?, "
			+ COL_ACCESS_APPROVAL_MODIFIED_ON + " = ?, "
			+ COL_ACCESS_APPROVAL_STATE + " = '" + ApprovalState.REVOKED.name() + "' " 
			+ " WHERE "
			+ COL_ACCESS_APPROVAL_ID + " = ? AND " + COL_ACCESS_APPROVAL_STATE + " = '" + ApprovalState.APPROVED.name() + "'";

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
		createOrUpdateBatch(Arrays.asList(dto));
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
			}

			@Override
			public int getBatchSize() {
				return dbos.size();
			}
		});
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
			return Collections.emptyList();
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
			return Collections.emptySet();
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementIds);
		params.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, userId);
		return Sets.newHashSet(namedJdbcTemplate.queryForList(SELECT_REQUIREMENTS_WITH_APPROVAL, params, String.class));
	}
	
	@Override
	public List<Long> listApprovalsByAccessor(String accessRequirementId, String accessorId) {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(accessorId, "accessorId");
		
		return jdbcTemplate.queryForList(SQL_SELECT_APPROVALS_BY_ACCESSOR, Long.class, accessRequirementId, accessorId);
	}
	
	@Override
	public List<Long> listApprovalsBySubmitter(String accessRequirementId, String submitterId) {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(submitterId, "submitterId");
		
		return jdbcTemplate.queryForList(SQL_SELECT_APPROVALS_BY_SUBMITTER, Long.class, accessRequirementId, submitterId);
	}
	
	@Override
	public List<Long> listApprovalsBySubmitter(String accessRequirementId, String submitterId,
			List<String> accessorIds) {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(submitterId, "submitterId");
		ValidateArgument.required(accessorIds, "accessorIds");
		
		if (accessorIds.isEmpty()) {
			return Collections.emptyList();
		}
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementId);
		params.addValue(COL_ACCESS_APPROVAL_SUBMITTER_ID, submitterId);
		params.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, accessorIds);
		
		return namedJdbcTemplate.queryForList(SQL_SELECT_APPROVALS_BY_SUBMITTER_AND_ACCESSORS, params, Long.class);
	}
	
	@Override
	public List<Long> listExpiredApprovals(Instant expiredAfter, int limit) {
		ValidateArgument.required(expiredAfter, "expiredAfter");
		ValidateArgument.requirement(limit > 0, "The limit must be greater than 0.");
		
		return jdbcTemplate.queryForList(SQL_SELECT_EXPIRED_APPROVALS, Long.class,
				expiredAfter.toEpochMilli(), 
				Instant.now().toEpochMilli(),
				limit);
	}
	
	@Override
	public boolean hasAccessorApproval(String accessRequirementId, String accessorId) {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(accessorId, "accessorId");
		
		return jdbcTemplate.queryForObject(SQL_SELECT_APPROVALS_FOR_ACCESSOR_COUNT, Long.class,
				accessRequirementId,
				accessorId) > 0;
	}
	
	@Override
	public boolean hasSubmitterApproval(String accessRequirementId, String submitterId, Instant expireAfter) {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(submitterId, "submitterId");
		ValidateArgument.required(expireAfter, "expireAfter");
		
		return jdbcTemplate.queryForObject(SQL_SELECT_APPROVALS_FOR_SUBMITTER_COUNT, Long.class,
				accessRequirementId,
				submitterId, 
				expireAfter.toEpochMilli()) > 0;
	}
	
	@Override
	@WriteTransaction
	public List<Long> revokeBatch(Long userId, List<Long> ids) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(ids, "ids");
		
		if (ids.isEmpty()) {
			return Collections.emptyList();
		}
		
		int[] updateResult = jdbcTemplate.batchUpdate(REVOKE_BY_ID, new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				
				Long id = ids.get(i);
				
				int paramIndex = 0;

				ps.setLong(++paramIndex, userId);
				ps.setLong(++paramIndex, System.currentTimeMillis());
				ps.setLong(++paramIndex, id);
			}
			
			@Override
			public int getBatchSize() {
				return ids.size();
			}
		});
		
		List<Long> updatedIdsList = new ArrayList<>(updateResult.length);
		
		for (int i = 0; i < updateResult.length; i++) {
			if (updateResult[i] > 0) {
				updatedIdsList.add(ids.get(i));
			}
		}
		
		return updatedIdsList;
	}
	
	@Override
	public void clear() {
		jdbcTemplate.update("DELETE FROM " + TABLE_ACCESS_APPROVAL);
	}
}
