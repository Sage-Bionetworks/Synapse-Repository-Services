package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
				Arrays.asList(new String[] {
						AuthorizationConstants.READ_ACCESS, 
						AuthorizationConstants.CHANGE_ACCESS}),

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
				Arrays.asList(new String[] {
						AuthorizationConstants.READ_ACCESS}),
				creatableTypes,  // for admin' group, don't have to explicitly declare types, rather anything can be created
				pm);
	}
	
	public JDOUserGroup createSystemGroup(String name, 
			boolean isIndividualGroup, 
			Collection<String> selfAccess, // the type of access members have on the group itself
			Set<String> creatableTypes,
			PersistenceManager pm) {
		JDOUserGroup g = newJDO();
		g.setName(name);
		g.setCreationDate(new Date());
		g.setIsSystemGroup(true);
		g.setIsIndividual(isIndividualGroup);
		g.getCreatableTypes().addAll(creatableTypes);
		g.setResourceAccess(new HashSet<JDOResourceAccess>());
		g.setUsers(new HashSet<Long>());
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			pm.makePersistent(g);
			// now give the group members read-access to the group itself
			addResourceToGroup(g, g, selfAccess);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
		}
		return g;
	}

	public static void addResourceToGroup(JDOUserGroup group, JDOBase resource,
			Collection<String> accessTypes) {
		if (resource == null)
			throw new NullPointerException();
		Set<JDOResourceAccess> ras = group.getResourceAccess();
		for (String accessType : accessTypes) {
			JDOResourceAccess ra = new JDOResourceAccess();
			ra.setResourceType(resource.getClass().getName());
			ra.setResourceId(resource.getId());
			ra.setAccessType(accessType);
			ras.add(ra);
		}
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
	 * Create a group for a particular user. Give the user READ and CHANGE
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
				Arrays.asList(new String[] {
						AuthorizationConstants.READ_ACCESS}),
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
		return new UserGroup();
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
	}

	protected void copyFromDto(UserGroup dto, JDOUserGroup jdo)
			throws InvalidModelException {
		jdo.setName(dto.getName());
		jdo.setCreationDate(dto.getCreationDate());
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
		if (!canAccess(userId, getJdoClass().getName(), KeyFactory.stringToKey(userGroup.getId()), AuthorizationConstants.CHANGE_ACCESS, pm))
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
			jdoGroup.getUsers().add(jdoUser.getId());
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
			if (!canAccess(userId, getJdoClass().getName(), jdoGroup.getId(), AuthorizationConstants.CHANGE_ACCESS, pm))
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

	public Collection<User> getUsers(UserGroup userGroup)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		try {
			Long groupKey = KeyFactory.stringToKey(userGroup.getId());
			JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
					JDOUserGroup.class, groupKey);
			if (!canAccess(userId, getJdoClass().getName(), jdoGroup.getId(), AuthorizationConstants.READ_ACCESS, pm))
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

	public void addResource(UserGroup userGroup, Base resource,
			String accessType) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Long resourceKey = KeyFactory.stringToKey(resource.getId());
		if (!canAccess(userId, getResourceType(resource), resourceKey, AuthorizationConstants.SHARE_ACCESS, pm))
			throw new UnauthorizedException();
		Long groupKey = KeyFactory.stringToKey(userGroup.getId());
		JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
				JDOUserGroup.class, groupKey);
		if (!canAccess(userId, getJdoClass().getName(), jdoGroup.getId(), AuthorizationConstants.CHANGE_ACCESS, pm))
			throw new UnauthorizedException();

		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			JDOResourceAccess ra = new JDOResourceAccess();
			ra.setResourceType(getResourceType(resource));
			ra.setResourceId(resourceKey);
			ra.setAccessType(accessType);
			// TODO make sure it's not a duplicate
			jdoGroup.getResourceAccess().add(ra);
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
//		} catch (UnauthorizedException e) {
//			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public void removeResource(UserGroup userGroup, Base resource,
			String accessType) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			Long resourceKey = KeyFactory.stringToKey(resource.getId());
			if (!canAccess(userId, getResourceType(resource), resourceKey, AuthorizationConstants.SHARE_ACCESS, pm))
				throw new UnauthorizedException();
			Long groupKey = KeyFactory.stringToKey(userGroup.getId());
			JDOUserGroup jdoGroup = (JDOUserGroup) pm.getObjectById(
					JDOUserGroup.class, groupKey);
			if (!canAccess(userId, getJdoClass().getName(), jdoGroup.getId(), AuthorizationConstants.CHANGE_ACCESS, pm))
				throw new UnauthorizedException();
			Collection<JDOResourceAccess> ras = jdoGroup.getResourceAccess();
			tx = pm.currentTransaction();
			tx.begin();
			for (JDOResourceAccess ra : ras) {
				if (ra.getResourceType().equals(getResourceType(resource)) && 
						ra.getResourceId().equals(resourceKey)
						&& ra.getAccessType().equals(accessType)) {
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

//	public Collection<String> getResources(UserGroup userGroup)
//			throws NotFoundException, DatastoreException, UnauthorizedException {
//		throw new RuntimeException("Not yet implemented");
//	}
//
//	public Collection<String> getResources(UserGroup userGroup,
//			String accessType) throws NotFoundException, DatastoreException,
//			UnauthorizedException {
//		throw new RuntimeException("Not yet implemented");
//	}
//
//	public Collection<String> getAccessTypes(UserGroup userGroup, String resourceType,
//			String resourceId) throws NotFoundException, DatastoreException,
//			UnauthorizedException {
//		PersistenceManager pm = PMF.get();
//		Long rId = Long.parseLong(userGroup.getId());
//		if (!canAccess(userId, rId, AuthorizationConstants.READ_ACCESS, pm)) throw new UnauthorizedException();
//		JDOUserGroup group = pm.getObjectById(JDOUserGroup.class, userGroup.getId());
//		return getAccessTypes(group, resourceType, rId);
//	}
	
	// the internal method, with no authorization filter
	private static Collection<String> getAccessTypes(JDOUserGroup userGroup,
			String resourceType, Long resourceId) throws NotFoundException, DatastoreException {
		Collection<String> ans = new HashSet<String>();
		for (JDOResourceAccess ra : userGroup.getResourceAccess()) {
			if (ra.getResourceType().equals(resourceType) && ra.getResourceId().equals(resourceId)) ans.add(ra.getAccessType());
		}
		return ans;
	}
	
	
	// authorization queries (could go in another class)
	
	/**
	 * @return true iff the given user is in some group that has the given access to the given resource.
	 * This is a utility method, not exposed by a service, and has no authorization filter
	 * @throws NotFoundException if 'userId' is not a user in the system
	 */
	public static boolean canAccess(String userId, String resourceType, Long resourceId, String accessType, PersistenceManager pm) throws NotFoundException, DatastoreException {
		// if the public can access the resource, then no need to check the user, just return true
		if(getAccessTypes(getPublicGroup(pm), resourceType, resourceId).contains(accessType)) return true;
		// if not publicly accessible, then we WILL have to check the user, so a null userId->false
		if (userId==null) return false;
		JDOUserDAOImpl userDAO = new JDOUserDAOImpl(userId);
		JDOUser user = userDAO.getUser(pm);
		if (user==null) throw new NotFoundException(userId+" does not exist");
		// must look-up access, allowing that admin's can access anything
		Query query = pm.newQuery(JDOUserGroup.class);
		query.setFilter("users.contains(pUser) && ((isSystemGroup==true && name==pAdminName) || "+
				"(resourceAccess.contains(vra) && vra.resourceType==pResourceType && vra.resourceId==pResourceId && vra.accessType==pAccessType))");
		query.declareVariables(JDOResourceAccess.class.getName()+" vra");
		query.declareParameters(Long.class.getName()+" pUser, "+
				String.class.getName()+" pAdminName, "+
				String.class.getName()+" pResourceType, "+
				Long.class.getName() + " pResourceId, "+
				String.class.getName()+" pAccessType");
		@SuppressWarnings("unchecked")
		Collection<JDOUserGroup> c = (Collection<JDOUserGroup>) query.
		executeWithArray(new Object[]{user.getId(), AuthorizationConstants.ADMIN_GROUP_NAME, resourceType, resourceId, accessType});
		return c.size()>0;
	}

	/**
	 * @return true iff the given user is in some group that can create the given object type.
	 * This is a utility method, not exposed by a service, and has no authorization filter
	 * @throws NotFoundException if 'userId' is not a user in the system
	 */
	@SuppressWarnings("rawtypes")
	public boolean canCreate(String userId, Class createableType, PersistenceManager pm) throws NotFoundException, DatastoreException {
		// if the public can access the resource, then no need to check the user, just return true
		if(getPublicGroup(pm).getCreatableTypes().contains(createableType.getName())) return true;
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
			PersistenceManager pm, String resourceType, Long resourceKey, String accessType) {
		Query query = pm.newQuery(JDOResourceAccess.class);
		query
				.setFilter("this.resourceType==pResourceType && this.resourceId==pResourceKey && this.accessType==pAccessType");
		query.declareParameters(
				String.class.getName() + " pResourceType, "+
				Long.class.getName() + " pResourceKey, "
				+ String.class.getName() + " pAccessType");
		// query.setFilter("accessType==pAccessType");
		// query.declareParameters(String.class+" pAccessType");
		@SuppressWarnings("unchecked")
		Collection<JDOResourceAccess> ras = (Collection<JDOResourceAccess>) query
				.execute(resourceType, resourceKey, accessType);
		return ras;
	}
	/**
	 * @return the user-groups that have the given access to the given resource
	 */
	public Collection<UserGroup> getAccessGroups(Base resource, String accessType) throws DatastoreException, UnauthorizedException, NotFoundException {
		Collection<UserGroup> ans = new HashSet<UserGroup>();
		Long resourceKey = KeyFactory.stringToKey(resource.getId());
		String resourceType = getResourceType(resource);
		PersistenceManager pm = PMF.get();
		Collection<JDOResourceAccess> ras = getAccess(pm, resourceType, resourceKey,
				accessType);
		for (JDOResourceAccess ra : ras) {
			ans.add(get(KeyFactory.keyToString(ra.getOwner().getId())));
		}
		
		// now just add the Admin group, which can access anything
		UserGroup dto = newDTO();
		JDOUserGroup adminGroup = getAdminGroup(pm);
		copyToDto(adminGroup, dto);
		ans.add(dto);
		return ans;
	}
}
