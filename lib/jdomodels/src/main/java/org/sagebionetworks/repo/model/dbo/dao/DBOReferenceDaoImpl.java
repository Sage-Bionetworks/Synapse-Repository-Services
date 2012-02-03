package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOReference;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.jdo.JDONodeQueryDaoImpl;
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
	private static final String REFERRER_SELECT_SQL = 
			"SELECT "+
					TABLE_NODE+"."+COL_NODE_ID+", "+
					TABLE_NODE+"."+COL_NODE_NAME+", "+
					TABLE_NODE+"."+COL_NODE_TYPE+", "+
					TABLE_NODE+"."+COL_NODE_BENEFACTOR_ID+", "+
					TABLE_REFERENCE+"."+COL_REFERENCE_OWNER_NODE+
			" FROM "+TABLE_REFERENCE+", "+TABLE_NODE+
			" WHERE "+TABLE_NODE+"."+COL_NODE_ID+" = "+TABLE_REFERENCE+"."+COL_REFERENCE_OWNER_NODE+" AND "+
				TABLE_REFERENCE+"."+COL_REFERENCE_TARGET_NODE+" = :"+REFERENCE_TARGET_NODE_BIND_VAR+" ";
	private static final String REFERRER_SELECT_SQL_WITH_REVISION = REFERRER_SELECT_SQL+
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
	public Collection<EntityHeader> getReferrers(Long targetId, UserInfo userInfo) {
		if(targetId == null) throw new IllegalArgumentException("targetId cannot be null");
		// Build up the results from the DB.
		final Set<EntityHeader> results = new HashSet<EntityHeader>();
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(REFERENCE_TARGET_NODE_BIND_VAR, targetId);
		// TODO migrate 'buildAuthorizationFilter' into a 'util' or 'helper' class
		String queryString = REFERRER_SELECT_SQL;
		String authorizationFilter = QueryUtils.buildAuthorizationFilter(userInfo, parameters);
		if (authorizationFilter.length()>0) {
			queryString = "SELECT * FROM ("+queryString+") " + SqlConstants.NODE_ALIAS + " "+authorizationFilter;
		}
		System.out.println("DBOReferenceDaoImpl.getReferrers: "+queryString);
		simpleJdbcTemplate.query(queryString, new RowMapper<EntityHeader>() {
			@Override
			public EntityHeader mapRow(ResultSet rs, int rowNum) throws SQLException {
				EntityHeader referrer = new EntityHeader();
				referrer.setId(""+rs.getLong(COL_NODE_ID));
				referrer.setName(COL_NODE_NAME);
				referrer.setType(rs.getString(COL_NODE_TYPE));
				results.add(referrer);
				return referrer;
			}
		}, parameters);
		return results;
	}

	@Transactional(readOnly = true)
	@Override
	public Collection<EntityHeader> getReferrers(Long targetId, int targetVersion, UserInfo userInfo) {
		if(targetId == null) throw new IllegalArgumentException("targetId cannot be null");
		// Build up the results from the DB.
		final Set<EntityHeader> results = new HashSet<EntityHeader>();
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(REFERENCE_TARGET_NODE_BIND_VAR, targetId);
		parameters.put(REFERENCE_TARGET_REVISION_NO_BIND_VAR, targetVersion);
		String queryString = REFERRER_SELECT_SQL_WITH_REVISION;
		String authorizationFilter = QueryUtils.buildAuthorizationFilter(userInfo, parameters);
		if (authorizationFilter.length()>0) {
			queryString = "SELECT * FROM ("+queryString+") " + SqlConstants.NODE_ALIAS + " "+authorizationFilter;
		}
		System.out.println("DBOReferenceDaoImpl.getReferrers: "+queryString);
		simpleJdbcTemplate.query(queryString, new RowMapper<EntityHeader>() {
			@Override
			public EntityHeader mapRow(ResultSet rs, int rowNum) throws SQLException {
				EntityHeader referrer = new EntityHeader();
				referrer.setId(""+rs.getLong(COL_NODE_ID));
				referrer.setName(COL_NODE_NAME);
				referrer.setType(rs.getString(COL_NODE_TYPE));
				results.add(referrer);
				return referrer;
			}
		}, parameters);
		return results;
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
				try {
					reference.setTargetId(KeyFactory.keyToString(rs.getLong(COL_REFERENCE_TARGET_NODE)));
					reference.setTargetVersionNumber(rs.getLong(COL_REFERENCE_TARGET_REVISION_NUMBER));
					if(rs.wasNull()){
						reference.setTargetVersionNumber(null);
					}
				} catch (DatastoreException e) {
					throw new SQLException(e);
				}
				// Add it to its group
				set.add(reference);
				return groupName;
			}
		}, ownerId);
		return results;
	}

}
