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
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_APPROVAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SUBJECT_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.ids.ETagGenerator;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.RestricableODUtil;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSubjectAccessRequirement;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.ObjectDescriptorUtils;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author brucehoff
 *
 */
public class DBOAccessRequirementDAOImpl implements AccessRequirementDAO {
	
	@Autowired
	private DBOBasicDao basicDao;	
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private ETagGenerator eTagGenerator;
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private static final String SELECT_ALL_IDS_SQL = 
		"SELECT "+SqlConstants.COL_ACCESS_REQUIREMENT_ID+" FROM "+SqlConstants.TABLE_ACCESS_REQUIREMENT;

	private static final String SELECT_FOR_SUBJECT_SQL = 
		"SELECT * FROM "+SqlConstants.TABLE_ACCESS_REQUIREMENT+" ar, "+ 
		SqlConstants.TABLE_SUBJECT_ACCESS_REQUIREMENT +" nar WHERE ar."+
		SqlConstants.COL_ACCESS_REQUIREMENT_ID+" = nar."+SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+
		" AND nar."+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+
		" AND nar."+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+
		" order by "+SqlConstants.COL_ACCESS_REQUIREMENT_ID;


	private static final String SELECT_FOR_SAR_SQL = "select * from "+TABLE_SUBJECT_ACCESS_REQUIREMENT+" where "+
	COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;


	private static final String SELECT_FOR_RANGE_SQL = "select * from "+TABLE_ACCESS_REQUIREMENT+" order by "+COL_ACCESS_REQUIREMENT_ID+
	" limit :"+LIMIT_PARAM_NAME+" offset :"+OFFSET_PARAM_NAME;

	private static final String SELECT_FOR_MULTIPLE_SAR_SQL = "select * from "+TABLE_SUBJECT_ACCESS_REQUIREMENT+" where "+
		COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+" IN (:"+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+")";

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
	") where ar."+COL_ACCESS_REQUIREMENT_ACCESS_TYPE+"=:"+COL_ACCESS_REQUIREMENT_ACCESS_TYPE+
	" order by "+UNMET_REQUIREMENTS_AR_COL_ID;
	
	// select ar.id as ar_id, aa.id as aa_id
	// from ACCESS_REQUIREMENT ar 
	// join NODE_ACCESS_REQUIREMENT nar on nar.requirement_id=ar.id and nar.and nar.node_id=1072
	// left join ACCESS_APPROVAL aa on ar.id=aa.requirement_id and aa.accessor_id=682
	// where ar.access_type='DOWNLOAD'
	private static final String SELECT_UNMET_REQUIREMENTS_SQL = UNMET_REQUIREMENTS_SQL_PREFIX +
		" join "+TABLE_SUBJECT_ACCESS_REQUIREMENT+" nar on nar."+COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=ar."+COL_ACCESS_REQUIREMENT_ID+
				" and nar."+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE+
				" and nar."+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+"=:"+COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID+
				UNMET_REQUIREMENTS_SQL_SUFFIX;

	private static final RowMapper<DBOAccessRequirement> accessRequirementRowMapper = (new DBOAccessRequirement()).getTableMapping();
	private static final RowMapper<DBOSubjectAccessRequirement> subjectAccessRequirementRowMapper = (new DBOSubjectAccessRequirement()).getTableMapping();

