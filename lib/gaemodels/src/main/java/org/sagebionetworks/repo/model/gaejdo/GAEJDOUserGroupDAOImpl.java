package org.sagebionetworks.repo.model.gaejdo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class GAEJDOUserGroupDAOImpl extends
		GAEJDOBaseDAOImpl<UserGroup, GAEJDOUserGroup> implements UserGroupDAO {
	
	public static final String READ_ACCESS = "read";
	public static final String CHANGE_ACCESS = "change";
	public static final String SHARE_ACCESS = "share";
	
	
	public GAEJDOUserGroupDAOImpl(String userId) {super(userId);}
	
	// the group name for a system defined group which allows access to its
	// resources to all (including anonymous users)
	public static final String PUBLIC_GROUP_NAME = "Public";
	
	public static boolean isPublicGroup(GAEJDOUserGroup g) {
		return g.getIsSystemGroup() && PUBLIC_GROUP_NAME.equals(g.getName());
	}
	
	/**
	 * Create a default Public Group.  By default, everyone is allowed to
	 * create g users and groups.  This is necessary to bootstrap the system,
	 * after which permissions can be locked down.
	 * 
	 * @return
	 */
	public GAEJDOUserGroup createPublicGroup(PersistenceManager pm) {
		GAEJDOUserGroup g = newJDO();
		g.setName(PUBLIC_GROUP_NAME);
		g.setCreationDate(new Date());
		g.setIsSystemGroup(true);
		g.setIsIndividual(false);
		Set<String> creatableTypes = g.getCreatableTypes();
		creatableTypes.add(GAEJDOUser.class.getName());
		creatableTypes.add(GAEJDOUserGroup.class.getName());
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			pm.makePersistent(g);
			// now give the public access to this group
			addResourceToGroup(g, g.getId(), Arrays.asList(new String[]{READ_ACCESS, CHANGE_ACCESS, SHARE_ACCESS}));
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
		}
		return g;
	}
	
	public static void addResourceToGroup(GAEJDOUserGroup group, Key resource, List<String> accessTypes) {
		if (resource==null) throw new NullPointerException();
		Set<GAEJDOResourceAccess> ras = group.getResourceAccess();
		for (String accessType: accessTypes) {
			GAEJDOResourceAccess ra = new GAEJDOResourceAccess();
			ra.setResource(resource); 
			ra.setAccessType(accessType);
			ras.add(ra);
		}
	}
	
	/**
	 * This is the externally facing method.  Not sure if it's really needed.
	 */
	public UserGroup getPublicGroup() throws NotFoundException, DatastoreException {
		// TODO
		throw new RuntimeException("Not yet implemented");
	}

	public static GAEJDOUserGroup getPublicGroup(PersistenceManager pm) {
		Query query = pm.newQuery(GAEJDOUserGroup.class);
		query.setFilter("isSystemGroup==true && name==\""+PUBLIC_GROUP_NAME+"\"");
		@SuppressWarnings("unchecked")
		Collection<GAEJDOUserGroup> ans = (Collection<GAEJDOUserGroup>)query.execute();
		if (ans.size()>1) throw new IllegalStateException("Expected 0-1 but found "+ans.size());
		if (ans.size()==0) return null;
		return ans.iterator().next();
	}
	
	/**
	 * There must be one public group. This method returns it if it exists, and creates one if it doesn't
	 * @param pm
	 * @return
	 */
	public GAEJDOUserGroup getOrCreatePublicGroup(PersistenceManager pm) {
		// get the Public group
		GAEJDOUserGroup group = getPublicGroup(pm);
		if (/*public group doesn't exist*/null==group) {
			// create a Public group
			group = createPublicGroup(pm);
		}
		return group;
	}
	
	/**
	 * Create a group for a particular user.  Give the user READ and CHANGE 
	 * access to their own group.
	 * @param pm
	 * @return
	 */
	public GAEJDOUserGroup createIndividualGroup(PersistenceManager pm, GAEJDOUser user) {
		GAEJDOUserGroup g = newJDO();
		g.setName(user.getUserId());
		g.setCreationDate(new Date());
		g.setIsSystemGroup(true);
		g.setIsIndividual(true);
		Set<String> creatableTypes = g.getCreatableTypes();
		creatableTypes.add(GAEJDOUser.class.getName());
		creatableTypes.add(GAEJDOUserGroup.class.getName());
		g.getUsers().add(user.getId());
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			pm.makePersistent(g);
			// give the group total access to the created group itself.
			addResourceToGroup(g, g.getId(), Arrays.asList(new String[]{READ_ACCESS, CHANGE_ACCESS, SHARE_ACCESS}));
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
		}			
		return g;
	}
	
	public GAEJDOUserGroup getIndividualGroup(PersistenceManager pm) {
		if (null==userId) return null;
		Query query = pm.newQuery(GAEJDOUserGroup.class);
		query.setFilter("isSystemGroup==true && name==pName && isIndividual==true");
		query.declareParameters(String.class.getName()+" pName");
		@SuppressWarnings("unchecked")
		Collection<GAEJDOUserGroup> ans = (Collection<GAEJDOUserGroup>)query.execute(userId);
		if (ans.size()>1) throw new IllegalStateException("Expected 0-1 but found "+ans.size());
		if (ans.size()==0) return null;
		return ans.iterator().next();
	}
	
	public GAEJDOUserGroup getOrCreateIndividualGroup(PersistenceManager pm) {
		if (null==userId) throw new NullPointerException();
		//get the individual group
		GAEJDOUserGroup group = getIndividualGroup(pm);
		if (/*individual group doesn't exist*/null==group) {
			// create an Individual group
			GAEJDOUser user = (new GAEJDOUserDAOImpl(userId)).getUser(pm);
			group = createIndividualGroup(pm, user);
		}
		return group;
	}
	

	protected UserGroup newDTO() {
		return new UserGroup();
	}

	protected GAEJDOUserGroup newJDO() {
		GAEJDOUserGroup g = new GAEJDOUserGroup();
		g.setUsers(new HashSet<Key>());
		g.setResourceAccess(new HashSet<GAEJDOResourceAccess>());
		g.setCreatableTypes(new HashSet<String>());
		return g;
	}

	protected void copyToDto(GAEJDOUserGroup jdo, UserGroup dto)
			throws DatastoreException {
		dto.setId(jdo.getId() == null ? null : KeyFactory.keyToString(jdo
				.getId()));
		dto.setCreationDate(jdo.getCreationDate());
		dto.setName(jdo.getName());
	}

	protected void copyFromDto(UserGroup dto, GAEJDOUserGroup jdo)
			throws InvalidModelException {
		jdo.setName(dto.getName());
		jdo.setCreationDate(dto.getCreationDate());
	}

	protected Class getJdoClass() {
		return GAEJDOUserGroup.class;
	}

	public Collection<String> getPrimaryFields() {
		return Arrays.asList(new String[] { "name" });
	}

	public void addUser(UserGroup userGroup, User user) throws NotFoundException, DatastoreException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		if (!hasAccessIntern(pm, KeyFactory.stringToKey(userGroup.getId()), CHANGE_ACCESS)) throw new UnauthorizedException();
		Transaction tx = null;
		try {
			Key userKey = KeyFactory.stringToKey(user.getId());
			// this is done simply to make check that the user exists
			GAEJDOUser jdoUser = (GAEJDOUser) pm.getObjectById(GAEJDOUser.class, userKey);
			Key groupKey = KeyFactory.stringToKey(userGroup.getId());
			GAEJDOUserGroup jdoGroup = (GAEJDOUserGroup) pm.getObjectById(GAEJDOUserGroup.class, groupKey);
			tx = pm.currentTransaction();
			tx.begin();
			jdoGroup.getUsers().add(userKey);
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

	public void removeUser(UserGroup userGroup, User user) throws NotFoundException, DatastoreException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Key userKey = KeyFactory.stringToKey(user.getId());
			// this is done simply to make check that the user exists
			GAEJDOUser jdoUser = (GAEJDOUser) pm.getObjectById(GAEJDOUser.class, userKey);
			Key groupKey = KeyFactory.stringToKey(userGroup.getId());
			GAEJDOUserGroup jdoGroup = (GAEJDOUserGroup) pm.getObjectById(GAEJDOUserGroup.class, groupKey);
			if (!hasAccessIntern(pm, jdoGroup.getId(), CHANGE_ACCESS)) throw new UnauthorizedException();
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

	public Collection<User> getUsers(UserGroup userGroup) throws NotFoundException, DatastoreException, UnauthorizedException{
		PersistenceManager pm = PMF.get();
		try {
			Key groupKey = KeyFactory.stringToKey(userGroup.getId());
			GAEJDOUserGroup jdoGroup = (GAEJDOUserGroup) pm.getObjectById(GAEJDOUserGroup.class, groupKey);
			if (!hasAccessIntern(pm, jdoGroup.getId(), READ_ACCESS)) throw new UnauthorizedException();
			Collection<Key> userKeys = jdoGroup.getUsers();
			GAEJDOUserDAOImpl userDAO = new GAEJDOUserDAOImpl(userId);
			Collection<User> ans = new HashSet<User>();
			for (Key userKey : userKeys) {
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

	public void addResource(UserGroup userGroup, String resourceId,
			String accessType)  throws NotFoundException, DatastoreException, UnauthorizedException{
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			Key resourceKey = KeyFactory.stringToKey(resourceId);
			if (!hasAccessIntern(pm, resourceKey, SHARE_ACCESS)) throw new UnauthorizedException();
			Key groupKey = KeyFactory.stringToKey(userGroup.getId());
			GAEJDOUserGroup jdoGroup = (GAEJDOUserGroup) pm.getObjectById(GAEJDOUserGroup.class, groupKey);
			if (!hasAccessIntern(pm, jdoGroup.getId(), CHANGE_ACCESS)) throw new UnauthorizedException();
			tx = pm.currentTransaction();
			tx.begin();
			GAEJDOResourceAccess ra = new GAEJDOResourceAccess();
			ra.setResource(resourceKey);
			ra.setAccessType(accessType);
			// TODO make sure it's not a duplicate
			jdoGroup.getResourceAccess().add(ra);
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

	public void removeResource(UserGroup userGroup, String resourceId,
			String accessType)  throws NotFoundException, DatastoreException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			Key resourceKey = KeyFactory.stringToKey(resourceId);
			Key groupKey = KeyFactory.stringToKey(userGroup.getId());
			GAEJDOUserGroup jdoGroup = (GAEJDOUserGroup) pm.getObjectById(GAEJDOUserGroup.class, groupKey);
			if (!hasAccessIntern(pm, jdoGroup.getId(), CHANGE_ACCESS)) throw new UnauthorizedException();
			Collection<GAEJDOResourceAccess> ras = jdoGroup.getResourceAccess();
			tx = pm.currentTransaction();
			tx.begin();
			for (GAEJDOResourceAccess ra : ras) {
				if (ra.getResource().equals(resourceKey) && ra.getAccessType().equals(accessType)) {
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

	public Collection<String> getResources(UserGroup userGroup)  throws NotFoundException, DatastoreException, UnauthorizedException {
		// TODO
		throw new RuntimeException("Not yet implemented");
	}

	public Collection<String> getResources(UserGroup userGroup,
			String accessType)  throws NotFoundException, DatastoreException, UnauthorizedException {
		// TODO
		throw new RuntimeException("Not yet implemented");
	}

	public Collection<String> getAccessTypes(UserGroup userGroup,
			String resourceId)  throws NotFoundException, DatastoreException, UnauthorizedException {
		// TODO
		throw new RuntimeException("Not yet implemented");
	}


}
