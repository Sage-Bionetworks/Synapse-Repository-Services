package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * Implementation of GroupMembersDAO which gets information from Crowd
 * This class will have a limited life span (1 or 2 weeks) while transitioning user groups away from Crowd
 *
 */
public class CrowdGroupMembersDAO implements GroupMembersDAO {
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Override
	public List<UserGroup> getMembers(String principalId)
			throws DatastoreException, NotFoundException {
		return getMembers(principalId, false);
	}
	
	@Override
	public List<UserGroup> getMembers(String principalId, boolean nested)
			throws DatastoreException, NotFoundException {
		UserGroup group = userGroupDAO.get(principalId);

		// Fetch all the members (users) of the group from Crowd
		List<String> usernames;
		try {
			// Since Crowd's groups aren't nested, the boolean does not change the results of the query
			usernames = CrowdAuthUtil.getAllUsersInGroup(group.getName(), nested);
			
		} catch (IOException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		} catch (AuthenticationException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		}

		// Convert the usernames back into userGroups
		List<UserGroup> members = new ArrayList<UserGroup>();
		if (usernames.size() > 0) {
			members.addAll(userGroupDAO.getGroupsByNames(usernames).values());
		}
		return members;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addMembers(String groupId, List<String> memberIds) 
			throws DatastoreException, NotFoundException, IllegalArgumentException {
		UserGroup group = userGroupDAO.get(groupId);
		
		// Make sure the group is a group, not an individual
		if (group.getIsIndividual()) {
			throw new IllegalArgumentException("Members cannot be added to an individual");
		}
		
		// Only the case for this DAO, due to its non-transaction-ality otherwise
		if (memberIds.size() > 1) {
			throw new IllegalArgumentException("Only one member can be added at a time");
		}
		
		if (memberIds.size() <= 0) {
			return;
		}
		
		try {
			UserGroup member = userGroupDAO.get(memberIds.get(0));
			CrowdAuthUtil.addUserToGroup(group.getName(), member.getName());
			// Note: Updating the etags of all affected group is the job of the caller for this DAO
			
		} catch (AuthenticationException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		} catch (IOException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		}
	}

	@Override
	public void removeMembers(String groupId, List<String> memberIds) 
			throws DatastoreException, NotFoundException {
		UserGroup group = userGroupDAO.get(groupId);
		
		// Make sure the group is a group, not an individual
		if (group.getIsIndividual()) {
			throw new IllegalArgumentException("Members cannot be removed from an individual");
		}
		
		// Only the case for this DAO, due to its non-transaction-ality otherwise
		if (memberIds.size() > 1) {
			throw new IllegalArgumentException("Only one member can be removed at a time");
		}
		
		if (memberIds.size() <= 0) {
			return;
		}
		
		try {
			UserGroup member = userGroupDAO.get(memberIds.get(0));
			CrowdAuthUtil.removeUserFromGroup(group.getName(), member.getName());
			// Note: Updating the etags of all affected group is the job of the caller for this DAO

		} catch (AuthenticationException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		} catch (IOException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		}
	}

	@Override
	public List<UserGroup> getUsersGroups(String principalId) 
			throws DatastoreException, NotFoundException {
		try {
			UserGroup group = userGroupDAO.get(principalId);
			List<String> groupNames = CrowdAuthUtil.getAllUsersGroups(group.getName(), true);
			
			// Convert group names back into principal IDs
			List<UserGroup> groupIDs = new ArrayList<UserGroup>();
			if (groupNames.size() > 0) {
				groupIDs.addAll(userGroupDAO.getGroupsByNames(groupNames).values());
			}
			return groupIDs;
			
		} catch (NotFoundException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		} catch (IOException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		}
	}
	
}
