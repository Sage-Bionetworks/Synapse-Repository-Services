package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserGroupDAO {
		
	/**
	 * @return the list of user groups for the given principal IDs
	 */
	public List<UserGroup> get(List<String> ids) throws DatastoreException;

	/**
	 * a variant of the generic 'getAll' query, this allows the caller to
	 * separately retrieve the individual and non-individual groups.
	 */	
	public Collection<UserGroup> getAll(boolean isIndividual) throws DatastoreException;


	/**
	 * a variant of the generic 'getInRange' query, this allows the caller to
	 * separately retrieve the individual and non-individual groups.
	 */

	public List<UserGroup> getInRange(long fromIncl, long toExcl, boolean isIndividual) throws DatastoreException;

	
	/**
	 * Ensure the bootstrap users exist
	 */
	public void bootstrapUsers() throws Exception;
	
	/**
	 * Gets and locks a row of the table
	 */
	public String getEtagForUpdate(String id);

	/**
	 * Updates the etag the group with the given ID
	 */
	public void touch(Long principalId);
	
	/**
	 * @param dto
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public Long create(UserGroup dto) throws DatastoreException,
			InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public UserGroup get(Long id) throws DatastoreException, NotFoundException;


	/**
	 * This updates the 'shallow' properties of an object
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public void update(UserGroup dto) throws DatastoreException, InvalidModelException,
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
	 * Get the total count of all users and groups.
	 * @return
	 * @throws DatastoreException
	 */
	long getCount() throws DatastoreException;
	
	/**
	 * Get the bootstrap principals
	 * @return
	 */
	List<BootstrapPrincipal> getBootstrapPrincipals();
	
	/**
	 * Does a principal exist with this id.
	 * @param id
	 * @return
	 */
	boolean doesIdExist(Long id);

	/**
	 * Get all of the principals
	 * @return
	 */
	public List<UserGroup> getAllPrincipals();


}
