package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.BooleanUtils;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserInfoHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.UserProfileManagerUtils;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.manager.verification.VerificationHelper;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.ProjectHeaderList;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.ResponseMessage;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserBundle;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.AliasList;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.principal.TypeFilter;
import org.sagebionetworks.repo.model.principal.UserGroupHeaderResponse;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class UserProfileServiceImpl implements UserProfileService {
	
	/**
	 * The maximum number of headers per request.
	 */
	public static int MAX_HEADERS_PER_REQUEST = 100;

	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	PrincipalAliasDAO principalAliasDAO;
	
	@Autowired
	private UserManager userManager;

	@Autowired
	private EntityAuthorizationManager entityAuthorizationManager;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	PrincipalPrefixDAO principalPrefixDAO;
	
	@Autowired
	TokenGenerator tokenGenerator;
	
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
		List<UserProfile> page = userProfileManager.getInRange(userInfo, offset, endExcl);
		for (UserProfile profile : page) {
			UserProfileManagerUtils.clearPrivateFields(userInfo, profile);
		}
		return PaginatedResults.createWithLimitAndOffset(page, (long)limit, (long)offset);
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
	public UserProfile updateUserProfile(Long userId, UserProfile userProfile) 
			throws NotFoundException, ConflictingUpdateException, DatastoreException, InvalidModelException, UnauthorizedException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.updateUserProfile(userInfo, userProfile);
	}
	
	@Override
	public UserGroupHeaderResponsePage getUserGroupHeadersByIds(List<Long> ids) 
			throws DatastoreException, NotFoundException {
		List<UserGroupHeader> headers = principalAliasDAO.listPrincipalHeaders(ids);
		UserGroupHeaderResponsePage response = new UserGroupHeaderResponsePage();
		response.setChildren(headers);
		response.setTotalNumberOfResults((long) headers.size());
		response.setPrefixFilter(null);
		return response;
	}

	@Override
	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(String prefix, TypeFilter filter,
			Integer offset, Integer limit) 
					throws DatastoreException, NotFoundException {
		if(filter == null){
			filter = TypeFilter.ALL;
		}
		long limitLong = 10;
		if(limit != null){
			limitLong = limit.longValue();
		}
		long offsetLong = 0;
		if(offset != null){
			offsetLong = offset.longValue();
		}
		List<Long> ids = listPrincipalsForPrefix(prefix, filter,  offsetLong, limitLong);
		UserGroupHeaderResponsePage response = getUserGroupHeadersByIds(ids);
		response.setPrefixFilter(prefix);
		// The total is estimated.
		response.setTotalNumberOfResults(PaginatedResults.calculateTotalWithLimitAndOffset(response.getChildren().size(), limit, offset));
		return response;
	}
	
	/**
	 * The type filter determines which query is run.
	 * 
	 * @param prefix
	 * @param filter
	 * @param offset
	 * @param limit
	 * @return
	 */
	List<Long> listPrincipalsForPrefix(String prefix, TypeFilter filter,
			long offset, long limit){
		ValidateArgument.required(filter, "filter");
		boolean isIndividual = false;
		switch(filter){
		case ALL:
			// not filtered by type.
			return principalPrefixDAO.listPrincipalsForPrefix(prefix, limit, offset);
		case USERS_ONLY:
			isIndividual = true;
			break;
		case TEAMS_ONLY:
			isIndividual = false;
			break;
		default: 
			throw new IllegalArgumentException("Unknown type: "+filter);
		}
		// filter by type
		return principalPrefixDAO.listPrincipalsForPrefix(prefix, isIndividual, limit, offset);
	}
	
	@Override
	public EntityHeader addFavorite(Long userId, String entityId)
			throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if(!entityAuthorizationManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ).isAuthorized())
			throw new UnauthorizedException("READ access denied to id: "+ entityId +". Favorite not added.");
		Favorite favorite = userProfileManager.addFavorite(userInfo, entityId);
		return entityManager.getEntityHeader(userInfo, favorite.getEntityId()); // current version
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
	public ProjectHeaderList getProjects(Long userId, Long otherUserId, Long teamId, ProjectListType type,
			ProjectListSortColumn sortColumn, SortDirection sortDirection, String nextPageToken) throws DatastoreException,
			InvalidModelException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);

		ValidateArgument.required(otherUserId, "subject");
		
		UserInfo userToGetInfoFor;
		if (otherUserId.equals(userId)) {
			userToGetInfoFor = userInfo;
		} else {
			userToGetInfoFor = userManager.getUserInfo(otherUserId);
		}
		
		if (type==null) {
			type = ProjectListType.ALL;
		}

		if(sortColumn==null){
			sortColumn = ProjectListSortColumn.LAST_ACTIVITY;
		}
		if (sortDirection==null) {
			sortDirection = SortDirection.DESC;
		}

		return userProfileManager.getProjects(userInfo, userToGetInfoFor, teamId, type, sortColumn, sortDirection, nextPageToken);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#getUserProfileImage(java.lang.String)
	 */
	@Override
	public String getUserProfileImage(Long userId, String profileId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getUserProfileImageUrl(userInfo, profileId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#getUserProfileImagePreview(java.lang.String)
	 */
	@Override
	public String getUserProfileImagePreview(Long userId, String profileId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getUserProfileImagePreviewUrl(userInfo, profileId);
	}
	
	@Override
	public ResponseMessage updateNotificationSettings(NotificationSettingsSignedToken notificationSettingsSignedToken) {
		tokenGenerator.validateToken(notificationSettingsSignedToken);
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
			result.setIsACTMember(UserInfoHelper.isACTMember(userInfo));
		}
		if ((mask&IS_CERTIFIED_MASK)!=0) {
			result.setIsCertified(UserInfoHelper.isCertified(userInfo));
		}
		VerificationSubmission verificationSubmission = null;
		if ((mask&(VERIFICATION_MASK|IS_VERIFIED_MASK))!=0) {
			verificationSubmission = userProfileManager.getCurrentVerificationSubmission(profileId);
		}
		if ((mask&VERIFICATION_MASK)!=0) {
			result.setVerificationSubmission(verificationSubmission);
		}
		if ((mask&IS_VERIFIED_MASK)!=0) {
			result.setIsVerified(VerificationHelper.isVerified(verificationSubmission));
		}
		if ((mask&ORCID_MASK)!=0) {
			result.setORCID(userProfileManager.getOrcid(profileId));
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
			if (BooleanUtils.isTrue(result.getIsVerified())) {
				UserProfileManagerUtils.clearPrivateFields(result.getVerificationSubmission());
			} else {
				// public doesn't get to see the VerificationSubmission unless it's 'APPROVED'
				result.setVerificationSubmission(null);
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#getUserGroupHeadersByAlias(org.sagebionetworks.repo.model.principal.AliasList)
	 */
	@Override
	public UserGroupHeaderResponse getUserGroupHeadersByAlias(AliasList request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getList(), "request.list");
		ValidateArgument.requirement(!request.getList().isEmpty(),
				"Request must include at least one alias.");
		ValidateArgument.requirement(
				request.getList().size() < MAX_HEADERS_PER_REQUEST + 1 ,
				"Request exceeds the maximum number of "
						+ MAX_HEADERS_PER_REQUEST + " aliases.");
		/*
		 * Note: Callers can only lookup team names and user names. They
		 * cannot lookup email addresses or other alias types.
		 */
		List<AliasType> types = Lists.newArrayList(AliasType.TEAM_NAME, AliasType.USER_NAME);
		List<Long> principalIds = principalAliasDAO.findPrincipalsWithAliases(request.getList(), types);
		// Convert the Ids to headers
		List<UserGroupHeader> resultList = principalAliasDAO.listPrincipalHeaders(principalIds);
		UserGroupHeaderResponse response = new UserGroupHeaderResponse();
		response.setList(resultList);
		return response;
	}

}
