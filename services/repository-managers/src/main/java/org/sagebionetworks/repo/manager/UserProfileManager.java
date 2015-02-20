package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.IdSet;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.file.S3FileHandle;
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
	 * List the UserProfiles for the given IDs
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public ListWrapper<UserProfile> list(IdSet ids) throws DatastoreException, NotFoundException;

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
			ProjectListSortColumn sortColumn, SortDirection sortDirection, Integer limit, Integer offset) throws DatastoreException,
			InvalidModelException, NotFoundException;

	/**
	 * Create an S3FileHandle for the given attachment data.
	 * @param createdBy
	 * @param attachment
	 * @return
	 */
	S3FileHandle createFileHandleFromAttachment(String createdBy, AttachmentData attachment);
}
