package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;


public class DBOGroupMembersDAOImpl implements GroupMembersDAO {

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private TransactionalMessenger transactionalMessenger;

	private static final String PRINCIPAL_ID_PARAM_NAME = "principalId";
	private static final String GROUP_ID_PARAM_NAME     = "groupId";
	private static final String MEMBER_ID_PARAM_NAME    = "memberId";
	
	private static final String SELECT_MEMBER_IDS = 
			"SELECT "+COL_GROUP_MEMBERS_MEMBER_ID+
			" FROM "+TABLE_GROUP_MEMBERS+
			" WHERE "+COL_GROUP_MEMBERS_GROUP_ID+" = ?";
	
	private static final String SELECT_DIRECT_MEMBERS_OF_GROUP = 
			"SELECT ug.* FROM "+SqlConstants.TABLE_USER_GROUP+" ug"+
			" INNER JOIN "+SqlConstants.TABLE_GROUP_MEMBERS+
			" WHERE "+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+"=:"+PRINCIPAL_ID_PARAM_NAME+
			" AND ug."+SqlConstants.COL_USER_GROUP_ID+"="+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
	
	private static final String SELECT_GROUPS_FOR_USER = 
			"SELECT "+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+
			" FROM "+SqlConstants.TABLE_GROUP_MEMBERS+
			" WHERE "+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID+"=:"+PRINCIPAL_ID_PARAM_NAME+
			" AND "+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+" in (:"+GROUP_ID_PARAM_NAME+")";
	
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

	private static final String IDS_PARAM = "ids";
	private static final String LIMIT_PARAM = "limit";
	private static final String OFFSET_PARAM = "offset";

	private static final String SQL_GET_ALL_INDIVIDUALS = "SELECT DISTINCT (CASE"
					+ " WHEN "+TABLE_USER_GROUP+"."+COL_USER_GROUP_IS_INDIVIDUAL
					+" THEN "+TABLE_USER_GROUP+"."+COL_USER_GROUP_ID
					+ " ELSE "+TABLE_GROUP_MEMBERS+"."+COL_GROUP_MEMBERS_MEMBER_ID
				+ " END) AS USER_ID"
			+ " FROM "+ TABLE_USER_GROUP +" LEFT OUTER JOIN "+TABLE_GROUP_MEMBERS
				+ " ON "+TABLE_USER_GROUP+"."+COL_USER_GROUP_ID +" = "+TABLE_GROUP_MEMBERS+"."+COL_GROUP_MEMBERS_GROUP_ID
			+ " WHERE "+TABLE_USER_GROUP+"."+COL_USER_GROUP_ID +" IN ( :"+IDS_PARAM+" )"
			+ " ORDER BY USER_ID "
			+ " LIMIT :"+LIMIT_PARAM+" OFFSET :"+OFFSET_PARAM;

	private static final String SQL_GET_COUNT_ALL_INDIVIDUALS = "SELECT COUNT(DISTINCT CASE"
			+ " WHEN "+TABLE_USER_GROUP+"."+COL_USER_GROUP_IS_INDIVIDUAL
			+" THEN "+TABLE_USER_GROUP+"."+COL_USER_GROUP_ID
			+ " ELSE "+TABLE_GROUP_MEMBERS+"."+COL_GROUP_MEMBERS_MEMBER_ID
		+ " END)"
	+ " FROM "+ TABLE_USER_GROUP +" LEFT OUTER JOIN "+TABLE_GROUP_MEMBERS
		+ " ON "+TABLE_USER_GROUP+"."+COL_USER_GROUP_ID +" = "+TABLE_GROUP_MEMBERS+"."+COL_GROUP_MEMBERS_GROUP_ID
	+ " WHERE "+TABLE_USER_GROUP+"."+COL_USER_GROUP_ID +" IN ( :"+IDS_PARAM+" )";

	private static final String SQL_COUNT_MEMBERS = "SELECT COUNT(DISTINCT "+COL_GROUP_MEMBERS_MEMBER_ID+")"
			+ " FROM "+TABLE_GROUP_MEMBERS
			+ " WHERE "+COL_GROUP_MEMBERS_GROUP_ID+" = :"+GROUP_ID_PARAM_NAME
			+ " AND "+COL_GROUP_MEMBERS_MEMBER_ID+ " IN ( :"+IDS_PARAM+" )";

	private static final RowMapper<DBOUserGroup> userGroupRowMapper =  (new DBOUserGroup()).getTableMapping();
	
