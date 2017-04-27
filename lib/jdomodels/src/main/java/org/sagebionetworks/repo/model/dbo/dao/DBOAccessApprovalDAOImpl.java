package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_SERIALIZED_ENTITY;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author brucehoff
 *
 */
public class DBOAccessApprovalDAOImpl implements AccessApprovalDAO {
	public static final String LIMIT_PARAM = "LIMIT";
	public static final String OFFSET_PARAM = "OFFSET";
	
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

	private static final String SELECT_FOR_REQUIREMENT_SQL = 
			"SELECT * FROM "+TABLE_ACCESS_APPROVAL
			+" WHERE "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+"=:"+COL_ACCESS_APPROVAL_REQUIREMENT_ID;

	private static final String SELECT_FOR_REQUIREMENTS_AND_PRINCIPALS_SQL = 
			"SELECT * "
			+ "FROM "+TABLE_ACCESS_APPROVAL
			+" WHERE "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+" IN (:"+COL_ACCESS_APPROVAL_REQUIREMENT_ID+") "
			+ "AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" IN (:"+COL_ACCESS_APPROVAL_ACCESSOR_ID+")";

	private static final String SELECT_MET_ACCESS_REQUIREMENT_COUNT =
			"SELECT COUNT(DISTINCT "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+")"
			+ " FROM "+TABLE_ACCESS_APPROVAL
			+ " WHERE "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+" IN (:"+COL_ACCESS_APPROVAL_REQUIREMENT_ID+")"
			+ " AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" = :"+COL_ACCESS_APPROVAL_ACCESSOR_ID;

	private static final String SELECT_FOR_UPDATE_SQL = "select "
				+COL_ACCESS_APPROVAL_CREATED_BY+", "
				+COL_ACCESS_APPROVAL_CREATED_ON+", "
				+COL_ACCESS_APPROVAL_ETAG
			+" from "+TABLE_ACCESS_APPROVAL
			+" where "+COL_ACCESS_APPROVAL_ID+"=:"+COL_ACCESS_APPROVAL_ID+" for update";

	private static final String DELETE_ACCESS_APPROVAL = "DELETE"
			+ " FROM "+TABLE_ACCESS_APPROVAL
			+ " WHERE "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+" = :"+COL_ACCESS_APPROVAL_REQUIREMENT_ID
			+ " AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" = :"+COL_ACCESS_APPROVAL_ACCESSOR_ID;

	private static final String DELETE_ACCESS_APPROVALS = "DELETE"
			+ " FROM "+TABLE_ACCESS_APPROVAL
			+ " WHERE "+COL_ACCESS_APPROVAL_ID+" IN (:"+COL_ACCESS_APPROVAL_ID+")";

	private static final String SQL_INSERT_IGNORE = "INSERT IGNORE INTO "
			+TABLE_ACCESS_APPROVAL+"("
			+COL_ACCESS_APPROVAL_ID+", "
			+COL_ACCESS_APPROVAL_ETAG+", "
			+COL_ACCESS_APPROVAL_CREATED_BY+", "
			+COL_ACCESS_APPROVAL_CREATED_ON+", "
			+COL_ACCESS_APPROVAL_MODIFIED_BY+", "
			+COL_ACCESS_APPROVAL_MODIFIED_ON+", "
			+COL_ACCESS_APPROVAL_REQUIREMENT_ID+", "
			+COL_ACCESS_APPROVAL_ACCESSOR_ID+", "
			+COL_ACCESS_APPROVAL_SERIALIZED_ENTITY
			+") VALUES (?,?,?,?,?,?,?,?,?)";

	private static final String SELECT_APPROVED_USERS = 
				"SELECT DISTINCT "+COL_ACCESS_APPROVAL_ACCESSOR_ID
			+" FROM "+TABLE_ACCESS_APPROVAL
			+" WHERE "+COL_ACCESS_APPROVAL_REQUIREMENT_ID+" = :"+COL_ACCESS_APPROVAL_REQUIREMENT_ID
			+" AND "+COL_ACCESS_APPROVAL_ACCESSOR_ID+" IN (:"+COL_ACCESS_APPROVAL_ACCESSOR_ID+")";

	private static final RowMapper<DBOAccessApproval> rowMapper = (new DBOAccessApproval()).getTableMapping();


