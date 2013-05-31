package org.sagebionetworks.repo.model;

import java.util.Collection;

import org.sagebionetworks.repo.web.NotFoundException;

public interface BaseDAO<T> {

	/**
	 * @param dto
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(T dto) throws DatastoreException,
			InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public T get(String id) throws DatastoreException, NotFoundException;

	/**
	 * Retrieves all objects of type T in the datastore
	 *
	 * @return all objects of type T in the datastore
	 * @throws DatastoreException
	 */
	public Collection<T> getAll() throws DatastoreException;

	/**
	 * This updates the 'shallow' properties of an object
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public void update(T dto) throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException;

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

	long getCount() throws DatastoreException;

}
