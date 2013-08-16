package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembers;
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
	public GroupMembers getMembers(String principalId)
			throws DatastoreException {
		
		GroupMembers members = new GroupMembers();
		members.setId(principalId);
		UserGroup group;
		try {
			group = userGroupDAO.get(principalId);
		} catch (NotFoundException e) {
			throw new DatastoreException(e);
		}

		// Fetch all the members (users) of the group from Crowd
		List<String> usernames;
		try {
			usernames = CrowdAuthUtil.getAllUsersInGroup(group.getName(), true);
			
		} catch (IOException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		} catch (AuthenticationException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		}
		
		// Convert the usernames back into userGroups
		members.setMembers(new ArrayList<UserGroup>());
		if (usernames.size() > 0) {
			members.getMembers().addAll(userGroupDAO.getGroupsByNames(usernames).values());
		}
		return members;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addMembers(GroupMembers dto) throws DatastoreException {
		// Make sure the DTO holds a group, not an individual
		UserGroup group;
		try {
			group = userGroupDAO.get(dto.getId());
			if (group.getIsIndividual()) {
				throw new DatastoreException("Members cannot be added to an individual");
			}
		} catch (NotFoundException e) {
			throw new DatastoreException("The group "+dto.getId()+" does not exist");
		}
		
		try {
			// This isn't transactional... which is one reason we're migrating away from this
			for (UserGroup ug : dto.getMembers()) {
				CrowdAuthUtil.addUserToGroup(group.getName(), ug.getName());
			}
			
			// Update the etags of all affected items
			List<String> ids = new ArrayList<String>();
			ids.add(group.getId());
			for (UserGroup added : dto.getMembers()) {
				ids.add(added.getId());
			}
			userGroupDAO.touchList(ids);
		} catch (AuthenticationException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		} catch (IOException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		}
	}

	@Override
	public void removeMembers(GroupMembers dto) throws DatastoreException {
		UserGroup group;
		try {
			group = userGroupDAO.get(dto.getId());
		} catch (NotFoundException e) {
			throw new DatastoreException(e);
		}
		
		try {
			// This isn't transactional either...
			for (UserGroup ug : dto.getMembers()) {
				CrowdAuthUtil.removeUserFromGroup(group.getName(), ug.getName());
			}
			
			// Update the etags of all affected items
			List<String> ids = new ArrayList<String>();
			ids.add(group.getId());
			for (UserGroup removed : dto.getMembers()) {
				ids.add(removed.getId());
			}
			userGroupDAO.touchList(ids);
		} catch (AuthenticationException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		} catch (IOException e) {
			throw new DatastoreException("500 Server Error - "+e.getMessage(), e);
		}
	}

	@Override
	public List<UserGroup> getUsersGroups(String principalId)
			throws DatastoreException {
		try {
			UserGroup group;
			try {
				group = userGroupDAO.get(principalId);
			} catch (NotFoundException e) {
				throw new DatastoreException(e);
			}
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
