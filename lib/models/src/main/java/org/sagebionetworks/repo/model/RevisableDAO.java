package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;

/**
 * This interface defines the DAO methods for all objects of the Revisable type
 * 
 * @author bhoff
 * 
 * @param <T>
 *            the data transfer object type
 */
public interface RevisableDAO<T extends Revisable> extends BaseDAO<T> {
	/**
	 * Create a revision of the object specified by the 'id' field, having the
	 * shallow properties from the given 'revision', and the deep properties of
	 * the current object. The new revision will have the version given by the
	 * version field.
	 * 
	 * @param revision
	 *            id of object to be revised; new values for 'shallow' fields;
	 *            new version; creationDate is the date of revision
	 * @return id of the new revision
	 */
	public String revise(T revision, Date revisionDate)
			throws DatastoreException;

	/**
	 * 
	 * @param id
	 *            the id of any object in the revision series
	 * @return the latest version of the object
	 * @throws DatastoreException
	 *             if no result
	 */
	public T getLatest(String id) throws DatastoreException;

	/**
	 * Get all versions of an object
	 * 
	 * @param id
	 *            the id of any object in the revision series
	 * @return all revisions of the given object
	 */
	public Collection<T> getAllVersions(String id) throws DatastoreException;

	/**
	 * Consensus on 1/17/2011 was that we don't need this
	 * 
	 * @param id
	 *            the id of any object in the revision series
	 * @throws DatastoreException
	 */
	public void deleteAllVersions(String id) throws DatastoreException;

}
