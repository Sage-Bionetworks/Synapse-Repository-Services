package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import org.sagebionetworks.repo.transactions.WriteTransaction;


public class DBOGroupMembersDAOImpl implements GroupMembersDAO {

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	private UserGroupDAO userGroupDAO;

	private static final String PRINCIPAL_ID_PARAM_NAME = "principalId";
	private static final String GROUP_ID_PARAM_NAME     = "groupId";
	private static final String MEMBER_ID_PARAM_NAME    = "memberId";
	
	private static final String SELECT_DIRECT_MEMBERS_OF_GROUP = 
			"SELECT ug.* FROM "+SqlConstants.TABLE_USER_GROUP+" ug"+
			" INNER JOIN "+SqlConstants.TABLE_GROUP_MEMBERS+
			" WHERE "+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+"=:"+PRINCIPAL_ID_PARAM_NAME+
			" AND ug."+SqlConstants.COL_USER_GROUP_ID+"="+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
	
	private static final String SELECT_DIRECT_PARENTS_OF_GROUP = 
			"SELECT ug.* FROM "+SqlConstants.TABLE_USER_GROUP+" ug"+
			" INNER JOIN "+SqlConstants.TABLE_GROUP_MEMBERS+
			" WHERE "+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID+"=:"+PRINCIPAL_ID_PARAM_NAME+
			" AND ug."+SqlConstants.COL_USER_GROUP_ID+"="+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
	
	private static final String INSERT_NEW_MEMBERS_OF_GROUP = 
			"INSERT IGNORE INTO "+SqlConstants.TABLE_GROUP_MEMBERS+
			" VALUES (:"+GROUP_ID_PARAM_NAME+",:"+MEMBER_ID_PARAM_NAME+")";
	
	private static final String DELETE_MEMBERS_OF_GROUP = 
			"DELETE FROM "+SqlConstants.TABLE_GROUP_MEMBERS+
			" WHERE "+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+"=:"+GROUP_ID_PARAM_NAME+
			" AND "+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID+"=:"+MEMBER_ID_PARAM_NAME;
			
	private static final RowMapper<DBOUserGroup> userGroupRowMapper =  (new DBOUserGroup()).getTableMapping();
	
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

	@WriteTransaction
	@Override
	public void addMembers(String groupId, List<String> memberIds) 
			throws DatastoreException, NotFoundException, IllegalArgumentException {
		if (memberIds.isEmpty()) {
			return;
		}
		
		// The insertion into GroupMembers requires a read lock on many rows of the UserGroup table
		// So fetch a write lock on all necessary rows
		List<String> locks = new ArrayList<String>(memberIds);
		locks.add(groupId);
		for (Long id : sortIds(locks)) {
			userGroupDAO.getEtagForUpdate(id.toString());
		}
		
		// Make sure the UserGroup corresponding to the ID holds a group, not an individual
		if (userGroupDAO.get(Long.parseLong(groupId)).getIsIndividual()) {
			throw new IllegalArgumentException("Members cannot be added to an individual");
		}
		
		// Make sure all UserGroups corresponding to the member IDs are individuals
		Collection<UserGroup> members = userGroupDAO.get(memberIds);
		for (UserGroup member : members) {
			if (!member.getIsIndividual()) {
				throw new IllegalArgumentException("Only individuals may be added to groups");
			}
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
		userGroupDAO.touch(Long.parseLong(groupId));
	}

	@WriteTransaction
	@Override
	public void removeMembers(String groupId, List<String> memberIds) 
			throws DatastoreException, NotFoundException {
		if (memberIds.isEmpty()) {
			return;
		}
		
		// Use the affected UserGroup row as a lock 
		userGroupDAO.getEtagForUpdate(groupId);

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
		userGroupDAO.touch(Long.parseLong(groupId));
	}

	@Override
	public List<UserGroup> getUsersGroups(String principalId)
			throws DatastoreException, NotFoundException {
		List<UserGroup> members = new ArrayList<UserGroup>();
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PRINCIPAL_ID_PARAM_NAME, principalId);
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_DIRECT_PARENTS_OF_GROUP, userGroupRowMapper, param);
		
		UserGroupUtils.copyDboToDto(dbos, members);
		return members;
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

	/**
	 * This is called by Spring after all properties are set
	 */
	@WriteTransaction
	@Override
	public void bootstrapGroups() throws Exception {
		// in the case that the groups are initialized as Teams this is done in TeamManagerImpl.bootstrapTeams()
	}
}
