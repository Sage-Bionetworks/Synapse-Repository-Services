package org.sagebionetworks.bridge.manager.community;

import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CommunityManager {

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
	 */
	public Community create(UserInfo userInfo, Community community) throws DatastoreException, InvalidModelException, UnauthorizedException,
			NotFoundException, NameConflictException, ACLInheritanceException;

	/**
	 * get the team that is associated with this community
	 * 
	 * @param userInfo
	 * @param community
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public Team getCommunityTeam(UserInfo userInfo, String communityId) throws DatastoreException, NotFoundException;

	/**
	 * Retrieve the Communities to which the given user belongs, paginated
	 * 
	 * @param userInfo
	 * @param principalId
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DatastoreException
	 */
	public PaginatedResults<Community> getByMember(UserInfo userInfo, String principalId, int limit, int offset) throws DatastoreException,
			NotFoundException;

	/**
	 * Retrieve the Communities to which the given user belongs, paginated
	 * 
	 * @param userInfo
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DatastoreException
	 */
	public PaginatedResults<Community> getAll(UserInfo userInfo, int limit, int offset) throws DatastoreException, NotFoundException;

	/**
	 * Get a Community by its ID
	 * 
	 * @param userInfo
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Community get(UserInfo userInfo, String id) throws DatastoreException, NotFoundException;

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
	public Community update(UserInfo userInfo, Community community) throws InvalidModelException, DatastoreException, UnauthorizedException,
			NotFoundException;

	/**
	 * Delete a Community by its ID
	 * 
	 * @param userInfo
	 * @param communityId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void delete(UserInfo userInfo, String communityId) throws DatastoreException, UnauthorizedException, NotFoundException;

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
	// public AccessControlList getACL(UserInfo userInfo, String communityId) throws DatastoreException,
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
	// public void updateACL(UserInfo userInfo, AccessControlList acl) throws DatastoreException, UnauthorizedException,
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
	// public void setPermissions(UserInfo userInfo, String communityId, String principalId, boolean isAdmin) throws
	// DatastoreException,
	// UnauthorizedException, NotFoundException;
}
