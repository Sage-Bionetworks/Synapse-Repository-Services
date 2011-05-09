package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDODataset;
import org.sagebionetworks.repo.model.jdo.persistence.JDOInputDataLayer;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.web.NotFoundException;


public class JDOUserGroupDAOImpl extends
		JDOBaseDAOImpl<UserGroup, JDOUserGroup> implements UserGroupDAO {
	
	private static final Logger log = Logger.getLogger(JDOUserGroupDAOImpl.class.getName());


	public JDOUserGroupDAOImpl(String userId) {
		super(userId);
	}

	public static boolean isPublicGroup(JDOUserGroup g) {
		return g.getIsSystemGroup()
				&& AuthorizationConstants.PUBLIC_GROUP_NAME.equals(g.getName());
	}

	/**
	 * Create the Public Group. 
	 * 
	 * @return
	 */
	public JDOUserGroup createPublicGroup(PersistenceManager pm) {
		Set<String> creatableTypes = new HashSet<String>();
		creatableTypes.add(JDOUser.class.getName());
		creatableTypes.add(JDOUserGroup.class.getName());
		return createSystemGroup(
				AuthorizationConstants.PUBLIC_GROUP_NAME, 
				false,
				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[] {
						AuthorizationConstants.ACCESS_TYPE.READ, 
						AuthorizationConstants.ACCESS_TYPE.CHANGE}),

				creatableTypes, 
				pm);
	}
	
	/**
	 * Create the Admin Group. 
	 * 
	 * @return
	 */
	public JDOUserGroup createAdminGroup(PersistenceManager pm) {
		Set<String> creatableTypes = new HashSet<String>();
		return createSystemGroup(
				AuthorizationConstants.ADMIN_GROUP_NAME, 
				false,
				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[] {
						AuthorizationConstants.ACCESS_TYPE.READ}),
				creatableTypes,  // for admin' group, don't have to explicitly declare types, rather anything can be created
				pm);
	}
	
	public JDOUserGroup createSystemGroup(String name, 
			boolean isIndividualGroup, 
			Collection<AuthorizationConstants.ACCESS_TYPE> selfAccess, // the type of access members have on the group itself
			Set<String> creatableTypes,
			PersistenceManager pm) {
		JDOUserGroup g = newJDO();
		g.setName(name);
		g.setCreationDate(new Date());
		g.setIsSystemGroup(true);
		g.setIsIndividual(isIndividualGroup);
//		Set<String> ts = new HashSet<String>();
//		for (AuthorizationConstants.ACCESS_TYPE t: creatableTypes) ts.add(t.name());
		g.getCreatableTypes().addAll(creatableTypes);
		g.setResourceAccess(new HashSet<JDOResourceAccess>());
		g.setUsers(new HashSet<Long>());
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			pm.makePersistent(g);
			// now give the group members read-access to the group itself
			addResourceToGroup(g, JDOUserGroup.class.getName(), g.getId(), selfAccess);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
		}
		return g;
	}

	/**
	 * This is the externally facing method. 
	 */
	public UserGroup getPublicGroup() throws NotFoundException,
			DatastoreException {
		PersistenceManager pm = PMF.get();
		JDOUserGroup jdo = getPublicGroup(pm);
		UserGroup dto = new UserGroup();
		copyToDto(jdo, dto);
		return dto;		
	}

	public static JDOUserGroup getPublicGroup(PersistenceManager pm) {
		return getSystemGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false, pm);
	}
		
	public static JDOUserGroup getAdminGroup(PersistenceManager pm) {
		return getSystemGroup(AuthorizationConstants.ADMIN_GROUP_NAME, false, pm);
	}
		
	/**
	 * 
	 * @param user
	 * @return true iff the given user is in the admin group
	 */
	public static boolean isAdmin(JDOUser user, PersistenceManager pm) {
		JDOUserGroup adminGroup = getAdminGroup(pm);
		return adminGroup.getUsers().contains(user.getId());
	}
	
	public static boolean isAdmin(String userId, PersistenceManager pm) {
		if (userId==null || AuthUtilConstants.ANONYMOUS_USER_ID.equals(userId)) return false;
		JDOUser user = (new JDOUserDAOImpl(userId)).getUser(pm);
		if (user==null) throw new IllegalArgumentException("No user "+userId);
		return isAdmin(user, pm);
	}
	
	/**
	 * Retrieves system-generated groups
	 * @param name
	 * @param pm
	 * @return
	 */
	public static JDOUserGroup getSystemGroup(String name, boolean isIndividual, PersistenceManager pm) {
		Query query = pm.newQuery(JDOUserGroup.class);
		query.setFilter("isSystemGroup==true && name==pName && isIndividual==pIsIndividual");
		query.declareParameters(String.class.getName()+" pName, "+Boolean.class.getName()+" pIsIndividual");
		@SuppressWarnings("unchecked")
		Collection<JDOUserGroup> ans = (Collection<JDOUserGroup>) query
				.execute(name, isIndividual);
		if (ans.size() > 1)
			throw new IllegalStateException("Expected 0-1 but found "
					+ ans.size());
		if (ans.size() == 0)
			return null;
		return ans.iterator().next();
	}

	/**
	 * Create a group for a particular user. Give the user READ
	 * access to their own group.
	 * 
	 * @param pm
	 * @return
	 */
	public JDOUserGroup createIndividualGroup(PersistenceManager pm,
			JDOUser user) {
		Set<String> creatableTypes = new HashSet<String>();
		JDOUserGroup  g =  createSystemGroup(
				user.getUserId(), 
				true,
				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[] {
						AuthorizationConstants.ACCESS_TYPE.READ}),
				creatableTypes, 
				pm);
		g.getUsers().add(user.getId());
		return g;
	}

	public JDOUserGroup getIndividualGroup(PersistenceManager pm) {
		if (null == userId)
			return null;
		return getSystemGroup(userId, true, pm);
	}

	public JDOUserGroup getOrCreateIndividualGroup(PersistenceManager pm) {
		if (null == userId)
			throw new NullPointerException();
		// get the individual group
		JDOUserGroup group = getIndividualGroup(pm);
		if (/* individual group doesn't exist */null == group) {
			// create an Individual group
			JDOUser user = (new JDOUserDAOImpl(userId)).getUser(pm);
			group = createIndividualGroup(pm, user);
		}
		return group;
	}

	protected UserGroup newDTO() {
		UserGroup ug = new UserGroup();
		ug.setCreatableTypes(new HashSet<String>());
		return ug;
	}

	protected JDOUserGroup newJDO() {
		JDOUserGroup g = new JDOUserGroup();
		g.setUsers(new HashSet<Long>());
		g.setResourceAccess(new HashSet<JDOResourceAccess>());
		g.setCreatableTypes(new HashSet<String>());
		return g;
	}

	protected void copyToDto(JDOUserGroup jdo, UserGroup dto)
			throws DatastoreException {
		dto.setId(jdo.getId() == null ? null : KeyFactory.keyToString(jdo
				.getId()));
		dto.setCreationDate(jdo.getCreationDate());
		dto.setName(jdo.getName());
		dto.setCreatableTypes(new HashSet<String>(jdo.getCreatableTypes()));
	}

	protected void copyFromDto(UserGroup dto, JDOUserGroup jdo)
			throws InvalidModelException {
		if (null == dto.getName()) {
			throw new InvalidModelException(
					"'name' is a required property for UserGroup");
		}
		jdo.setName(dto.getName());
		jdo.setCreationDate(dto.getCreationDate());
		Set<String> cts = new HashSet<String>();
		if (dto.getCreatableTypes()!=null) cts.addAll(dto.getCreatableTypes());
		jdo.setCreatableTypes(cts);
	}

	protected Class<JDOUserGroup> getJdoClass() {
		return JDOUserGroup.class;
	}

	public Collection<String> getPrimaryFields() {
		return Arrays.asList(new String[] { "name" });
	}

	public void addUser(UserGroup userGroup, User user)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		if (!canAccess(userId, getJdoClass().getName(), KeyFactory.stringToKey(userGroup.getId()), AuthorizationConstants.ACCESS_TYPE.CHANGE, pm))
			throw new UnauthorizedException();
		Long userKey = KeyFactory.stringToKey(user.getId());
		// this is done simply to make check that the user exists
		JDOUser jdoUser =(JDOUser) pm.getObjectById(JDOUser.class, userKey);
		Long groupKey = KeyFactory.stringToKey(userGroup.getId());
		JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
				JDOUserGroup.class, groupKey);
		addUser(jdoGroup, jdoUser, pm);
	}
	
	public void addUser(JDOUserGroup jdoGroup, JDOUser jdoUser, PersistenceManager pm) 
		throws NotFoundException, DatastoreException, UnauthorizedException {
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Set<Long> users = jdoGroup.getUsers();
			if (!users.contains(jdoUser.getId())) {
				users.add(jdoUser.getId());
			}
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public void removeUser(UserGroup userGroup, User user)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Long userKey = KeyFactory.stringToKey(user.getId());
			// this is done simply to make check that the user exists
			//JDOUser jdoUser = (JDOUser) 
			pm.getObjectById(JDOUser.class, userKey);
			Long groupKey = KeyFactory.stringToKey(userGroup.getId());
			JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
					JDOUserGroup.class, groupKey);
			if (!canAccess(userId, getJdoClass().getName(), jdoGroup.getId(), AuthorizationConstants.ACCESS_TYPE.CHANGE, pm))
				throw new UnauthorizedException();
			jdoGroup.getUsers().remove(userKey);
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}
	
	/**
	 * This is used, for example before deleting a user from the system, at which time it is to be removed from all groups to which it belongs.
	 */
	public void removeUserFromAllGroups(JDOUser user) throws NotFoundException, DatastoreException {
		// query for all groups having the user as a member
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Query query = pm.newQuery(JDOUserGroup.class);
			query.setFilter("this.users.contains(pUser)");
			query.declareParameters(java.lang.Long.class.getName()+" pUser");
			@SuppressWarnings("unchecked")
			Collection<JDOUserGroup> groups = (Collection<JDOUserGroup>)query.execute(user.getId());
			for (JDOUserGroup g : groups) {
				g.getUsers().remove(user.getId());
			}
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	/**
	 * This is used, for example before deleting a resource from the system, at which time it is to be removed from all groups to which it belongs.
	 */
	public void removeResourceFromAllGroups(JDOBase resource) throws NotFoundException, DatastoreException {
		// query for all groups having the resource
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Query query = pm.newQuery(JDOResourceAccess.class);
			query.setFilter("resourceType==pType && resourceId==pId");
			query.declareParameters(java.lang.String.class.getName()+" pType, "+java.lang.Long.class.getName()+" pId");
			@SuppressWarnings("unchecked")
			Collection<JDOResourceAccess> ras = (Collection<JDOResourceAccess>)query.execute(getResourceType(resource), resource.getId());
			for (JDOResourceAccess ra : ras) {
				ra.getOwner().getResourceAccess().remove(ra);
			}
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public Collection<User> getUsers(UserGroup userGroup)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		try {
			Long groupKey = KeyFactory.stringToKey(userGroup.getId());
			JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
					JDOUserGroup.class, groupKey);
			if (!canAccess(userId, getJdoClass().getName(), jdoGroup.getId(), AuthorizationConstants.ACCESS_TYPE.READ, pm))
				throw new UnauthorizedException();

			Collection<Long> userKeys = jdoGroup.getUsers();
			JDOUserDAOImpl userDAO = new JDOUserDAOImpl(userId);
			Collection<User> ans = new HashSet<User>();
			for (Long userKey : userKeys) {
				ans.add(userDAO.get(KeyFactory.keyToString(userKey)));
			}
			return ans;
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}
	
	private static Map<String,String> resourceTypes;
	static {
		resourceTypes = new HashMap<String,String>();
		resourceTypes.put(Dataset.class.getName(), JDODataset.class.getName());
		resourceTypes.put(InputDataLayer.class.getName(), JDOInputDataLayer.class.getName());
		resourceTypes.put(User.class.getName(), JDOUser.class.getName());
		resourceTypes.put(UserGroup.class.getName(), JDOUserGroup.class.getName());
		//resourceTypes.put(.class.getName(), JDO.class.getName());
	}
	
	private static String getResourceType(Base resource) {
		String type = resourceTypes.get(resource.getClass().getName());
		if (type==null) throw new IllegalArgumentException("Unrecognized type "+resource.getClass().getName());
		return type;
	}
	
	private static String getResourceType(JDOBase resource) {
		String type = resource.getClass().getName();
		return type;
	}
	
	public void addResource(UserGroup userGroup, Base resource,
			Collection<AuthorizationConstants.ACCESS_TYPE> accessType) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Long resourceKey = KeyFactory.stringToKey(resource.getId());
		String type = getResourceType(resource);
		if (!canAccess(userId, getResourceType(resource), resourceKey, AuthorizationConstants.ACCESS_TYPE.SHARE, pm))
			throw new UnauthorizedException();
		Long groupKey = KeyFactory.stringToKey(userGroup.getId());
		JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
				JDOUserGroup.class, groupKey);

		Transaction tx = null;
		tx = pm.currentTransaction();
		tx.begin();
 		try {
 			addResourceToGroup(jdoGroup, type, resourceKey, accessType);
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public static void addResourceToGroup(JDOUserGroup group, String type, Long resourceKey,
			Collection<AuthorizationConstants.ACCESS_TYPE> accessTypes) {
	
		Set<JDOResourceAccess> ras = group.getResourceAccess();
		boolean foundit = false;
		// if you can find the reference resource, then update it...
		for (JDOResourceAccess ra: ras) {
			if (type.equals(ra.getResourceType()) && resourceKey.equals(ra.getResourceId())) {
				foundit = true;
				ra.setAccessTypeByEnum(new HashSet<AuthorizationConstants.ACCESS_TYPE>(accessTypes));
				break;
			}
		}
		// ... else add a new record for the resource, with the specified access types.
		if (!foundit) {
			JDOResourceAccess ra = new JDOResourceAccess();
			ra.setResourceType(type);
			ra.setResourceId(resourceKey);
			ra.setAccessTypeByEnum(new HashSet<AuthorizationConstants.ACCESS_TYPE>(accessTypes));
			group.getResourceAccess().add(ra);
		}
	}
	



	public void removeResource(UserGroup userGroup, Base resource) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			String type = getResourceType(resource);
			Long resourceKey = KeyFactory.stringToKey(resource.getId());
			if (!canAccess(userId, type, resourceKey, AuthorizationConstants.ACCESS_TYPE.SHARE, pm))
				throw new UnauthorizedException();
			Long groupKey = KeyFactory.stringToKey(userGroup.getId());
			JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
					JDOUserGroup.class, groupKey);
			// see comments in 'addResource' as to why this was removed
