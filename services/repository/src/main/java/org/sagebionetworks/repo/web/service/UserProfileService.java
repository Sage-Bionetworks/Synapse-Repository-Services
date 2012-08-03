package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.NotFoundException;
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
	public UserProfile updateUserProfile(String userId, HttpHeaders header,
			String etag, HttpServletRequest request) throws NotFoundException,
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

	
	

}
