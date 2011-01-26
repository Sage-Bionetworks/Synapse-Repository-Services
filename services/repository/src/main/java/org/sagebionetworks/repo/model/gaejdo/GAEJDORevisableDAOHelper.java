package org.sagebionetworks.repo.model.gaejdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Revisable;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

abstract public class GAEJDORevisableDAOHelper<S extends Revisable, T extends GAEJDORevisable<T>> {

	/**
	 * Create a new instance of the data transfer object.  
	 * Introducing this abstract method helps us avoid making assumptions about constructors.
	 * @return the new object
	 */
	abstract public S newDTO();

	/**
	 * Create a new instance of the persistable object.
	 * Introducing this abstract method helps us avoid making assumptions about constructors.
	 * @return the new object
	 */
	abstract public T newJDO();

	/**
	 * Do a shallow copy from the JDO object to the DTO object.
	 * 
	 * @param jdo
	 * @param dto
	 */
	abstract public void copyToDto(T jdo, S dto);

	/**
	 * Do a shallow copy from the DTO object to the JDO object.
	 * 
	 * @param dto
	 * @param jdo
	 * @throws InvalidModelException
	 */
	abstract public void copyFromDto(S dto, T jdo) throws InvalidModelException;

	/**
	 * @param jdoClass
	 *            the class parameterized by T
	 */
	abstract public Class<T> getJdoClass();

	/**
	 * Create a new Revisable object
	 * @param dto
	 *            an original (not revised) object
	 * @param createDate
	 *            the date of creation
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public T create(PersistenceManager pm, S dto) throws DatastoreException,
			InvalidModelException {
		T jdo = newJDO();
		GAEJDORevision<T> r = jdo.getRevision();
		r.setRevisionDate(dto.getCreationDate());
		r.setVersion(new Version(dto.getVersion()));
		r.setLatest(true);
		copyFromDto(dto, jdo);
		pm.makePersistent(jdo);
		r.setOriginal(r.getId()); // points to itself
		pm.makePersistent(jdo); // not sure if it's necessary to 'persist' again
		jdo.getRevision().getVersion().toString();
		return jdo;
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
			throw new DatastoreException("id is null");
		Key id = KeyFactory.stringToKey(dto.getId());
		T jdo = (T) pm.getObjectById(getJdoClass(), id);
		if (!jdo.getRevision().getVersion()
				.equals(new Version(dto.getVersion())))
			throw new DatastoreException("Wrong version");
		copyFromDto(dto, jdo);
		pm.makePersistent(jdo);
	}

	/**
	 * Create a revision of the object specified by the 'id' field, having the
	 * shallow properties from 'revision', including the new Version. It is the
	 * callers responsibility to copy the 'deep' properties.
	 * 
	 * @param pm
	 *            Persistence Manager for accessing objects
	 * @param revision
	 *            indicates (1) the object to be revised and (2) the new
	 *            'shallow' properties
	 * @param revisionDate
	 * @return the JDO object for the new revision
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @exception if
	 *                the version of this revision is not greater than the
	 *                version of the latest revision
	 */
	public T revise(PersistenceManager pm, S revision, Date revisionDate)
			throws DatastoreException, InvalidModelException {
		if (revision.getId() == null)
			throw new DatastoreException("id is null");
		if (revision.getVersion() == null)
			throw new DatastoreException("version is null");

		Key id = KeyFactory.stringToKey(revision.getId());

		Version newVersion = new Version(revision.getVersion());
		T latest = getLatest(pm, id);
		Version latestVersion = latest.getRevision().getVersion();
		if (newVersion.compareTo(latestVersion) <= 0) {
			throw new DatastoreException("New version " + newVersion
					+ " must be later than latest (" + latestVersion + ").");
		}
		T jdo = newJDO();
		copyFromDto(revision, jdo);
		GAEJDORevision<T> r = jdo.getRevision();
		r.setRevisionDate(revisionDate);
		r.setVersion(newVersion);
		r.setOriginal(latest.getRevision().getOriginal());
		r.setLatest(true);
		latest.getRevision().setLatest(false);
		pm.makePersistent(jdo);
		pm.makePersistent(latest);
		return jdo;
	}
	
	/**
	 * @param id is the key for the original revision
	 */
	public S getLatest(PersistenceManager pm, String id)
			throws DatastoreException {
		Key key = KeyFactory.stringToKey(id);
		T latest = getLatest(pm, key);
		S dto = newDTO();
		copyToDto(latest, dto);
		return dto;
	}

	
	/**
	 * 
	 * returns the number of objects of a certain revisable type, which are the
	 * latest in their revision history
	 * 
	 */
	public int getCount(PersistenceManager pm) throws DatastoreException {
		Query query = pm.newQuery(getJdoClass());
		query.setFilter("revision==r && r.latest==true");
		query.declareVariables(GAEJDORevision.class.getName() + " r");
		@SuppressWarnings("unchecked")
		Collection<T> c = (Collection<T>) query.execute();
		return c.size();
	}

	// 
	
	/**
	 * @param id is the key for some revision
	 */
	public T getLatest(PersistenceManager pm, Key id) throws DatastoreException {
		// some revision, not necessarily first or last
		T someRev = (T) pm.getObjectById(getJdoClass(), id); 

		Query query = pm.newQuery(getJdoClass());
		query.setFilter("revision==r && r.original==pFirstRevision && r.latest==true");
		query.declareVariables(GAEJDORevision.class.getName() + " r");
		query.declareParameters(Key.class.getName() + " pFirstRevision");
		@SuppressWarnings("unchecked")
		Collection<T> c = (Collection<T>) query.execute(someRev.getRevision()
				.getOriginal());
		if (c.size() != 1)
			throw new DatastoreException("Expected one object but found "
					+ c.size());
		return c.iterator().next();
	}

