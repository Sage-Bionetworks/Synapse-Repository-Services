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
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ACCESS_REQUIREMENT_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_EVALUATION_ACCESS_REQUIREMENT_EVALUATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_EVALUATION_ACCESS_REQUIREMENT_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_APPROVAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE_ACCESS_REQUIREMENT;
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
import org.sagebionetworks.repo.model.dbo.persistence.DBOEvaluationAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBONodeAccessRequirement;
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

	private static final String SELECT_FOR_NODE_SQL = 
		"SELECT * FROM "+SqlConstants.TABLE_ACCESS_REQUIREMENT+" ar, "+ 
		SqlConstants.TABLE_NODE_ACCESS_REQUIREMENT +" nar WHERE ar."+
		SqlConstants.COL_ACCESS_REQUIREMENT_ID+" = nar."+SqlConstants.COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID+
		" AND nar."+COL_NODE_ACCESS_REQUIREMENT_NODE_ID+"=:"+COL_NODE_ACCESS_REQUIREMENT_NODE_ID+
		" order by "+SqlConstants.COL_ACCESS_REQUIREMENT_ID;

	private static final String SELECT_FOR_EVALUATION_SQL = 
		"SELECT * FROM "+SqlConstants.TABLE_ACCESS_REQUIREMENT+" ar, "+ 
		SqlConstants.TABLE_EVALUATION_ACCESS_REQUIREMENT +" ear WHERE ar."+
		SqlConstants.COL_ACCESS_REQUIREMENT_ID+" = ear."+SqlConstants.COL_EVALUATION_ACCESS_REQUIREMENT_REQUIREMENT_ID+
		" AND ear."+COL_EVALUATION_ACCESS_REQUIREMENT_EVALUATION_ID+"=:"+COL_EVALUATION_ACCESS_REQUIREMENT_EVALUATION_ID+
		" order by "+SqlConstants.COL_ACCESS_REQUIREMENT_ID;

	private static final String SELECT_FOR_NAR_SQL = "select * from "+TABLE_NODE_ACCESS_REQUIREMENT+" where "+
	COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=:"+COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID;

	private static final String SELECT_FOR_EAR_SQL = "select * from "+TABLE_EVALUATION_ACCESS_REQUIREMENT+" where "+
	COL_EVALUATION_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=:"+COL_EVALUATION_ACCESS_REQUIREMENT_REQUIREMENT_ID;

	private static final String SELECT_FOR_RANGE_SQL = "select * from "+TABLE_ACCESS_REQUIREMENT+" order by "+COL_ACCESS_REQUIREMENT_ID+
	" limit :"+LIMIT_PARAM_NAME+" offset :"+OFFSET_PARAM_NAME;

	private static final String SELECT_FOR_MULTIPLE_NAR_SQL = "select * from "+TABLE_NODE_ACCESS_REQUIREMENT+" where "+
		COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID+" IN (:"+COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID+")";

	private static final String SELECT_FOR_UPDATE_SQL = "select "+
			COL_ACCESS_REQUIREMENT_CREATED_BY+", "+
			COL_ACCESS_REQUIREMENT_CREATED_ON+", "+
			COL_ACCESS_REQUIREMENT_ETAG+
			" from "+TABLE_ACCESS_REQUIREMENT+" where "+COL_ACCESS_REQUIREMENT_ID+
			"=:"+COL_ACCESS_REQUIREMENT_ID+" for update";
	
	private static final String DELETE_NODE_ACCESS_REQUIREMENT_SQL = 
		"delete from "+TABLE_NODE_ACCESS_REQUIREMENT+" where "+
		COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=:"+COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID;
	
	private static final String DELETE_EVALUATION_ACCESS_REQUIREMENT_SQL = 
		"delete from "+TABLE_EVALUATION_ACCESS_REQUIREMENT+" where "+
		COL_EVALUATION_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=:"+COL_EVALUATION_ACCESS_REQUIREMENT_REQUIREMENT_ID;
	
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
	// join NODE_ACCESS_REQUIREMENT nar on nar.requirement_id=ar.id and nar.node_id=1072
	// left join ACCESS_APPROVAL aa on ar.id=aa.requirement_id and aa.accessor_id=682
	// where ar.access_type='DOWNLOAD'
	private static final String SELECT_UNMET_ENTITY_REQUIREMENTS_SQL = UNMET_REQUIREMENTS_SQL_PREFIX +
		" join "+TABLE_NODE_ACCESS_REQUIREMENT+" nar on nar."+COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=ar."+COL_ACCESS_REQUIREMENT_ID+
				" and nar."+COL_NODE_ACCESS_REQUIREMENT_NODE_ID+"=:"+COL_NODE_ACCESS_REQUIREMENT_NODE_ID+
				UNMET_REQUIREMENTS_SQL_SUFFIX;

	// select ar.id as ar_id, aa.id as aa_id
	// from ACCESS_REQUIREMENT ar 
	// join EVALUATION_ACCESS_REQUIREMENT ear on ear.requirement_id=ar.id and ear.node_id=1072
	// left join ACCESS_APPROVAL aa on ar.id=aa.requirement_id and aa.accessor_id=682
	// where ar.access_type='DOWNLOAD'
	private static final String SELECT_UNMET_EVALUATION_REQUIREMENTS_SQL = UNMET_REQUIREMENTS_SQL_PREFIX+
		" join "+TABLE_EVALUATION_ACCESS_REQUIREMENT+" ear on ear."+COL_EVALUATION_ACCESS_REQUIREMENT_REQUIREMENT_ID+"=ar."+COL_ACCESS_REQUIREMENT_ID+
				" and ear."+COL_EVALUATION_ACCESS_REQUIREMENT_EVALUATION_ID+"=:"+COL_EVALUATION_ACCESS_REQUIREMENT_EVALUATION_ID+
				UNMET_REQUIREMENTS_SQL_SUFFIX;

	private static final RowMapper<DBOAccessRequirement> accessRequirementRowMapper = (new DBOAccessRequirement()).getTableMapping();
	private static final RowMapper<DBONodeAccessRequirement> nodeAccessRequirementRowMapper = (new DBONodeAccessRequirement()).getTableMapping();
	private static final RowMapper<DBOEvaluationAccessRequirement> evaluationAccessRequirementRowMapper = (new DBOEvaluationAccessRequirement()).getTableMapping();

	@Override
	public List<Long> unmetAccessRequirements(RestrictableObjectDescriptor subject, Collection<Long> principalIds, ACCESS_TYPE accessType) throws DatastoreException {

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_APPROVAL_ACCESSOR_ID, principalIds);
		param.addValue(COL_ACCESS_REQUIREMENT_ACCESS_TYPE, accessType.name());
		String unmetRequirementsSQL = null;
		RestrictableObjectType type = subject.getType();
		if (RestrictableObjectType.ENTITY.equals(type)) {
			unmetRequirementsSQL = SELECT_UNMET_ENTITY_REQUIREMENTS_SQL;
			param.addValue(COL_NODE_ACCESS_REQUIREMENT_NODE_ID, KeyFactory.stringToKey(subject.getId()));
		} else if (RestrictableObjectType.EVALUATION.equals(type)) {
			unmetRequirementsSQL = SELECT_UNMET_EVALUATION_REQUIREMENTS_SQL;
			param.addValue(COL_EVALUATION_ACCESS_REQUIREMENT_EVALUATION_ID, subject.getId());
		} else {
			throw new IllegalArgumentException(type.toString());
		}
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
		populateNodeAndEntityAccessRequirement(dbo.getId(), dto.getSubjectIds());
		T result = (T)AccessRequirementUtils.copyDboToDto(dbo, getEntities(dbo.getId()));
		return result;
	}

	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void populateNodeAndEntityAccessRequirement(Long accessRequirementId, List<RestrictableObjectDescriptor> subjectIds) throws DatastoreException {
		if (subjectIds==null || subjectIds.isEmpty()) return;
		Map<RestrictableObjectType, Collection<String>> sorted = RestricableODUtil.sortByType(subjectIds);
		// take care of the Node/Entity IDs
		{
			List<DBONodeAccessRequirement> batch = new ArrayList<DBONodeAccessRequirement>();
			Collection<String> entityIds = sorted.get(RestrictableObjectType.ENTITY);
			if (entityIds!=null) {
				for (String id: entityIds) {
					DBONodeAccessRequirement nar = new DBONodeAccessRequirement();
					nar.setAccessRequirementId(accessRequirementId);
					Long nodeId = KeyFactory.stringToKey(id);
					nar.setNodeId(nodeId);
					batch.add(nar);
				}
			}
			if (batch.size()>0) basicDao.createBatch(batch);
		}
		// take care of the Evaluation IDs
		{
			List<DBOEvaluationAccessRequirement> batch = new ArrayList<DBOEvaluationAccessRequirement>();
			Collection<String> evaluationIds = sorted.get(RestrictableObjectType.EVALUATION);
			if (evaluationIds!=null) {
				for (String id: evaluationIds) {
					DBOEvaluationAccessRequirement ear = new DBOEvaluationAccessRequirement();
					ear.setAccessRequirementId(accessRequirementId);
					Long evaluationId = Long.parseLong(id);
					ear.setEvaluationId(evaluationId);
					batch.add(ear);
				}
			}
			if (batch.size()>0) basicDao.createBatch(batch);
		}
	}

	@Override
	public AccessRequirement get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID.toLowerCase(), id);
		DBOAccessRequirement dbo = basicDao.getObjectByPrimaryKey(DBOAccessRequirement.class, param);
		List<RestrictableObjectDescriptor> entities = getEntities(dbo.getId());
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
	
	public List<RestrictableObjectDescriptor> getEntities(Long accessRequirementId) {
		List<RestrictableObjectDescriptor> ans = new ArrayList<RestrictableObjectDescriptor>();	
		{
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID, accessRequirementId);
			List<DBONodeAccessRequirement> nars = simpleJdbcTemplate.query(SELECT_FOR_NAR_SQL, nodeAccessRequirementRowMapper, param);
			for (DBONodeAccessRequirement nar: nars) {
				RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
				rod.setType(RestrictableObjectType.ENTITY);
				rod.setId(KeyFactory.keyToString(nar.getNodeId()));
				ans.add(rod);
			}
		}
		{
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(COL_EVALUATION_ACCESS_REQUIREMENT_REQUIREMENT_ID, accessRequirementId);
			List<DBOEvaluationAccessRequirement> nars = simpleJdbcTemplate.query(SELECT_FOR_EAR_SQL, evaluationAccessRequirementRowMapper, param);
			for (DBOEvaluationAccessRequirement nar: nars) {
				RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
				rod.setType(RestrictableObjectType.EVALUATION);
				rod.setId(Long.toString(nar.getEvaluationId()));
				ans.add(rod);
			}
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
			
			List<DBONodeAccessRequirement> nars = null;
			{
				MapSqlParameterSource param = new MapSqlParameterSource();
				param.addValue(COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID, arMap.keySet());
				nars = simpleJdbcTemplate.query(SELECT_FOR_MULTIPLE_NAR_SQL, nodeAccessRequirementRowMapper, param);
			}

			// (3) add the dependencies to the objects
			for (DBONodeAccessRequirement nar : nars) {
				MigratableObjectDescriptor od = ObjectDescriptorUtils.createEntityObjectDescriptor(nar.getNodeId());
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
		if (RestrictableObjectType.ENTITY.equals(subject.getType())) {
			param.addValue(COL_NODE_ACCESS_REQUIREMENT_NODE_ID, KeyFactory.stringToKey(subject.getId()));
			List<DBOAccessRequirement> dbos = simpleJdbcTemplate.query(SELECT_FOR_NODE_SQL, accessRequirementRowMapper, param);
			for (DBOAccessRequirement dbo : dbos) {
				AccessRequirement dto = AccessRequirementUtils.copyDboToDto(dbo, getEntities(dbo.getId()));
				dtos.add(dto);
			}
		} else if (RestrictableObjectType.EVALUATION.equals(subject.getType())) {
			param.addValue(COL_EVALUATION_ACCESS_REQUIREMENT_EVALUATION_ID, subject.getId());
			List<DBOAccessRequirement> dbos = simpleJdbcTemplate.query(SELECT_FOR_EVALUATION_SQL, accessRequirementRowMapper, param);
			for (DBOAccessRequirement dbo : dbos) {
				AccessRequirement dto = AccessRequirementUtils.copyDboToDto(dbo, getEntities(dbo.getId()));
				dtos.add(dto);
			}
		} else {
			throw new IllegalArgumentException("Unsupported type: "+subject.getType());
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
		updateNodeAndEntityAccessRequirement(dbo.getId(), dto.getSubjectIds());
		T updatedAR = (T)AccessRequirementUtils.copyDboToDto(dbo, getEntities(dbo.getId()));

		return updatedAR;
	} // the 'commit' is implicit in returning from a method annotated 'Transactional'
	
	// to update the node ids for the Access Requirement, we just delete the existing ones and repopulate
	private void updateNodeAndEntityAccessRequirement(Long acessRequirementId, List<RestrictableObjectDescriptor> subjectIds) throws DatastoreException {
		{
			// delete the existing node-access-requirement records...
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID, acessRequirementId);
			simpleJdbcTemplate.update(DELETE_NODE_ACCESS_REQUIREMENT_SQL, param);
		}
		{
			// ... delete the existing evaluation-access-requirement records...
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(COL_EVALUATION_ACCESS_REQUIREMENT_REQUIREMENT_ID, acessRequirementId);
			simpleJdbcTemplate.update(DELETE_EVALUATION_ACCESS_REQUIREMENT_SQL, param);
		}
		// ... now populate with the updated values
		populateNodeAndEntityAccessRequirement(acessRequirementId, subjectIds);
	}
	
	public MigratableObjectType getMigratableObjectType() {
		return MigratableObjectType.ACCESSREQUIREMENT;
	}
	
}
