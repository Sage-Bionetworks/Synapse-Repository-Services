package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOGroupParentsCache;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public class DBOGroupMembersDAOImpl implements GroupMembersDAO {

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	private UserGroupDAO userGroupDAO;

	private static final String PRINCIPAL_ID_PARAM_NAME = "principalId";
	private static final String GROUP_ID_PARAM_NAME     = "groupId";
	private static final String MEMBER_ID_PARAM_NAME    = "memberId";
	private static final String PARENT_BLOB_PARAM_NAME  = "parents";
	
	private static final String SELECT_DIRECT_MEMBERS_OF_GROUP = 
			"SELECT ug.* FROM "+SqlConstants.TABLE_USER_GROUP+" ug"+
			" INNER JOIN "+SqlConstants.TABLE_GROUP_MEMBERS+
			" WHERE "+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+"=:"+PRINCIPAL_ID_PARAM_NAME+
			" AND ug."+SqlConstants.COL_USER_GROUP_ID+"="+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
	
	private static final String INSERT_NEW_MEMBERS_OF_GROUP = 
			"INSERT IGNORE INTO "+SqlConstants.TABLE_GROUP_MEMBERS+
			" VALUES (:"+GROUP_ID_PARAM_NAME+",:"+MEMBER_ID_PARAM_NAME+")";
	
	private static final String DELETE_MEMBERS_OF_GROUP = 
			"DELETE FROM "+SqlConstants.TABLE_GROUP_MEMBERS+
			" WHERE "+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+"=:"+GROUP_ID_PARAM_NAME+
			" AND "+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID+"=:"+MEMBER_ID_PARAM_NAME;
	
	private static final String SELECT_PARENTS_FROM_CACHE = 
			"SELECT * FROM "+SqlConstants.TABLE_GROUP_PARENTS_CACHE+
			" WHERE "+SqlConstants.COL_GROUP_PARENTS_CACHE_GROUP_ID+"=:"+PRINCIPAL_ID_PARAM_NAME+
			" FOR UPDATE";
	
	private static final String UPDATE_BLOB_IN_PARENTS_CACHE = 
			"UPDATE "+SqlConstants.TABLE_GROUP_PARENTS_CACHE+
			" SET "+SqlConstants.COL_GROUP_PARENTS_CACHE_PARENTS+"=:"+PARENT_BLOB_PARAM_NAME+
			" WHERE "+SqlConstants.COL_GROUP_PARENTS_CACHE_GROUP_ID+"=:"+GROUP_ID_PARAM_NAME;
			
	private static final RowMapper<DBOUserGroup> userGroupRowMapper =  (new DBOUserGroup()).getTableMapping();
	private static final RowMapper<DBOGroupParentsCache> parentsCacheRowMapper =  (new DBOGroupParentsCache()).getTableMapping();

	private static final String SELECT_MEMBERS_OF_GROUP = 
			"SELECT "+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID+" FROM "+SqlConstants.TABLE_GROUP_MEMBERS+
			" WHERE "+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+" IN (:"+GROUP_ID_PARAM_NAME+")";
	
	private static final String SELECT_GROUPS_OF_MEMBER = 
			"SELECT "+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+" FROM "+SqlConstants.TABLE_GROUP_MEMBERS+
			" WHERE "+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID+" IN (:"+MEMBER_ID_PARAM_NAME+")";
	
	private static final RowMapper<String> pioneerRowMapper = 
			new RowMapper<String>() {
				@Override
				public String mapRow(ResultSet rs, int rowNum) throws SQLException {
					return rs.getString(1);
				}
			};
	
	private enum SearchDirection {
		PARENTS, 
		CHILDREN
	};
	
	/**
	 * Iteratively moves along the group hierarchy and fetches all unique IDs encountered
	 * @param startingPrincipalIds The group(s) to start the search from.  
	 *   The groups should all have a common parent or child.  
	 *   If there is a cycle in the graph, all starting IDs will appear in the result.
	 */
	private Set<String> breadthFirstSearch(SearchDirection direction, List<String> startingPrincipalIds) {
		Set<String> explored = new HashSet<String>();
		List<String> frontier = new ArrayList<String>();
		frontier.addAll(startingPrincipalIds);

		// Traverse the group hierarchy until the frontier has no more elements
		while (frontier.size() > 0) {
			MapSqlParameterSource param = new MapSqlParameterSource();
			switch (direction) {
				case CHILDREN:
					param.addValue(GROUP_ID_PARAM_NAME, frontier);
					frontier = simpleJdbcTemplate.query(SELECT_MEMBERS_OF_GROUP, pioneerRowMapper, param);
					break;
				case PARENTS:
					param.addValue(MEMBER_ID_PARAM_NAME, frontier);
					frontier = simpleJdbcTemplate.query(SELECT_GROUPS_OF_MEMBER, pioneerRowMapper, param);
					break;
				default:
					throw new RuntimeException("Unsupported search direction");
			}
			
			// Keep track of all the unique groups
			for (int i = 0; i < frontier.size(); i++) {
				if (!explored.add(frontier.get(i))) {
					// This group has already been included in the search frontier
					frontier.remove(i);
					i--;
				}
			}
		}
		return explored;
	}
	
	@Override
	public List<UserGroup> getMembers(String principalId) 
			throws DatastoreException, NotFoundException {
		List<UserGroup> members = new ArrayList<UserGroup>();
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PRINCIPAL_ID_PARAM_NAME, principalId);
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_DIRECT_MEMBERS_OF_GROUP, userGroupRowMapper, param);
		
		UserGroupUtils.copyDboToDto(dbos, members);
		return members;
	}
	
	@Override
	public List<UserGroup> getMembers(String principalId, boolean nested) 
			throws DatastoreException, NotFoundException {
		List<String> id = new ArrayList<String>();
		id.add(principalId);
		Set<String> memberIds = breadthFirstSearch(SearchDirection.CHILDREN, id);
		return userGroupDAO.get(new ArrayList<String>(memberIds));
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addMembers(String groupId, List<String> memberIds) 
			throws DatastoreException, NotFoundException, IllegalArgumentException {
		if (memberIds.isEmpty()) {
			return;
		}
		
		// The insertion into GroupMembers requires a read lock many rows of the UserGroup table
		// So fetch a write lock on all necessary rows
		List<String> locks = new ArrayList<String>(memberIds);
		locks.add(groupId);
		for (Long id : sortIds(locks)) {
			userGroupDAO.getForUpdate(id.toString());
		}
		
		// Make sure the UserGroup corresponding to the ID holds a group, not an individual
		if (userGroupDAO.get(groupId).getIsIndividual()) {
			throw new IllegalArgumentException("Members cannot be added to an individual");
		}
		
		// Mark all affected children of this operation as updated
		Set<String> updatedIds = markAsUpdated(memberIds);
		
		// Check to see if the insert results in a circular membership graph
		if (updatedIds.contains(groupId)) {
			// The search found the root node within the search, meaning the insert would result in a cycle
			throw new IllegalArgumentException("Group "+groupId+" is already a child of the member(s) specified");
		}

		// Insert all the new members
		List<Long> sortedMemberIds = sortIds(memberIds);
		MapSqlParameterSource params[] = new MapSqlParameterSource[memberIds.size()];
		for (int i = 0; i < params.length; i++) {
			params[i] = new MapSqlParameterSource();
			params[i].addValue(GROUP_ID_PARAM_NAME, groupId);
			params[i].addValue(MEMBER_ID_PARAM_NAME, sortedMemberIds.get(i));
		}
		simpleJdbcTemplate.batchUpdate(INSERT_NEW_MEMBERS_OF_GROUP, params);
		
		// Update the etag on the parent group
		userGroupDAO.touch(groupId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void removeMembers(String groupId, List<String> memberIds) 
			throws DatastoreException, NotFoundException {
		if (memberIds.isEmpty()) {
			return;
		}
		
		// Use the affected UserGroup row as a lock 
		userGroupDAO.getForUpdate(groupId);
		
		// Mark each of the children as modified
		markAsUpdated(memberIds);

		// Delete some members
		List<Long> sortedMemberIds = sortIds(memberIds);
		MapSqlParameterSource params[] = new MapSqlParameterSource[memberIds.size()];
		for (int i = 0; i < params.length; i++) {
			params[i] = new MapSqlParameterSource();
			params[i].addValue(GROUP_ID_PARAM_NAME, groupId);
			params[i].addValue(MEMBER_ID_PARAM_NAME, sortedMemberIds.get(i));
		}
		simpleJdbcTemplate.batchUpdate(DELETE_MEMBERS_OF_GROUP, params);
		
		// Update the etag on the parent group
		userGroupDAO.touch(groupId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<UserGroup> getUsersGroups(String principalId)
			throws DatastoreException, NotFoundException {
		// Use the affected UserGroup row as a lock 
		userGroupDAO.getForUpdate(principalId);
		
		// Check the cache for the parents, this also locks the row
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PRINCIPAL_ID_PARAM_NAME, principalId);
		DBOGroupParentsCache dbo = simpleJdbcTemplate.queryForObject(SELECT_PARENTS_FROM_CACHE, parentsCacheRowMapper, param);

		// Use the zipped up parents
		if (dbo.getParents() != null) {
			try {
			return userGroupDAO.get(GroupMembersUtils.unzip(dbo.getParents()));
			} catch (IOException e) {
				throw new DatastoreException(e);
			}
		}
		
		// No cached parents, so perform the search
		List<String> id = new ArrayList<String>();
		id.add(principalId);
		Set<String> parentGroupIds = breadthFirstSearch(SearchDirection.PARENTS, id);
		List<String> parents = new ArrayList<String>(parentGroupIds);
		
		// Cache the parents
		byte[] cache;
		try {
			cache = GroupMembersUtils.zip(parents);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		param = new MapSqlParameterSource();
		param.addValue(GROUP_ID_PARAM_NAME, principalId);
		param.addValue(PARENT_BLOB_PARAM_NAME, cache);
		simpleJdbcTemplate.update(UPDATE_BLOB_IN_PARENTS_CACHE, param);
		
		return userGroupDAO.get(parents);
	}
	
	/**
	 * Nullifies cached parents of all children and their children
	 * @return All IDs where the cache has been cleared
	 */
	private Set<String> markAsUpdated(List<String> memberIds)
			throws DatastoreException {
		// Clear the group-parent cache of all children
		Set<String> descendents = breadthFirstSearch(SearchDirection.CHILDREN, memberIds);
		descendents.addAll(memberIds);
		
		// Sort the rows to prevent deadlock
		List<Long> ascendents = sortIds(memberIds);
		
		// Delete all the affected caches
		MapSqlParameterSource params[] = new MapSqlParameterSource[ascendents.size()];
		for (int i = 0; i < params.length; i++) {
			params[i] = new MapSqlParameterSource();
			params[i].addValue(GROUP_ID_PARAM_NAME, ascendents.get(i));
			params[i].addValue(PARENT_BLOB_PARAM_NAME, null);
		}
		simpleJdbcTemplate.batchUpdate(UPDATE_BLOB_IN_PARENTS_CACHE, params);
		
		return descendents;
	}
	
	/**
	 * Parses a list of strings into Longs and returns the sorted list of longs
	 */
	private List<Long> sortIds(List<String> ids) {
		List<Long> ascendents = new ArrayList<Long>();
		for (String id : ids) {
			ascendents.add(Long.parseLong(id));
		}
		Collections.sort(ascendents);
		return ascendents;
	}
}
