package org.sagebionetworks.repo.manager;

import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
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
	 * @throws IOException 
	 */
	public UserProfile updateUserProfile(UserInfo userInfo, UserProfile updated) throws DatastoreException, UnauthorizedException, InvalidModelException, NotFoundException, AuthenticationException, IOException, XPathExpressionException;

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
	
	/**
	 * Adds the entity id to the users's favorites list
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public Favorite addFavorite(UserInfo userInfo, String entityId) throws DatastoreException, InvalidModelException;
	
	/**
	 * Removes the specified entity id from the users's favorites list, if exists
	 * @param userInfo
	 * @param entityId
	 * @throws DatastoreException
	 */
	public void removeFavorite(UserInfo userInfo, String entityId) throws DatastoreException;

	/**
	 * Retrieve users list of favorites, paginated
	 * @param userInfo
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public PaginatedResults<EntityHeader> getFavorites(UserInfo userInfo, int limit, int offset) throws DatastoreException, InvalidModelException, NotFoundException;

}
