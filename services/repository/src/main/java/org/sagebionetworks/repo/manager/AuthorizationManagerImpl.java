package org.sagebionetworks.repo.manager;


import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembershipDAO;
import org.sagebionetworks.repo.model.GroupPermissionsDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class AuthorizationManagerImpl implements AuthorizationManager {
	@Autowired
	GroupPermissionsDAO groupPermissionsDAO;
	
	@Autowired
	NodeInheritanceDAO nodeInheritanceDAO;
	
	private boolean isAdmin(UserInfo userInfo) throws DatastoreException, NotFoundException {
		Collection<UserGroup> userGroups = userInfo.getGroups();
		UserGroup adminGroup = groupPermissionsDAO.getAdminGroup();
		for (UserGroup ug: userGroups) if (ug.getId().equals(adminGroup.getId())) return true;
		return false;
	}
	
	/**
	 * @param groupId
	 * @param nodeId
	 * @param accessType
	 * 
	 * @return true iff the given group has the given access to the given node
	 * 
	 * @exception NotFoundException if the group or node is invalid
	 * 
	 */
	public boolean canAccess(UserInfo userInfo, final String nodeId, AuthorizationConstants.ACCESS_TYPE accessType) 
		throws NotFoundException, DatastoreException {
		User user = userInfo.getUser();
		String userId = user.getId();
		if (userId==null) throw new IllegalArgumentException();
		// if is an administrator, return true
		if (isAdmin(userInfo)) return true;
		// if the public can access the resource, then no need to check the user, just return true
		if(groupPermissionsDAO.getAccessTypes(groupPermissionsDAO.getPublicGroup(), nodeId).contains(accessType)) return true;
		// if not publicly accessible, then we WILL have to check the user, so a null userId->false
		if (userId.equals(AuthUtilConstants.ANONYMOUS_USER_ID)) return false;
		// must look-up access
		String permissionsBenefactor = nodeInheritanceDAO.getBenefactor(nodeId);
		return groupPermissionsDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, accessType);
	}
	
	public void addUserAccess(Node node, String userName) throws NotFoundException, DatastoreException {
		UserGroup group = null;
		if (AuthUtilConstants.ANONYMOUS_USER_ID.equals(userName)) {
			group = groupPermissionsDAO.getPublicGroup();
			if (group==null) throw new DatastoreException("Public group not found.");
		} else {
			group = groupPermissionsDAO.getIndividualGroup(userName);
			if (group==null) throw new DatastoreException("Individual group for "+userName+" not found.");
		}
		
		groupPermissionsDAO.addResource(group, node.getId(), Arrays
				.asList(new AuthorizationConstants.ACCESS_TYPE[] { AuthorizationConstants.ACCESS_TYPE.READ,
						AuthorizationConstants.ACCESS_TYPE.CHANGE,
						AuthorizationConstants.ACCESS_TYPE.SHARE }));

	}	

	public boolean canCreate(UserInfo userInfo, String nodeType) throws NotFoundException, DatastoreException {
		if (isAdmin(userInfo)) return true;
		// if the public can access the resource, then no need to check the user, just return true
		UserGroup publicGroup = groupPermissionsDAO.getPublicGroup();
		Collection<String> publicCreatableTypes = groupPermissionsDAO.getCreatableTypes(publicGroup);
		if(publicCreatableTypes.contains(nodeType)) return true;
		// if not publicly accessible, then we WILL have to check the user, so a null userId->false
		User user = userInfo.getUser();
		if (AuthUtilConstants.ANONYMOUS_USER_ID.equals(user.getId())) return false;
		// must look-up access
		return groupPermissionsDAO.canCreate(userInfo.getGroups(), nodeType);
	}


//	// should we add access to the node itself or to the node's permissions benefactor?
//	@Override
//	public void addUserAccess(Node node, UserInfo userInfo)
//			throws NotFoundException, DatastoreException {
//		groupPermissionsDAO.addResource(
//				userInfo.getIndividualGroup(), node.getId(), 
//				new HashSet<AuthorizationConstants.ACCESS_TYPE>(
//						Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[] {
//								AuthorizationConstants.ACCESS_TYPE.READ, 
//								AuthorizationConstants.ACCESS_TYPE.CHANGE, 
//								AuthorizationConstants.ACCESS_TYPE.SHARE} )));
//		
//	}


	/**
	 * @param nodeId the resource whose authorization is to be removed
	 * 
	 * Removes all authorization for this resource, e.g. just before deletion.
	 */
	public void removeAuthorization(String nodeId) throws NotFoundException, DatastoreException {
		groupPermissionsDAO.removeAuthorization(nodeId);
	}

	/**
	 * @return the SQL to find the root-accessible nodes that a specified user-group list can access
	 * using a specified access type
	 */
	public String authorizationSQL(AuthorizationConstants.ACCESS_TYPE accessType, List<String> groupIds) {
		return groupPermissionsDAO.authorizationSQL(accessType, groupIds);
	}

}
