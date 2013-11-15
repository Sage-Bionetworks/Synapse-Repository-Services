package org.sagebionetworks.bridge.service;

import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CommunityService {
	/**
	 * Create a new Community
	 * 
	 * @param userInfo
	 * @param community
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws ACLInheritanceException
	 * @throws NameConflictException
	 */
	public Community create(String userId, Community community) throws DatastoreException, InvalidModelException, UnauthorizedException,
			NotFoundException, NameConflictException, ACLInheritanceException;

	/**
	 * Retrieve the Communities to which the given user belongs, paginated
	 * 
	 * @param userId
	 * @param principalId
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<Community> getByMember(String userId, String principalId, int limit, int offset) throws DatastoreException,
			NotFoundException;

	/**
	 * Retrieve all visible communities, paginated
	 * 
	 * @param userId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<Community> getAll(String userId, int limit, int offset) throws DatastoreException, NotFoundException;

	/**
	 * Get a Community by its ID
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Community get(String userId, String id) throws DatastoreException, NotFoundException;

	/**
	 * Update a Community
	 * 
	 * @param userInfo
	 * @param community
	 * @return
	 * @throws InvalidModelException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public Community update(String userId, Community community) throws InvalidModelException, DatastoreException, UnauthorizedException,
			NotFoundException;

	/**
	 * Delete a Community by its ID
	 * 
	 * @param userInfo
	 * @param id
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void delete(String userId, String communityId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Join a community
	 * 
	 * @param userId
	 * @param communityId
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public void joinCommunity(String userId, String communityId) throws DatastoreException, NotFoundException;

	/**
	 * Leave a community
	 * 
	 * @param userId
	 * @param communityId
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public void leaveCommunity(String userId, String communityId) throws DatastoreException, NotFoundException;

	/**
	 * Get the ACL for a Community
	 * 
	 * @param userInfo
	 * @param communityId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	// public AccessControlList getACL(String userId, String communityId) throws DatastoreException,
	// UnauthorizedException,
	// NotFoundException;

	/**
	 * Update the ACL for a Community
	 * 
	 * @param userInfo
	 * @param acl
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	// public void updateACL(String userId, AccessControlList acl) throws DatastoreException, UnauthorizedException,
	// NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param communityId
	 * @param principalId
	 * @param isAdmin
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	// public void setPermissions(String userId, String communityId, String principalId, boolean isAdmin) throws
	// DatastoreException,
	// UnauthorizedException, NotFoundException;
}