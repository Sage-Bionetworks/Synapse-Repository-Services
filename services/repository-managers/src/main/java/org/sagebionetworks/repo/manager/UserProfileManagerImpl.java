package org.sagebionetworks.repo.manager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
		return userProfile;
	}
	
	@Override
	public QueryResults<UserProfile> getInRange(UserInfo userInfo, long startIncl, long endExcl) throws DatastoreException, NotFoundException{
		List<UserProfile> userProfiles = userProfileDAO.getInRange(startIncl, endExcl);
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
		Long principalId = Long.parseLong(updated.getOwnerId());
		boolean canUpdate = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, updated.getOwnerId());
		if (!canUpdate) throw new UnauthorizedException("Only owner or administrator may update UserProfile.");
		attachmentManager.checkAttachmentsForPreviews(updated);
		// Update the DAO first
		userProfileDAO.update(updated);
		// Bind all aliases
		List<PrincipalAlias> newEmails = bindAllAliases(updated, principalId);
		// We have temporarily turned-off the ability to add new email. See PLFM-2405
		if(newEmails.size() > 0){
			throw new IllegalArgumentException("Adding new email addresses to a UserProfile is currently disabled.");
		}
		
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
		Long principalId = Long.parseLong(profile.getOwnerId());
		bindAllAliases(profile, principalId);
	
		// Save the profile
		this.userProfileDAO.create(profile);
		try {
			return getUserProfilePrivate(profile.getOwnerId());
		} catch (NotFoundException e) {
			throw new DatastoreException(e);
		}
	}

	/**
	 * This method is idempotent.
	 * @param profile
	 * @param principalId
	 */
	private List<PrincipalAlias> bindAllAliases(UserProfile profile, Long principalId) {
		validateProfile(profile);
		// Bind all aliases
		bindUserName(profile.getUserName(), principalId);
		List<PrincipalAlias> newEmails = new LinkedList<PrincipalAlias>();
		newEmails.addAll(bindAliases(principalId, profile.getEmails(), AliasType.USER_EMAIL));
		// A user might not have any open IDs.
		if(profile.getOpenIds() != null){
			bindAliases(principalId, profile.getOpenIds(), AliasType.USER_OPEN_ID);
		}
		return newEmails;
	}

	private void bindUserName(String username, Long principalId) {
		// bind the username to this user
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(username);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		alias.setType(AliasType.USER_NAME);
		try {
			principalAliasDAO.bindAliasToPrincipal(alias);
		} catch (NotFoundException e1) {
			throw new DatastoreException(e1);
		}
	}
	
	/**
	 * 
	 * @param emails
	 * @param principalId
	 */
	private List<PrincipalAlias> bindAliases(Long principalId, List<String> aliases, AliasType type) {
		List<PrincipalAlias> currentAliases = principalAliasDAO.listPrincipalAliases(principalId);;
		Map<String, PrincipalAlias> map = new HashMap<String, PrincipalAlias>();
		// Map the current by name
		for(PrincipalAlias alias: currentAliases){
			map.put(alias.getAlias(), alias);
		}
		List<PrincipalAlias> newAliases = new LinkedList<PrincipalAlias>();
		// Bind all all new
		for(String aliasValue: aliases){
			// First determine if this alias already exists?
			PrincipalAlias alias = map.get(aliasValue);
			if(alias == null){
				// This is a new alias so bind it.
				alias = new PrincipalAlias();
				alias.setAlias(aliasValue);
				alias.setIsValidated(false);
				alias.setPrincipalId(principalId);
				alias.setType(type);
				// bind this alias
				try {
					alias = principalAliasDAO.bindAliasToPrincipal(alias);
					newAliases.add(alias);
				} catch (NotFoundException e1) {
					throw new DatastoreException(e1);
				}
			}
		}
		return newAliases;
	}
	
	private void validateProfile(UserProfile profile) {
		if(profile == null) throw new IllegalArgumentException("UserProfile cannot be null");
		if(profile.getOwnerId() == null) throw new IllegalArgumentException("OwnerId cannot be null");
		if(profile.getUserName() == null) throw new IllegalArgumentException("Username cannot be null");
		if(profile.getEmails() == null) throw new IllegalArgumentException("Emails cannot be null");
		if(profile.getEmails().size() < 1) throw new IllegalArgumentException("A user profile must contain at least one email");
	}

}
