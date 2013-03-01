/**
 * 
 */
package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.util.StringUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author brucehoff
 *
 */
public class UserProfileManagerImpl implements UserProfileManager {
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private UserProfileDAO userProfileDAO;
	@Autowired
	private S3TokenManager s3TokenManager;
	@Autowired
	AttachmentManager attachmentManager;
	@Autowired
	private FavoriteDAO favoriteDAO;
	@Autowired 
	private NodeDAO nodeDAO;
	
	public UserProfileManagerImpl() {
	}

	/**
	 * Used by unit tests
	 * @param userProfileDAO
	 * @param s3TokenManager
	 */
	public UserProfileManagerImpl(UserProfileDAO userProfileDAO,
			S3TokenManager s3TokenManager, FavoriteDAO favoriteDAO) {
		super();
		this.userProfileDAO = userProfileDAO;
		this.s3TokenManager = s3TokenManager;
		this.favoriteDAO = favoriteDAO;
	}

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
		UserGroup userGroup = userGroupDAO.get(ownerId);
		if (userInfo != null && userInfo.getUser() != null) {
			userProfile.setUserName(userInfo.getUser().getUserId());
			if (userGroup != null)
				userProfile.setEmail(userGroup.getName());
		}
		boolean canSeePrivate = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, userProfile.getOwnerId());
		if (!canSeePrivate) {
			UserProfileManagerUtils.clearPrivateFields(userProfile);
			if (userGroup != null)
				userProfile.setEmail(StringUtil.obfuscateEmailAddress(userGroup.getName()));
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
		anonymousUserProfile.setEmail(AuthorizationConstants.ANONYMOUS_USER_ID);
		anonymousUserProfile.setDisplayName(AuthorizationConstants.ANONYMOUS_USER_DISPLAY_NAME);
		anonymousUserProfile.setFirstName(AuthorizationConstants.ANONYMOUS_USER_DISPLAY_NAME);
		anonymousUserProfile.setLastName("");
		return anonymousUserProfile;
	}
	
	@Override
	public QueryResults<UserProfile> getInRange(UserInfo userInfo, long startIncl, long endExcl, boolean includeEmail) throws DatastoreException, NotFoundException{
		ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		List<UserProfile> userProfiles = userProfileDAO.getInRange(startIncl, endExcl, schema);
		long totalNumberOfResults = userProfileDAO.getCount();
		for (UserProfile userProfile : userProfiles) {
			UserProfileManagerUtils.clearPrivateFields(userProfile);
			if (includeEmail) {
				UserGroup userGroup = userGroupDAO.get(userProfile.getOwnerId());
				if (userGroup != null)
					userProfile.setEmail(StringUtil.obfuscateEmailAddress(userGroup.getName()));
			}
		}
		QueryResults<UserProfile> result = new QueryResults<UserProfile>(userProfiles, (int)totalNumberOfResults);
		return result;
	}
	
	@Override
	public QueryResults<UserProfile> getInRange(UserInfo userInfo, long startIncl, long endExcl) throws DatastoreException, NotFoundException{
		return getInRange(userInfo, startIncl, endExcl, false);
	}
	

	/**
	 * This method is only available to the object owner or an admin
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
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

	@Override
	public Favorite addFavorite(UserInfo userInfo, String entityId)
			throws DatastoreException, InvalidModelException {
		Favorite favorite = new Favorite();
		favorite.setPrincipalId(userInfo.getIndividualGroup().getId());
		favorite.setEntityId(entityId);
		return favoriteDAO.add(favorite);
	}

	@Override
	public void removeFavorite(UserInfo userInfo, String entityId)
			throws DatastoreException {
		favoriteDAO.remove(userInfo.getIndividualGroup().getId(), entityId);
	}

	@Override
	public PaginatedResults<EntityHeader> getFavorites(UserInfo userInfo,
			int limit, int offset) throws DatastoreException,
			InvalidModelException, NotFoundException {
		return favoriteDAO.getFavoritesEntityHeader(userInfo.getIndividualGroup().getId(), limit, offset);
	}
}
