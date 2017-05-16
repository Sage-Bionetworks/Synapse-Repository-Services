package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ACCESS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CONCRETE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_APPROVAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SUBJECT_ACCESS_REQUIREMENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementRevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSubjectAccessRequirement;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;

public class DBOAccessRequirementDAOImpl implements AccessRequirementDAO {
	public static final String LIMIT_PARAM = "LIMIT";
	public static final String OFFSET_PARAM = "OFFSET";
	public static final Long DEFAULT_VERSION = 1L;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final String UPDATE_ACCESS_REQUIREMENT_SQL = "UPDATE "
			+ TABLE_ACCESS_REQUIREMENT
			+ " SET "+COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER+" = :"+COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER+", "
			+ COL_ACCESS_REQUIREMENT_ETAG+" = :"+COL_ACCESS_REQUIREMENT_ETAG
			+ " WHERE "+COL_ACCESS_REQUIREMENT_ID+" = :"+COL_ACCESS_REQUIREMENT_ID;

	private static final String GET_ACCESS_REQUIREMENTS_FOR_SUBJECTS_SQL = 
			"SELECT DISTINCT ar.*"
			+" FROM "+TABLE_ACCESS_REQUIREMENT+" ar, "+TABLE_SUBJECT_ACCESS_REQUIREMENT+" nar"
			+" WHERE ar."+COL_ACCESS_REQUIREMENT_ID+" = nar."+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID
			+" AND nar."+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+" IN (:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+") "
			+" AND nar."+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE
			+" ORDER BY "+COL_ACCESS_REQUIREMENT_ID;

	private static final String GET_ACCESS_REQUIREMENT_SQL = "SELECT *"
			+" FROM "+TABLE_ACCESS_REQUIREMENT
			+" WHERE "+COL_ACCESS_REQUIREMENT_ID+"=:"+COL_ACCESS_REQUIREMENT_ID;

	private static final String GET_SUBJECT_ACCESS_REQUIREMENT_SQL = "SELECT *"
			+" FROM "+TABLE_SUBJECT_ACCESS_REQUIREMENT
			+" WHERE "+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;

	private static final String SELECT_FOR_UPDATE_SQL = GET_ACCESS_REQUIREMENT_SQL
			+ " FOR UPDATE";

	private static final String DELETE_SUBJECT_ACCESS_REQUIREMENTS_SQL = 
			"DELETE FROM "+TABLE_SUBJECT_ACCESS_REQUIREMENT
			+ " WHERE "+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;

	private static final String GET_ACCESS_REQUIREMENTS_PAGE_SQL =
			GET_ACCESS_REQUIREMENTS_FOR_SUBJECTS_SQL+" "
			+LIMIT_PARAM+" :"+LIMIT_PARAM+" "
			+OFFSET_PARAM+" :"+OFFSET_PARAM;

	private static final String SELECT_CONCRETE_TYPE_SQL = "SELECT "+COL_ACCESS_REQUIREMENT_CONCRETE_TYPE
			+" FROM "+TABLE_ACCESS_REQUIREMENT
			+" WHERE "+COL_ACCESS_REQUIREMENT_ID+" = ?";

	private static final String SELECT_ACCESS_REQUIREMENT_STATS = "SELECT "
				+COL_ACCESS_REQUIREMENT_ID+", "
				+COL_ACCESS_REQUIREMENT_CONCRETE_TYPE
			+" FROM "+TABLE_ACCESS_REQUIREMENT+", "
				+TABLE_SUBJECT_ACCESS_REQUIREMENT
			+" WHERE "+COL_ACCESS_REQUIREMENT_ID+" = "+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID
			+" AND "+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+" IN (:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+")"
			+" AND "+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+" = :"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE;

	private static final RowMapper<DBOAccessRequirement> accessRequirementRowMapper = (new DBOAccessRequirement()).getTableMapping();
	private static final RowMapper<DBOSubjectAccessRequirement> subjectAccessRequirementRowMapper = (new DBOSubjectAccessRequirement()).getTableMapping();

	// DEPRECATED SQL
	private static final String UNMET_REQUIREMENTS_AR_COL_ID = "ar_id";
	private static final String UNMET_REQUIREMENTS_AA_COL_ID = "aa_id";

	private static final String UNMET_REQUIREMENTS_SQL_PREFIX = "select"
			+ " ar."+COL_ACCESS_REQUIREMENT_ID+" as "+UNMET_REQUIREMENTS_AR_COL_ID+","
			+ " aa."+COL_ACCESS_APPROVAL_ID+" as "+UNMET_REQUIREMENTS_AA_COL_ID
			+ " FROM "+TABLE_ACCESS_REQUIREMENT+" ar ";
	
	private static final String UNMET_REQUIREMENTS_SQL_SUFFIX = 
			" left join "+TABLE_ACCESS_APPROVAL+" aa"
			+ " on ar."+COL_ACCESS_REQUIREMENT_ID+"=aa."+COL_ACCESS_APPROVAL_REQUIREMENT_ID
			+ " and aa."+COL_ACCESS_APPROVAL_ACCESSOR_ID+" in (:"+COL_ACCESS_APPROVAL_ACCESSOR_ID+")"
			+ " where ar."+COL_ACCESS_REQUIREMENT_ACCESS_TYPE+" in (:"+COL_ACCESS_REQUIREMENT_ACCESS_TYPE+")"
			+ " order by "+UNMET_REQUIREMENTS_AR_COL_ID;

	// select ar.id as ar_id, aa.id as aa_id
	// from ACCESS_REQUIREMENT ar 
	// join NODE_ACCESS_REQUIREMENT nar on nar.requirement_id=ar.id and 
	// nar.subject_type=:subject_type and nar.subject_id in (:subject_id)
	// left join ACCESS_APPROVAL aa on ar.id=aa.requirement_id and aa.accessor_id in (:accessor_id)
	// where ar.access_type=:access_type
	private static final String SELECT_UNMET_REQUIREMENTS_SQL = 
			UNMET_REQUIREMENTS_SQL_PREFIX
			+" join "+TABLE_SUBJECT_ACCESS_REQUIREMENT+" nar"
			+ " on nar."+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=ar."+COL_ACCESS_REQUIREMENT_ID+" "
			+"and nar."+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+" "
			+"and nar."+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+" in (:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+") "
			+UNMET_REQUIREMENTS_SQL_SUFFIX;

	@Deprecated
	@Override
	public List<AccessRequirement> getAllAccessRequirementsForSubject(List<String> subjectIds, RestrictableObjectType type)  throws DatastoreException {
		List<AccessRequirement>  dtos = new ArrayList<AccessRequirement>();
		if (subjectIds.isEmpty()) return dtos;
		List<Long> subjectIdsAsLong = new ArrayList<Long>();
		for (String id: subjectIds) {
			subjectIdsAsLong.add(KeyFactory.stringToKey(id));
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, subjectIdsAsLong);
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, type.name());
		List<DBOAccessRequirement> dbos = namedJdbcTemplate.query(GET_ACCESS_REQUIREMENTS_FOR_SUBJECTS_SQL, param, accessRequirementRowMapper);
		for (DBOAccessRequirement dbo : dbos) {
			AccessRequirement dto = AccessRequirementUtils.copyDboToDto(dbo, getSubjects(dbo.getId()));
			dtos.add(dto);
		}
		return dtos;
	}

	@Deprecated
	@Override
	public List<Long> getAllUnmetAccessRequirements(List<String> subjectIds, RestrictableObjectType subjectType, Collection<Long> principalIds, Collection<ACCESS_TYPE> accessTypes) throws DatastoreException {
		if (subjectIds.isEmpty()) return new ArrayList<Long>();
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, principalIds);
		List<String> accessTypeStrings = new ArrayList<String>();
		for (ACCESS_TYPE type : accessTypes) {
			accessTypeStrings.add(type.toString());
		}
		List<Long> subjectIdsAsLong = new ArrayList<Long>();
		for (String id: subjectIds) {
			subjectIdsAsLong.add(KeyFactory.stringToKey(id));
		}
		param.addValue(COL_ACCESS_REQUIREMENT_ACCESS_TYPE, accessTypeStrings);
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, subjectIdsAsLong);
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, subjectType.name());
		List<Long> arIds = namedJdbcTemplate.query(SELECT_UNMET_REQUIREMENTS_SQL, param, new RowMapper<Long>(){
			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				rs.getLong(UNMET_REQUIREMENTS_AA_COL_ID);
				if (rs.wasNull()) { // no access approval, so this is one of the requirements we've been looking for
					return rs.getLong(UNMET_REQUIREMENTS_AR_COL_ID);
				} else {
					return null; 
				}
			}
		});
		// now jus strip out the nulls and return the list
		List<Long> result = new ArrayList<Long>();
		for (Long arId : arIds) if (arId!=null) result.add(arId);
		return result;
	}

	@WriteTransactionReadCommitted
	@Override
	public void delete(String id) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOAccessRequirement.class, param);
	}

	@WriteTransactionReadCommitted
	@Override
	public <T extends AccessRequirement> T create(T dto) {
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		dbo.setId(idGenerator.generateNewId(IdType.ACCESS_REQUIREMENT_ID));
		dbo.seteTag(UUID.randomUUID().toString());
		dbo.setCurrentRevNumber(DEFAULT_VERSION);
		dbo = basicDao.createNew(dbo);

		dto.setId(dbo.getId());
		DBOAccessRequirementRevision dboRevision = new DBOAccessRequirementRevision();
		AccessRequirementUtils.copyDTOToDBOAccessRequirementRevision(dto, dboRevision, DEFAULT_VERSION);
		basicDao.createNew(dboRevision);

		populateSubjectAccessRequirement(dbo.getId(), dto.getSubjectIds());
		return (T) get(dbo.getId().toString());
	}

	private void populateSubjectAccessRequirement(Long accessRequirementId, List<RestrictableObjectDescriptor> rodList) {
		if (rodList == null || rodList.isEmpty()) {
			return;
		}
		List<DBOSubjectAccessRequirement> batch = AccessRequirementUtils.createBatchDBOSubjectAccessRequirement(accessRequirementId, rodList);
		if (batch.size()>0) {
			basicDao.createBatch(batch);
		}
	}

	private void clearSubjectAccessRequirement(Long accessRequirementId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID, accessRequirementId);
		namedJdbcTemplate.update(DELETE_SUBJECT_ACCESS_REQUIREMENTS_SQL, param);
	}

	@Override
	public AccessRequirement get(String id) throws NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, id);
		DBOAccessRequirement dbo = namedJdbcTemplate.queryForObject(GET_ACCESS_REQUIREMENT_SQL, param, accessRequirementRowMapper);
		List<RestrictableObjectDescriptor> entities = getSubjects(dbo.getId());
		return AccessRequirementUtils.copyDboToDto(dbo, entities);
	}

	@Override
	public List<RestrictableObjectDescriptor> getSubjects(Long accessRequirementId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID, accessRequirementId);
		List<DBOSubjectAccessRequirement> nars = namedJdbcTemplate.query(GET_SUBJECT_ACCESS_REQUIREMENT_SQL, param, subjectAccessRequirementRowMapper);
		return AccessRequirementUtils.copyDBOSubjectsToDTOSubjects(nars);
	}

	private DBOAccessRequirement getAccessRequirementForUpdate(Long accessRequirementId) throws NotFoundException{
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, accessRequirementId);
		try {
			return namedJdbcTemplate.queryForObject(SELECT_FOR_UPDATE_SQL, param, accessRequirementRowMapper);
		}catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public <T extends AccessRequirement> T update(T dto) {
		DBOAccessRequirement current = getAccessRequirementForUpdate(dto.getId());
		if(!current.geteTag().equals(dto.getEtag())) {
			throw new ConflictingUpdateException("Access Requirement was updated since you last fetched it,"
					+ " retrieve it again and reapply the update.");
		}

		Long newVersion = 1L;
		if (current.getCurrentRevNumber() != null) {
			newVersion = current.getCurrentRevNumber()+1;
		}

		DBOAccessRequirement toUpdate = new DBOAccessRequirement();
		AccessRequirementUtils.copyDtoToDbo(dto, toUpdate);
		toUpdate.setCurrentRevNumber(newVersion);
		toUpdate.seteTag(UUID.randomUUID().toString());
		basicDao.update(toUpdate);

		// TODO: after established AccessRequirementRevision and use it in get, only update etag and versionNumber
		/*MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, dto.getId());
		param.addValue(COL_ACCESS_REQUIREMENT_ETAG, UUID.randomUUID().toString());
		param.addValue(COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER, newVersion);
		namedJdbcTemplate.update(UPDATE_ACCESS_REQUIREMENT_SQL, param);*/

		DBOAccessRequirementRevision dboRevision = new DBOAccessRequirementRevision();
		AccessRequirementUtils.copyDTOToDBOAccessRequirementRevision(dto, dboRevision, newVersion);
		basicDao.createNew(dboRevision);

		clearSubjectAccessRequirement(dto.getId());
		populateSubjectAccessRequirement(dto.getId(), dto.getSubjectIds());

		return (T) get(dto.getId().toString());
	}

	@Override
	public List<AccessRequirement> getAccessRequirementsForSubject(
			List<String> subjectIds, RestrictableObjectType type,
			Long limit, Long offset) throws DatastoreException {
		List<AccessRequirement>  dtos = new ArrayList<AccessRequirement>();
		if (subjectIds.isEmpty()) {
			return dtos;
		}
		List<Long> subjectIdsAsLong = new ArrayList<Long>();
		for (String id: subjectIds) {
			subjectIdsAsLong.add(KeyFactory.stringToKey(id));
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, subjectIdsAsLong);
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, type.name());
		param.addValue(LIMIT_PARAM, limit);
		param.addValue(OFFSET_PARAM, offset);
		List<DBOAccessRequirement> dbos = namedJdbcTemplate.query(GET_ACCESS_REQUIREMENTS_PAGE_SQL, param, accessRequirementRowMapper);
		for (DBOAccessRequirement dbo : dbos) {
			AccessRequirement dto = AccessRequirementUtils.copyDboToDto(dbo, getSubjects(dbo.getId()));
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public String getConcreteType(String accessRequirementId) {
		try {
			return jdbcTemplate.queryForObject(SELECT_CONCRETE_TYPE_SQL, String.class, accessRequirementId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@Override
	public AccessRequirementStats getAccessRequirementStats(List<String> subjectIds, RestrictableObjectType type) {
		ValidateArgument.requirement(subjectIds != null && !subjectIds.isEmpty(), "subjectIds must contain at least one ID.");
		ValidateArgument.required(type, "type");
		final AccessRequirementStats stats = new AccessRequirementStats();
		stats.setHasACT(false);
		stats.setHasToU(false);
		stats.setHasLock(false);
		final Set<String> requirementIdSet = new HashSet<String>();
		stats.setRequirementIdSet(requirementIdSet);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, KeyFactory.stringToKey(subjectIds));
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, type.name());
		namedJdbcTemplate.query(SELECT_ACCESS_REQUIREMENT_STATS, param, new RowMapper<Void>(){

			@Override
			public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
				requirementIdSet.add(rs.getString(COL_ACCESS_REQUIREMENT_ID));
				String type = rs.getString(COL_ACCESS_REQUIREMENT_CONCRETE_TYPE);
				if (type.equals(TermsOfUseAccessRequirement.class.getName())) {
					stats.setHasToU(true);
				} else if (type.equals(ACTAccessRequirement.class.getName())) {
					stats.setHasACT(true);
				} else if (type.equals(LockAccessRequirement.class.getName())) {
					stats.setHasLock(true);
				}
				return null;
			}
		});
		return stats;
	}

	@WriteTransactionReadCommitted
	@Override
	public AccessRequirement updateVersion(Long accessRequirementId) {
		DBOAccessRequirement current = getAccessRequirementForUpdate(accessRequirementId);
		Long newVersion = 1L;
		if (current.getCurrentRevNumber() != null) {
			newVersion = current.getCurrentRevNumber()+1;
		}
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, accessRequirementId);
		param.addValue(COL_ACCESS_REQUIREMENT_ETAG, UUID.randomUUID().toString());
		param.addValue(COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER, newVersion);
		namedJdbcTemplate.update(UPDATE_ACCESS_REQUIREMENT_SQL, param);

		DBOAccessRequirementRevision dboRevision = new DBOAccessRequirementRevision();
		dboRevision.setOwnerId(current.getId());
		dboRevision.setNumber(newVersion);
		dboRevision.setModifiedBy(current.getModifiedBy());
		dboRevision.setModifiedOn(current.getModifiedOn());
		dboRevision.setAccessType(current.getAccessType());
		dboRevision.setConcreteType(current.getConcreteType());
		dboRevision.setSerializedEntity(current.getSerializedEntity());
		basicDao.createNew(dboRevision);

		return get(accessRequirementId.toString());
	}
}
