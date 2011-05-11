package org.sagebionetworks.repo.model.jdo;


import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.aw.JDOUserDAO;
import org.sagebionetworks.repo.model.jdo.aw.JDOUserGroupDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class JDOAuthorizationManagerImpl implements JDOAuthorizationManager {
	
	@Autowired
	private JdoTemplate jdoTemplate;
	@Autowired
	JDOUserGroupDAO userGroupDAO;
	
	@Autowired
	JDOUserDAO userDAO;
	
	public User createUser(String userName) throws DatastoreException {
		try {
			User user = new User();
			user.setUserId(userName);
			userDAO.create(user);
			UserGroup individualGroup = userGroupDAO.createIndividualGroup(userName);
			userGroupDAO.addUser(individualGroup, KeyFactory.stringToKey(user.getId()));
			// let the user access (READ, CHANGE) herself
			userGroupDAO.addResource(individualGroup, 
					new AuthorizableImpl(user.getId(), User.class.getName()), 
					Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[] { 
							AuthorizationConstants.ACCESS_TYPE.READ,
							AuthorizationConstants.ACCESS_TYPE.CHANGE}));
			return user;
		} catch (DatastoreException e) {
			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}
	
	public void deleteUser(String userName) throws DatastoreException, NotFoundException {
		UserGroup individualGroup = userGroupDAO.getIndividualGroup(userName);
		userGroupDAO.delete(individualGroup.getId());
		User user = userDAO.getUser(userName);
		userDAO.delete(user.getId());
	}
	
	public boolean isAdmin(User user) throws DatastoreException, NotFoundException {
		UserGroup adminGroup = userGroupDAO.getAdminGroup();
		return userGroupDAO.getUsers(adminGroup).contains(KeyFactory.stringToKey(user.getId()));
	}
	
	public boolean isAdmin(String userName) throws DatastoreException, NotFoundException {
		User user = userDAO.getUser(userName);
		return isAdmin(user);
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
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		Collection<JDOUserGroup> c = exec.execute(JDOUserGroup.class, "resourceAccess.contains(vra) && "+
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
				JDOExecutor exec = new JDOExecutor(jdoTemplate);
				Collection<JDOResourceAccess> ras = exec.execute(JDOResourceAccess.class, "resourceId==pId", 
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
		if (AuthUtilConstants.ANONYMOUS_USER_ID.equals(userName)) {
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
		if (AuthUtilConstants.ANONYMOUS_USER_ID.equals(userName)) return false;
		User user = userDAO.getUser(userName);
		if (user==null) throw new NotFoundException(userName+" does not exist");
		// must look-up access, allowing that admin's can access anything
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		Collection<JDOUserGroup> c = exec.execute(
				JDOUserGroup.class, "users.contains(pUser) && ((isSystemGroup && name==pAdminName) || "+
				"resourceAccess.contains(pCreateableType))",
				Long.class.getName()+" pUser, "+String.class.getName()+" pAdminName, "+String.class.getName()+" pCreateableType",
				null,
				user.getId(), AuthorizationConstants.ADMIN_GROUP_NAME, nodeType
		);
		return c.size()>0;
	
	}

	private Map<String, String> classTables = null;
	private String authorizationSQL = null;
//	private String adminSQL = null;
	
	private void initClassTables() {
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		List<Object[]> resultsSet = exec.execute("select class_name, table_name from nucleus_tables".toUpperCase());
		classTables = new HashMap<String,String>();
		for (Object[] row : resultsSet) {
			if (row.length!=2) throw new IllegalStateException("Unexpected number of columns: "+row.length);
			classTables.put((String)row[0], (String)row[1]);
		}
	}
	
	private String getFromClassTables(String key) {
		if (!classTables.containsKey(key)) throw new IllegalStateException("ClassTables is missing "+key);
		return classTables.get(key);
	}
	
	private void initAuthorizationSQL() {
		if (classTables==null) initClassTables();
		authorizationSQL = ("select distinct ra.resource_id from "+
			getFromClassTables(JDOResourceAccess.class.getName())+" ra, "+
			getFromClassTables(JDOResourceAccess.class.getName()+".accessType")+" t, "+
			getFromClassTables(JDOUserGroup.class.getName())+" ug, "+
			getFromClassTables(JDOUserGroup.class.getName()+".users")+" ugu, "+
			getFromClassTables(JDOUser.class.getName())+" u "+
			"where "+
			"ra.owner_id_oid=ug.id and ra.id=t.id_oid and t.string_ele = :accessType and "+
			" ra.resource_type='"+AuthorizationManager.NODE_RESOURCE_TYPE+"' and "+
			"( "+
			// the user group contains the given user
			"(ug.id = ugu.id_oid and ugu.long_ele=u.id and u.user_id = :userName) or"+
			// the user group is Public
			"(ug.is_system_group=true and ug.is_individual=false and ug.name='"+AuthorizationConstants.PUBLIC_GROUP_NAME+"')"+
			")")/*.toUpperCase()*/;
	}
	
//	private void initAdminSQL() {
//		if (classTables==null) initClassTables();
//		// count the users matching the given user name in the admin group
//		// thus it returns 1 if an admin, 0 otherwise
//		adminSQL = ("select count(*) from "+
//			getFromClassTables(JDOUserGroup.class.getName())+" ug, "+
//			getFromClassTables(JDOUserGroup.class.getName()+".users")+" ugu, "+
//			getFromClassTables(JDOUser.class.getName())+" u "+
//			"where "+
//			// it's the admin group
//			"ug.is_system_group=true and ug.is_individual=false and ug.name='"+AuthorizationConstants.ADMIN_GROUP_NAME+"' and "+
//			// and the user is in it
//			"ug.id = ugu.id_oid and ugu.long_ele=u.id and u.user_id = :userName")/*.toUpperCase()*/;
//	}
//	
	
	/**
	 * @return the SQL to find the root-accessible nodes that a specified user can access
	 * using a specified access type
	 */
	public String authorizationSQL() {
		if (authorizationSQL==null) initAuthorizationSQL();
		return authorizationSQL;
	}
	
//	/**
//	 * @return the SQL to determine whether a user is an administrator
//	 * Returns 1 if an admin, 0 otherwise
//	 */
//	public String adminSQL() {
//		if (adminSQL==null) initAdminSQL();
//		return adminSQL;
//	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void afterPropertiesSet() throws Exception {
		// ensure admin user is created
		User adminUser = userDAO.getUser(AuthUtilConstants.ADMIN_USER_ID);
		if (adminUser==null) {
			adminUser = new User();
			adminUser.setCreationDate(new Date());
			adminUser.setUserId(AuthUtilConstants.ADMIN_USER_ID);
			userDAO.create(adminUser);
		}
		// ensure admin group is created, and that 'admin' is a member
		UserGroup ag = userGroupDAO.getAdminGroup();
		if (ag==null) throw new IllegalStateException("Admin Group should have been created during UserGroupDAO autowiring!");
		userGroupDAO.addUser(ag, KeyFactory.stringToKey(adminUser.getId()));
	}

}
