package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class UserProfileManagerImpl implements UserProfileManager {
	

	@Autowired
	private UserProfileDAO userProfileDAO;
		
	@Autowired
	private S3TokenManager s3TokenManager;
	
	@Autowired
	private AttachmentManager attachmentManager;
	
	@Autowired
	private FavoriteDAO favoriteDAO;
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
	public UserProfileManagerImpl() {
	}

	/**
	 * Used by unit tests
	 */
	public UserProfileManagerImpl(UserProfileDAO userProfileDAO, UserGroupDAO userGroupDAO,
			S3TokenManager s3TokenManager, FavoriteDAO favoriteDAO, AttachmentManager attachmentManager, PrincipalAliasDAO principalAliasDAO) {
		super();
		this.userProfileDAO = userProfileDAO;
		this.s3TokenManager = s3TokenManager;
		this.favoriteDAO = favoriteDAO;
		this.attachmentManager = attachmentManager;
		this.principalAliasDAO = principalAliasDAO;
	}

	@Override
	public UserProfile getUserProfile(UserInfo userInfo, String ownerId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		if(userInfo == null) throw new IllegalArgumentException("userInfo can not be null");
		if(ownerId == null) throw new IllegalArgumentException("ownerId can not be null");
		return getUserProfilePrivate(ownerId);
	}

	private UserProfile getUserProfilePrivate(String ownerId)
			throws NotFoundException {
		UserProfile userProfile = userProfileDAO.get(ownerId);
		List<PrincipalAlias> aliases = principalAliasDAO.
				listPrincipalAliases(Long.parseLong(ownerId));
		List<String> emails = new ArrayList<String>();
		List<String> openIds = new ArrayList<String>();
		for (PrincipalAlias alias : aliases) {
			if (true /*alias.getIsValidated() this is to be restored as part of PLFM-2487*/) {
				if (alias.getType().equals(AliasType.USER_EMAIL)) {
					emails.add(alias.getAlias());
				} else if (alias.getType().equals(AliasType.USER_OPEN_ID)) {
					openIds.add(alias.getAlias());
				}
			}
		}
		userProfile.setEmails(emails);
		userProfile.setOpenIds(openIds);
		return userProfile;
	}
	
	@Override
	public QueryResults<UserProfile> getInRange(UserInfo userInfo, long startIncl, long endExcl) throws DatastoreException, NotFoundException{
		List<UserProfile> userProfiles = userProfileDAO.getInRange(startIncl, endExcl);
		Set<Long> principalIds = new HashSet<Long>();
		Map<Long,List<String>> profileIdToEmailListMap = new HashMap<Long,List<String>>();
		Map<Long,List<String>> profileIdToOpenIDListMap = new HashMap<Long,List<String>>();
		for (UserProfile profile : userProfiles) {
			Long ownerIdLong = Long.parseLong(profile.getOwnerId());
			principalIds.add(ownerIdLong);
			List<String> profileEmails = new ArrayList<String>();
			profile.setEmails(profileEmails);
			// add to a map so we can find quickly, below
			profileIdToEmailListMap.put(ownerIdLong, profileEmails);
			List<String> openIds = new ArrayList<String>();
			profile.setOpenIds(openIds);
			// add to a map so we can find quickly, below
			profileIdToOpenIDListMap.put(ownerIdLong, openIds);
		}
		for (PrincipalAlias alias : principalAliasDAO.listPrincipalAliases(principalIds)) {
			if (true/*alias.getIsValidated() this is to be restored as part of PLFM-2487*/) {
				if (alias.getType().equals(AliasType.USER_EMAIL)) {
					profileIdToEmailListMap.get(alias.getPrincipalId()).add(alias.getAlias());
				} else if (alias.getType().equals(AliasType.USER_OPEN_ID)) {
					profileIdToOpenIDListMap.get(alias.getPrincipalId()).add(alias.getAlias());
				}
			}
		}
		long totalNumberOfResults = userProfileDAO.getCount();
		QueryResults<UserProfile> result = new QueryResults<UserProfile>(userProfiles, (int)totalNumberOfResults);
		return result;
	}

	/**
	 * This method is only available to the object owner or an admin
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserProfile updateUserProfile(UserInfo userInfo, UserProfile updated) 
			throws DatastoreException, UnauthorizedException, InvalidModelException, NotFoundException {
		validateProfile(updated);
		clearAliasFields(updated);
		boolean canUpdate = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, updated.getOwnerId());
		if (!canUpdate) throw new UnauthorizedException("Only owner or administrator may update UserProfile.");
		attachmentManager.checkAttachmentsForPreviews(updated);
		// Update the DAO first
		userProfileDAO.update(updated);
		
		// Get the updated value
		return getUserProfilePrivate(updated.getOwnerId());
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
		return s3TokenManager.createS3AttachmentToken(userInfo.getId(), profileId, token);
	}
	
	@Override
	public PresignedUrl getUserProfileAttachmentUrl(Long userId,
			String profileId, String tokenID) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		//anyone can see the public profile pictures
		return s3TokenManager.getAttachmentUrl(userId, profileId, tokenID);
	}

	@Override
	public Favorite addFavorite(UserInfo userInfo, String entityId)
			throws DatastoreException, InvalidModelException {
		Favorite favorite = new Favorite();
		favorite.setPrincipalId(userInfo.getId().toString());
		favorite.setEntityId(entityId);
		return favoriteDAO.add(favorite);
	}

	@Override
	public void removeFavorite(UserInfo userInfo, String entityId)
			throws DatastoreException {
		favoriteDAO.remove(userInfo.getId().toString(), entityId);
	}

	@Override
	public PaginatedResults<EntityHeader> getFavorites(UserInfo userInfo,
			int limit, int offset) throws DatastoreException,
			InvalidModelException, NotFoundException {
		return favoriteDAO.getFavoritesEntityHeader(userInfo.getId().toString(), limit, offset);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserProfile createUserProfile(UserProfile profile) {
		validateProfile(profile);
		clearAliasFields(profile);
		// Save the profile
		this.userProfileDAO.create(profile);
		try {
			return getUserProfilePrivate(profile.getOwnerId());
		} catch (NotFoundException e) {
			throw new DatastoreException(e);
		}
	}

	private void validateProfile(UserProfile profile) {
		if(profile == null) throw new IllegalArgumentException("UserProfile cannot be null");
		if(profile.getOwnerId() == null) throw new IllegalArgumentException("OwnerId cannot be null");
		if(profile.getUserName() == null) throw new IllegalArgumentException("Username cannot be null");
	}
	
	private void clearAliasFields(UserProfile profile) {
		profile.setEmail(null);
		profile.setEmails(null);
		profile.setOpenIds(null);
	}

}
