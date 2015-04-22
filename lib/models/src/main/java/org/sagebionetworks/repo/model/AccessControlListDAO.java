package org.sagebionetworks.repo.model;

import java.util.Set;

import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessControlListDAO  {

	/**
	 * @return true iff some group in 'groups' has explicit permission to access 'resourceId' using access type 'accessType'
	 * @throws DatastoreException 
	 */
	public boolean canAccess(Set<Long> groups, String resourceId, ObjectType resourceType, ACCESS_TYPE accessType) throws DatastoreException;

	/**
	 * Create a new ACL
	 * @param dto
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException 
	 */
	public String create(AccessControlList dto, ObjectType ownerType) throws DatastoreException,	InvalidModelException, NotFoundException;

	/**
	 * Get an ACL using the Node's ID
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public AccessControlList get(String id, ObjectType objectType) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the ACL's ID using ownerId and objectType
	 * @param id
	 * @param objectType
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Long getAclId(String id, ObjectType objectType) throws DatastoreException, NotFoundException;
	
	/**
	 * Get an ACL using the ACL's ID
	 * @param id - the id of the acl (not the ownerId)
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public AccessControlList get(Long id) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the OwnerType using the ACL's ID
	 * @param id - the id of the acl (not the ownerId)
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public ObjectType getOwnerType(Long id) throws DatastoreException, NotFoundException;

	/**
	 * Update the JDO
	 * @param dto
	 * @param ownerType
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	public void update(AccessControlList dto, ObjectType ownerType) throws DatastoreException, InvalidModelException, NotFoundException,ConflictingUpdateException;

	/**
	 * Delete a ACL using the Node's Id.
	 * @param id
	 * @param ownerType
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id, ObjectType ownerType) throws DatastoreException, NotFoundException;
}
