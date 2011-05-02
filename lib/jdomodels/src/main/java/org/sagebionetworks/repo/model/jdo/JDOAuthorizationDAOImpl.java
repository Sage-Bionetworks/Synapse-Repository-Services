package org.sagebionetworks.repo.model.jdo;


import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
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
	
	
	private static final String NODE_RESOURCE_TYPE = JDONode.class.getName();
	
	public JDOUserGroup getPublicGroup() {
		return getSystemGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
	}
	
	public JDOUserGroup getIndividualGroup(String userName) {
		return getSystemGroup(userName, true);
	}
	
	public JDOUserGroup getSystemGroup(String name, boolean isIndividualGroup) {
		JDOExecutor<JDOUserGroup> exec = new JDOExecutor<JDOUserGroup>(jdoTemplate, JDOUserGroup.class);
		Collection<JDOUserGroup> ans = exec.execute(
				"isSystemGroup==true && name==pName && isIndividual==pIsIndividual",
				String.class.getName()+" pName, "+Boolean.class.getName()+" pIsIndividual",
				null,
				name, isIndividualGroup);
		if (ans.size() > 1)
			throw new IllegalStateException("Expected 0-1 but found "
					+ ans.size());
		if (ans.size() == 0)
			return null;
		return ans.iterator().next();
	}
	
	public JDOUser getUser(String userName) throws DatastoreException {
		JDOExecutor<JDOUser> exec = new JDOExecutor<JDOUser>(jdoTemplate, JDOUser.class);
		Collection<JDOUser> u = exec.execute("userId==pUserId", String.class.getName()+" pUserId", null, userName);
		if (u.size()>1) throw new DatastoreException("Expected one user named "+userName+" but found "+u.size());
		if (u.size()==0) return null;
		return u.iterator().next();
	}
	
	public JDOUserGroup getAdminGroup() {
		return getSystemGroup(AuthorizationConstants.ADMIN_GROUP_NAME, false);
	}

	public boolean isAdmin(JDOUser user) throws DatastoreException {
		JDOUserGroup adminGroup = getAdminGroup();
		return adminGroup.getUsers().contains(user.getId());
	}
	
	public static Collection<String> getAccessTypes(JDOUserGroup userGroup,
			String resourceType, Long resourceId) throws NotFoundException, DatastoreException {
		Collection<String> ans = new HashSet<String>();
		for (JDOResourceAccess ra : userGroup.getResourceAccess()) {
			if (ra.getResourceType().equals(resourceType) && ra.getResourceId().equals(resourceId)) {
				ans = ra.getAccessType();
				break;
			}
		}
		return ans;
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
	public boolean canAccess(String userName, String nodeId, String accessType) 
		throws NotFoundException, DatastoreException {
		Long resourceId = Long.parseLong(nodeId);
		// if the public can access the resource, then no need to check the user, just return true
		if(getAccessTypes(getPublicGroup(), NODE_RESOURCE_TYPE, resourceId).contains(accessType)) return true;
		// if not publicly accessible, then we WILL have to check the user, so a null userId->false
		if (userName==null) return false;
		JDOUser user = getUser(userName);
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
		JDOUserGroup group = null;
		if (userName == null) {
			group = getPublicGroup();

		} else {
			group = getIndividualGroup(userName);
		}
		// now add the object to the group

		addResourceToGroup(group, NODE_RESOURCE_TYPE, Long.parseLong(node.getId()), Arrays
					.asList(new String[] { AuthorizationConstants.READ_ACCESS,
							AuthorizationConstants.CHANGE_ACCESS,
							AuthorizationConstants.SHARE_ACCESS }));

	}
	
	public static void addResourceToGroup(JDOUserGroup group, String type, Long resourceKey,
			Collection<String> accessTypes) {
	
		Set<JDOResourceAccess> ras = group.getResourceAccess();
		boolean foundit = false;
		// if you can find the reference resource, then update it...
		for (JDOResourceAccess ra: ras) {
			if (type.equals(ra.getResourceType()) && resourceKey.equals(ra.getResourceId())) {
				foundit = true;
				ra.setAccessType(new HashSet<String>(accessTypes));
				break;
			}
		}
		// ... else add a new record for the resource, with the specified access types.
		if (!foundit) {
			JDOResourceAccess ra = new JDOResourceAccess();
			ra.setResourceType(type);
			ra.setResourceId(resourceKey);
			ra.setAccessType(new HashSet<String>(accessTypes));
			group.getResourceAccess().add(ra);
		}
	}
	



}