	@Override
	public List<UserGroup> getMembers(String principalId) 
			throws DatastoreException, NotFoundException {
		List<UserGroup> members = new ArrayList<UserGroup>();
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PRINCIPAL_ID_PARAM_NAME, principalId);
		List<DBOUserGroup> dbos = namedJdbcTemplate.query(SELECT_DIRECT_MEMBERS_OF_GROUP, param, userGroupRowMapper);
		
		UserGroupUtils.copyDboToDto(dbos, members);
		return members;
	}
	
	@Override
	public List<String> filterUserGroups(String principalId, List<String> groupIds) {
		ValidateArgument.required(principalId, "userId");
		if (groupIds==null || groupIds.isEmpty()) return Collections.emptyList();
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PRINCIPAL_ID_PARAM_NAME, principalId);
		param.addValue(GROUP_ID_PARAM_NAME, groupIds);
		return namedJdbcTemplate.queryForList(SELECT_GROUPS_FOR_USER, param, String.class);
	}

	@WriteTransaction
	@Override
	public void addMembers(String groupId, List<String> memberIds) 
			throws DatastoreException, NotFoundException, IllegalArgumentException {
		if (memberIds.isEmpty()) {
			return;
		}

		// get the lock on group principal
		String etag = userGroupDAO.getEtagForUpdate(groupId);
		transactionalMessenger.sendMessageAfterCommit(groupId, ObjectType.PRINCIPAL, ChangeType.UPDATE);

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
		namedJdbcTemplate.batchUpdate(INSERT_NEW_MEMBERS_OF_GROUP, params);
		
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
		namedJdbcTemplate.batchUpdate(DELETE_MEMBERS_OF_GROUP, params);
		
		// Update the etag on the parent group
		userGroupDAO.touch(Long.parseLong(groupId));
	}

	@Override
	public List<UserGroup> getUsersGroups(String principalId)
			throws DatastoreException, NotFoundException {
		List<UserGroup> members = new ArrayList<UserGroup>();
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PRINCIPAL_ID_PARAM_NAME, principalId);
		List<DBOUserGroup> dbos = namedJdbcTemplate.query(SELECT_DIRECT_PARENTS_OF_GROUP, param, userGroupRowMapper);
		
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

	@Override
	public Set<String> getIndividuals(Set<String> principalIds, Long limit, Long offset) {
		ValidateArgument.required(principalIds, "principalIds");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		Set<String> results = new HashSet<String>();
		if (principalIds.isEmpty()) {
			return results;
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(IDS_PARAM, principalIds);
		params.addValue(LIMIT_PARAM, limit);
		params.addValue(OFFSET_PARAM, offset);
		results.addAll(namedJdbcTemplate.queryForList(SQL_GET_ALL_INDIVIDUALS, params, String.class));
		return results;
	}

	@Override
	public Long getIndividualCount(Set<String> principalIds) {
		ValidateArgument.required(principalIds, "principalIds");
		if (principalIds.isEmpty()) {
			return 0L;
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(IDS_PARAM, principalIds);
		return namedJdbcTemplate.queryForObject(SQL_GET_COUNT_ALL_INDIVIDUALS, params, Long.class);
	}

	@Override
	public boolean areMemberOf(String groupId, Set<String> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return false;
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(IDS_PARAM, userIds);
		params.addValue(GROUP_ID_PARAM_NAME, groupId);
		Integer count = namedJdbcTemplate.queryForObject(SQL_COUNT_MEMBERS, params, Integer.class);
		return count.equals(userIds.size());
	}
	
	@Override
	public Set<Long> getMemberIdsForUpdate(Long teamId) {
		return getMemberIds(teamId, true);
	}

	@Override
	public Set<Long> getMemberIds(Long teamId) {
		return getMemberIds(teamId, false);
	}
	
	private Set<Long> getMemberIds(Long teamId, boolean forUpdate) {
		ValidateArgument.required(teamId, "teamId");
		final HashSet<Long> results = new HashSet<Long>();
		StringBuilder sql = new StringBuilder(SELECT_MEMBER_IDS);
		if (forUpdate) {
			sql.append(" FOR UPDATE");
		}
		jdbcTemplate.query(sql.toString(), new RowCallbackHandler(){
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				results.add(rs.getLong(COL_GROUP_MEMBERS_MEMBER_ID));
			}}, teamId);
		return results;
	}
}
