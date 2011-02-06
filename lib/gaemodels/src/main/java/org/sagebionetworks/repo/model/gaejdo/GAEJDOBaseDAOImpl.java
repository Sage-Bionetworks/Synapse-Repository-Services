package org.sagebionetworks.repo.model.gaejdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

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
abstract public class GAEJDOBaseDAOImpl<S extends Base, T extends GAEJDOBase>
		implements BaseDAO<S> {

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
	 */
	abstract protected void copyToDto(T jdo, S dto);

	/**
	 * Do a shallow copy from the DTO object to the JDO object.
	 * 
	 * @param dto
	 * @param jdo
	 * @throws InvalidModelException
	 */
	abstract protected void copyFromDto(S dto, T jdo) throws InvalidModelException;

	/**
	 * @param jdoClass
	 *            the class parameterized by T
	 */
	abstract protected Class<T> getJdoClass();

	/**
	 * Create a clone of the given object in memory (no datastore operations)
	 * Extentions of this class can go as deep as needed in copying data to
	 * create a clone
	 * 
	 * @param jdo
	 *            the object to clone
	 * @return the clone
	 */
	protected T cloneJdo(T jdo) {
		S dto = newDTO();

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

	/**
	 * take care of any work that has to be done before deleting the persistent
	 * object but within the same transaction (for example, deleteing objects
	 * which this object composes, but which are not represented by owned
	 * relationships)
	 * 
	 * @param pm
	 * @param jdo
	 *            the object to be deleted
	 */
	protected void preDelete(PersistenceManager pm, T jdo) {
		// for the base DAO, nothing needs to be done
	}
	
	/**
	 * This may be overridden by subclasses to generate the
	 * object's key.  Returning null causes the system to
	 * generate the key itself.
	 * @return the key for a new object, or null if none
	 */
	protected Key generateKey(PersistenceManager pm)  throws DatastoreException {
		return null;
	}

	/**
	 * take care of any work that has to be done after creating the persistent
	 * object but within the same transaction
	 * 
	 * @param pm
	 * @param jdo
	 */
	protected void postCreate(PersistenceManager pm, T jdo) {
		// for the base DAO, nothing needs to be done
	}

	protected T create(PersistenceManager pm, S dto)
			throws InvalidModelException, DatastoreException {
		T jdo = newJDO();
		//
		// Set system-controlled immutable fields
		//
		// Question: is this where we want to be setting immutable
		// system-controlled fields for our
		// objects? This should only be set at creation time so its not
		// appropriate to put it in copyFromDTO.
		dto.setCreationDate(new Date()); // now

		copyFromDto(dto, jdo);
		jdo.setId(generateKey(pm));
		pm.makePersistent(jdo);
		postCreate(pm, jdo);
		dto.setId(KeyFactory.keyToString(jdo.getId()));
		return jdo;
	}

	/**
	 * Create a new object, using the information in the passed DTO
	 * 
	 * @param dto
	 * @return the ID of the created object
	 * @throws InvalidModelException
	 */
	public String create(S dto) throws InvalidModelException,
			DatastoreException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			T jdo = create(pm, dto);
			tx.commit();
			copyToDto(jdo, dto);
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
	public S get(String id) throws DatastoreException, NotFoundException {
		PersistenceManager pm = PMF.get();
		try {
			Key key = KeyFactory.stringToKey(id);
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

	// sometimes we need to delete from within another transaction
	public void delete(PersistenceManager pm, T jdo) {
		preDelete(pm, jdo);
		pm.deletePersistent(jdo);
	}

	/**
	 * Delete the specified object
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Key key = KeyFactory.stringToKey(id);
			T jdo = (T) pm.getObjectById(getJdoClass(), key);
			delete(pm, jdo);
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
			InvalidModelException {
		if (dto.getId() == null)
			throw new InvalidModelException("id is null");
		Key id = KeyFactory.stringToKey(dto.getId());
		T jdo = (T) pm.getObjectById(getJdoClass(), id);
		copyFromDto(dto, jdo);
		pm.makePersistent(jdo);
	}

	public void update(S dto) throws DatastoreException, InvalidModelException,
			NotFoundException {
		PersistenceManager pm = PMF.get();
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
	 * returns the number of objects of a certain revisable type, which are the
	 * latest in their revision history
	 * 
	 */
	protected int getCount(PersistenceManager pm) throws DatastoreException {
		Query query = pm.newQuery(getJdoClass());
		@SuppressWarnings("unchecked")
		Collection<T> c = (Collection<T>) query.execute();
		return c.size();
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
			query.setRange(start, end);
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>) query.execute());
			List<S> ans = new ArrayList<S>();
			for (T jdo : list) {
				S dto = newDTO();
				copyToDto(jdo, dto);
				ans.add(dto);
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
		PersistenceManager pm = null;
		try {
			pm = PMF.get();
			Query query = pm.newQuery(getJdoClass());
			query.setRange(start, end);
			query.setOrdering(sortBy + (asc ? " ascending" : " descending"));
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>) query.execute());
			List<S> ans = new ArrayList<S>();
			for (T jdo : list) {
				S dto = newDTO();
				copyToDto(jdo, dto);
				ans.add(dto);
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
			query.setRange(start, end);
			query.setFilter(attribute + "==pValue");
			query.declareParameters(value.getClass().getName() + " pValue");
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>) query.execute(value));
			List<S> ans = new ArrayList<S>();
			for (T jdo : list) {
				S dto = newDTO();
				copyToDto(jdo, dto);
				ans.add(dto);
			}
			return ans;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

}
