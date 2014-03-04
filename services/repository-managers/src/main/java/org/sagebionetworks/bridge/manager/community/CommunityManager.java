package org.sagebionetworks.bridge.manager.community;

import java.io.IOException;

import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

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
	 * @throws ServiceUnavailableException
	 * @throws IOException
	 */
	public Community create(UserInfo userInfo, Community community) throws DatastoreException, InvalidModelException, UnauthorizedException,
			NotFoundException, NameConflictException, ACLInheritanceException, IOException, ServiceUnavailableException;

	/**
	 * Retrieve the Communities to which the given user belongs, paginated
	 * 
	 * @param userInfo
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DatastoreException
	 */
	public PaginatedResults<Community> getForMember(UserInfo userInfo, int limit, int offset) throws DatastoreException,
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
	 * Get all members of a community
	 * 
	 * @param communityId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	public PaginatedResults<UserGroupHeader> getMembers(UserInfo userInfo, String communityId, Integer limit, Integer offset)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * join the community
	 * 
	 * @param userInfo
	 * @param communityId
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	public void join(UserInfo userInfo, String communityId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * leave the community
	 * 
	 * @param userInfo
	 * @param communityId
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	public void leave(UserInfo userInfo, String communityId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * make a member an admin of the community
	 * 
	 * @param userInfo
	 * @param communityId
	 * @param principalId
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws AuthenticationException
	 */
	public void addAdmin(UserInfo userInfo, String communityId, String principalId) throws DatastoreException,
			NotFoundException;

	/**
	 * remove a member as an admin of the community
	 * 
	 * @param userInfo
	 * @param communityId
	 * @param principalId
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws AuthenticationException
	 */
	public void removeAdmin(UserInfo userInfo, String communityId, String principalId) throws DatastoreException,
			NotFoundException;
}
