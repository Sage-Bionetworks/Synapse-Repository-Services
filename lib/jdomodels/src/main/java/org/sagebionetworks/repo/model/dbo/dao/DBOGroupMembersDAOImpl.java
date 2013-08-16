package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembers;
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
	
	private static final String SELECT_MEMBERS_OF_GROUP = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_ID+" IN"+
					" (SELECT "+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID+" FROM "+SqlConstants.TABLE_GROUP_MEMBERS+
					" WHERE "+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+"=:"+PRINCIPAL_ID_PARAM_NAME+")";
	
	private static final String INSERT_NEW_MEMBERS_OF_GROUP = 
			"INSERT IGNORE INTO "+SqlConstants.TABLE_GROUP_MEMBERS+
			"("+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+","+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID+")"+
			"VALUES (:"+GROUP_ID_PARAM_NAME+",:"+MEMBER_ID_PARAM_NAME+")";
	
	private static final String DELETE_MEMBERS_OF_GROUP = 
			"DELETE FROM "+SqlConstants.TABLE_GROUP_MEMBERS+
			" WHERE "+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+"=:"+GROUP_ID_PARAM_NAME+
			" AND "+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID+"=:"+MEMBER_ID_PARAM_NAME;
	
	private static final String SELECT_GROUPS_OF_MEMBER = 
			"SELECT DISTINCT "+SqlConstants.COL_GROUP_MEMBERS_GROUP_ID+" FROM "+SqlConstants.TABLE_GROUP_MEMBERS+
			" WHERE "+SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID+" IN (:"+MEMBER_ID_PARAM_NAME+")";
	
	private static final String SELECT_USER_GROUP_BY_PRINCIPAL_IDS = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_ID+" IN (:"+GROUP_ID_PARAM_NAME+")";
			
	private static final RowMapper<DBOUserGroup> userGroupRowMapper =  (new DBOUserGroup()).getTableMapping();
	
	private static final RowMapper<String> groupIdRowMapper = 
			new RowMapper<String>() {
				@Override
				public String mapRow(ResultSet rs, int rowNum) throws SQLException {
					return rs.getString(SqlConstants.COL_GROUP_MEMBERS_GROUP_ID);
				}
			};
	
	@Override
	public GroupMembers getMembers(String principalId) throws DatastoreException {
		GroupMembers members = new GroupMembers();
		members.setId(principalId);
		members.setMembers(new ArrayList<UserGroup>());
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PRINCIPAL_ID_PARAM_NAME, principalId);
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_MEMBERS_OF_GROUP, userGroupRowMapper, param);
		
		for (DBOUserGroup dbo : dbos) {
			UserGroup dto = new UserGroup();
			UserGroupUtils.copyDboToDto(dbo, dto);
			members.getMembers().add(dto);
		}

		return members;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addMembers(GroupMembers dto) throws DatastoreException {
		Set<String> parents = getParentGroupIds(dto.getId());
		List<String> updatedIds = new ArrayList<String>();
		updatedIds.add(dto.getId());
		updatedIds.addAll(parents);
		
		// Make sure the DTO holds a group, not an individual
		try {
			if (userGroupDAO.get(dto.getId()).getIsIndividual()) {
				throw new DatastoreException("Members cannot be added to an individual");
			}
		} catch (NotFoundException e) {
			throw new DatastoreException("The group "+dto.getId()+" does not exist");
		}
		
		MapSqlParameterSource params[] = new MapSqlParameterSource[dto.getMembers().size()];
		for (int i = 0; i < params.length; i++) {
			String memberId = dto.getMembers().get(i).getId();
			params[i] = new MapSqlParameterSource();
			params[i].addValue(GROUP_ID_PARAM_NAME, dto.getId());
			params[i].addValue(MEMBER_ID_PARAM_NAME, memberId);
			updatedIds.add(memberId);
			
			// Check for circularity
			if (dto.getId().equals(memberId)) {
				throw new DatastoreException("A group may not be a member of itself");
			}
			if (parents.contains(memberId)) {
				throw new DatastoreException("Group "+memberId+"is already a parent group of "+dto.getId());
			}
		}
		simpleJdbcTemplate.batchUpdate(INSERT_NEW_MEMBERS_OF_GROUP, params);
		userGroupDAO.touchList(updatedIds);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void removeMembers(GroupMembers dto) throws DatastoreException {
		Set<String> parents = getParentGroupIds(dto.getId());
		List<String> updatedIds = new ArrayList<String>();
		updatedIds.add(dto.getId());
		updatedIds.addAll(parents);
		
		MapSqlParameterSource params[] = new MapSqlParameterSource[dto.getMembers().size()];
		for (int i = 0; i < params.length; i++) {
			String memberId = dto.getMembers().get(i).getId();
			params[i] = new MapSqlParameterSource();
			params[i].addValue(GROUP_ID_PARAM_NAME, dto.getId());
			params[i].addValue(MEMBER_ID_PARAM_NAME, memberId);
			updatedIds.add(memberId);
		}
		simpleJdbcTemplate.batchUpdate(DELETE_MEMBERS_OF_GROUP, params);
		userGroupDAO.touchList(updatedIds);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public List<UserGroup> getUsersGroups(String principalId)
			throws DatastoreException {
		List<String> parentGroupIds = new ArrayList<String>(getParentGroupIds(principalId));
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(GROUP_ID_PARAM_NAME, parentGroupIds);
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_USER_GROUP_BY_PRINCIPAL_IDS, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		
		for (DBOUserGroup dbo : dbos) {
			UserGroup dto = new UserGroup();
			UserGroupUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	/**
	 * Iteratively moves up the group hierarchy and fetches all parents of the given Principal
	 */
	private Set<String> getParentGroupIds(String principalId) {
		Set<String> groupIds = new HashSet<String>();
		List<String> memberIds = new ArrayList<String>();
		memberIds.add(principalId);

		// Traverse up the group hierarchy until no more groups have any parents
		// It is assumed that there are no cycles in the hierarchy (enforced by addMembers())
		// Note: the frontier-search prevents infinite loops
		while (memberIds.size() > 0) {
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(MEMBER_ID_PARAM_NAME, memberIds);
			memberIds = simpleJdbcTemplate.query(SELECT_GROUPS_OF_MEMBER, groupIdRowMapper, param);
			
			// Keep track of all the unique groups
			for (int i = 0; i < memberIds.size(); i++) {
				if (!groupIds.add(memberIds.get(i))) {
					// This group has already been included in the search frontier
					memberIds.remove(i);
					i--;
				}
			}
		}
		return groupIds;
	}
}
