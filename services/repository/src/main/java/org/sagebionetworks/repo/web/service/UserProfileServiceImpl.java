package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.UserProfileManagerUtils;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class UserProfileServiceImpl implements UserProfileService {

	private final Logger logger = LogManager.getLogger(UserProfileServiceImpl.class);

	@Autowired
	private UserProfileManager userProfileManager;
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

	@Override
	public UserProfile getMyOwnUserProfile(Long userId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getUserProfile(userInfo, userInfo.getId().toString());
	}
	
	@Override
	public UserProfile getUserProfileByOwnerId(Long userId, String profileId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserProfile userProfile = userProfileManager.getUserProfile(userInfo, profileId);
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
		return new PaginatedResults<UserProfile>(
				request.getServletPath()+UrlHelpers.USER, 
				profileResults,
				(int)results.getTotalNumberOfResults(), 
				offset, 
				limit,
				sort, 
				ascending);
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


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
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
		IdList idList = new IdList();
		idList.setList(ids);
		ListWrapper<UserProfile> profiles = userProfileManager.list(idList);
		List<UserGroupHeader> ugHeaders = new ArrayList<UserGroupHeader>(profiles.getList().size());
		for(UserProfile profile: profiles.getList()){
			ugHeaders.add(convertUserProfileToHeader(profile));
		}
		UserGroupHeaderResponsePage response = new UserGroupHeaderResponsePage();
		response.setChildren(ugHeaders);
		response.setTotalNumberOfResults((long) ugHeaders.size());
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
	
	// setters for managers (for testing)
	@Override
	public void setObjectTypeSerializer(ObjectTypeSerializer objectTypeSerializer) {
		this.objectTypeSerializer = objectTypeSerializer;
	}

	@Override
	public void setPermissionsManager(EntityPermissionsManager permissionsManager) {
		this.entityPermissionsManager = permissionsManager;
	}

	@Override
	public void setUserManager(UserManager userManager) {
		this.userManager = userManager;
	}

	@Override
	public void setUserProfileManager(UserProfileManager userProfileManager) {
		this.userProfileManager = userProfileManager;
	}

	@Override
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
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
	
	@Override
	public void setPrincipalAlaisDAO(PrincipalAliasDAO mockPrincipalAlaisDAO) {
		this.principalAliasDAO = mockPrincipalAlaisDAO;
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

}
