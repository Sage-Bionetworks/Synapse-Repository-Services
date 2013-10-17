package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class UserProfileManagerImpl implements UserProfileManager {
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private S3TokenManager s3TokenManager;
	
	@Autowired
	private AttachmentManager attachmentManager;
	
	@Autowired
	private FavoriteDAO favoriteDAO;
	
	public UserProfileManagerImpl() {
	}

	/**
	 * Used by unit tests
	 */
	public UserProfileManagerImpl(UserProfileDAO userProfileDAO, UserGroupDAO userGroupDAO,
			S3TokenManager s3TokenManager, FavoriteDAO favoriteDAO, AttachmentManager attachmentManager) {
		super();
		this.userProfileDAO = userProfileDAO;
		this.userGroupDAO = userGroupDAO;
		this.s3TokenManager = s3TokenManager;
		this.favoriteDAO = favoriteDAO;
		this.attachmentManager = attachmentManager;
	}

	@Override
	public UserProfile getUserProfile(UserInfo userInfo, String ownerId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		if(userInfo == null) throw new IllegalArgumentException("userInfo can not be null");
		if(ownerId == null) throw new IllegalArgumentException("ownerId can not be null");
		
		UserProfile userProfile = userProfileDAO.get(ownerId);
		UserGroup userGroup = userGroupDAO.get(ownerId);
		if (userGroup != null) {
			userProfile.setEmail(userGroup.getName());
			userProfile.setUserName(userGroup.getName());
		}
		return userProfile;
	}
	
	@Override
	public QueryResults<UserProfile> getInRange(UserInfo userInfo, long startIncl, long endExcl, boolean includeEmail) throws DatastoreException, NotFoundException{
		List<UserProfile> userProfiles = userProfileDAO.getInRange(startIncl, endExcl);
		long totalNumberOfResults = userProfileDAO.getCount();
		for (UserProfile userProfile : userProfiles) {
			if (includeEmail) {
				UserGroup userGroup = userGroupDAO.get(userProfile.getOwnerId());
				if (userGroup != null)
					userProfile.setEmail(userGroup.getName());
			}
		}
		QueryResults<UserProfile> result = new QueryResults<UserProfile>(userProfiles, (int)totalNumberOfResults);
		return result;
	}
	
	@Override
	public QueryResults<UserProfile> getInRange(UserInfo userInfo, long startIncl, long endExcl) 
			throws DatastoreException, NotFoundException {
		return getInRange(userInfo, startIncl, endExcl, false);
	}
	

	/**
	 * This method is only available to the object owner or an admin
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserProfile updateUserProfile(UserInfo userInfo, UserProfile updated) 
			throws DatastoreException, UnauthorizedException, InvalidModelException, NotFoundException {
		UserProfile userProfile = userProfileDAO.get(updated.getOwnerId());
		boolean canUpdate = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, userProfile.getOwnerId());
		if (!canUpdate) throw new UnauthorizedException("Only owner or administrator may update UserProfile.");
		String oldEmail = userInfo.getIndividualGroup().getName();
		attachmentManager.checkAttachmentsForPreviews(updated);
		UserProfile returnProfile = userProfileDAO.update(updated);
		
		//and update email if it is also set (and is different)
		if (updated.getEmail() != null && !updated.getEmail().equals(oldEmail)) {
			userManager.updateEmail(userInfo, updated.getEmail());
		}
		
		return returnProfile;
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
