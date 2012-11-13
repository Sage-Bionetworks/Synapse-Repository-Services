package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.PermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.http.HttpHeaders;

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
	public UserProfile getMyOwnUserProfile(String userId) throws DatastoreException,
			UnauthorizedException, NotFoundException;

	/**
	 * Get a user profile specifying the individual group id for the user of interest
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The UserProfile
	 * @throws DatastoreException - Thrown when there is a server-side problem.
	 */
	public UserProfile getUserProfileByOwnerId(String userId, String profileId)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get all the UserProfiles in the system (paginated
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The UserProfiles 
	 * @throws DatastoreException - Thrown when there is a server-side problem.
	 */
	public PaginatedResults<UserProfile> getUserProfilesPaginated(
			HttpServletRequest request, String userId, Integer offset,
			Integer limit, String sort, Boolean ascending)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get a user profile
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The updated UserProfile
	 * @throws DatastoreException - Thrown when there is a server-side problem.
	 */
	public UserProfile updateUserProfile(String userId, HttpHeaders header, HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, IOException;

	/**
	 * Create a security token for use for a particular with a particular
	 * locationable user profile picture to be stored in AWS S3
	 * 
	 * @param userId
	 * @param id
	 * @param etag
	 * @param s3Token
	 * @param request
	 * @return a filled-in S3Token
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	public S3AttachmentToken createUserProfileS3AttachmentToken(String userId,
			String profileId, S3AttachmentToken token,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException;
	/**
	 * Create a token used to upload an attachment.
	 * 
	 * @param userId
	 * @param id
	 * @param token
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	public PresignedUrl getUserProfileAttachmentUrl(String userId, 
			String profileId, PresignedUrl url, HttpServletRequest request)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException;

	/**
	 * Batch get headers for users matching a list of supplied Synapse IDs.
	 * 
	 * @param ids
	 * @return
	 */
	public UserGroupHeaderResponsePage getUserGroupHeadersByIds(List<String> ids)
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
			String prefixFilter, Integer offset, Integer limit,
			HttpHeaders header, HttpServletRequest request)
			throws DatastoreException, NotFoundException;

	/**
	 * Populate a cache of headers for all Synapse users.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void refreshCache() throws DatastoreException, NotFoundException;

	/**
	 * Get the time (in milliseconds) since the user/group header cache was last
	 * updated. Returns null if the cache has not yet been populated.
	 * 
	 * @return
	 */
	public Long millisSinceLastCacheUpdate();

	public void setObjectTypeSerializer(ObjectTypeSerializer objectTypeSerializer);

	public void setPermissionsManager(PermissionsManager permissionsManager);

	public void setUserManager(UserManager userManager);

	public void setUserProfileManager(UserProfileManager userProfileManager);	

}