	public T getVersion(PersistenceManager pm, String id, String v)
			throws DatastoreException {
		Key key = KeyFactory.stringToKey(id);
		return getVersion(pm, key, new Version(v));
	}

	// id is the key for some revision
	public T getVersion(PersistenceManager pm, Key id, Version v)
			throws DatastoreException {
		// some revision, not necessarily first or last
		T someRev = (T) pm.getObjectById(getJdoClass(), id); 

		Query query = pm.newQuery(getJdoClass());
		query.setFilter("revision==r && r.original==pFirstRevision && r.version==pVersion");
		query.declareVariables(GAEJDORevision.class.getName() + " r");
		query.declareParameters(Key.class.getName() + " pFirstRevision, "
				+ Version.class.getName() + " pVersion");
		@SuppressWarnings("unchecked")
		Collection<T> c = (Collection<T>) query.execute(someRev.getRevision()
				.getOriginal(), v);
		if (c.size() != 1)
			throw new DatastoreException("Expected one object but found "
					+ c.size());
		return c.iterator().next();
	}

	/**
	 * @param id the key for some revision
	 */
	public Collection<S> getAllVersions(PersistenceManager pm, String id) {
		Key key = KeyFactory.stringToKey(id);
		Collection<T> jdos = getAllVersions(pm, key);
		Collection<S> dtos = new HashSet<S>();
		for (T jdo : jdos) {
			S dto = newDTO();
			copyToDto(jdo, dto);
			dto.setId(id);
			dto.setVersion(jdo.getRevision().getVersion().toString());
			dtos.add(dto);
		}
		return dtos;
	}

	/**
	 * @param id the key for some revision
	 */
	public Collection<T> getAllVersions(PersistenceManager pm, Key id) {
		// some revision, not necessarily first or last
		T someRev = (T) pm.getObjectById(getJdoClass(), id); 
		Query query = pm.newQuery(getJdoClass());
		query.setFilter("revision==r && r.original==pFirstRevision");
		query.declareVariables(GAEJDORevision.class.getName() + " r");
		query.declareParameters(Key.class.getName() + " pFirstRevision");
		@SuppressWarnings("unchecked")
		Collection<T> ans = (Collection<T>) query.execute(someRev.getRevision()
				.getOriginal());
		return ans;
	}
	
	/**
	 * @param id the key for some revision
	 */
	public void deleteAllVersions(PersistenceManager pm, Key id) {
		// some revision, not necessarily first or last
		T someRev = (T) pm.getObjectById(getJdoClass(), id); 

		Query query = pm.newQuery(getJdoClass());
		query.setFilter("revision==r && r.original==pFirstRevision");
		query.declareVariables(GAEJDORevision.class.getName() + " r");
		query.declareParameters(Key.class.getName() + " pFirstRevision");
		@SuppressWarnings("unchecked")
		Collection<T> jdos = (Collection<T>) query.execute(someRev
				.getRevision().getOriginal());
		pm.deletePersistentAll(jdos);
	}

	/**
	 * Note:  Invalid range returns empty list rather than throwing exception
	 * @param start
	 * @param end
	 * @return a subset of the results, starting at index 'start' and less than index 'end'
	 */
	public List<S> getInRange(int start, int end) throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			Query query = pm.newQuery(getJdoClass());
			query.setFilter("revision==vRevision && vRevision.latest==true");
			query.declareVariables(GAEJDORevision.class.getName()
					+ " vRevision");
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
	 * Note:  Invalid range returns empty list rather than throwing exception
	 * 
	 * @param start
	 * @param end
	 * @param sortBy
	 * @param asc if true then ascending, else descending
	 * @return a subset of the results, starting at index 'start' and not going
	 *         beyond index 'end' and sorted by the given primary field
	 */
	public List<S> getInRangeSortedByPrimaryField(int start, int end,
			String sortBy, boolean asc) throws DatastoreException {
		PersistenceManager pm = null;
		try {
			pm = PMF.get();
			Query query = pm.newQuery(getJdoClass());
			query.setOrdering(sortBy + (asc ? " ascending" : " descending"));
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>) query.execute());
			List<S> ans = new ArrayList<S>();
			// but we only want the latest! (We can't do this in the query while
			// sorting.)
			for (int i = 0, latestCounter = 0; i < list.size()
					&& latestCounter < end; i++) {
				T jdo = list.get(i);
				if (jdo.getRevision().getLatest()) {
					if (latestCounter >= start) {
						S dto = newDTO();
						copyToDto(jdo, dto);
						ans.add(dto);
					}
					latestCounter++;
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
	 * Note:  Invalid range returns empty list rather than throwing exception
	 * 
	 * @param start
	 * @param end
	 * @param attribute
	 * @param value
	 * @return a subset of results, starting at index 'start' and not going
	 *         beyond index 'end', having the given value for the given field
	 */
	public List<S> getInRangeHavingPrimaryField(int start, int end,
			String attribute, Object value) throws DatastoreException {
		PersistenceManager pm = null;
		try {
			pm = PMF.get();
			Query query = pm.newQuery(getJdoClass());
			query.setRange(start, end);
			query.setFilter(attribute
					+ "==pValue && revision==vRevision && vRevision.latest==true");
			query.declareVariables(GAEJDORevision.class.getName()
					+ " vRevision");
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
