package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.CertifiedUserManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.UserProfileManagerUtils;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ResponseMessage;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserBundle;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.util.SignedTokenUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

public class UserProfileServiceImpl implements UserProfileService {

	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	private VerificationDAO verificationDao;
	
	@Autowired
	PrincipalAliasDAO principalAliasDAO;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private TeamManager teamManager;

	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	
	@Autowired
	private ObjectTypeSerializer objectTypeSerializer;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	PrincipalPrefixDAO principalPrefixDAO;
	
	@Autowired
	UserGroupDAO userGroupDao;
	
	@Override
	public UserProfile getMyOwnUserProfile(Long userId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getUserProfile(userInfo.getId().toString());
	}
	
	@Override
	public UserProfile getUserProfileByOwnerId(Long userId, String profileId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserProfile userProfile = userProfileManager.getUserProfile(profileId);
		UserProfileManagerUtils.clearPrivateFields(userInfo, userProfile);
		return userProfile;
	}
	
	
	@Override
	public PaginatedResults<UserProfile> getUserProfilesPaginated(HttpServletRequest request,
			Long userId, Integer offset, Integer limit, String sort, Boolean ascending)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		long endExcl = offset+limit;
		QueryResults<UserProfile> results = userProfileManager.getInRange(userInfo, offset, endExcl);
		List<UserProfile> profileResults = results.getResults();
		for (UserProfile profile : profileResults) {
			UserProfileManagerUtils.clearPrivateFields(userInfo, profile);
		}
		return new PaginatedResults<UserProfile>(profileResults,
				(int)results.getTotalNumberOfResults());
	}
	
	/**
	 * Return UserProfiles for the given ids
	 * @param userId
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public ListWrapper<UserProfile> listUserProfiles(Long userId, IdList ids)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		ListWrapper<UserProfile> results = userProfileManager.list(ids);
		for (UserProfile profile : results.getList()) {
			UserProfileManagerUtils.clearPrivateFields(userInfo, profile);
		}
		return results;
	}


	@WriteTransaction
	@Override
	public UserProfile updateUserProfile(Long userId, HttpHeaders header, HttpServletRequest request) 
			throws NotFoundException, ConflictingUpdateException, DatastoreException, InvalidModelException, UnauthorizedException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserProfile entity = (UserProfile) objectTypeSerializer.deserialize(request.getInputStream(), header, UserProfile.class, header.getContentType());
		return userProfileManager.updateUserProfile(userInfo, entity);
	}
	
	@Override
	public UserGroupHeaderResponsePage getUserGroupHeadersByIds(Long userId, List<Long> ids) 
			throws DatastoreException, NotFoundException {
		// split users and groups using the alias.
		List<PrincipalAlias> aliases = principalAliasDAO.listPrincipalAliases(new HashSet<Long>(ids));
		Map<Long, UserGroupHeader> map = new HashMap<Long, UserGroupHeader>(ids.size());
		// Track all users
		Set<Long> userIdSet = new HashSet<Long>(ids.size());
		for(PrincipalAlias alias: aliases){
			if(AliasType.TEAM_NAME.equals(alias.getType())){
				// Team
				UserGroupHeader teamHeader = new UserGroupHeader();
				teamHeader.setIsIndividual(false);
				teamHeader.setUserName(alias.getAlias());
				teamHeader.setOwnerId(alias.getPrincipalId().toString());
				map.put(alias.getPrincipalId(), teamHeader);
			}else if(AliasType.USER_EMAIL.equals(alias.getType())){
				// This is a user
				userIdSet.add(alias.getPrincipalId());
			}
		}
		// Fetch all users
		IdList idList = new IdList();
		idList.setList(new LinkedList<Long>(userIdSet));
		ListWrapper<UserProfile> profiles = userProfileManager.list(idList);
		for(UserProfile profile: profiles.getList()){
			// Convert the profiles to headers
			UserGroupHeader userHeader = convertUserProfileToHeader(profile);
			map.put(Long.parseLong(profile.getOwnerId()), userHeader);
		}
		// final results will be in this list.
		List<UserGroupHeader> finalList = new LinkedList<UserGroupHeader>();
		// Now put all of the parts back together in the requested order
		for(Long principalId: ids){
			finalList.add(map.get(principalId));
		}
		UserGroupHeaderResponsePage response = new UserGroupHeaderResponsePage();
		response.setChildren(finalList);
		response.setTotalNumberOfResults((long) finalList.size());
		response.setPrefixFilter(null);
		return response;
	}

	@Override
	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(String prefix,
			Integer offset, Integer limit, HttpHeaders header, HttpServletRequest request) 
					throws DatastoreException, NotFoundException {
		long limitLong = 10;
		if(limit != null){
			limitLong = limit.longValue();
		}
		long offsetLong = 0;
		if(offset != null){
			offsetLong = offset.longValue();
		}
		List<Long> ids = principalPrefixDAO.listPrincipalsForPrefix(prefix, limitLong, offsetLong);
		UserGroupHeaderResponsePage response = getUserGroupHeadersByIds(null, ids);
		response.setPrefixFilter(prefix);
		response.setTotalNumberOfResults(principalPrefixDAO.countPrincipalsForPrefix(prefix));
		return response;
	}
	
	@Override
	public EntityHeader addFavorite(Long userId, String entityId)
			throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if(!entityPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, userInfo).getAuthorized()) 
			throw new UnauthorizedException("READ access denied to id: "+ entityId +". Favorite not added.");
		Favorite favorite = userProfileManager.addFavorite(userInfo, entityId);
		return entityManager.getEntityHeader(userInfo, favorite.getEntityId(), null); // current version
	}

	@Override
	public void removeFavorite(Long userId, String entityId)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);	
		userProfileManager.removeFavorite(userInfo, entityId);
	}

	@Override
	public PaginatedResults<EntityHeader> getFavorites(Long userId, int limit,
			int offset) throws DatastoreException, InvalidModelException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getFavorites(userInfo, limit, offset);
	}
	
	@Override
	public PaginatedResults<ProjectHeader> getProjects(Long userId, Long otherUserId, Long teamId, ProjectListType type,
			ProjectListSortColumn sortColumn, SortDirection sortDirection, Integer limit, Integer offset) throws DatastoreException,
			InvalidModelException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserInfo userToGetInfoFor = userInfo;

		ValidateArgument.required(type, "type");

		// validate for different types of lists
		switch (type) {
		case OTHER_USER_PROJECTS:
			ValidateArgument.required(otherUserId, "user");
			break;
		case TEAM_PROJECTS:
			ValidateArgument.required(teamId, "team");
			break;
		default:
			break;
		}

		if(sortColumn ==null){
			sortColumn = ProjectListSortColumn.LAST_ACTIVITY;
		}
		if (sortDirection == null) {
			sortDirection = SortDirection.DESC;
		}

		if (otherUserId != null) {
			userToGetInfoFor = userManager.getUserInfo(otherUserId);
		}
		Team teamToFetch = null;
		if (teamId != null) {
			teamToFetch = teamManager.get(teamId.toString());
		}

		return userProfileManager.getProjects(userInfo, userToGetInfoFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
	}

	/*
	 * Private Methods
	 */

	private UserGroupHeader convertUserProfileToHeader(UserProfile profile) {
		UserGroupHeader header = new UserGroupHeader();
		header.setFirstName(profile.getFirstName());
		header.setLastName(profile.getLastName());
		header.setOwnerId(profile.getOwnerId());
		header.setIsIndividual(true);
		header.setUserName(profile.getUserName());
		return header;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#getUserProfileImage(java.lang.String)
	 */
	@Override
	public String getUserProfileImage(String profileId) throws NotFoundException {
		return userProfileManager.getUserProfileImageUrl(profileId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#getUserProfileImagePreview(java.lang.String)
	 */
	@Override
	public String getUserProfileImagePreview(String profileId) throws NotFoundException {
		return userProfileManager.getUserProfileImagePreviewUrl(profileId);
	}
	
	@Override
	public ResponseMessage updateNotificationSettings(NotificationSettingsSignedToken notificationSettingsSignedToken) {
		SignedTokenUtil.validateToken(notificationSettingsSignedToken);
		String userId = notificationSettingsSignedToken.getUserId();
		UserInfo userInfo = userManager.getUserInfo(Long.parseLong(userId));

		UserProfile userProfile = userProfileManager.getUserProfile(userId);
		Settings settings = userProfile.getNotificationSettings();
		if (settings==null) {
			settings = new Settings();
		}
		Settings newSettings = notificationSettingsSignedToken.getSettings();
		if (newSettings.getSendEmailNotifications()!=null) {
			settings.setSendEmailNotifications(newSettings.getSendEmailNotifications());
		}
		if (newSettings.getMarkEmailedMessagesAsRead()!=null) {
			settings.setMarkEmailedMessagesAsRead(newSettings.getMarkEmailedMessagesAsRead());
		}
		userProfile.setNotificationSettings(settings);
		userProfileManager.updateUserProfile(userInfo, userProfile);
		ResponseMessage responseMessage = new ResponseMessage();
		responseMessage.setMessage("You have successfully updated your email notification settings.");
		return responseMessage;
	}

	@Override
	public UserBundle getMyOwnUserBundle(Long userId, int mask)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		return getUserBundleWithAllPrivateFields(userId, mask);
	}
	
	private static int USER_PROFILE_MASK = 0x1;
	private static int ORCID_MASK = 0x2;
	private static int VERIFICATION_MASK = 0x4;
	private static int IS_CERTIFIED_MASK = 0x8;
	private static int IS_VERIFIED_MASK = 0x10;
	private static int IS_ACT_MEMBER_MASK = 0x20;
	
	private UserBundle getUserBundleWithAllPrivateFields(Long profileId, int mask) {
		UserBundle result = new UserBundle();
		result.setUserId(profileId.toString());
		if ((mask&USER_PROFILE_MASK)!=0) {
			result.setUserProfile(userProfileManager.getUserProfile(profileId.toString()));
		}
		UserInfo userInfo = userManager.getUserInfo(profileId);
		if ((mask&IS_ACT_MEMBER_MASK)!=0) {
			result.setIsACTMember(userInfo.getGroups().contains(TeamConstants.ACT_TEAM_ID));
		}
		if ((mask&IS_CERTIFIED_MASK)!=0) {
			result.setIsCertified(userInfo.getGroups().contains(
					BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		}
		VerificationSubmission verificationSubmission = null;
		if ((mask&(VERIFICATION_MASK|IS_VERIFIED_MASK))!=0) {
			verificationSubmission = verificationDao.getCurrentVerificationSubmissionForUser(profileId);
		}
		if ((mask&IS_VERIFIED_MASK)!=0) {
			result.setIsVerified(false);
			if (verificationSubmission!=null) {
				List<VerificationState> list = verificationSubmission.getStateHistory();
				VerificationStateEnum currentState = list.get(list.size()-1).getState();
				result.setIsVerified(currentState==VerificationStateEnum.APPROVED);
			}
		}
		if ((mask&ORCID_MASK)!=0) {
			List<PrincipalAlias> orcidAliases = principalAliasDAO.listPrincipalAliases(profileId, AliasType.USER_ORCID);
			if (orcidAliases.size()>1) throw new IllegalStateException("Cannot have multiple ORCIDs.");
			result.setORCID(orcidAliases.isEmpty() ? null : orcidAliases.get(0).getAlias());
		}
		if ((mask&VERIFICATION_MASK)!=0) {
			result.setVerificationSubmission(verificationSubmission);
		}
		return result;
		
	}

	@Override
	public UserBundle getUserBundleByOwnerId(Long userId, String profileId, int mask)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserBundle result = getUserBundleWithAllPrivateFields(Long.parseLong(profileId), mask);
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserProfileManagerUtils.clearPrivateFields(userInfo, result.getUserProfile());
		if (!UserProfileManagerUtils.isOwnerACTOrAdmin(userInfo, profileId)) {
			if (result.getIsVerified()) {
				UserProfileManagerUtils.clearPrivateFields(result.getVerificationSubmission());
			} else {
				// public doesn't get to see the VerificationSubmission unless it's 'APPROVED'
				result.setVerificationSubmission(null);
			}
		}
		return result;
	}

}
