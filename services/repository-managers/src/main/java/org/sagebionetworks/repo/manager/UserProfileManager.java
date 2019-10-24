package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ProjectHeaderList;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserProfileManager {

	/**
	 * Get an existing UserProfile
	 */
	public UserProfile getUserProfile(String ownerid)
			throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * 
	 * @param userId
	 * @return the user's current verification document
	 */
	public VerificationSubmission getCurrentVerificationSubmission(Long userId);
	
	/**
	 * 
	 * @param userId
	 * @return the user's linked ORCID
	 */
	public String getOrcid(Long userId);

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
	 * Retrieve list of projects and activity history for 'userToGetInfoFor' in each project.
	 * The results are paginated and sorted.  The content of the returned list depends 
	 * on the 'type' parameter.  The result for each type is:
	 * 
	 * if type is ALL: the projects that the user has READ access to by virtue of being 
	 * included in the project's ACL personally or via a team in which they are a member
	 * 
	 * if type is CREATED: the projects that the user has READ access to by virtue of being 
	 * included in the project's ACL personally or via a team in which they are a member, and
	 * which they have created
	 * 
	 * if type is PARTICIPATED: the projects the user has READ access to by virtue of being 
	 * included in the project's ACL personally or via a team in which they are a member, but which
	 * the user has not created
	 * 
	 * if type is TEAM: the projects that the user has READ access by virtue of being 
	 * included in the project's ACL via the team given by the 'teamId' parameter or, if
	 * omitted, via any team the user is a member of
	 * 
	 * The results include only the projects that 'userInfo'  also has READ access to
	 * 
	 * @param userInfo
	 * @param userToGetInfoFor
	 * @param teamId
	 * @param type
	 * @param sortColumn
	 * @param sortDirection
	 * @param nextPageToken
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public ProjectHeaderList getProjects(UserInfo userInfo, UserInfo userToGetInfoFor, Long teamId, ProjectListType type,
			ProjectListSortColumn sortColumn, SortDirection sortDirection, String nextPageToken) throws DatastoreException,
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
