package org.sagebionetworks.repo.model.gaejdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Revisable;
import org.sagebionetworks.repo.model.RevisableDAO;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

abstract public class GAEJDORevisableDAOImpl<S extends Revisable, T extends GAEJDORevisable<T>>
		extends GAEJDOBaseDAOImpl<S, T> implements RevisableDAO<S> {

	public T cloneJdo(T jdo) throws DatastoreException {
		T clone = super.cloneJdo(jdo);

		clone.setRevision(jdo.getRevision().cloneJdo());
		if (null!=jdo.getNextVersion()) {
			clone.setNextVersion(cloneJdo(jdo.getNextVersion()));
		}
		
		return clone;
	}

	// Question: is this the right spot for this sort of constant?
	private static final String DEFAULT_VERSION = "0.0.1";

	/**
	 * Create a new Revisable object
	 * 
	 * @param dto
	 *            an original (not revised) object
	 * @param createDate
	 *            the date of creation
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	protected T createIntern(S dto) throws DatastoreException,
			InvalidModelException {
		//
		// Set default values for optional fields that have defaults
		//
		// Question: is this where we want to specify reasonable default
		// values?
		if (null == dto.getVersion()) {
			dto.setVersion(DEFAULT_VERSION);
		}
		
		//
		// Set system-controlled immutable fields
		//
		// Question: is this where we want to be setting immutable
		// system-controlled fields for our
		// objects? This should only be set at creation time so its not
		// appropriate to put it in copyFromDTO.
		dto.setCreationDate(new Date()); // now

		T jdo = super.createIntern(dto);
		GAEJDORevision<T> r = jdo.getRevision();
		r.setRevisionDate(dto.getCreationDate());
		r.setVersion(new Version(dto.getVersion()));
		r.setLatest(true);
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
			T jdo = createIntern(dto);
			// copyFromDto(dto, jdo);
			pm.makePersistent(jdo); // persist the owned Revision object
			GAEJDORevision<T> r = jdo.getRevision();
			r.setOriginal(r.getId()); // points to itself
			pm.makePersistent(jdo); // not sure if it's necessary to 'persist' again
			tx.commit();
			copyToDto(jdo, dto);
			dto.setId(KeyFactory.keyToString(jdo.getId())); // TODO Consider putting this line in 'copyToDto'
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
		if (!jdo.getRevision().getVersion()
				.equals(new Version(dto.getVersion())))
			throw new InvalidModelException("Wrong version " + dto.getVersion());
		copyFromDto(dto, jdo);
		pm.makePersistent(jdo);
	}

	/**
	 * Create a revision of the object specified by the 'id' field, having the
	 * shallow properties from 'revision', including the new Version. 
	 * 
	 * @param pm
	 *            Persistence Manager for accessing objects
	 * @param revision
	 *            indicates (1) the object to be revised
	 * @param revisionDate
	 * @return the JDO object for the new revision
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @exception if
	 *                the version of this revision is not greater than the
	 *                version of the latest revision
	 */
	private T revise(PersistenceManager pm, S revision, Date revisionDate)
			throws DatastoreException, InvalidModelException, NotFoundException {
		if (revision.getId() == null)
			throw new InvalidModelException("id is null");
		if (revision.getVersion() == null)
			throw new InvalidModelException("version is null");

		Key id = KeyFactory.stringToKey(revision.getId());

		Version newVersion = new Version(revision.getVersion());
		Key reviseeId = KeyFactory.stringToKey(revision.getId());
		T revisee = (T) pm.getObjectById(getJdoClass(), reviseeId);
		if (revisee==null) throw new NotFoundException();
		T latest = getLatest(pm, revisee);
		Version latestVersion = latest.getRevision().getVersion();
		if (newVersion.compareTo(latestVersion) <= 0) {
			throw new DatastoreException("New version " + newVersion
					+ " must be later than latest (" + latestVersion + ").");
		}

		// now copy the 'deep' properties
		T jdo = cloneJdo(revisee);
		copyFromDto(revision, jdo);
//		jdo.setId(generateKey(pm));
		GAEJDORevision<T> r = jdo.getRevision();
		r.setRevisionDate(revisionDate);
		r.setVersion(newVersion);
		r.setOriginal(latest.getRevision().getOriginal());
		r.setLatest(true);
		latest.getRevision().setLatest(false);
		latest.setNextVersion(jdo);
//		pm.makePersistent(jdo); Now that 'latest' owns 'jdo' this should not be necessary
		pm.makePersistent(latest); // because of this line, I'm afraid to make the method anything but 'private'
//		postCreate(pm, jdo);
		return jdo;
	}

	/**
	 * Create a revision of the object specified by the 'id' and 'version'
	 * fields, having the shallow properties from the given 'revision', and the
	 * deep properties of the given 'version'. The new revision will have the
	 * version given by the 'newVersion' parameter.
	 * 
	 * @param revision
	 * @param newVersion
	 * @param revisionDate
	 */
	public String revise(S revision, Date revisionDate)
			throws DatastoreException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			T newRevision = revise(pm, revision, revisionDate);
//			pm.makePersistent(newRevision); // TODO try removing this line, which should be unnecessary
			tx.commit();
			return KeyFactory.keyToString(newRevision.getId());
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
	 * @param id
	 *            is the key for some revision
	 */
	public T getLatest(PersistenceManager pm, T someRev) throws DatastoreException {
		// some revision, not necessarily first or last
		//T someRev = (T) pm.getObjectById(getJdoClass(), id);

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

	/**
	 * @param id
	 *            is the key for the original revision
	 */
	public S getLatest(PersistenceManager pm, String id)
			throws DatastoreException {
		Key key = KeyFactory.stringToKey(id);
		T someRev = (T) pm.getObjectById(getJdoClass(), key);
		T latest = getLatest(pm, someRev);
		S dto = newDTO();
		copyToDto(latest, dto);
		return dto;
	}

	/**
	 * 
	 * @param id
	 *            the id of any revision of the object
	 * @return the latest version of the object
	 * @throws DatastoreException
	 *             if no result
	 */
	public S getLatest(String id) throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			S latest = getLatest(pm, id);
			return latest;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
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
		query.setFilter("revision==r && r.latest==true");
		query.declareVariables(GAEJDORevision.class.getName() + " r");
		@SuppressWarnings("unchecked")
		Collection<T> c = (Collection<T>) query.execute();
		return c.size();
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
	 * @param id
	 *            the key for some revision
	 * @throws DatastoreException 
	 */
	public Collection<S> getAllVersions(PersistenceManager pm, String id) throws DatastoreException {
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
	 * @param id
	 *            the key for some revision
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
	 * Get all versions of an object
	 * 
	 * @param id
	 * @return all revisions of the given object
	 */
	public Collection<S> getAllVersions(String id) throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			Collection<S> allVersions = getAllVersions(pm, id);
			return allVersions;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * Deletes all revisions of a S
	 * 
	 * @param id
	 *            the id of any version of a revision series
	 * @throws DatastoreException
	 */
	public void deleteAllVersions(String id) throws DatastoreException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Key key = KeyFactory.stringToKey(id);
			Collection<T> allVersions = getAllVersions(pm, key);
			for (T jdo : allVersions) {
//				preDelete(pm, jdo);
				pm.deletePersistent(jdo);
			}
			tx.commit();
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
	 * Note: Invalid range returns empty list rather than throwing exception
	 * 
	 * @param start
	 * @param end
	 * @return a subset of the results, starting at index 'start' and less than
	 *         index 'end'
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
	 * Note: Invalid range returns empty list rather than throwing exception
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
	 * Note: Invalid range returns empty list rather than throwing exception
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
