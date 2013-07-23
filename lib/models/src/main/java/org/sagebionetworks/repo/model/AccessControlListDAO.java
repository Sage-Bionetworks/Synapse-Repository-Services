package org.sagebionetworks.repo.model;

import java.util.Collection;

import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessControlListDAO  {

	/**
	 * @return true iff some group in 'groups' has explicit permission to access 'resourceId' using access type 'accessType'
	 * @throws DatastoreException 
	 */
	public boolean canAccess(Collection<UserGroup> groups, String resourceId, ACCESS_TYPE accessType) throws DatastoreException;

	/**
	 * Create a new ACL
	 * @param dto
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException 
	 */
	public String create(AccessControlList dto) throws DatastoreException,	InvalidModelException, NotFoundException;

	/**
	 * Get an ACL using the Node's ID
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public AccessControlList get(String id, ObjectType objectType) throws DatastoreException, NotFoundException;

	/**
	 * Update the JDO
	 * @param dto
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	public void update(AccessControlList dto) throws DatastoreException, InvalidModelException, NotFoundException,ConflictingUpdateException;

	/**
	 * Delete a ACL using the Node's Id.
	 * @param id
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

}
