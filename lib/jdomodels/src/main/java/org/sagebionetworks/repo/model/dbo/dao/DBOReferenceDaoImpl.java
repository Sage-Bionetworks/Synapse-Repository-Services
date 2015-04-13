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

import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOReference;
import org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.jdo.QueryUtils;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import org.sagebionetworks.repo.transactions.WriteTransaction;

/**
 * Implementation of the DBOReferenceDao.
 * 
 * @author John
 *
 */
public class DBOReferenceDaoImpl implements DBOReferenceDao {
	
	private static final String ID_PARAM = "idParam";
	private static final String SQL_DELETE_BATCH_BY_PRIMARY_KEY = "DELETE FROM "+TABLE_REFERENCE+" WHERE ID = :"+ID_PARAM;
	private static final String SQL_IDS_FOR_DELETE = "SELECT ID FROM "+TABLE_REFERENCE+" WHERE "+COL_REFERENCE_OWNER_NODE+" = ? ORDER BY ID ASC";
	private static final String SELECT_SQL = "SELECT "+COL_REFERENCE_GROUP_NAME+", "+COL_REFERENCE_TARGET_NODE+", "+COL_REFERENCE_TARGET_REVISION_NUMBER+" FROM "+TABLE_REFERENCE+" WHERE "+COL_REFERENCE_OWNER_NODE+" = ?";
	private static final String REFERENCE_TARGET_NODE_BIND_VAR = "rtn";
	private static final String REFERENCE_TARGET_REVISION_NO_BIND_VAR = "rtrn";	
	private static final String REFERRER_SELECT_SQL_COLUMNS =
					TABLE_NODE+"."+COL_NODE_ID+", "+
					TABLE_NODE+"."+COL_NODE_NAME+", "+
					TABLE_NODE+"."+COL_NODE_TYPE+", "+
					TABLE_NODE+"."+COL_NODE_BENEFACTOR_ID+", "+
					TABLE_REFERENCE+"."+COL_REFERENCE_OWNER_NODE;
	private static final String REFERRER_SELECT_SQL_FROM_TABLE = TABLE_REFERENCE + ", " + TABLE_NODE;
	private static final String REFERRER_SELECT_SQL_DEFAULT_WHERE = TABLE_NODE + "." + COL_NODE_ID + " = " + TABLE_REFERENCE + "."
			+ COL_REFERENCE_OWNER_NODE + " AND " + TABLE_REFERENCE + "." + COL_REFERENCE_TARGET_NODE + " = :"
			+ REFERENCE_TARGET_NODE_BIND_VAR;
	private static final String REFERRER_SELECT_SQL_WHERE_WITH_REVISION = " AND " + TABLE_REFERENCE + "."
			+ COL_REFERENCE_TARGET_REVISION_NUMBER + " = :" + REFERENCE_TARGET_REVISION_NO_BIND_VAR;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@WriteTransaction
	@Override
	public Map<String, Set<Reference>> replaceReferences(Long ownerId, Map<String, Set<Reference>> references) throws DatastoreException {
		if(ownerId == null) throw new IllegalArgumentException("Owner id cannot be null");
		if(references == null) throw new IllegalArgumentException("References cannot be null");
		// First delete all references for this entity.
		deleteWithoutGapLockFromTable(ownerId);
		// Create the list of references
		List<DBOReference> batch = ReferenceUtil.createDBOReferences(ownerId, references);
		if(batch.size() > 0 ){
			dboBasicDao.createBatch(batch);
		}
		return references;
	}

	@WriteTransaction
	@Override
	public void deleteReferencesByOwnderId(Long ownerId) {
		if (ownerId == null) throw new IllegalArgumentException("Owner id cannot be null");
		deleteWithoutGapLockFromTable(ownerId);
	}

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

	@Override
	public QueryResults<EntityHeader> getReferrers(Long targetId, Integer targetVersion, UserInfo userInfo, Integer offset, Integer limit) throws DatastoreException {
		if(targetId == null) throw new IllegalArgumentException("targetId cannot be null");

		StringBuilder whereClause = new StringBuilder(1000);
		// Build up the results from the DB.
		final List<EntityHeader> results = new ArrayList<EntityHeader>();
		Map<String, Object> baseParameters = new HashMap<String, Object>();
		baseParameters.put(REFERENCE_TARGET_NODE_BIND_VAR, targetId);

		whereClause.append(REFERRER_SELECT_SQL_DEFAULT_WHERE);

		// if target version is supplied, add it
		if (targetVersion != null) {
			baseParameters.put(REFERENCE_TARGET_REVISION_NO_BIND_VAR, targetVersion);
			whereClause.append(REFERRER_SELECT_SQL_WHERE_WITH_REVISION);
		}

		baseParameters.put(AuthorizationSqlUtil.RESOURCE_TYPE_BIND_VAR, ObjectType.ENTITY.name());
		String authorizationInClause = QueryUtils.buildAuthorizationFilter(userInfo.isAdmin(), userInfo.getGroups(), baseParameters,
				SqlConstants.TABLE_NODE, 0);
		if (!StringUtils.isBlank(authorizationInClause)) {
			whereClause.append(" AND ");
			whereClause.append(authorizationInClause);
		}

		String paging = "";
		Map<String, Object> fullParameters = new HashMap<String, Object>(baseParameters);
		if (offset != null && limit != null) {
			paging = " " + QueryUtils.buildPaging(offset, limit, fullParameters);
		}

		String fullQuery = "SELECT " + REFERRER_SELECT_SQL_COLUMNS + " FROM " + REFERRER_SELECT_SQL_FROM_TABLE + " WHERE "
				+ whereClause.toString() + paging;
		String countQuery = "SELECT COUNT(*) FROM " + REFERRER_SELECT_SQL_FROM_TABLE + " WHERE " + whereClause.toString();
		
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
		QueryResults<EntityHeader> ehqr = new QueryResults<EntityHeader>();
		ehqr.setResults(results);
		ehqr.setTotalNumberOfResults(simpleJdbcTemplate.queryForLong(countQuery, baseParameters));
		return ehqr;
	}
	
	/**
	 * In order to avoid MySQL gap locks which cause deadlock, we need to delete by a unique key.
	 * This means we need to first for row IDs that match the owner.  We then use the ids to
	 * delete the rows.  
	 * @param tableName
	 * @param ownerId
	 */
	private void deleteWithoutGapLockFromTable(Long ownerId){
		// First get all IDs for rows that belong to the passed owner.
		List<Long> idsToDelete = simpleJdbcTemplate.query(SQL_IDS_FOR_DELETE, new RowMapper<Long>(){
			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getLong("ID");
			}}, ownerId);
		// Prepare to batch delete the rows by their primary key.
		MapSqlParameterSource[] params = new MapSqlParameterSource[idsToDelete.size()];
		for(int i=0; i<idsToDelete.size(); i++){
			params[i] = new MapSqlParameterSource(ID_PARAM, idsToDelete.get(i));
		}
		simpleJdbcTemplate.batchUpdate(SQL_DELETE_BATCH_BY_PRIMARY_KEY, params);
	}

}