	@WriteTransactionReadCommitted
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOAccessApproval.class, param);
	}

	@WriteTransactionReadCommitted
	@Override
	public <T extends AccessApproval> T create(T dto) throws DatastoreException {
		return (T) createBatch(Arrays.asList((AccessApproval)dto)).get(0);
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
	public List<AccessApproval> getForAccessRequirementsAndPrincipals(Collection<String> accessRequirementIds, Collection<String> principalIds) throws DatastoreException {
		List<AccessApproval> dtos = new ArrayList<AccessApproval>();
		if (accessRequirementIds.isEmpty() || principalIds.isEmpty()) return dtos;
		MapSqlParameterSource params = new MapSqlParameterSource();		
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementIds);
		params.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, principalIds);
		List<DBOAccessApproval> dbos = namedJdbcTemplate.query(SELECT_FOR_REQUIREMENTS_AND_PRINCIPALS_SQL, params, rowMapper);
		for (DBOAccessApproval dbo : dbos) {
			AccessApproval dto = AccessApprovalUtils.copyDboToDto(dbo);
			// validate:  The principal ID and accessor ID should each be from the passed in lists
			if (!principalIds.contains(dto.getAccessorId())) 
				throw new IllegalStateException("PrincipalIDs: "+principalIds+" but accessorId: "+dto.getAccessorId());
			if (!accessRequirementIds.contains(dto.getRequirementId().toString()))
				throw new IllegalStateException("accessRequirementIds: "+accessRequirementIds+" but requirementId: "+dto.getRequirementId());
			dtos.add(dto);
		}
		return dtos;
	}

	@WriteTransactionReadCommitted
	@Override
	public <T extends AccessApproval> T  update(T dto) throws DatastoreException,
			InvalidModelException, NotFoundException, ConflictingUpdateException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ID, dto.getId());
		List<DBOAccessApproval> aas = null;
		try{
			aas = namedJdbcTemplate.query(SELECT_FOR_UPDATE_SQL, param, new RowMapper<DBOAccessApproval>(){
				@Override
				public DBOAccessApproval mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					DBOAccessApproval aa = new DBOAccessApproval();
					aa.setCreatedOn(rs.getLong(COL_ACCESS_APPROVAL_CREATED_ON));
					aa.setCreatedBy(rs.getLong(COL_ACCESS_APPROVAL_CREATED_BY));
					aa.seteTag(rs.getString(COL_ACCESS_APPROVAL_ETAG));
					return aa;
				}
			});
		}catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
		if (aas.isEmpty()) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");			
		}

		// Check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		DBOAccessApproval dbo = aas.get(0);
		if (!dbo.geteTag().equals(dto.getEtag())) {
			throw new ConflictingUpdateException("Access Approval was updated since you last fetched it, retrieve it again and reapply the update.");
		}
		AccessApprovalUtils.copyDtoToDbo(dto, dbo);
		
		// Update with a new e-tag
		dbo.seteTag(UUID.randomUUID().toString());

		boolean success = basicDao.update(dbo);
		if (!success) throw new DatastoreException("Unsuccessful updating user Access Approval in database.");

		T resultantDto = (T)AccessApprovalUtils.copyDboToDto(dbo);

		return resultantDto;
	}

	@Override
	public List<AccessApproval> getForAccessRequirement(String accessRequirementId) throws DatastoreException {
		List<AccessApproval> dtos = new ArrayList<AccessApproval>();
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementId);
		List<DBOAccessApproval> dbos = namedJdbcTemplate.query(SELECT_FOR_REQUIREMENT_SQL, params, rowMapper);
		for (DBOAccessApproval dbo : dbos) {
			AccessApproval dto = AccessApprovalUtils.copyDboToDto(dbo);
			if (!accessRequirementId.equals(dto.getRequirementId().toString()))
				throw new IllegalStateException("accessRequirementId: "+accessRequirementId+
						" but dto.getRequirementId(): "+dto.getRequirementId());
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public void delete(String accessRequirementId, String accessorId) {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(accessorId, "accessorId");
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementId);
		params.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, accessorId);
		namedJdbcTemplate.update(DELETE_ACCESS_APPROVAL, params);
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

	@WriteTransactionReadCommitted
	@Override
	public List<AccessApproval> createBatch(List<AccessApproval> dtos) {
		final List<DBOAccessApproval> dbos = AccessApprovalUtils.copyDtosToDbos(dtos, true/*for creation*/, idGenerator);
		final List<String> principalIds = new LinkedList<String>();
		final List<String> requirementIds = new LinkedList<String>();
		jdbcTemplate.batchUpdate(SQL_INSERT_IGNORE, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setLong(1, dbos.get(i).getId());
				ps.setString(2, dbos.get(i).geteTag());
				ps.setLong(3, dbos.get(i).getCreatedBy());
				ps.setLong(4, dbos.get(i).getCreatedOn());
				ps.setLong(5, dbos.get(i).getModifiedBy());
				ps.setLong(6, dbos.get(i).getModifiedOn());
				ps.setLong(7, dbos.get(i).getRequirementId());
				ps.setLong(8, dbos.get(i).getAccessorId());
				ps.setBytes(9, dbos.get(i).getSerializedEntity());
				principalIds.add(dbos.get(i).getAccessorId().toString());
				requirementIds.add(dbos.get(i).getRequirementId().toString());
			}

			@Override
			public int getBatchSize() {
				// TODO Auto-generated method stub
				return dbos.size();
			}
		});
		return getForAccessRequirementsAndPrincipals(requirementIds, principalIds);
	}

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

	@WriteTransactionReadCommitted
	@Override
	public int deleteBatch(List<Long> toDelete) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_ID, toDelete);
		return namedJdbcTemplate.update(DELETE_ACCESS_APPROVALS, params);
	}

	@Override
	public Set<String> getApprovedUsers(List<String> userIds, String accessRequirementId) {
		Set<String> result = new HashSet<String>();
		if (userIds.isEmpty()){
			return result;
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_ACCESS_APPROVAL_REQUIREMENT_ID, accessRequirementId);
		params.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, userIds);
		List<String> approvedUsers = namedJdbcTemplate.queryForList(SELECT_APPROVED_USERS, params, String.class);
		result.addAll(approvedUsers);
		return result;
	}
}
