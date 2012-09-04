package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserProfileManager {

	/**
	 * Get an existing UserProfile
	 * @param userInfo
	 * @param ownerid
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public UserProfile getUserProfile(UserInfo userInfo, String ownerid) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the public profiles of the users in the system, paginated. Default to
	 * not include e-mail addresses.
	 * 
	 * @param userInfo
	 * @param startIncl
	 * @param endExcl
	 * @param sort
	 * @param ascending
	 * @return
	 */
	public QueryResults<UserProfile> getInRange(UserInfo userInfo, long startIncl, long endExcl) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the public profiles of the users in the system, paginated.
	 * 
	 * @param userInfo
	 * @param startIncl
	 * @param endExcl
	 * @param includeEmail
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public QueryResults<UserProfile> getInRange(UserInfo userInfo, long startIncl, long endExcl, boolean includeEmail) throws DatastoreException, NotFoundException;

	/**
	 * Update a UserProfile.
	 * @param userInfo
	 * @param updated
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	public UserProfile updateUserProfile(UserInfo userInfo, UserProfile updated) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException, InvalidModelException;

	/**
	 * userId may not match the profileId
	 * @param userId
	 * @param userInfo
	 * @param profileId
	 * @param token
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	public S3AttachmentToken createS3UserProfileAttachmentToken(
			UserInfo userInfo, String profileId, S3AttachmentToken token)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException;

	/**
	 * return the preassigned url for the user profile attachment
	 * @param userInfo
	 * @param profileId
	 * @param tokenID
	 * @return
	 * @throws InvalidModelException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public PresignedUrl getUserProfileAttachmentUrl(UserInfo userInfo,
			String profileId, String tokenID) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;
	
}
