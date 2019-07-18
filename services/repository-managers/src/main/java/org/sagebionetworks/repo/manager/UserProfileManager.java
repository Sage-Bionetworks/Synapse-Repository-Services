package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserProfileManager {

	/**
	 * Get an existing UserProfile
	 */
	public UserProfile getUserProfile(String ownerid)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get the public profiles of the users in the system, paginated. 
	 * Default to not include e-mail addresses.
	 */
	public List<UserProfile> getInRange(UserInfo userInfo,
			long startIncl, long endExcl) throws DatastoreException,
			NotFoundException;

	/**
	 * List the UserProfiles for the given IDs
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public ListWrapper<UserProfile> list(IdList ids) throws DatastoreException, NotFoundException;

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
	 * Retrieve list of projects, paginated
	 */
	public PaginatedResults<ProjectHeader> getProjects(UserInfo userInfo, UserInfo userToGetInfoFor, Team teamToFetch, ProjectListType type,
			ProjectListSortColumn sortColumn, SortDirection sortDirection, Long limit, Long offset) throws DatastoreException,
			InvalidModelException, NotFoundException;
	
	/**
	 * Get the pre-signed URL for a user's profile picture.
	 * 
	 * @param userInfo The info about the user requesting the pre-signed URL for the profile picture of the given user
	 * @param userId
	 * @return The pre-signed URL that can be used to download the user's profile picture.
	 * @throws NotFoundException Thrown if the user does not have a profile picture.
	 */
	public String getUserProfileImageUrl(UserInfo userInfo, String userId) throws NotFoundException;
	
	/**
	 * Get the pre-signed URL for a user's profile picture preview.
	 * 
	 * @param userInfo The info about the user requesting the pre-signed URL for the profile picture of the given user
	 * @param userId
	 * @return The pre-signed URL that can be used to download the user's profile picture preview.
	 * @throws NotFoundException Thrown if the user does not have a profile picture.
	 */
	public String getUserProfileImagePreviewUrl(UserInfo userInfo, String userId) throws NotFoundException;
	
}
