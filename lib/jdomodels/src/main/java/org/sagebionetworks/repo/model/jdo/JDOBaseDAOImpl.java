package org.sagebionetworks.repo.model.jdo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.web.NotFoundException;




/**
 * This class contains helper methods for DAOs. Since each DAO may need to pick
 * and choose methods from various helpers, the chosen design pattern for the
 * DAOs was that of a wrapper or adapter, rather than of a base class with
 * extensions.
 * 
 * This class is parameterized by an (implementation independent) DTO type and a
 * JDO specific JDO type. It's the DAO's job to translate between these types as
 * it persists and retrieves data.
 * 
 * @author bhoff
 * 
 * @param <S>
 *            the DTO class
 * @param <T>
 *            the JDO class
 */
abstract public class JDOBaseDAOImpl<S extends Base, T extends JDOBase>
		implements BaseDAO<S> {

	protected String userId; // the id of the user performing the DAO
								// operations;

	public JDOBaseDAOImpl(String userId) {
		this.userId = userId;
	}

	/**
	 * Create a new instance of the data transfer object. Introducing this
	 * abstract method helps us avoid making assumptions about constructors.
	 * 
	 * @return the new object
	 */
	abstract protected S newDTO();

	/**
	 * Create a new instance of the persistable object. Introducing this
	 * abstract method helps us avoid making assumptions about constructors.
	 * 
	 * @return the new object
	 */
	abstract protected T newJDO();

	/**
	 * Do a shallow copy from the JDO object to the DTO object.
	 * 
	 * @param jdo
	 * @param dto
	 * @throws DatastoreException
	 */
	abstract protected void copyToDto(T jdo, S dto) throws DatastoreException;

	/**
	 * Do a shallow copy from the DTO object to the JDO object.
	 * 
	 * @param dto
	 * @param jdo
	 * @throws InvalidModelException
	 */
	abstract protected void copyFromDto(S dto, T jdo)
			throws InvalidModelException;

	/**
	 * @param jdoClass
	 *            the class parameterized by T
	 */
	abstract protected Class<T> getJdoClass();

	/**
	 * Create a clone of the given object in memory (no datastore operations)
	 * Extensions of this class can go as deep as needed in copying data to
	 * create a clone
	 * 
	 * @param jdo
	 *            the object to clone
	 * @return the clone
	 * @throws DatastoreException
	 */
	protected T cloneJdo(T jdo) throws DatastoreException {
		S dto = newDTO();

		// TODO this assumes that all DTOs reflect the contents of the JDO (a
		// one-to-one mapping), since this may not always be the case it would
		// be better to implement jdo.clone()
		copyToDto(jdo, dto);
		T clone = newJDO();
		try {
			copyFromDto(dto, clone);
		} catch (InvalidModelException ime) {
			// better not, the content just came from a jdo!
			throw new IllegalStateException(ime);
		}

		return clone;
	}

	// /**
	// * take care of any work that has to be done before deleting the
	// persistent
	// * object but within the same transaction (for example, deleteing objects
	// * which this object composes, but which are not represented by owned
	// * relationships)
	// *
	// * @param pm
	// * @param jdo
	// * the object to be deleted
	// */
	// protected void preDelete(PersistenceManager pm, T jdo) {
	// // for the base DAO, nothing needs to be done
	// }

	// /**
	// * This may be overridden by subclasses to generate the
	// * object's key. Returning null causes the system to
	// * generate the key itself.
	// * @return the key for a new object, or null if none
	// */
	// protected Long generateKey(PersistenceManager pm) throws
	// DatastoreException {
	// return null;
	// }

	// /**
	// * take care of any work that has to be done after creating the persistent
	// * object but within the same transaction
	// *
	// * @param pm
	// * @param jdo
	// */
	// protected void postCreate(PersistenceManager pm, T jdo) {
	// // for the base DAO, nothing needs to be done
	// }

	protected T createIntern(S dto) throws InvalidModelException,
			DatastoreException {
		T jdo = newJDO();
		copyFromDto(dto, jdo);
		return jdo;
	}

	/**
	 * Add access to the given (newly created) object. If the user==null then
	 * make the object publicly accessible.
	 * 
	 * @param pm
	 * @param jdo
	 */
	protected void addUserAccess(PersistenceManager pm, T jdo) {
		JDOUserGroupDAOImpl groupDAO = new JDOUserGroupDAOImpl(userId);
		JDOUserGroup group = null;
		if (userId == null) {
			group = groupDAO.getOrCreatePublicGroup(pm);

		} else {
			group = groupDAO.getOrCreateIndividualGroup(pm);
		}
		// System.out.println("addUserAccess: Group is "+group.getName());
		// now add the object to the group
		JDOUserGroupDAOImpl.addResourceToGroup(group, jdo.getId(), Arrays
				.asList(new String[] { AuthorizationConstants.READ_ACCESS,
						AuthorizationConstants.CHANGE_ACCESS,
						AuthorizationConstants.SHARE_ACCESS }));
	}

	/**
	 * Create a new object, using the information in the passed DTO
	 * 
	 * @param dto
	 * @return the ID of the created object
	 * @throws InvalidModelException
	 */
	public String create(S dto) throws InvalidModelException,
			DatastoreException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		if (!canCreate(pm))
			throw new UnauthorizedException(
					"Cannot create objects of this type.");
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			T jdo = createIntern(dto);
			pm.makePersistent(jdo);
			tx.commit();
			tx = pm.currentTransaction();
			tx.begin();
			addUserAccess(pm, jdo); // TODO Am I do the transaction control
									// correctly?
			tx.commit();
			copyToDto(jdo, dto);
			dto.setId(KeyFactory.keyToString(jdo.getId())); // TODO Consider
															// putting this line
															// in 'copyToDto'
			return KeyFactory.keyToString(jdo.getId());
		} catch (InvalidModelException ime) {
			throw ime;
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
	 * 
	 * @param id
	 *            id of the object to be retrieved
	 * @return the DTO version of the retrieved object
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public S get(String id) throws DatastoreException, NotFoundException,
			UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Long key = KeyFactory.stringToKey(id);
		if (!hasAccessIntern(pm, key, AuthorizationConstants.READ_ACCESS))
			throw new UnauthorizedException();
		try {
			T jdo = (T) pm.getObjectById(getJdoClass(), key);
			S dto = newDTO();
			copyToDto(jdo, dto);
			return dto;
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	// // sometimes we need to delete from within another transaction
	// public void delete(PersistenceManager pm, T jdo) {
	// preDelete(pm, jdo);
	// pm.deletePersistent(jdo);
	// }

	/**
	 * Delete the specified object
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException,
			UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Long key = KeyFactory.stringToKey(id);
		if (!hasAccessIntern(pm, key, AuthorizationConstants.CHANGE_ACCESS))
			throw new UnauthorizedException();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			T jdo = (T) pm.getObjectById(getJdoClass(), key);
			// delete(pm, jdo);
			pm.deletePersistent(jdo);
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
	 * This updates the 'shallow' properties. Version doesn't change.
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 *             if version in dto doesn't match version of object
	 * @throws InvalidModelException
	 */
	public void update(PersistenceManager pm, S dto) throws DatastoreException,
			InvalidModelException, NotFoundException, UnauthorizedException {
		if (dto.getId() == null)
			throw new InvalidModelException("id is null");
		Long id = KeyFactory.stringToKey(dto.getId());
		T jdo = (T) pm.getObjectById(getJdoClass(), id);
		copyFromDto(dto, jdo);
		pm.makePersistent(jdo);
	}

	public void update(S dto) throws DatastoreException, InvalidModelException,
			NotFoundException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		// *** NOTE, if you try to do this within the transaction, below, it
		// breaks!!
		Long id = KeyFactory.stringToKey(dto.getId());
		if (!hasAccessIntern(pm, id, AuthorizationConstants.CHANGE_ACCESS))
			throw new UnauthorizedException();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			update(pm, dto);
			tx.commit();
		} catch (InvalidModelException ime) {
			throw ime;
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
	 * 
	 * returns the number of objects of a certain type
	 * 
	 */
	protected int getCount(PersistenceManager pm) throws DatastoreException {
		Query query = pm.newQuery(getJdoClass());
		@SuppressWarnings("unchecked")
		Collection<T> c = (Collection<T>) query.execute();
		Collection<Long> keys = new HashSet<Long>();
		for (T elem : c)
			keys.add(elem.getId());
		// System.out.println("JDOBaseDAOImpl.getCount: keys "+keys);
		Collection<Long> canAccess = getCanAccess(pm,
				AuthorizationConstants.READ_ACCESS);
		// System.out.println("JDOBaseDAOImpl.getCount: canAccess "+canAccess);
		keys.retainAll(canAccess);
		return keys.size();
	}

	public int getCount() throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			int count = getCount(pm);
			return count;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * Retrieve all objects of the given type, 'paginated' by the given start
	 * and end
	 * 
	 * @param start
	 * @param end
	 * @return a subset of the results, starting at index 'start' and not going
	 *         beyond index 'end'
	 */
	public List<S> getInRange(int start, int end) throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			Query query = pm.newQuery(getJdoClass());
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>) query.execute());
			Collection<Long> canAccess = getCanAccess(pm,
					AuthorizationConstants.READ_ACCESS);
			List<S> ans = new ArrayList<S>();
			int count = 0;
			for (T jdo : list) {
				if (canAccess.contains(jdo.getId())) {
					if (count >= start && count < end) {
						S dto = newDTO();
						copyToDto(jdo, dto);
						ans.add(dto);
					}
					count++;
				}
			}
			return ans;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * Retrieve all objects of the given type, 'paginated' by the given start
	 * and end and sorted by the specified primary field
	 * 
	 * @param start
	 * @param end
	 * @param sortBy
	 * @param asc
	 *            if true then ascending, else descending
	 * @return a subset of the results, starting at index 'start' and not going
	 *         beyond index 'end' and sorted by the given primary field
	 */
	public List<S> getInRangeSortedByPrimaryField(int start, int end,
			String sortBy, boolean asc) throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			Query query = pm.newQuery(getJdoClass());
			query.setOrdering(sortBy + (asc ? " ascending" : " descending"));
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>) query.execute());
			Collection<Long> canAccess = getCanAccess(pm,
					AuthorizationConstants.READ_ACCESS);
			List<S> ans = new ArrayList<S>();
			int count = 0;
			for (T jdo : list) {
				if (canAccess.contains(jdo.getId())) {
					if (count >= start && count < end) {
						S dto = newDTO();
						copyToDto(jdo, dto);
						ans.add(dto);
					}
					count++;
				}
			}
			return ans;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * Get the objects of the given type having the specified value in the given
	 * primary field, and 'paginated' by the given start/end limits
	 * 
	 * @param start
	 * @param end
	 * @param attribute
	 *            the name of the primary field
	 * @param value
	 * @return
	 */
	public List<S> getInRangeHavingPrimaryField(int start, int end,
			String attribute, Object value) throws DatastoreException {
		PersistenceManager pm = null;
		try {
			pm = PMF.get();
			Query query = pm.newQuery(getJdoClass());
			// query.setRange(start, end);
			query.setFilter(attribute + "==pValue");
			query.declareParameters(value.getClass().getName() + " pValue");
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>) query.execute(value));
			Collection<Long> canAccess = getCanAccess(pm,
					AuthorizationConstants.READ_ACCESS);
			List<S> ans = new ArrayList<S>();
			int count = 0;
			for (T jdo : list) {
				if (canAccess.contains(jdo.getId())) {
					if (count >= start && count < end) {
						S dto = newDTO();
						copyToDto(jdo, dto);
						ans.add(dto);
					}
					count++;
				}
			}
			return ans;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * 
	 * @return the user credentials under which the DAO operations are being
	 *         performed, or 'null' if anonymous
	 */
	// protected JDOUser getUser(PersistenceManager pm) {
	// if (this.userId==null) return null;
	// Query query = pm.newQuery
	// return (JDOUser) pm.getObjectById(JDOUser.class,
	// KeyFactory.stringToKey(userId));
	// }

	private static Collection<JDOResourceAccess> getAccess(
			PersistenceManager pm, Long resourceKey, String accessType) {
		Query query = pm.newQuery(JDOResourceAccess.class);
		query
				.setFilter("this.resource==pResourceKey && this.accessType==pAccessType");
		query.declareParameters(Long.class.getName() + " pResourceKey, "
				+ String.class.getName() + " pAccessType");
		// query.setFilter("accessType==pAccessType");
		// query.declareParameters(String.class+" pAccessType");
		@SuppressWarnings("unchecked")
		Collection<JDOResourceAccess> ras = (Collection<JDOResourceAccess>) query
				.execute(resourceKey, accessType);
		return ras;
	}
	
	public void dumpAllAccess() throws Exception {
		PersistenceManager pm = PMF.get();
		Query query = pm.newQuery(JDOResourceAccess.class);
		@SuppressWarnings("unchecked")
		Collection<JDOResourceAccess> ras = (Collection<JDOResourceAccess>) query
		.execute();
		for (JDOResourceAccess ra: ras) {
			System.out.println("Owner: "+ra.getOwner()+" Resource: "+ra.getResource()+" type: "+ra.getAccessType());
		}
	}

	public Collection<UserGroup> whoHasAccess(String id, String accessType)
			throws NotFoundException, DatastoreException {
		// search for all JDOResourceAccess objects having the given object
		// and access type
		// return a collection of the user groups
		PersistenceManager pm = PMF.get();
		try {
			JDOUserGroupDAOImpl groupDAO = new JDOUserGroupDAOImpl(userId);
			Collection<UserGroup> ans = new HashSet<UserGroup>();
			Long resourceKey = KeyFactory.stringToKey(id);
			Collection<JDOResourceAccess> ras = getAccess(pm, resourceKey,
					accessType);
			for (JDOResourceAccess ra : ras) {
				ans.add(groupDAO.get(KeyFactory.keyToString(ra.getOwner()
						.getId())));
			}
			return ans;
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	protected boolean canCreate(PersistenceManager pm) {
		JDOUserGroupDAOImpl groupDAO = new JDOUserGroupDAOImpl(userId);
		JDOUserGroup group = null;
		if (userId == null) {
			group = groupDAO.getOrCreatePublicGroup(pm);
		} else {
			group = groupDAO.getOrCreateIndividualGroup(pm);
		}
		return group.getCreatableTypes().contains(getJdoClass().getName());
	}

	protected boolean hasAccessIntern(PersistenceManager pm, Long resourceKey,
			String accessType) {
		JDOUser thisUser = (new JDOUserDAOImpl(userId)).getUser(pm);
		Collection<JDOResourceAccess> ras = getAccess(pm, resourceKey,
				accessType);
		// System.out.println("JDOBaseDAOImpl.hasAccessIntern: ras.size()=="+ras.size());
		for (JDOResourceAccess ra : ras) {
			JDOUserGroup group = ra.getOwner();
			// System.out.println("JDOBaseDAOImpl.hasAccessIntern: \tGroup "+group.getName()+" has "+group.getUsers().size()+" users.");
			// for (Long u : group.getUsers()) System.out.print(u+", ");
			// System.out.println();
			if (JDOUserGroupDAOImpl.isPublicGroup(group)
					|| (thisUser != null && group.getUsers().contains(
							thisUser.getId())))
				return true;
		}
		return false;
	}

	public boolean hasAccess(String resourceId, String accessType)
			throws NotFoundException, DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			Long resourceKey = KeyFactory.stringToKey(resourceId);
			return hasAccessIntern(pm, resourceKey, accessType);
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * TODO this used to be implemented via Google Appengine Datastore JDO.  That means
	 * that joins of any complexity had to be done in
	 * memory. We can now update this code to push that logic back into the relational database.
	 * 
	 * @return all objects in the system that the user can access with the given
	 *         accesstype
	 */
	public Collection<Long> getCanAccess(PersistenceManager pm, String accessType) {
		// find all the groups the user is a member of
		Collection<JDOUserGroup> groups = new HashSet<JDOUserGroup>();
		if (userId != null) {
			JDOUser user = (new JDOUserDAOImpl(userId)).getUser(pm);
			Query query = pm.newQuery(JDOUserGroup.class);
			query.setFilter("users.contains(pUser)");
			query.declareParameters(Long.class + " pUser");
			@SuppressWarnings("unchecked")
			Collection<JDOUserGroup> c = (Collection<JDOUserGroup>) query
					.execute(user.getId());
			groups.addAll(c);
		}
		// add in Public group
		groups.add(JDOUserGroupDAOImpl.getPublicGroup(pm));
		// get all objects that these groups can access

		Query query = pm.newQuery(JDOResourceAccess.class);
		query.setFilter("owner==pUserGroup && accessType==pAccessType");
		query.declareParameters(JDOUserGroup.class.getName()
				+ " pUserGroup, " + String.class.getName() + " pAccessType");
		Collection<Long> ans = new HashSet<Long>();
		for (JDOUserGroup ug : groups) {
			@SuppressWarnings("unchecked")
			Collection<JDOResourceAccess> ras = (Collection<JDOResourceAccess>) query
					.execute(ug, accessType);
			for (JDOResourceAccess ra : ras)
				ans.add(ra.getResource());
		}
		return ans;
	}

}
