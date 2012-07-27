/**
 * 
 */
package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author brucehoff
 *
 */
public class UserProfileManagerImpl implements UserProfileManager {
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	@Autowired
	private S3TokenManager s3TokenManager;
	@Autowired
	AttachmentManager attachmentManager;
	
	public UserProfileManagerImpl() {
	}

	/**
	 * Used by unit tests
	 * @param userProfileDAO
	 * @param s3TokenManager
	 */
	public UserProfileManagerImpl(UserProfileDAO userProfileDAO,
			S3TokenManager s3TokenManager) {
		super();
		this.userProfileDAO = userProfileDAO;
		this.s3TokenManager = s3TokenManager;
	}

	/***
	 *
	 * This method retrieves the JDO from the object ID and transfers it to the DTO, filtering
	 * out private fields if the 'userInfo' is not for the object owner or an admin 
	 *
	 */
	@Override
	public UserProfile getUserProfile(UserInfo userInfo, String ownerId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		//if the user is set, and it's the anonymous user, then return an anonymous profile
		if (userInfo != null && userInfo.getUser() != null && userInfo.getUser().getUserId() != null && 
				AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getUser().getUserId())){
			return getAnonymousUserProfile(userInfo.getIndividualGroup().getId());
		}
		
		ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		UserProfile userProfile = userProfileDAO.get(ownerId, schema);
		boolean canSeePrivate = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, userProfile.getOwnerId());
		if (!canSeePrivate) {
			UserProfileManagerUtils.clearPrivateFields(userProfile);
		}
		return userProfile;
	}

	/**
	 * Returns the anonymous user's profile - essentially an empty profile that points to the correct principle id
	 * @param principleId
	 * @return
	 */
	private UserProfile getAnonymousUserProfile(String principleId){
		UserProfile anonymousUserProfile = new UserProfile();
		anonymousUserProfile.setOwnerId(principleId);
		anonymousUserProfile.setUserName(AuthorizationConstants.ANONYMOUS_USER_ID);
		anonymousUserProfile.setEmail(AuthorizationConstants.ANONYMOUS_USER_ID);
		anonymousUserProfile.setDisplayName(AuthorizationConstants.ANONYMOUS_USER_DISPLAY_NAME);
		anonymousUserProfile.setFirstName(AuthorizationConstants.ANONYMOUS_USER_DISPLAY_NAME);
		anonymousUserProfile.setLastName("");
		return anonymousUserProfile;
	}
	
	/**
	 * Get the public profiles of the users in the system, paginated
	 * 
	 * @param userInfo
	 * @param startIncl
	 * @param endExcl
	 * @param sort
	 * @param ascending
	 * @return
	 */
	@Override
	public QueryResults<UserProfile> getInRange(UserInfo userInfo, long startIncl, long endExcl) throws DatastoreException, NotFoundException{
		ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		List<UserProfile> userProfiles = userProfileDAO.getInRange(startIncl, endExcl, schema);
		long totalNumberOfResults = userProfileDAO.getCount();
		for (UserProfile userProfile : userProfiles) {
			UserProfileManagerUtils.clearPrivateFields(userProfile);
		}
		QueryResults<UserProfile> result = new QueryResults<UserProfile>(userProfiles, (int)totalNumberOfResults);
		return result;
	}
	

	/**
	 * 
	 * This method is only available to the object owner or an admin
	 * 
	 */
	@Override
	public UserProfile updateUserProfile(UserInfo userInfo, UserProfile updated)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ConflictingUpdateException,
			InvalidModelException {
		ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		UserProfile userProfile = userProfileDAO.get(updated.getOwnerId(), schema);
		boolean canUpdate = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, userProfile.getOwnerId());
		if (!canUpdate) throw new UnauthorizedException("Only owner or administrator may update UserProfile.");
		attachmentManager.checkAttachmentsForPreviews(updated);
		return userProfileDAO.update(updated, schema);
	}


	@Override
	public S3AttachmentToken createS3UserProfileAttachmentToken(
			UserInfo userInfo, String profileId, S3AttachmentToken token)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException {
		boolean isOwnerOrAdmin = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, profileId);
		if (!isOwnerOrAdmin)
			throw new UnauthorizedException("Can't assign attachment to another user's profile");
		if (!AttachmentManagerImpl.isPreviewType(token.getFileName()))
			throw new IllegalArgumentException("User profile attachment is not a recognized image type, please try a different file.");
		return s3TokenManager.createS3AttachmentToken(userInfo.getIndividualGroup().getId(), profileId, token);
	}
	
	@Override
	public PresignedUrl getUserProfileAttachmentUrl(UserInfo userInfo,
			String profileId, String tokenID) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		//anyone can see the public profile pictures
		return s3TokenManager.getAttachmentUrl(userInfo, profileId, tokenID);
	}
}
