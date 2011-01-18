package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Revisable;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

abstract public class GAEJDORevisableDAOHelper<S extends Revisable, T extends GAEJDORevisable<T>> {
	
	abstract public S newDTO();
	
	/**
	 * Note:  This method is responsible for instantiating the Revision object owned by the JDO object.
	 * @return
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
	 */
	abstract public void copyFromDto(S dto, T jdo);
	
	abstract public Class<T> getJdoClass();
	
		/**
	 * @param dto an original (not revised) object
	 * @param createDate the date of creation
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 */
	public T create(PersistenceManager pm, S dto) throws DatastoreException {
			T jdo = newJDO();
			GAEJDORevision<T> r = jdo.getRevision();
			r.setRevisionDate(dto.getCreationDate());
			r.setVersion(new Version(dto.getVersion()));
			r.setLatest(true);
			copyFromDto(dto, jdo);
			pm.makePersistent(jdo);
			r.setOriginal(r.getId()); // points to itself
			pm.makePersistent(jdo); // not sure if it's necessary to 'persist' again
			return jdo;					
	}
	

	/**
	 * This updates the 'shallow' properties.   Version doesn't change.
	 * @param dto non-null id is required
	 * @throws DatastoreException if version in dto doesn't match version of object
	 */
	public void update(PersistenceManager pm, S dto) throws DatastoreException {
		if (dto.getId()==null) throw new DatastoreException("id is null");
		Key id = KeyFactory.stringToKey(dto.getId());
		@SuppressWarnings("unchecked")
		T jdo = (T)pm.getObjectById(id);
		if (!jdo.getRevision().getVersion().equals(new Version(dto.getVersion())))
			throw new DatastoreException("Wrong version");
		copyFromDto(dto, jdo);
		pm.makePersistent(jdo);
	}
	
	/**
	 * Create a revision of the object specified by the 'id' field, having
	 * the shallow properties from 'revision', including the new Version. 
	 * It is the callers responsibility to copy the 'deep' properties.
	 * @param pm Persistence Manager for accessing objects
	 * @param revision indicates (1) the object to be revised and (2) the new 'shallow' properties
	 * @param revisionDate
	 * @return the JDO object for the new revision
	 * @exception if the version of this revision is not greater than the version of the latest revision
	 */
	public T revise(PersistenceManager pm, S revision, Date revisionDate) throws DatastoreException {
		if (revision.getId()==null) throw new DatastoreException("id is null");
		if (revision.getVersion()==null) throw new DatastoreException("version is null");
		
		Key id = KeyFactory.stringToKey(revision.getId());
		
		Version newVersion = new Version(revision.getVersion());
		T latest = getLatest(pm, id);
		Version latestVersion = latest.getRevision().getVersion();
		if (newVersion.compareTo(latestVersion)<=0) {
			throw new DatastoreException("New version "+newVersion+
					" must be later than latest ("+latestVersion+").");
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
	
	
	// id is the key for the original revision
	public S getLatest(PersistenceManager pm, String id) throws DatastoreException {
		Key key = KeyFactory.stringToKey(id);
		T latest = getLatest(pm, key);
		S dto = newDTO();
		copyToDto(latest, dto);
		return dto;
	}
	
	// returns the number of objects of a certain revisable type, which are the latest
	// in their revision history
	public int getCount(PersistenceManager pm) throws DatastoreException {
		Query query = pm.newQuery(getJdoClass());
		query.setFilter("revision==r && r.latest==true");
		query.declareVariables(GAEJDORevision.class.getName()+" r");
		@SuppressWarnings("unchecked")
		Collection<T> c = (Collection<T>)query.execute();
		return c.size();
	}

	// id is the key for some revision
	public T getLatest(PersistenceManager pm, Key id) throws DatastoreException {
		@SuppressWarnings("unchecked")
		T someRev = (T)pm.getObjectById(id); // some revision, not necessarily first or last
		
		Query query = pm.newQuery(getJdoClass());
		query.setFilter("revision==r && r.original==pFirstRevision && r.latest==true");
		query.declareVariables(GAEJDORevision.class.getName()+" r");
		query.declareParameters(Key.class.getName()+" pFirstRevision");
		@SuppressWarnings("unchecked")
		Collection<T> c = (Collection<T>)query.execute(someRev.getRevision().getOriginal());
		if (c.size()!=1) throw new DatastoreException("Expected one object but found "+c.size());
		return c.iterator().next();
	}

	
	public T getVersion(PersistenceManager pm, String id, String v) throws DatastoreException {
		Key key = KeyFactory.stringToKey(id);
		return getVersion(pm, key, new Version(v));
	}
		
	// id is the key for some revision
	public T getVersion(PersistenceManager pm, Key id, Version v) throws DatastoreException {
		@SuppressWarnings("unchecked")
		T someRev = (T)pm.getObjectById(id); // some revision, not necessarily first or last

		Query query = pm.newQuery(getJdoClass());
		query.setFilter("revision==r && r.original==pFirstRevision && r.version==pVersion");
		query.declareVariables(GAEJDORevision.class.getName()+" r");
		query.declareParameters(Key.class.getName()+" pFirstRevision, "+
				Version.class.getName()+" pVersion");
		@SuppressWarnings("unchecked")
		Collection<T> c = (Collection<T>)query.execute(someRev.getRevision().getOriginal(), v);
		if (c.size()!=1) throw new DatastoreException("Expected one object but found "+c.size());
		return c.iterator().next();
	}	
	
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

	// id is the key for some revision
	public Collection<T> getAllVersions(PersistenceManager pm, Key id) {
		@SuppressWarnings("unchecked")
		T someRev = (T)pm.getObjectById(id); // some revision, not necessarily first or last

		Query query = pm.newQuery(getJdoClass());
		query.setFilter("revision==r && r.original==pFirstRevision");
		query.declareVariables(GAEJDORevision.class.getName()+" r");
		query.declareParameters(Key.class.getName()+" pFirstRevision");
		@SuppressWarnings("unchecked")
		Collection<T> ans = (Collection<T>)query.execute(someRev.getRevision().getOriginal());
		return ans;
	}	
	
	// id is the key for some revision
	public void deleteAllVersions(PersistenceManager pm, Key id) {
		@SuppressWarnings("unchecked")
		T someRev = (T)pm.getObjectById(id); // some revision, not necessarily first or last

		Query query = pm.newQuery(getJdoClass());
		query.setFilter("revision==r && r.original==pFirstRevision");
		query.declareVariables(GAEJDORevision.class.getName()+" r");
		query.declareParameters(Key.class.getName()+" pFirstRevision");
		@SuppressWarnings("unchecked")
		Collection<T> jdos = (Collection<T>)query.execute(someRev.getRevision().getOriginal());
		pm.deletePersistentAll(jdos);
	}	
	
	/**
	 * 
	 * @param start
	 * @param end
	 * @return a subset of the results, starting at index 'start' and not going beyond index 'end'
	 */
	public List<S> getInRange(int start, int end) {
		throw new RuntimeException("Not yet implemented");
	}
	
	/**
	 * 
	 * @param start
	 * @param end
	 * @param sortBy
	 * @param asc if true then ascending, else descending
	 * @return a subset of the results, starting at index 'start' and not going beyond index 'end'
	 * and sorted by the given primary field
	 */
	public List<S> getInRangeSortedByPrimaryField(int start, int end, String sortBy, boolean asc) {
		throw new RuntimeException("Not yet implemented");
	}
	
	public List<S> getInRangeHavingPrimaryField(int start, int end, String attribute, Object value) {
		throw new RuntimeException("Not yet implemented");
	}
		

	

}
