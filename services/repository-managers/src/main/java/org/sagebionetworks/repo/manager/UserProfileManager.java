package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserProfileManager {

	/**
	 * Get an existing UserProfile
	 */
	public UserProfile getUserProfile(UserInfo userInfo, String ownerid)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get the public profiles of the users in the system, paginated. 
	 * Default to not include e-mail addresses.
	 */
	public QueryResults<UserProfile> getInRange(UserInfo userInfo,
			long startIncl, long endExcl) throws DatastoreException,
			NotFoundException;

	/**
	 * Update a UserProfile.
	 */
	public UserProfile updateUserProfile(UserInfo userInfo, UserProfile updated)
			throws DatastoreException, UnauthorizedException,
			InvalidModelException, NotFoundException;
	
	/**
	 * Create a Users profile
	 * @param userInfo
	 * @param updated
	 * @return
	 */
	public UserProfile createUserProfile(UserProfile updated);

	/**
	 * userId may not match the profileId
	 */
	public S3AttachmentToken createS3UserProfileAttachmentToken(
			UserInfo userInfo, String profileId, S3AttachmentToken token)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException;

	/**
	 * return the preassigned url for the user profile attachment
	 */
	public PresignedUrl getUserProfileAttachmentUrl(Long userId,
			String profileId, String tokenID) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException;

	/**
	 * Adds the entity id to the users's favorites list
	 */
	public Favorite addFavorite(UserInfo userInfo, String entityId)
			throws DatastoreException, InvalidModelException;

	/**
	 * Removes the specified entity id from the users's favorites list, if exists
	 */
	public void removeFavorite(UserInfo userInfo, String entityId)
			throws DatastoreException;

	/**
	 * Retrieve users list of favorites, paginated
	 */
	public PaginatedResults<EntityHeader> getFavorites(UserInfo userInfo,
			int limit, int offset) throws DatastoreException,
			InvalidModelException, NotFoundException;

	/**
	 * Retrieve my list of projects, paginated
	 */
	public PaginatedResults<ProjectHeader> getMyProjects(UserInfo userInfo, int limit, int offset) throws DatastoreException,
			InvalidModelException, NotFoundException;

	/**
	 * Retrieve users list of projects, paginated
	 */
	public PaginatedResults<ProjectHeader> getProjectsForUser(UserInfo userInfo, UserInfo userToFetch, int limit, int offset)
			throws DatastoreException, InvalidModelException, NotFoundException;

	/**
	 * Retrieve teams list of projects, paginated
	 */
	public PaginatedResults<ProjectHeader> getProjectsForTeam(UserInfo userInfo, Team teamToFetch, int limit, int offset)
			throws DatastoreException, InvalidModelException, NotFoundException;
}