	@Override
	public List<Long> unmetAccessRequirements(RestrictableObjectDescriptor subject, Collection<Long> principalIds, ACCESS_TYPE accessType) throws DatastoreException {

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, principalIds);
		param.addValue(COL_ACCESS_REQUIREMENT_ACCESS_TYPE, accessType.toString());
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, KeyFactory.stringToKey(subject.getId()));
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, subject.getType().toString());
		String unmetRequirementsSQL = SELECT_UNMET_REQUIREMENTS_SQL;
		List<Long> arIds = simpleJdbcTemplate.query(unmetRequirementsSQL, new RowMapper<Long>(){
			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				rs.getLong(UNMET_REQUIREMENTS_AA_COL_ID);
				if (rs.wasNull()) { // no access approval, so this is one of the requirements we've been looking for
					return rs.getLong(UNMET_REQUIREMENTS_AR_COL_ID);
				} else {
					return null; 
				}
			}
		}, param);
		// now jus strip out the nulls and return the list
		List<Long> result = new ArrayList<Long>();
		for (Long arId : arIds) if (arId!=null) result.add(arId);
		return result;
	}
	

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOAccessRequirement.class, param);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessRequirement> T create(T dto) throws DatastoreException, InvalidModelException{
	
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		if (dbo.getId()==null) dbo.setId(idGenerator.generateNewId());
		if (dbo.geteTag()==null) dbo.seteTag(eTagGenerator.generateETag());
		dbo = basicDao.createNew(dbo);
		populateSubjectAccessRequirement(dbo.getId(), dto.getSubjectIds());
		T result = (T)AccessRequirementUtils.copyDboToDto(dbo, getSubjects(dbo.getId()));
		return result;
	}

	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void populateSubjectAccessRequirement(Long accessRequirementId, List<RestrictableObjectDescriptor> subjectIds) throws DatastoreException {
		if (subjectIds==null || subjectIds.isEmpty()) return;

		List<DBOSubjectAccessRequirement> batch = new ArrayList<DBOSubjectAccessRequirement>();

		for (RestrictableObjectDescriptor subjectId: new HashSet<RestrictableObjectDescriptor>(subjectIds)) {
			DBOSubjectAccessRequirement nar = new DBOSubjectAccessRequirement();
			nar.setAccessRequirementId(accessRequirementId);
			RestrictableObjectType subjectType = subjectId.getType();
			String typeName = subjectType.toString();
			if (subjectType==RestrictableObjectType.ENTITY) {
				Long nodeId = KeyFactory.stringToKey(subjectId.getId());
				nar.setSubjectId(nodeId);
			} else  if (subjectType==RestrictableObjectType.EVALUATION) {
				nar.setSubjectId(Long.parseLong(subjectId.getId()));
			} else {
				throw new IllegalArgumentException("Unsupported type: "+subjectType);
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
		return simpleJdbcTemplate.query(SELECT_ALL_IDS_SQL, new RowMapper<String>() {
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
		List<DBOSubjectAccessRequirement> nars = simpleJdbcTemplate.query(SELECT_FOR_SAR_SQL, subjectAccessRequirementRowMapper, param);
		for (DBOSubjectAccessRequirement nar: nars) {
			RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
			subjectId.setType(RestrictableObjectType.valueOf(nar.getSubjectType()));
			if (RestrictableObjectType.ENTITY==subjectId.getType()) {
				subjectId.setId(KeyFactory.keyToString(nar.getSubjectId()));
			} else if (RestrictableObjectType.EVALUATION==subjectId.getType()) {
				subjectId.setId(nar.getSubjectId().toString());
			} else {
				throw new IllegalStateException("Unsupported type: "+subjectId.getType());
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
	public QueryResults<MigratableObjectData> getMigrationObjectData(long offset, long limit, boolean includeDependencies) throws DatastoreException {
		// (1) get one 'page' of AccessRequirements (just their IDs and Etags)
		List<MigratableObjectData> ods = null;
		{
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(OFFSET_PARAM_NAME, offset);
			param.addValue(LIMIT_PARAM_NAME, limit);
			ods = simpleJdbcTemplate.query(SELECT_FOR_RANGE_SQL, new RowMapper<MigratableObjectData>() {

				@Override
				public MigratableObjectData mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					String id = rs.getString(COL_ACCESS_REQUIREMENT_ID);
					String etag = rs.getString(COL_ACCESS_REQUIREMENT_ETAG);
					MigratableObjectData objectData = new MigratableObjectData();
					MigratableObjectDescriptor od = new MigratableObjectDescriptor();
					od.setId(id);
					od.setType(MigratableObjectType.ACCESSREQUIREMENT);
					objectData.setId(od);
					objectData.setEtag(etag);
					objectData.setDependencies(new HashSet<MigratableObjectDescriptor>(0));
					return objectData;
				}
			
			}, param);
		}
		
		// (2) find the dependencies
		if (includeDependencies && !ods.isEmpty()) {
			Map<String, MigratableObjectData> arMap = new HashMap<String, MigratableObjectData>();	
			for (MigratableObjectData od: ods) arMap.put(od.getId().getId(), od);
			
			List<DBOSubjectAccessRequirement> nars = null;
			{
				MapSqlParameterSource param = new MapSqlParameterSource();
				param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID, arMap.keySet());
				nars = simpleJdbcTemplate.query(SELECT_FOR_MULTIPLE_SAR_SQL, subjectAccessRequirementRowMapper, param);
			}

			// (3) add the dependencies to the objects
			for (DBOSubjectAccessRequirement nar : nars) {
				MigratableObjectDescriptor od = ObjectDescriptorUtils.createEntityObjectDescriptor(nar.getSubjectId());
				MigratableObjectData objectData = arMap.get(nar.getAccessRequirementId().toString());
				objectData.getDependencies().add(od);
			}
		}
		// (4) return the 'page' of objects, along with the total result count
		QueryResults<MigratableObjectData> queryResults = new QueryResults<MigratableObjectData>();
		queryResults.setResults(ods);
		queryResults.setTotalNumberOfResults((int)getCount());
		return queryResults;
	}

	@Transactional(readOnly = true)
	@Override
	public List<AccessRequirement> getForSubject(RestrictableObjectDescriptor subject)  throws DatastoreException {
		List<AccessRequirement>  dtos = new ArrayList<AccessRequirement>();
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, KeyFactory.stringToKey(subject.getId()));
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, subject.getType().toString());
		List<DBOAccessRequirement> dbos = simpleJdbcTemplate.query(SELECT_FOR_SUBJECT_SQL, accessRequirementRowMapper, param);
		for (DBOAccessRequirement dbo : dbos) {
			AccessRequirement dto = AccessRequirementUtils.copyDboToDto(dbo, getSubjects(dbo.getId()));
			dtos.add(dto);
		}
		return dtos;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessRequirement> T update(T dto) throws DatastoreException,
			InvalidModelException,NotFoundException, ConflictingUpdateException {
		return update(dto, false);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessRequirement> T updateFromBackup(T dto) throws DatastoreException,
			InvalidModelException,NotFoundException, ConflictingUpdateException {
		return update(dto, true);
	}

	/**
	 * @param fromBackup Whether we are updating from backup.
	 *                   Skip optimistic locking and accept the backup e-tag when restoring from backup.
	 */
	private <T extends AccessRequirement> T update(T dto, boolean fromBackup) throws DatastoreException,
			InvalidModelException,NotFoundException, ConflictingUpdateException {

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, dto.getId());
		List<DBOAccessRequirement> ars = null;
		try{
			ars = simpleJdbcTemplate.query(SELECT_FOR_UPDATE_SQL, new RowMapper<DBOAccessRequirement>() {
				@Override
				public DBOAccessRequirement mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					DBOAccessRequirement ar = new DBOAccessRequirement();
					ar.setCreatedBy(rs.getLong(COL_ACCESS_REQUIREMENT_CREATED_BY));
					ar.setCreatedOn(rs.getLong(COL_ACCESS_REQUIREMENT_CREATED_ON));	
					ar.seteTag(rs.getString(COL_ACCESS_REQUIREMENT_ETAG));
					return ar;
				}
				
			}, param);
		}catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
		if (ars.isEmpty()) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");			
		}

		DBOAccessRequirement dbo = ars.get(0);
		if (!fromBackup) {
			// check dbo's etag against dto's etag
			// if different rollback and throw a meaningful exception
			if (!dbo.geteTag().equals(dto.getEtag())) {
				throw new ConflictingUpdateException("Access Requirement was updated since you last fetched it, retrieve it again and reapply the update.");
			}
		}
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		if (!fromBackup) {
			// Update with a new e-tag; otherwise, the backup e-tag is used implicitly
			dbo.seteTag(eTagGenerator.generateETag());
		}

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
			simpleJdbcTemplate.update(DELETE_SUBJECT_ACCESS_REQUIREMENT_SQL, param);
		}
		// ... now populate with the updated values
		populateSubjectAccessRequirement(acessRequirementId, subjectIds);
	}
	
	public MigratableObjectType getMigratableObjectType() {
		return MigratableObjectType.ACCESSREQUIREMENT;
	}
	
}
