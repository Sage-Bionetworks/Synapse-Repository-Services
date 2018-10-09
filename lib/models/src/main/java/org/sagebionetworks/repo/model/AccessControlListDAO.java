package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessControlListDAO  {

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
	 * Get the ACL IDs, if they exist, using ownerId and objectType
	 * .
	 * @param ownerIds List of ownerId as long
	 * @param objectType
	 * @return List of ACL Ids as longs. Sorted in ascending order by their corresponding ownerIds 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Long> getAclIds(List<Long> ownerIds, ObjectType objectType);
	
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
	
	/**
	 * Deletes ACLs using a List of Node Ids.
	 * @param ids list of id's of the nodes to delete
	 * @param ownerType
	 * @return int number of ACLs deleted
	 * @throws DatastoreException
	 */
	int delete(List<Long> ids, ObjectType ownerType) throws DatastoreException;

	/**
	 * Retrieve all user groups that have ACCESS_TYPE accessType to the given object
	 * 
	 * @param objectId
	 * @param objectType
	 * @param accessType
	 * @return
	 */
	public Set<String> getPrincipalIds(String objectId, ObjectType objectType, ACCESS_TYPE accessType);

	/**
	 * Get the projectIds that can be read by the passed principal ids.
	 * 
	 * @param principalIds
	 * @param read
	 * @return
	 */
	public Set<Long> getAccessibleProjectIds(Set<Long> principalIds,
			ACCESS_TYPE read);

	/**
	 * Get the children entities that have ACLs for the given
	 * entity parentIds.
	 * @param parentIds
	 */
	public List<Long> getChildrenEntitiesWithAcls(List<Long> parentIds);

}
