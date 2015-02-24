package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.web.NotFoundException;

public interface UserProfileDAO {

	/**
	 * @param dto
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(UserProfile dto) throws DatastoreException,
			InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public UserProfile get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * Get all the UserProfiles, in a given range
	 * 
	 * @param startIncl
	 * @param endExcl
	 * @param sort
	 * @param ascending
	 * @param schema
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<UserProfile> getInRange(long startIncl, long endExcl) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<UserProfile> list(List<Long> ids) throws DatastoreException, NotFoundException;

	/**
	 * Get the total count of UserProfiles in the system
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCount() throws DatastoreException, NotFoundException;

	/**
	 * Updates the 'shallow' properties of an object.
	 *
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public UserProfile update(UserProfile dto) throws DatastoreException, InvalidModelException,
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
	
	/**
	 * Ensure the bootstrap user's profiles exist
	 */
	public void bootstrapProfiles();
}
