package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.collect.Sets;

public class UserProfileManagerImpl implements UserProfileManager {

	
	@Autowired
	private UserProfileDAO userProfileDAO;
	@Autowired
	private FavoriteDAO favoriteDAO;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private FileHandleManager fileHandleManager;
	

	@Override
	public UserProfile getUserProfile(String ownerId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
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
		} else if (alias.getType().equals(AliasType.USER_ORCID)) {
			// not added to user profile
		} else {
			throw new IllegalStateException("Expected user name, email or open id but found "+alias.getType());
		}
	}
	
	/**
	 * Extract the file name from the keys
	 * @param key
	 * @return
	 */
	public static String extractFileNameFromKey(String key){
		if(key == null){
			return null;
		}
		String[] slash = key.split("/");
		if(slash.length > 0){
			return slash[slash.length-1];
		}
		return null;
	}

	private void addAliasesToProfiles(List<UserProfile> userProfiles) {
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
	}
	@Override
	public List<UserProfile> getInRange(UserInfo userInfo, long startIncl, long endExcl) throws DatastoreException, NotFoundException{
		List<UserProfile> userProfiles = userProfileDAO.getInRange(startIncl, endExcl);
		addAliasesToProfiles(userProfiles);
		return userProfiles;
	}
	/**
	 * List the UserProfiles for the given IDs
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public ListWrapper<UserProfile> list(IdList ids) throws DatastoreException, NotFoundException {
		List<UserProfile> userProfiles = userProfileDAO.list(ids.getList());
		addAliasesToProfiles(userProfiles);
		return ListWrapper.wrap(userProfiles, UserProfile.class);
	}
	/**
	 * This method is only available to the object owner or an admin
	 */
	@WriteTransaction
	public UserProfile updateUserProfile(UserInfo userInfo, UserProfile updated) 
			throws DatastoreException, UnauthorizedException, InvalidModelException, NotFoundException {
		validateProfile(updated);
		Long principalId = Long.parseLong(updated.getOwnerId());
		String updatedUserName = updated.getUserName();
		clearAliasFields(updated);
		boolean canUpdate = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, updated.getOwnerId());
		if (!canUpdate) throw new UnauthorizedException("Only owner or administrator may update UserProfile.");
		
		if(updated.getProfilePicureFileHandleId() != null){
			// The user must own the file handle to set it as a picture.
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(authorizationManager.canAccessRawFileHandleById(userInfo, updated.getProfilePicureFileHandleId()));
		}
		// Update the DAO first
		userProfileDAO.update(updated);
		// Bind all aliases
		bindUserName(updatedUserName, principalId);
		// Get the updated value
		return getUserProfilePrivate(updated.getOwnerId());
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
	public PaginatedResults<ProjectHeader> getProjects(UserInfo caller,
			UserInfo userToGetInfoFor, Team teamToFetch, ProjectListType type,
			ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Long limit, Long offset) throws DatastoreException,
			InvalidModelException, NotFoundException {
		/*
		 * The current users's princpalIds minus PUBLIC, AUTHENTICATED_USERS, /*
		 * and CERTIFIED_USERS
		 */
		Set<Long> userToGetPrincipalIds;
		switch (type) {
		case MY_PROJECTS:
		case OTHER_USER_PROJECTS:
		case MY_CREATED_PROJECTS:
		case MY_PARTICIPATED_PROJECTS:
			userToGetPrincipalIds = getGroupsMinusPublic(userToGetInfoFor.getGroups());
			break;
		case MY_TEAM_PROJECTS:
			userToGetPrincipalIds = getGroupsMinusPublicAndSelf(userToGetInfoFor.getGroups(), userToGetInfoFor.getId());
			break;
		case TEAM_PROJECTS:
			// this case requires a team
			ValidateArgument.required(teamToFetch, "teamToFetch");
			long teamId = Long.parseLong(teamToFetch.getId());
			userToGetPrincipalIds = Sets.newHashSet(teamId);
			break;
		default:
			throw new NotImplementedException("project list type " + type
					+ " not yet implemented");
		}
		// Find the projectIds accessible to the user-to-get-for.
		Set<Long> userToGetAccessibleProjectIds = authorizationManager.getAccessibleProjectIds(userToGetPrincipalIds);
		Set<Long> projectIdsToFilterBy = null;
		if (!caller.isAdmin()
				&& !caller.getId().equals(userToGetInfoFor.getId())) {
			/*
			 * The caller is not an administrator and the caller is not the same
			 * as the user-to-get-for. Therefore, the return projects must only
			 * include the intersection of projects that both users can read.
			 */
			Set<Long> currentUserAccessibleProjectIds = authorizationManager.getAccessibleProjectIds(caller.getGroups());
			// use the intersection.
			projectIdsToFilterBy = Sets.intersection(userToGetAccessibleProjectIds,	currentUserAccessibleProjectIds);
		} else {
			/*
			 * The caller is either an administrator or the same user as the
			 * user-to-for. Therefore, the return projects include any project
			 * that the user-to-get-for can read.
			 */
			projectIdsToFilterBy = userToGetAccessibleProjectIds;
		}
		// run the query.
		List<ProjectHeader> page = nodeDao.getProjectHeaders(caller.getId(),
				projectIdsToFilterBy, type, sortColumn, sortDirection, limit,
				offset);
		return PaginatedResults.createWithLimitAndOffset(page, limit, offset);
	}

	/**
	 * Remove PUBLIC_GROUP, AUTHENTICATED_USERS_GROUP, and CERTIFIED_USERS from the passed set.
	 * @param usersGroups
	 * @return
	 */
	public static Set<Long> getGroupsMinusPublic(Set<Long> usersGroups){
		Set<Long> groups = Sets.newHashSet(usersGroups);
		groups.remove(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId().longValue());
		groups.remove(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().longValue());
		groups.remove(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().longValue());
		return groups;
	}

	/**
	 * Remove PUBLIC_GROUP, AUTHENTICATED_USERS_GROUP, and CERTIFIED_USERS, and the passed UserId
	 * from the passed principal IDs.
	 * @param usersGroups
	 * @param userId
	 * @return
	 */
	public static Set<Long> getGroupsMinusPublicAndSelf(Set<Long> usersGroups, final long userId) {
		Set<Long> groups = getGroupsMinusPublic(usersGroups);
		groups.remove(userId);
		return groups;
	}

	@WriteTransaction
	@Override
	public UserProfile createUserProfile(UserProfile profile) {
		validateProfile(profile);
		clearAliasFields(profile);
		// Save the profile
		this.userProfileDAO.create(profile);
		return getUserProfilePrivate(profile.getOwnerId());
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

	@Override
	public String getUserProfileImageUrl(String userId)
			throws NotFoundException {
		String handleId = userProfileDAO.getPictureFileHandleId(userId);
		return fileHandleManager.getRedirectURLForFileHandle(handleId);
	}

	@Override
	public String getUserProfileImagePreviewUrl(String userId)
			throws NotFoundException {
		String handleId = userProfileDAO.getPictureFileHandleId(userId);
		String privewId = fileHandleManager.getPreviewFileHandleId(handleId);
		return fileHandleManager.getRedirectURLForFileHandle(privewId);
	}

}
