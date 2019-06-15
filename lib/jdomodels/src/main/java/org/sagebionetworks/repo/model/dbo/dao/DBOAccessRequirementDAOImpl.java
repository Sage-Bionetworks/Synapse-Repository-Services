package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ACCESS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CONCRETE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_APPROVAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT_REVISION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SUBJECT_ACCESS_REQUIREMENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementInfoForUpdate;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementRevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSubjectAccessRequirement;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DBOAccessRequirementDAOImpl implements AccessRequirementDAO {
	public static final String LIMIT_PARAM = "LIMIT";
	public static final String OFFSET_PARAM = "OFFSET";
	public static final Long DEFAULT_VERSION = 0L;
	
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
			+ COL_ACCESS_REQUIREMENT_ETAG+" = :"+COL_ACCESS_REQUIREMENT_ETAG+", "
			+ COL_ACCESS_REQUIREMENT_CONCRETE_TYPE+" = :"+COL_ACCESS_REQUIREMENT_CONCRETE_TYPE
			+ " WHERE "+COL_ACCESS_REQUIREMENT_ID+" = :"+COL_ACCESS_REQUIREMENT_ID;
	
	private static final String SELECT_CURRENT_REQUIREMENTS_BY_ID = 
			"SELECT *"
			+ " FROM "+TABLE_ACCESS_REQUIREMENT+ " REQ"
			+ " JOIN "+TABLE_ACCESS_REQUIREMENT_REVISION+" REV"
			+ " ON (REQ."+COL_ACCESS_REQUIREMENT_ID+" = REV."+COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID
					+ " AND REQ."+COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER+" = REV."+COL_ACCESS_REQUIREMENT_REVISION_NUMBER+")"
			+ " WHERE REQ."+COL_ACCESS_REQUIREMENT_ID+" IN (:"+COL_ACCESS_REQUIREMENT_ID.toLowerCase()+")"
			+ " ORDER BY REQ."+COL_ACCESS_REQUIREMENT_ID;

	private static final String GET_ACCESS_REQUIREMENTS_IDS_FOR_SUBJECTS_SQL = 
			"SELECT DISTINCT "+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID
			+" FROM "+TABLE_SUBJECT_ACCESS_REQUIREMENT
			+" WHERE "+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+" IN (:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+") "
			+" AND "+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE;

	private static final String GET_SUBJECT_ACCESS_REQUIREMENT_SQL = "SELECT *"
			+" FROM "+TABLE_SUBJECT_ACCESS_REQUIREMENT
			+" WHERE "+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;

	private static final String GET_SUBJECT_ACCESS_REQUIREMENT_WITH_LIMIT_AND_OFFSET =
			GET_SUBJECT_ACCESS_REQUIREMENT_SQL+" "
			+LIMIT_PARAM+" :"+LIMIT_PARAM+" "
			+OFFSET_PARAM+" :"+OFFSET_PARAM;

	private static final String SELECT_INFO_FOR_UPDATE_SQL = "SELECT "
			+ COL_ACCESS_REQUIREMENT_ID+", "
			+ COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER+", "
			+ COL_ACCESS_REQUIREMENT_ETAG+", "
			+ COL_ACCESS_REQUIREMENT_ACCESS_TYPE+", "
			+ COL_ACCESS_REQUIREMENT_CONCRETE_TYPE
			+" FROM "+TABLE_ACCESS_REQUIREMENT
			+" WHERE "+COL_ACCESS_REQUIREMENT_ID+"=:"+COL_ACCESS_REQUIREMENT_ID
			+ " FOR UPDATE";

	private static final String SELECT_FOR_UPDATE_SQL = 
			"SELECT *"
			+ " FROM "+TABLE_ACCESS_REQUIREMENT+ " REQ"
			+ " JOIN "+TABLE_ACCESS_REQUIREMENT_REVISION+" REV"
			+ " ON (REQ."+COL_ACCESS_REQUIREMENT_ID+" = REV."+COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID
					+ " AND REQ."+COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER+" = REV."+COL_ACCESS_REQUIREMENT_REVISION_NUMBER+")"
			+" WHERE "+COL_ACCESS_REQUIREMENT_ID+"=:"+COL_ACCESS_REQUIREMENT_ID
			+ " FOR UPDATE";

	private static final String DELETE_SUBJECT_ACCESS_REQUIREMENTS_SQL = 
			"DELETE FROM "+TABLE_SUBJECT_ACCESS_REQUIREMENT
			+ " WHERE "+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;

	private static final String GET_ACCESS_REQUIREMENTS_IDS_PAGE_SQL =
			GET_ACCESS_REQUIREMENTS_IDS_FOR_SUBJECTS_SQL+" "
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

	private static final String SOURCE_SUBJECTS = "SOURCE_SUBJECTS";
	private static final String DEST_SUBJECTS = "DEST_SUBJECTS";
	private static final String SELECT_ACCESS_REQUIREMENT_DIFF = "SELECT DISTINCT "+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID
			+" FROM "+TABLE_SUBJECT_ACCESS_REQUIREMENT
			+" WHERE "+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+" IN (:"+SOURCE_SUBJECTS+")"
			+" AND "+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+" = :"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE
			+" AND "+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+" NOT IN ("
					+ "SELECT DISTINCT "+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID
					+" FROM "+TABLE_SUBJECT_ACCESS_REQUIREMENT
					+" WHERE "+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+" IN (:"+DEST_SUBJECTS+")"
					+" AND "+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+" = :"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+")";


	private static final RowMapper<DBOAccessRequirement> accessRequirementRowMapper = (new DBOAccessRequirement()).getTableMapping();
	private static final RowMapper<DBOSubjectAccessRequirement> subjectAccessRequirementRowMapper = (new DBOSubjectAccessRequirement()).getTableMapping();
	private static final RowMapper<DBOAccessRequirementRevision> revisionRowMapper = new DBOAccessRequirementRevision().getTableMapping();

	/*
	 * This mapper can be used for the join of requirement and revision.
	 */
	private static final RowMapper<AccessRequirement> requirementMapper = new RowMapper<AccessRequirement>() {
		@Override
		public AccessRequirement mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			DBOAccessRequirement dboRequirement = accessRequirementRowMapper.mapRow(rs, rowNum);
			DBOAccessRequirementRevision dboRevision = revisionRowMapper.mapRow(rs, rowNum);
			return AccessRequirementUtils.copyDboToDto(dboRequirement, dboRevision);
		}
	};

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
		if (subjectIds.isEmpty()){
			return new ArrayList<AccessRequirement>();
		}
		List<Long> subjectIdsAsLong = new ArrayList<Long>();
		for (String id: subjectIds) {
			subjectIdsAsLong.add(KeyFactory.stringToKey(id));
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, subjectIdsAsLong);
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, type.name());
		List<Long> ids = namedJdbcTemplate.queryForList(GET_ACCESS_REQUIREMENTS_IDS_FOR_SUBJECTS_SQL, param, Long.class);
		return getAccessRequirements(ids);
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
		List<Long> subjectIdsAsLong = KeyFactory.stringToKey(subjectIds);
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

	@WriteTransaction
	@Override
	public void delete(String id) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOAccessRequirement.class, param);
	}

	@WriteTransaction
	@Override
	public <T extends AccessRequirement> T create(T dto) {
		dto.setId(idGenerator.generateNewId(IdType.ACCESS_REQUIREMENT_ID));
		dto.setEtag(UUID.randomUUID().toString());
		dto.setVersionNumber(DEFAULT_VERSION);
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		DBOAccessRequirementRevision dboRevision = new DBOAccessRequirementRevision();
		AccessRequirementUtils.copyDtoToDbo(dto, dbo, dboRevision);
		dbo = basicDao.createNew(dbo);
		basicDao.createNew(dboRevision);
		populateSubjectAccessRequirement(dbo.getId(), dto.getSubjectIds());
		return (T) get(dbo.getId().toString());
	}

	/**
	 * Get the fully populated DTO for the given IDs.
	 * 
	 * @param requirementIds
	 * @return
	 */
	private List<AccessRequirement> getAccessRequirements(List<Long> requirementIds){
		if(requirementIds.isEmpty()){
			return new LinkedList<>();
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID.toLowerCase(), requirementIds);
		return namedJdbcTemplate.query(SELECT_CURRENT_REQUIREMENTS_BY_ID, param, requirementMapper);
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
		List<Long> ids = new LinkedList<>();
		ids.add(Long.parseLong(id));
		List<AccessRequirement> results = getAccessRequirements(ids);
		if(results.isEmpty()){
			throw new NotFoundException("AccessRequirement ID: "+id);
		}
		return results.get(0);
	}

	@Override
	public List<RestrictableObjectDescriptor> getSubjects(long accessRequirementId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID, accessRequirementId);
		List<DBOSubjectAccessRequirement> nars = namedJdbcTemplate.query(GET_SUBJECT_ACCESS_REQUIREMENT_SQL, param, subjectAccessRequirementRowMapper);
		return AccessRequirementUtils.copyDBOSubjectsToDTOSubjects(nars);
	}

	@Override
	public List<RestrictableObjectDescriptor> getSubjects(long accessRequirementId, long limit, long offset) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID, accessRequirementId);
		param.addValue(LIMIT_PARAM, limit);
		param.addValue(OFFSET_PARAM, offset);
		List<DBOSubjectAccessRequirement> nars = namedJdbcTemplate.query(GET_SUBJECT_ACCESS_REQUIREMENT_WITH_LIMIT_AND_OFFSET, param, subjectAccessRequirementRowMapper);
		return AccessRequirementUtils.copyDBOSubjectsToDTOSubjects(nars);
	}

	@MandatoryWriteTransaction
	@Override
	public AccessRequirementInfoForUpdate getForUpdate(String accessRequirementId) throws NotFoundException{
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, accessRequirementId);
		try {
			return namedJdbcTemplate.queryForObject(SELECT_INFO_FOR_UPDATE_SQL, param, new RowMapper<AccessRequirementInfoForUpdate>(){

				@Override
				public AccessRequirementInfoForUpdate mapRow(ResultSet rs, int rowNum) throws SQLException {
					AccessRequirementInfoForUpdate info = new AccessRequirementInfoForUpdate();
					info.setAccessRequirementId(rs.getLong(COL_ACCESS_REQUIREMENT_ID));
					info.setEtag(rs.getString(COL_ACCESS_REQUIREMENT_ETAG));
					info.setCurrentVersion(rs.getLong(COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER));
					info.setAccessType(ACCESS_TYPE.valueOf(rs.getString(COL_ACCESS_REQUIREMENT_ACCESS_TYPE)));
					info.setConcreteType(rs.getString(COL_ACCESS_REQUIREMENT_CONCRETE_TYPE));
					return info;
				}
				
			});
		}catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
	}

	@WriteTransaction
	@Override
	public <T extends AccessRequirement> T update(T dto) {

		DBOAccessRequirement toUpdate = new DBOAccessRequirement();
		DBOAccessRequirementRevision revision = new DBOAccessRequirementRevision();
		AccessRequirementUtils.copyDtoToDbo(dto, toUpdate, revision);

		// update the etag and version of the requirement
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, toUpdate.getId());
		param.addValue(COL_ACCESS_REQUIREMENT_ETAG, UUID.randomUUID().toString());
		param.addValue(COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER, toUpdate.getCurrentRevNumber());
		param.addValue(COL_ACCESS_REQUIREMENT_CONCRETE_TYPE, toUpdate.getConcreteType());
		namedJdbcTemplate.update(UPDATE_ACCESS_REQUIREMENT_SQL, param);

		// Create the new revision.
		basicDao.createNew(revision);

		clearSubjectAccessRequirement(dto.getId());
		populateSubjectAccessRequirement(dto.getId(), dto.getSubjectIds());

		return (T) get(dto.getId().toString());
	}

	@Override
	public List<AccessRequirement> getAccessRequirementsForSubject(
			List<String> subjectIds, RestrictableObjectType type,
			long limit, long offset) throws DatastoreException {
		List<AccessRequirement>  dtos = new ArrayList<AccessRequirement>();
		if (subjectIds.isEmpty()) {
			return dtos;
		}
		List<Long> subjectIdsAsLong = KeyFactory.stringToKey(subjectIds);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, subjectIdsAsLong);
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, type.name());
		param.addValue(LIMIT_PARAM, limit);
		param.addValue(OFFSET_PARAM, offset);
		List<Long> ids = namedJdbcTemplate.queryForList(GET_ACCESS_REQUIREMENTS_IDS_PAGE_SQL, param, Long.class);
		return getAccessRequirements(ids);
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
				if (type.equals(TermsOfUseAccessRequirement.class.getName())
						|| type.equals(SelfSignAccessRequirement.class.getName())) {
					stats.setHasToU(true);
				} else if (type.equals(ACTAccessRequirement.class.getName())
						|| type.equals(ManagedACTAccessRequirement.class.getName())) {
					stats.setHasACT(true);
				} else if (type.equals(LockAccessRequirement.class.getName())) {
					stats.setHasLock(true);
				}
				return null;
			}
		});
		return stats;
	}

	@Override
	public List<String> getAccessRequirementDiff(List<String> sourceSubjects, List<String> destSubjects, RestrictableObjectType type) {
		ValidateArgument.required(type, "type");
		ValidateArgument.required(sourceSubjects, "sourceSubjects");
		ValidateArgument.required(destSubjects, "destSubjects");
		ValidateArgument.requirement(!sourceSubjects.isEmpty(), "Need at least one source subject.");
		ValidateArgument.requirement(!destSubjects.isEmpty(), "Need at least one destination subject.");

		List<Long> sourceSubjectIdsAsLong = KeyFactory.stringToKey(sourceSubjects);
		List<Long> destSubjectIdsAsLong = KeyFactory.stringToKey(destSubjects);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SOURCE_SUBJECTS, sourceSubjectIdsAsLong);
		param.addValue(DEST_SUBJECTS, destSubjectIdsAsLong);
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, type.name());
		List<String> ids = namedJdbcTemplate.queryForList(SELECT_ACCESS_REQUIREMENT_DIFF, param, String.class);
		return ids;
	}

	@MandatoryWriteTransaction
	@Override
	public AccessRequirement getAccessRequirementForUpdate(String accessRequirementId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, accessRequirementId);
		try {
			return namedJdbcTemplate.queryForObject(SELECT_FOR_UPDATE_SQL, param, requirementMapper);
		}catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
	}
}
