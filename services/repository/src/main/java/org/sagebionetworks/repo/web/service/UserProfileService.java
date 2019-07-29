package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.ResponseMessage;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserBundle;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.principal.AliasList;
import org.sagebionetworks.repo.model.principal.TypeFilter;
import org.sagebionetworks.repo.model.principal.UserGroupHeaderResponse;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Generic service class to support controllers accessing UserProfiles.
 *
 */
public interface UserProfileService {

	/**
	 * Get a user profile
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The UserProfile
	 * @throws DatastoreException - Thrown when there is a server-side problem.
	 */
	public UserProfile getMyOwnUserProfile(Long userId) throws DatastoreException,
	UnauthorizedException, NotFoundException;

	/**
	 * 
	 * @param userId
	 * @param mask integer flag defining which components to include in the bundle
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public UserBundle getMyOwnUserBundle(Long userId, int mask) throws DatastoreException,
	UnauthorizedException, NotFoundException;

	/**
	 * Get a user profile specifying the individual group id for the user of interest
	 * @param userId - The user that is making the request.
	 * @param mask integer flag defining which components to include in the bundle
	 * @return The UserProfile
	 * @throws DatastoreException - Thrown when there is a server-side problem.
	 */
	public UserProfile getUserProfileByOwnerId(Long userId, String profileId)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * 
	 * @param userId
	 * @param profileId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public UserBundle getUserBundleByOwnerId(Long userId, String profileId, int mask)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get all the UserProfiles in the system (paginated
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The UserProfiles 
	 * @throws DatastoreException - Thrown when there is a server-side problem.
	 */
	public PaginatedResults<UserProfile> getUserProfilesPaginated(
			HttpServletRequest request, Long userId, Integer offset,
			Integer limit, String sort, Boolean ascending)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Return UserProfiles for the given ids
	 * @param userId
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public ListWrapper<UserProfile> listUserProfiles(Long userId, IdList ids)
			throws DatastoreException, UnauthorizedException, NotFoundException;


	/**
	 * Get a user profile
	 * @param userId - The user that is making the request.
	 * @param userProfile
	 * @return The updated UserProfile
	 * @throws DatastoreException - Thrown when there is a server-side problem.
	 */
	public UserProfile updateUserProfile(Long userId, UserProfile userProfile) throws NotFoundException,
			ConflictingUpdateException, DatastoreException, InvalidModelException, UnauthorizedException, IOException;

	/**
	 * Batch get headers for users matching a list of supplied Synapse IDs.
	 * 
	 * @param ids
	 * @return
	 */
	public UserGroupHeaderResponsePage getUserGroupHeadersByIds(List<Long> ids)
			throws DatastoreException, NotFoundException ;

	/**
	 * Get headers for users whose names begin with the supplied prefix.
	 * 
	 * @param id
	 * @param prefixFilter
	 * @param offset
	 * @param limit
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(
			String prefixFilter, TypeFilter filter, Integer offset, Integer limit)
			throws DatastoreException, NotFoundException;

	/**
	 * Adds the entity id to the users's favorites list
	 * @param userId
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public EntityHeader addFavorite(Long userId, String entityId) throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException;
	
	/**
	 * Removes the specified entity id from the users's favorites list, if exists
	 * @param userId
	 * @param entityId
	 * @throws DatastoreException
	 */
	public void removeFavorite(Long userId, String entityId) throws DatastoreException, NotFoundException;

	/**
	 * Retrieve users list of favorites, paginated
	 * @param userId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public PaginatedResults<EntityHeader> getFavorites(Long userId, int limit, int offset) throws DatastoreException, InvalidModelException, NotFoundException;

	/**
	 * Retrieve sorted list of projects, paginated
	 * 
	 * @param userId
	 * @param otherUserId optional other user id required when retrieving projects of other user
	 * @param teamId optional team id required when retrieving projects for a team
	 * @param type type of project list
	 * @param sortColumn optional sort column. default sort by last activity
	 * @param sortDirection optional sort direction. default sort descending
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException
	 * @throws InvalidModelException
	 * @throws DatastoreException
	 */
	public PaginatedResults<ProjectHeader> getProjects(Long userId, Long otherUserId, Long teamId, ProjectListType type,
			ProjectListSortColumn sortColumn, SortDirection sortDirection, Long limit, Long offset) throws DatastoreException,
			InvalidModelException, NotFoundException;

	/**
	 * Get the pre-signed URL for a user's profile image.
	 * 
	 * @param userId The id of the user performing the request
	 * @param profileId
	 * @return
	 * @throws NotFoundException 
	 */
	public String getUserProfileImage(Long userId, String profileId) throws NotFoundException;

	/**
	 * Get a pre-signed URL for a user's profile image preview.
	 * 
	 * @param userId The id of the user performing the request
	 * @param profileId
	 * @return
	 * @throws NotFoundException 
	 */
	public String getUserProfileImagePreview(Long userId, String profileId) throws NotFoundException;
	
	/**
	 * Update notification settings in user profile.
	 * 
	 * @param notificationSettingsSignedToken
	 */
	public ResponseMessage updateNotificationSettings(NotificationSettingsSignedToken notificationSettingsSignedToken);

	/**
	 * Get
	 * @param request
	 * @return
	 */
	public UserGroupHeaderResponse getUserGroupHeadersByAlias(
			AliasList request);

}
