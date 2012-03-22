package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REFERENCE_GROUP_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REFERENCE_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REFERENCE_TARGET_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REFERENCE_TARGET_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REFERENCE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityHeaderQueryResults;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOReference;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.jdo.QueryUtils;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the DBOReferenceDao.
 * 
 * @author John
 *
 */
@Transactional(readOnly = true)
public class DBOReferenceDaoImpl implements DBOReferenceDao {
	
	private static final String DELETE_SQL = "DELETE FROM "+TABLE_REFERENCE+" WHERE "+COL_REFERENCE_OWNER_NODE+" = ?";
	private static final String SELECT_SQL = "SELECT "+COL_REFERENCE_GROUP_NAME+", "+COL_REFERENCE_TARGET_NODE+", "+COL_REFERENCE_TARGET_REVISION_NUMBER+" FROM "+TABLE_REFERENCE+" WHERE "+COL_REFERENCE_OWNER_NODE+" = ?";
	private static final String REFERENCE_TARGET_NODE_BIND_VAR = "rtn";
	private static final String REFERENCE_TARGET_REVISION_NO_BIND_VAR = "rtrn";	
	private static final String REFERRER_SELECT_SQL_HEAD = 	
					"SELECT "+
					TABLE_NODE+"."+COL_NODE_ID+", "+
					TABLE_NODE+"."+COL_NODE_NAME+", "+
					TABLE_NODE+"."+COL_NODE_TYPE+", "+
					TABLE_NODE+"."+COL_NODE_BENEFACTOR_ID+", "+
					TABLE_REFERENCE+"."+COL_REFERENCE_OWNER_NODE;
	private static final String REFERRER_SELECT_SQL_TAIL = 
			" FROM "+TABLE_REFERENCE+", "+TABLE_NODE+
			" WHERE "+TABLE_NODE+"."+COL_NODE_ID+" = "+TABLE_REFERENCE+"."+COL_REFERENCE_OWNER_NODE+" AND "+
				TABLE_REFERENCE+"."+COL_REFERENCE_TARGET_NODE+" = :"+REFERENCE_TARGET_NODE_BIND_VAR+" ";
	private static final String REFERRER_SELECT_SQL_WITH_REVISION_TAIL = REFERRER_SELECT_SQL_TAIL+
			"AND "+TABLE_REFERENCE+"."+COL_REFERENCE_TARGET_REVISION_NUMBER+" = :"+REFERENCE_TARGET_REVISION_NO_BIND_VAR+" ";	
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Map<String, Set<Reference>> replaceReferences(Long ownerId, Map<String, Set<Reference>> references) throws DatastoreException {
		if(ownerId == null) throw new IllegalArgumentException("Owner id cannot be null");
		if(references == null) throw new IllegalArgumentException("References cannot be null");
		// First delete all references for this entity.
		simpleJdbcTemplate.update(DELETE_SQL, ownerId);
		// Create the list of references
		List<DBOReference> batch = ReferenceUtil.createDBOReferences(ownerId, references);
		if(batch.size() > 0 ){
			dboBasicDao.createBatch(batch);
		}
		return references;
	}
	@Transactional(readOnly = true)
	@Override
	public Map<String, Set<Reference>> getReferences(Long ownerId) {
		if(ownerId == null) throw new IllegalArgumentException("OwnerId cannot be null");
		// Build up the results from the DB.
		final Map<String, Set<Reference>> results = new HashMap<String, Set<Reference>>();
		simpleJdbcTemplate.query(SELECT_SQL, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				String groupName = rs.getString(COL_REFERENCE_GROUP_NAME);
				Set<Reference> set = results.get(groupName);
				if(set == null){
					set = new HashSet<Reference>();
					results.put(groupName, set);
				}
				// Create the reference
				Reference reference = new Reference();
				reference.setTargetId(KeyFactory.keyToString(rs.getLong(COL_REFERENCE_TARGET_NODE)));
				reference.setTargetVersionNumber(rs.getLong(COL_REFERENCE_TARGET_REVISION_NUMBER));
				if(rs.wasNull()){
					reference.setTargetVersionNumber(null);
				}
				// Add it to its group
				set.add(reference);
				return groupName;
			}
		}, ownerId);
		return results;
	}


	/**
	 * Get the EntityHeaders of the entities which refer to a given target
	 * if targetVersion is not null then return just the referrers of the given specific version of the target
	 * @param targetId the Node ID of the target
	 * @param targetVersion the version of the target
	 * @param offset ZERO based pagination param
	 * @param limit pagination param
	 * @return a List of EntityHeaders
	 * 
	 */
	@Transactional(readOnly = true)
	@Override
	public EntityHeaderQueryResults getReferrers(Long targetId, Integer targetVersion, UserInfo userInfo, Integer offset, Integer limit) throws DatastoreException {
		if(targetId == null) throw new IllegalArgumentException("targetId cannot be null");
		// Build up the results from the DB.
		final List<EntityHeader> results = new ArrayList<EntityHeader>();
		Map<String, Object> baseParameters = new HashMap<String, Object>();
		baseParameters.put(REFERENCE_TARGET_NODE_BIND_VAR, targetId);
		if (null!=targetVersion) baseParameters.put(REFERENCE_TARGET_REVISION_NO_BIND_VAR, targetVersion);
		String queryTail = null;
		if (null==targetVersion) {
			queryTail = REFERRER_SELECT_SQL_TAIL;
		} else {
			queryTail = REFERRER_SELECT_SQL_WITH_REVISION_TAIL;
		}
		String authorizationFilter = QueryUtils.buildAuthorizationFilter(userInfo, baseParameters);
		String fullQuery = null;
		if (authorizationFilter.length()>0) {
			queryTail = " FROM ("+REFERRER_SELECT_SQL_HEAD+queryTail+") " + SqlConstants.NODE_ALIAS + " "+authorizationFilter;
			fullQuery = "SELECT * "+queryTail;
		} else {
			fullQuery = REFERRER_SELECT_SQL_HEAD+queryTail;
		}
		
		Map<String, Object> fullParameters = new HashMap<String, Object>(baseParameters);
		if (offset!=null && limit!=null) {
			fullQuery += QueryUtils.buildPaging(offset, limit, fullParameters);
		}
		simpleJdbcTemplate.query(fullQuery, new RowMapper<EntityHeader>() {
			@Override
			public EntityHeader mapRow(ResultSet rs, int rowNum) throws SQLException {
				EntityHeader referrer = new EntityHeader();
				referrer.setId(KeyFactory.keyToString(rs.getLong(COL_NODE_ID)));
				referrer.setName(rs.getString(COL_NODE_NAME));
				referrer.setType(EntityType.getTypeForId((short)rs.getInt(COL_NODE_TYPE)).name());
				results.add(referrer);
				return referrer;
			}
		}, fullParameters);
		EntityHeaderQueryResults ehqr = new EntityHeaderQueryResults();
		ehqr.setEntityHeaders(results);
		String countQuery = "SELECT COUNT(*) "+queryTail;
		ehqr.setTotalNumberOfResults(simpleJdbcTemplate.queryForLong(countQuery, baseParameters));
		return ehqr;
	}

}
