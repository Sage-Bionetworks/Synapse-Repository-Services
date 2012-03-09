package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;

public interface UserProfileDAO {
	// TODO delete the commented lines
//	/**
//	 * Retrieves the UserProfile given the userName
//	 * 
//	 * @param userName
//	 * @return
//	 * @throws DatastoreException
//	 * @throws NotFoundException
//	 */
//	public UserProfile getFromUserName(String userName, ObjectSchema schema) throws DatastoreException, NotFoundException;


	/**
	 * @param dto
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(UserProfile dto, ObjectSchema schema) throws DatastoreException,
			InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public UserProfile get(String id, ObjectSchema schema) throws DatastoreException, NotFoundException;


	/**
	 * UserProfilehis updates the 'shallow' properties of an object
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public UserProfile update(UserProfile dto, ObjectSchema schema) throws DatastoreException, InvalidModelException,
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


}
