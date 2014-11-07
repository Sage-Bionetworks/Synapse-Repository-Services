package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.PaginatedResultsUtil;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Team;
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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

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
	private NodeDAO nodeDao;

	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;

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
		userProfile.setEmails(new ArrayList<String>());
		userProfile.setOpenIds(new ArrayList<String>());
		for (PrincipalAlias alias : aliases) {
			insertAliasIntoProfile(userProfile, alias);
		}
		return userProfile;
	}
	
	private static void insertAliasIntoProfile(UserProfile profile, PrincipalAlias alias) {
		String aliasName = alias.getAlias();
		if (alias.getType().equals(AliasType.USER_NAME)) {
			profile.setUserName(aliasName);
		} else if (alias.getType().equals(AliasType.USER_EMAIL)) {
			profile.getEmails().add(aliasName);
		} else if (alias.getType().equals(AliasType.USER_OPEN_ID)) {
			profile.getOpenIds().add(aliasName);
		} else {
			throw new IllegalStateException("Expected user name, email or open id but found "+alias.getType());
		}
	}
	
	@Override
	public QueryResults<UserProfile> getInRange(UserInfo userInfo, long startIncl, long endExcl) throws DatastoreException, NotFoundException{
		List<UserProfile> userProfiles = userProfileDAO.getInRange(startIncl, endExcl);
		Set<Long> principalIds = new HashSet<Long>();
		Map<Long,UserProfile> profileMap = new HashMap<Long,UserProfile>();
		for (UserProfile profile : userProfiles) {
			Long ownerIdLong = Long.parseLong(profile.getOwnerId());
			principalIds.add(ownerIdLong);
			profile.setUserName(null);
			profile.setEmails(new ArrayList<String>());
			profile.setOpenIds(new ArrayList<String>());
			// add to a map so we can find quickly, below
			profileMap.put(ownerIdLong, profile);
		}
		for (PrincipalAlias alias : principalAliasDAO.listPrincipalAliases(principalIds)) {
			UserProfile profile = profileMap.get(alias.getPrincipalId());
			insertAliasIntoProfile(profile, alias);
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
		Long principalId = Long.parseLong(updated.getOwnerId());
		String updatedUserName = updated.getUserName();
		clearAliasFields(updated);
		boolean canUpdate = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, updated.getOwnerId());
		if (!canUpdate) throw new UnauthorizedException("Only owner or administrator may update UserProfile.");
		attachmentManager.checkAttachmentsForPreviews(updated);
		// Update the DAO first
		userProfileDAO.update(updated);
		// Bind all aliases
		bindUserName(updatedUserName, principalId);
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

	@Override
	public PaginatedResults<ProjectHeader> getMyProjects(final UserInfo userInfo, int limit, int offset) throws DatastoreException,
			InvalidModelException, NotFoundException {
		PaginatedResults<ProjectHeader> projectHeaders = nodeDao.getMyProjectHeaders(userInfo, limit, offset);
		return projectHeaders;
	}

	@Override
	public PaginatedResults<ProjectHeader> getProjectsForUser(final UserInfo userInfo, UserInfo userToFetch, int limit, int offset)
			throws DatastoreException, InvalidModelException, NotFoundException {
		PaginatedResults<ProjectHeader> projectHeaders = nodeDao.getProjectHeadersForUser(userToFetch, userInfo, limit, offset);
		return projectHeaders;
	}

	@Override
	public PaginatedResults<ProjectHeader> getProjectsForTeam(final UserInfo userInfo, Team teamToFetch, int limit, int offset)
			throws DatastoreException, InvalidModelException, NotFoundException {
		PaginatedResults<ProjectHeader> projectHeaders = nodeDao.getProjectHeadersForTeam(teamToFetch, userInfo, limit, offset);
		return projectHeaders;
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

	private void bindUserName(String username, Long principalId) {
		// bind the username to this user
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(username);
		alias.setPrincipalId(principalId);
		alias.setType(AliasType.USER_NAME);
		try {
			principalAliasDAO.bindAliasToPrincipal(alias);
		} catch (NotFoundException e1) {
			throw new DatastoreException(e1);
		}
	}
	
	private void validateProfile(UserProfile profile) {
		if(profile == null) throw new IllegalArgumentException("UserProfile cannot be null");
		if(profile.getOwnerId() == null) throw new IllegalArgumentException("OwnerId cannot be null");
		if(profile.getUserName() == null) throw new IllegalArgumentException("Username cannot be null");
	}
	
	private void clearAliasFields(UserProfile profile) {
		profile.setUserName(null);
		profile.setEmail(null);
		profile.setEmails(null);
		profile.setOpenIds(null);
	}

}
