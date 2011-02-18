package org.sagebionetworks.repo.model;

import java.util.Collection;

import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This interface defines the basic data access functionality for DAO that
 * operate on just a portion of persisted object, where the persisted object is
 * exposed to clients by more than one DTO.
 * 
 * @author deflaux
 * 
 * @param <T> the dependent DTO
 * @param <S> the parent DTO
 */
public interface DependentPropertyDAO<T,S> {

	/**
	 * Retrieves the subset of the 'shallow' properties of an object accessible via this DTO
	 * 
	 * @param id
	 * @return the dependent DTO
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public T get(String id) throws DatastoreException, NotFoundException;

	/**
	 * This update the subset of the 'shallow' properties of an object accessible via this DTO
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public void update(T dto) throws DatastoreException, InvalidModelException,
			NotFoundException;

	/**
	 * @return the names of the fields accessible via this DTO
	 */
	public Collection<String> getPrimaryFields();
}
