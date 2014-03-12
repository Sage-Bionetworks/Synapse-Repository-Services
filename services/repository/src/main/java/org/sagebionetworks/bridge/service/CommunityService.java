package org.sagebionetworks.bridge.service;

import java.io.IOException;

import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

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
	 * @throws IOException
	 * @throws ServiceUnavailableException
	 */
	public Community create(Long userId, Community community) throws DatastoreException, InvalidModelException, UnauthorizedException,
			NotFoundException, NameConflictException, ACLInheritanceException, ServiceUnavailableException, IOException;

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
	public PaginatedResults<Community> getForMember(Long userId, int limit, int offset) throws DatastoreException,
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
	public PaginatedResults<Community> getAll(Long userId, int limit, int offset) throws DatastoreException, NotFoundException;

	/**
	 * Get a Community by its ID
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Community get(Long userId, String id) throws DatastoreException, NotFoundException;

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
	public Community update(Long userId, Community community) throws InvalidModelException, DatastoreException, UnauthorizedException,
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
	public void delete(Long userId, String communityId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get all members of a community
	 * 
	 * @param userId
	 * @param id
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	public PaginatedResults<UserGroupHeader> getMembers(Long userId, String id, Integer limit, Integer offset) throws DatastoreException,
			UnauthorizedException, NotFoundException;

	/**
	 * Join a community
	 * 
	 * @param userId
	 * @param communityId
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public void joinCommunity(Long userId, String communityId) throws DatastoreException, NotFoundException;

	/**
	 * Leave a community
	 * 
	 * @param userId
	 * @param communityId
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public void leaveCommunity(Long userId, String communityId) throws DatastoreException, NotFoundException;

	public void addCommunityAdmin(Long userId, String communityId, String principalId) throws DatastoreException,
			NotFoundException;

	public void removeCommunityAdmin(Long userId, String communityId, String principalId) throws DatastoreException, NotFoundException;
}
