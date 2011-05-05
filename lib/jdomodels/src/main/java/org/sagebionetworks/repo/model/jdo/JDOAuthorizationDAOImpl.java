package org.sagebionetworks.repo.model.jdo;


import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.jdo.Query;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.Authorizable;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.aw.JDOUserDAO;
import org.sagebionetworks.repo.model.jdo.aw.JDOUserGroupDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;

public class JDOAuthorizationDAOImpl implements AuthorizationDAO {
	
	@Autowired
	private JdoTemplate jdoTemplate;
	@Autowired
	JDOUserGroupDAO userGroupDAO;
	
	@Autowired
	JDOUserDAO userDAO;
	
	private static final String NODE_RESOURCE_TYPE = JDONode.class.getName();
	
	public boolean isAdmin(User user) throws DatastoreException, NotFoundException {
		UserGroup adminGroup = userGroupDAO.getAdminGroup();
		return userGroupDAO.getUsers(adminGroup).contains(user);
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
	public boolean canAccess(String userName, final String nodeId, AuthorizationConstants.ACCESS_TYPE accessType) 
		throws NotFoundException, DatastoreException {
		if (userName==null) throw new IllegalArgumentException();
		Long resourceId = Long.parseLong(nodeId);
		// if the public can access the resource, then no need to check the user, just return true
		if(userGroupDAO.getAccessTypes(userGroupDAO.getPublicGroup(), new AuthorizableImpl(nodeId, NODE_RESOURCE_TYPE)).contains(accessType)) return true;
		// if not publicly accessible, then we WILL have to check the user, so a null userId->false
		if (userName==AuthUtilConstants.ADMIN_USER_ID) return false;
		User user = userDAO.getUser(userName);
		if (user==null) throw new NotFoundException(userName+" does not exist");
		// if is an administrator, return true
		if (isAdmin(user)) return true;
		// must look-up access
		JDOExecutor<JDOUserGroup> exec = new JDOExecutor<JDOUserGroup>(jdoTemplate, JDOUserGroup.class);
		Collection<JDOUserGroup> c = exec.execute("resourceAccess.contains(vra) && "+
				"vra.resourceType==pResourceType && vra.resourceId==pResourceId",
				String.class.getName()+" pResourceType, "+
				Long.class.getName() + " pResourceId",
				JDOResourceAccess.class.getName()+" vra",
				NODE_RESOURCE_TYPE, resourceId
				);
		for (JDOUserGroup g: c) {
			if (!g.getUsers().contains(user.getId())) continue;
			for (JDOResourceAccess ra: g.getResourceAccess()) {
				if (ra.getResourceType().equals(NODE_RESOURCE_TYPE) && ra.getResourceId().equals(resourceId)) {
					if (ra.getAccessType().contains(accessType)) return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * @param nodeId the resource whose authorization is to be removed
	 * 
	 * Removes all authorization for this resource, e.g. just before deletion.
	 */
	public void removeAuthorization(String nodeId) throws DatastoreException {
		/**
		 * This is used, for example before deleting a resource from the system, at which time it is to be removed from all groups to which it belongs.
		 */
			// query for all groups having the resource
			try {
				JDOExecutor<JDOResourceAccess> exec = new JDOExecutor<JDOResourceAccess>(jdoTemplate, JDOResourceAccess.class);
				Collection<JDOResourceAccess> ras = exec.execute("resourceId==pId", 
						java.lang.Long.class.getName()+" pId", 
						null,
						nodeId);
				for (JDOResourceAccess ra : ras) {
					ra.getOwner().getResourceAccess().remove(ra);
				}
			} catch (Exception e) {
				throw new DatastoreException(e);
			}

	}
	
	public void addUserAccess(Node node, String userName) throws NotFoundException, DatastoreException {
		UserGroup group = null;
		if (userName == AuthUtilConstants.ANONYMOUS_USER_ID) {
			group = userGroupDAO.getPublicGroup();
			if (group==null) throw new DatastoreException("Public group not found.");
		} else {
			group = userGroupDAO.getIndividualGroup(userName);
			if (group==null) throw new DatastoreException("Individual group for "+userName+" not found.");
		}
		// now add the object to the group
//		addResourceToGroup(group, NODE_RESOURCE_TYPE, Long.parseLong(node.getId()), Arrays
//				.asList(new AuthorizationConstants.ACCESS_TYPE[] { AuthorizationConstants.ACCESS_TYPE.READ,
//						AuthorizationConstants.ACCESS_TYPE.CHANGE,
//						AuthorizationConstants.ACCESS_TYPE.SHARE }));
		
		userGroupDAO.addResource(group, new AuthorizableImpl(node.getId(), NODE_RESOURCE_TYPE), Arrays
				.asList(new AuthorizationConstants.ACCESS_TYPE[] { AuthorizationConstants.ACCESS_TYPE.READ,
						AuthorizationConstants.ACCESS_TYPE.CHANGE,
						AuthorizationConstants.ACCESS_TYPE.SHARE }));

	}	

	public boolean canCreate(String userName, String nodeType) throws NotFoundException, DatastoreException {
		// if the public can access the resource, then no need to check the user, just return true
		UserGroup publicGroup = userGroupDAO.getPublicGroup();
		Collection<String> publicCreatableTypes = userGroupDAO.getCreatableTypes(publicGroup);
		if(publicCreatableTypes.contains(nodeType)) return true;
		// if not publicly accessible, then we WILL have to check the user, so a null userId->false
		if (userName==AuthUtilConstants.ANONYMOUS_USER_ID) return false;
		User user = userDAO.getUser(userName);
		if (user==null) throw new NotFoundException(userName+" does not exist");
		// must look-up access, allowing that admin's can access anything
		JDOExecutor<JDOUserGroup> exec = new JDOExecutor<JDOUserGroup>(jdoTemplate, JDOUserGroup.class);
		Collection<JDOUserGroup> c = exec.execute(
				"users.contains(pUser) && ((isSystemGroup && name==pAdminName) || "+
				"resourceAccess.contains(pCreateableType))",
				String.class.getName()+" pAdminName, "+String.class.getName()+" pCreateableType",
				null,
				user.getId(), AuthorizationConstants.ADMIN_GROUP_NAME, nodeType
		);
		return c.size()>0;
	
	}


}
