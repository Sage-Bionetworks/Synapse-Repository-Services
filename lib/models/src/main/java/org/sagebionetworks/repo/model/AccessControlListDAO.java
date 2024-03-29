package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessControlListDAO  {

	/**
	 * @return true iff some group in 'groups' has explicit permission to access 'resourceId' using access type 'accessType'
	 * @throws DatastoreException 
	 */
	boolean canAccess(Set<Long> groups, String resourceId, ObjectType resourceType, ACCESS_TYPE accessType) throws DatastoreException;
	
	/**
	 * Does the user have the permission to access the given resource.
	 * 
	 * @param user         User attempting access.
	 * @param resourceId   Identifier of the resource to access.
	 * @param resourceType Type of resource to access.
	 * @param permission   The permission required for access.
	 * @return 
	 */
	AuthorizationStatus canAccess(UserInfo user, String resourceId, ObjectType resourceType,
			ACCESS_TYPE permission);
	
	/**
	 * Does the user have the given permission on any resource of the given type.
	 * 
	 * @param user
	 * @param resourceType
	 * @param permission
	 * @return
	 */
	boolean hasAccessToResourceOfType(UserInfo user, ObjectType resourceType, ACCESS_TYPE permission);

	/**
	 * Create a new ACL
	 * @param dto
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException 
	 */
	String create(AccessControlList dto, ObjectType ownerType) throws DatastoreException,	InvalidModelException, NotFoundException;

	/**
	 * Deprecated. Use: {@link #getAcl(String, ObjectType)}
	 */
	@Deprecated
	AccessControlList get(String id, ObjectType objectType) throws DatastoreException, NotFoundException;
	
	/**
	 * Get an ACL for the given object.  
	 * @param objectId The ID of the object.
	 * @param objectType The type of the object.
	 * @return Optional.empty() when an ACL does not exist for this object.
	 * @throws DatastoreException
	 */
	Optional<AccessControlList> getAcl(String objectId, ObjectType objectType) throws DatastoreException;
	
	/**
	 * Get the ACL's ID using ownerId and objectType
	 * @param id
	 * @param objectType
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	Long getAclId(String id, ObjectType objectType) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the ACL IDs, if they exist, using ownerId and objectType
	 * .
	 * @param ownerIds List of ownerId as long
	 * @param objectType
	 * @return List of ACL Ids as longs. Sorted in ascending order by their corresponding ownerIds 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	List<Long> getAclIds(List<Long> ownerIds, ObjectType objectType);	
	/**
	 * Get an ACL using the ACL's ID
	 * @param id - the id of the acl (not the ownerId)
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	AccessControlList get(Long id) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the OwnerType using the ACL's ID
	 * @param id - the id of the acl (not the ownerId)
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	ObjectType getOwnerType(Long id) throws DatastoreException, NotFoundException;

	/**
	 * Update the JDO
	 * @param dto
	 * @param ownerType
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	void update(AccessControlList dto, ObjectType ownerType) throws DatastoreException, InvalidModelException, NotFoundException,ConflictingUpdateException;

	/**
	 * Delete a ACL using the Node's Id.
	 * @param id
	 * @param ownerType
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	void delete(String id, ObjectType ownerType) throws DatastoreException, NotFoundException;
	
	/**
	 * Deletes ACLs using a List of Node Ids.
	 * @param ids list of id's of the nodes to delete
	 * @param ownerType
	 * @return int number of ACLs deleted
	 * @throws DatastoreException
	 */
	int delete(List<Long> ids, ObjectType ownerType) throws DatastoreException;

	/**
	 * Given a set of benefactors, and benefactors, return the sub-set of benefactors the that any given principal can see.
	 * @param groups
	 * @param benefactors
	 * @param entity
	 * @param read
	 * @return
	 */
	Set<Long> getAccessibleBenefactors(Set<Long> groups, Set<Long> benefactors,
			ObjectType entity, ACCESS_TYPE read);

	/**
	 * Retrieve all user groups that have ACCESS_TYPE accessType to the given object
	 * 
	 * @param objectId
	 * @param objectType
	 * @param accessType
	 * @return
	 */
	Set<String> getPrincipalIds(String objectId, ObjectType objectType, ACCESS_TYPE accessType);
	
	/**
	 * Fetches all the user groups that have the given access type for the given set of object ids of the given type
	 * 
	 * @param objectIds
	 * @param objectType
	 * @param accessType
	 * @return
	 */
	Map<String, Set<String>> getPrincipalIdsMap(Set<String> objectIds, ObjectType objectType, ACCESS_TYPE accessType);
	
	/**
	 * Get the projectIds that can be read by the passed principal ids.
	 * 
	 * @param principalIds
	 * @param read
	 * @return
	 */
	Set<Long> getAccessibleProjectIds(Set<Long> principalIds,
			ACCESS_TYPE read);

	/**
	 * Get the ids of the children of the given entity that the passed
	 * set of groups does not have the read permission.
	 * 
	 * @param groups
	 * @param parentId
	 * @return
	 */
	Set<Long> getNonVisibleChilrenOfEntity(Set<Long> groups,
			String parentId);

	/**
	 * Get the children entities that have ACLs for the given
	 * entity parentIds.
	 * @param parentIds
	 */
	List<Long> getChildrenEntitiesWithAcls(List<Long> parentIds);
	
	/**
	 * Truncate all data except bootstrap data.
	 */
	void truncateAll();

}