//			if (!canAccess(userId, getJdoClass().getName(), jdoGroup.getId(), AuthorizationConstants.CHANGE_ACCESS, pm))
//				throw new UnauthorizedException();
			Collection<JDOResourceAccess> ras = jdoGroup.getResourceAccess();
			tx = pm.currentTransaction();
			tx.begin();
			for (JDOResourceAccess ra : ras) {
				if (ra.getResourceType().equals(type) && 
						ra.getResourceId().equals(resourceKey)) {
					ras.remove(ra);
				}
			}
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public Collection<AuthorizationConstants.ACCESS_TYPE> getAccessTypes(UserGroup userGroup, Base resource) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Long gId = Long.parseLong(userGroup.getId());
		Long rId = Long.parseLong(resource.getId());
		if (!canAccess(userId, JDOUserGroup.class.getName(), gId, AuthorizationConstants.ACCESS_TYPE.READ, pm)) throw new UnauthorizedException();
		JDOUserGroup group = pm.getObjectById(JDOUserGroup.class, gId);
		return getAccessTypes(group, getResourceType(resource), rId);
	}
	
	// the internal method, with no authorization filter
	public static Collection<AuthorizationConstants.ACCESS_TYPE> getAccessTypes(JDOUserGroup userGroup,
			String resourceType, Long resourceId) throws NotFoundException, DatastoreException {
		Collection<AuthorizationConstants.ACCESS_TYPE> ans = new HashSet<AuthorizationConstants.ACCESS_TYPE>();
		for (JDOResourceAccess ra : userGroup.getResourceAccess()) {
			if (ra.getResourceType().equals(resourceType) && ra.getResourceId().equals(resourceId)) {
				ans = ra.getAccessTypeAsEnum();
				break;
			}
		}
		return ans;
	}
	
	
	// authorization queries (could go in another class)
	
	/**
	 * @return true iff the given user is in some group that has the given access to the given resource.
	 * This is a utility method, not exposed by a service, and has no authorization filter
	 * @throws NotFoundException if 'userId' is not a user in the system
	 */
	public static boolean canAccess(String userId, String resourceType, Long resourceId, AuthorizationConstants.ACCESS_TYPE accessType, PersistenceManager pm) throws NotFoundException, DatastoreException {
		// if the public can access the resource, then no need to check the user, just return true
		if(getAccessTypes(getPublicGroup(pm), resourceType, resourceId).contains(accessType)) return true;
		// if not publicly accessible, then we WILL have to check the user, so a null userId->false
		if (userId==null) return false;
		JDOUserDAOImpl userDAO = new JDOUserDAOImpl(userId);
		JDOUser user = userDAO.getUser(pm);
		if (user==null) throw new NotFoundException(userId+" does not exist");
		// if is an administrator, return true
		if (isAdmin(user, pm)) return true;
		// must look-up access
		Query query = pm.newQuery(JDOUserGroup.class);
		query.setFilter("resourceAccess.contains(vra) && "+
		//		query.setFilter("users.contains(pUser) && resourceAccess.contains(vra) && "+
				"vra.resourceType==pResourceType && vra.resourceId==pResourceId");
		query.declareVariables(JDOResourceAccess.class.getName()+" vra");
		query.declareParameters(/*Long.class.getName()+" pUser, "+*/
				String.class.getName()+" pResourceType, "+
				Long.class.getName() + " pResourceId");
		@SuppressWarnings("unchecked")
		Collection<JDOUserGroup> c = (Collection<JDOUserGroup>) query.
		//executeWithArray(new Object[]{user.getId(), resourceType, resourceId});
		executeWithArray(new Object[]{resourceType, resourceId});
		for (JDOUserGroup g: c) {
			if (!g.getUsers().contains(user.getId())) continue;
			for (JDOResourceAccess ra: g.getResourceAccess()) {
				if (ra.getResourceType().equals(resourceType) && ra.getResourceId().equals(resourceId)) {
					if (ra.getAccessTypeAsEnum().contains(accessType)) return true;
				}
			}
		}
		return false;
	}

	/**
	 * @return true iff the given user is in some group that can create the given object type.
	 * This is a utility method, not exposed by a service, and has no authorization filter
	 * @throws NotFoundException if 'userId' is not a user in the system
	 */
	@SuppressWarnings("rawtypes")
	public boolean canCreate(String userId, Class createableType, PersistenceManager pm) throws NotFoundException, DatastoreException {
		// if the public can access the resource, then no need to check the user, just return true
		JDOUserGroup publicGroup = getPublicGroup(pm);
		Set<String> publicCreatableTypes = publicGroup.getCreatableTypes();
		if(publicCreatableTypes.contains(createableType.getName())) return true;
		// if not publicly accessible, then we WILL have to check the user, so a null userId->false
		if (userId==null) return false;
		JDOUserDAOImpl userDAO = new JDOUserDAOImpl(userId);
		JDOUser user = userDAO.getUser(pm);
		if (user==null) throw new NotFoundException(userId+" does not exist");
		// must look-up access, allowing that admin's can access anything
		Query query = pm.newQuery(JDOUserGroup.class);
		query.setFilter("users.contains(pUser) && ((isSystemGroup && name==pAdminName) || "+
				"resourceAccess.contains(pCreateableType))");
		query.declareParameters(String.class.getName()+" pAdminName, "+String.class.getName()+" pCreateableType");
		@SuppressWarnings("unchecked")
		Collection<JDOUserGroup> c = (Collection<JDOUserGroup>) query.
			executeWithArray(new Object[]{user.getId(), AuthorizationConstants.ADMIN_GROUP_NAME, createableType.getName()});
		return c.size()>0;
	}
	
	private static Collection<JDOResourceAccess> getAccess(
			PersistenceManager pm, String resourceType, Long resourceKey, AuthorizationConstants.ACCESS_TYPE accessType) {
		Query query = pm.newQuery(JDOResourceAccess.class);
		query
				.setFilter("this.resourceType==pResourceType && this.resourceId==pResourceKey");
		query.declareParameters(
				String.class.getName() + " pResourceType, "+
				Long.class.getName() + " pResourceKey");
		@SuppressWarnings("unchecked")
		Collection<JDOResourceAccess> ras = (Collection<JDOResourceAccess>) query
				.execute(resourceType, resourceKey);
		Collection<JDOResourceAccess> ans = new HashSet<JDOResourceAccess>();
		for (JDOResourceAccess ra : ras) {
			if (ra.getAccessType().contains(accessType.name())) ans.add(ra);
		}
		return ans;
	}
	/**
	 * @return the user-groups that have the given access to the given resource
	 */
	public Collection<UserGroup> getAccessGroups(Base resource, AuthorizationConstants.ACCESS_TYPE accessType) throws DatastoreException, UnauthorizedException, NotFoundException {
		Collection<UserGroup> ans = new HashSet<UserGroup>();
		Long resourceKey = KeyFactory.stringToKey(resource.getId());
		String resourceType = getResourceType(resource);
		PersistenceManager pm = PMF.get();
		Collection<JDOResourceAccess> ras = getAccess(pm, resourceType, resourceKey,
				accessType);
		for (JDOResourceAccess ra : ras) {
			try {
				ans.add(get(KeyFactory.keyToString(ra.getOwner().getId())));
			} catch (UnauthorizedException ue) {
				// don't have read-access to the group, so skip it
			}
		}
		
		// now just add the Admin group, which can access anything
		UserGroup dto = newDTO();
		JDOUserGroup adminGroup = getAdminGroup(pm);
		copyToDto(adminGroup, dto);
		ans.add(dto);
		return ans;
	}
	
	public void setCreatableTypes(UserGroup userGroup, Collection<String> creatableTypes) throws NotFoundException, DatastoreException {
		PersistenceManager pm = PMF.get();
		Long key = KeyFactory.stringToKey(userGroup.getId());
		try {
			JDOUserGroup jdo = (JDOUserGroup) pm.getObjectById(getJdoClass(), key);
			jdo.setCreatableTypes(new HashSet<String>(creatableTypes));
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}

	}

}
