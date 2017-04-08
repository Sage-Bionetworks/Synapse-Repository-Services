/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ACCESS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ID;
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
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSubjectAccessRequirement;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.sagebionetworks.repo.transactions.WriteTransaction;

/**
 * @author brucehoff
 *
 */
public class DBOAccessRequirementDAOImpl implements AccessRequirementDAO {
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
	
	private static final String SELECT_ALL_IDS_SQL = 
		"SELECT "+COL_ACCESS_REQUIREMENT_ID+" FROM "+TABLE_ACCESS_REQUIREMENT;

	private static final String SELECT_FOR_SUBJECT_SQL = 
		"SELECT * FROM "+TABLE_ACCESS_REQUIREMENT+" ar, "+ 
		TABLE_SUBJECT_ACCESS_REQUIREMENT +" nar WHERE ar."+
		COL_ACCESS_REQUIREMENT_ID+" = nar."+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+
		" AND nar."+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+" in (:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+") "+
		" AND nar."+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+
		" order by "+COL_ACCESS_REQUIREMENT_ID;

	private static final String SELECT_FOR_SAR_SQL = "select * from "+TABLE_SUBJECT_ACCESS_REQUIREMENT+" where "+
	COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;

	private static final String SELECT_FOR_UPDATE_SQL = "select "+
			COL_ACCESS_REQUIREMENT_CREATED_BY+", "+
			COL_ACCESS_REQUIREMENT_CREATED_ON+", "+
			COL_ACCESS_REQUIREMENT_ETAG+
			" from "+TABLE_ACCESS_REQUIREMENT+" where "+COL_ACCESS_REQUIREMENT_ID+
			"=:"+COL_ACCESS_REQUIREMENT_ID+" for update";
	
	private static final String DELETE_SUBJECT_ACCESS_REQUIREMENT_SQL = 
		"delete from "+TABLE_SUBJECT_ACCESS_REQUIREMENT+" where "+
		COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;
	
	
	private static final String UNMET_REQUIREMENTS_AR_COL_ID = "ar_id";
	private static final String UNMET_REQUIREMENTS_AA_COL_ID = "aa_id";
	
	private static final String UNMET_REQUIREMENTS_SQL_PREFIX = "select ar."+COL_ACCESS_REQUIREMENT_ID+" as "+UNMET_REQUIREMENTS_AR_COL_ID+
	", aa."+COL_ACCESS_APPROVAL_ID+" as "+UNMET_REQUIREMENTS_AA_COL_ID+
	" from "+TABLE_ACCESS_REQUIREMENT+" ar ";
	
	private static final String UNMET_REQUIREMENTS_SQL_SUFFIX = " left join "+TABLE_ACCESS_APPROVAL+" aa on ar."+COL_ACCESS_REQUIREMENT_ID+"=aa."+COL_ACCESS_APPROVAL_REQUIREMENT_ID+
	" and aa."+COL_ACCESS_APPROVAL_ACCESSOR_ID+" in (:"+COL_ACCESS_APPROVAL_ACCESSOR_ID+
	") where ar."+COL_ACCESS_REQUIREMENT_ACCESS_TYPE+" in (:"+COL_ACCESS_REQUIREMENT_ACCESS_TYPE+
	") order by "+UNMET_REQUIREMENTS_AR_COL_ID;
	
	// select ar.id as ar_id, aa.id as aa_id
	// from ACCESS_REQUIREMENT ar 
	// join NODE_ACCESS_REQUIREMENT nar on nar.requirement_id=ar.id and 
	// nar.subject_type=:subject_type and nar.subject_id in (:subject_id)
	// left join ACCESS_APPROVAL aa on ar.id=aa.requirement_id and aa.accessor_id in (:accessor_id)
	// where ar.access_type=:access_type
	private static final String SELECT_UNMET_REQUIREMENTS_SQL = UNMET_REQUIREMENTS_SQL_PREFIX +
		" join "+TABLE_SUBJECT_ACCESS_REQUIREMENT+" nar on nar."+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=ar."+COL_ACCESS_REQUIREMENT_ID+
				" and nar."+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+
				" and nar."+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+" in (:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+") "+
				UNMET_REQUIREMENTS_SQL_SUFFIX;


	private static final String SELECT_FOR_SUBJECT_SQL_WITH_LIMIT_OFFSET =
			SELECT_FOR_SUBJECT_SQL+" "+LIMIT_PARAM+" :"+LIMIT_PARAM+" "
			+OFFSET_PARAM+" :"+OFFSET_PARAM;

	private static final String SELECT_CONCRETE_TYPE = "SELECT "+COL_ACCESS_REQUIREMENT_CONCRETE_TYPE
			+" FROM "+TABLE_ACCESS_REQUIREMENT
			+" WHERE "+COL_ACCESS_REQUIREMENT_ID+" = ?";

	private static final String SELECT_ACCESS_REQUIREMENT_STATS = "SELECT "
				+COL_ACCESS_REQUIREMENT_ID+", "
				+COL_ACCESS_REQUIREMENT_CONCRETE_TYPE
			+" FROM "+TABLE_ACCESS_REQUIREMENT+", "
				+TABLE_SUBJECT_ACCESS_REQUIREMENT
			+" WHERE "+COL_ACCESS_REQUIREMENT_ID+" = "+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID
			+" AND "+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+" = ?"
			+" AND "+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+" = ?";

	private static final RowMapper<DBOAccessRequirement> accessRequirementRowMapper = (new DBOAccessRequirement()).getTableMapping();
	private static final RowMapper<DBOSubjectAccessRequirement> subjectAccessRequirementRowMapper = (new DBOSubjectAccessRequirement()).getTableMapping();

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
	

	@WriteTransaction
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOAccessRequirement.class, param);
	}

	@WriteTransaction
	@Override
	public <T extends AccessRequirement> T create(T dto) throws DatastoreException, InvalidModelException{
	
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		if (dbo.getId() == null) {
			dbo.setId(idGenerator.generateNewId(IdType.ACCESS_REQUIREMENT_ID));
		}
		if (dbo.geteTag() == null) {
			dbo.seteTag(UUID.randomUUID().toString());
		}
		dbo = basicDao.createNew(dbo);
		populateSubjectAccessRequirement(dbo.getId(), dto.getSubjectIds());
		T result = (T)AccessRequirementUtils.copyDboToDto(dbo, getSubjects(dbo.getId()));
		return result;
	}

	
	@WriteTransaction
	public void populateSubjectAccessRequirement(Long accessRequirementId, List<RestrictableObjectDescriptor> subjectIds) throws DatastoreException {
		if (subjectIds==null || subjectIds.isEmpty()) return;

		List<DBOSubjectAccessRequirement> batch = new ArrayList<DBOSubjectAccessRequirement>();

		for (RestrictableObjectDescriptor subjectId: new HashSet<RestrictableObjectDescriptor>(subjectIds)) {
			DBOSubjectAccessRequirement nar = new DBOSubjectAccessRequirement();
			nar.setAccessRequirementId(accessRequirementId);
			RestrictableObjectType subjectType = subjectId.getType();
			if (subjectType==RestrictableObjectType.ENTITY) {
				Long nodeId = KeyFactory.stringToKey(subjectId.getId());
				nar.setSubjectId(nodeId);
			} else {
				nar.setSubjectId(Long.parseLong(subjectId.getId()));
			}
			nar.setSubjectType(subjectType.toString());
			batch.add(nar);
		}
			
		if (batch.size()>0) basicDao.createBatch(batch);

	}

	@Override
	public AccessRequirement get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID.toLowerCase(), id);
		DBOAccessRequirement dbo = basicDao.getObjectByPrimaryKey(DBOAccessRequirement.class, param);
		List<RestrictableObjectDescriptor> entities = getSubjects(dbo.getId());
		AccessRequirement dto = AccessRequirementUtils.copyDboToDto(dbo, entities);
		return dto;
	}
	
	@Override
	public List<String> getIds() {
		return namedJdbcTemplate.query(SELECT_ALL_IDS_SQL, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(SqlConstants.COL_ACCESS_REQUIREMENT_ID);
			}
		});
	}
	
	public List<RestrictableObjectDescriptor> getSubjects(Long accessRequirementId) {
		List<RestrictableObjectDescriptor> ans = new ArrayList<RestrictableObjectDescriptor>();	

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID, accessRequirementId);
		List<DBOSubjectAccessRequirement> nars = namedJdbcTemplate.query(SELECT_FOR_SAR_SQL, param, subjectAccessRequirementRowMapper);
		for (DBOSubjectAccessRequirement nar: nars) {
			RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
			subjectId.setType(RestrictableObjectType.valueOf(nar.getSubjectType()));
			if (RestrictableObjectType.ENTITY==subjectId.getType()) {
				subjectId.setId(KeyFactory.keyToString(nar.getSubjectId()));
			} else {
				subjectId.setId(nar.getSubjectId().toString());
			}
			ans.add(subjectId);
		}

		return ans;
	}
	
	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(DBOAccessRequirement.class);
	}

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
		List<DBOAccessRequirement> dbos = namedJdbcTemplate.query(SELECT_FOR_SUBJECT_SQL, param, accessRequirementRowMapper);
		for (DBOAccessRequirement dbo : dbos) {
			AccessRequirement dto = AccessRequirementUtils.copyDboToDto(dbo, getSubjects(dbo.getId()));
			dtos.add(dto);
		}
		return dtos;
	}

	@WriteTransaction
	@Override
	public <T extends AccessRequirement> T update(T dto) throws DatastoreException,
			InvalidModelException,NotFoundException, ConflictingUpdateException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, dto.getId());
		List<DBOAccessRequirement> ars = null;
		try{
			ars = namedJdbcTemplate.query(SELECT_FOR_UPDATE_SQL, param, new RowMapper<DBOAccessRequirement>() {
				@Override
				public DBOAccessRequirement mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					DBOAccessRequirement ar = new DBOAccessRequirement();
					ar.setCreatedBy(rs.getLong(COL_ACCESS_REQUIREMENT_CREATED_BY));
					ar.setCreatedOn(rs.getLong(COL_ACCESS_REQUIREMENT_CREATED_ON));	
					ar.seteTag(rs.getString(COL_ACCESS_REQUIREMENT_ETAG));
					return ar;
				}
				
			});
		}catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
		if (ars.isEmpty()) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");			
		}

		// Check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		DBOAccessRequirement dbo = ars.get(0);
		if (!dbo.geteTag().equals(dto.getEtag())) {
			throw new ConflictingUpdateException("Access Requirement was updated since you last fetched it, retrieve it again and reapply the update.");
		}
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		// Update with a new e-tag
		dbo.seteTag(UUID.randomUUID().toString());

		boolean success = basicDao.update(dbo);

		if (!success) throw new DatastoreException("Unsuccessful updating user Access Requirement in database.");
		updateSubjectAccessRequirement(dbo.getId(), dto.getSubjectIds());
		T updatedAR = (T)AccessRequirementUtils.copyDboToDto(dbo, getSubjects(dbo.getId()));

		return updatedAR;
	} // the 'commit' is implicit in returning from a method annotated 'Transactional'
	
	// to update the node ids for the Access Requirement, we just delete the existing ones and repopulate
	private void updateSubjectAccessRequirement(Long acessRequirementId, List<RestrictableObjectDescriptor> subjectIds) throws DatastoreException {
		{
			// delete the existing subject-access-requirement records...
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID, acessRequirementId);
			namedJdbcTemplate.update(DELETE_SUBJECT_ACCESS_REQUIREMENT_SQL, param);
		}
		// ... now populate with the updated values
		populateSubjectAccessRequirement(acessRequirementId, subjectIds);
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
		List<DBOAccessRequirement> dbos = namedJdbcTemplate.query(SELECT_FOR_SUBJECT_SQL_WITH_LIMIT_OFFSET, param, accessRequirementRowMapper);
		for (DBOAccessRequirement dbo : dbos) {
			AccessRequirement dto = AccessRequirementUtils.copyDboToDto(dbo, getSubjects(dbo.getId()));
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public String getConcreteType(String accessRequirementId) {
		try {
			return jdbcTemplate.queryForObject(SELECT_CONCRETE_TYPE, String.class, accessRequirementId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@Override
	public AccessRequirementStats getAccessRequirementStats(String subjectId, RestrictableObjectType type) {
		final AccessRequirementStats stats = new AccessRequirementStats();
		stats.setHasACT(false);
		stats.setHasToU(false);
		final Set<String> requirementIdSet = new HashSet<String>();
		stats.setRequirementIdSet(requirementIdSet);
		jdbcTemplate.query(SELECT_ACCESS_REQUIREMENT_STATS, new RowMapper<Void>(){

			@Override
			public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
				requirementIdSet.add(rs.getString(COL_ACCESS_REQUIREMENT_ID));
				String type = rs.getString(COL_ACCESS_REQUIREMENT_CONCRETE_TYPE);
				if (type.equals(TermsOfUseAccessRequirement.class.getName())) {
					stats.setHasToU(true);
				} else if (type.equals(ACTAccessRequirement.class.getName())) {
					stats.setHasACT(true);
				}
				return null;
			}
		}, KeyFactory.stringToKey(subjectId), type.name());
		return stats;
	}
}
