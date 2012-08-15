package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import javax.servlet.http.HttpServletRequest;

import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.ardverk.collection.Trie;
import org.sagebionetworks.repo.manager.PermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityQueryResults;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

public class UserProfileServiceImpl implements UserProfileService {
	
	@Autowired
	UserProfileManager userProfileManager;
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	PermissionsManager permissionsManager;
	
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	private Long cacheLastUpdated = 0L;
	private Trie<String, UserGroupHeader> userGroupHeadersCache;

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#getMyOwnUserProfile(java.lang.String)
	 */
	@Override
	public UserProfile getMyOwnUserProfile(String userId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getUserProfile(userInfo, userInfo.getIndividualGroup().getId());
	}
	

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#getUserProfileByOwnerId(java.lang.String, java.lang.String)
	 */
	@Override
	public UserProfile getUserProfileByOwnerId(String userId, String profileId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getUserProfile(userInfo, profileId);
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#getUserProfilesPaginated(javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.Boolean)
	 */
	@Override
	public PaginatedResults<UserProfile> getUserProfilesPaginated(HttpServletRequest request,
			String userId, Integer offset, Integer limit, String sort, Boolean ascending)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		long endExcl = offset+limit;
		QueryResults<UserProfile >results = userProfileManager.getInRange(userInfo, offset, endExcl);
		
		return new PaginatedResults<UserProfile>(
				request.getServletPath()+UrlHelpers.USER, 
				results.getResults(),
				(int)results.getTotalNumberOfResults(), 
				offset, 
				limit,
				sort, 
				ascending);
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#updateUserProfile(java.lang.String, org.springframework.http.HttpHeaders, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public UserProfile updateUserProfile(String userId, HttpHeaders header, 
			String etag, HttpServletRequest request) throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserProfile entity = (UserProfile) objectTypeSerializer.deserialize(request.getInputStream(), header, UserProfile.class, header.getContentType());
		if(etag != null){
			entity.setEtag(etag.toString());
		}
		return userProfileManager.updateUserProfile(userInfo, entity);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#createUserProfileS3AttachmentToken(java.lang.String, java.lang.String, org.sagebionetworks.repo.model.attachment.S3AttachmentToken, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public S3AttachmentToken createUserProfileS3AttachmentToken(String userId, String profileId, 
			S3AttachmentToken token, HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.createS3UserProfileAttachmentToken(userInfo, profileId, token);
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#getUserProfileAttachmentUrl(java.lang.String, java.lang.String, org.sagebionetworks.repo.model.attachment.PresignedUrl, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public PresignedUrl getUserProfileAttachmentUrl(String userId, String profileId,
			PresignedUrl url, HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		if(url == null) throw new IllegalArgumentException("A PresignedUrl must be provided");
		// Pass it along.
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getUserProfileAttachmentUrl(userInfo, profileId, url.getTokenID());
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#getUserGroupHeadersByPrefix(java.lang.String, java.lang.Integer, java.lang.Integer, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(String prefix,
			Integer offset, Integer limit, HttpHeaders header, HttpServletRequest request) 
					throws DatastoreException, NotFoundException, IOException {
		if (userGroupHeadersCache == null || userGroupHeadersCache.size() == 0)
			refreshCache();
		
		prefix = prefix.toLowerCase();
		int limitInt = 10;
		if(limit != null){
			limitInt = limit.intValue();
		}
		int offsetInt = 0;
		if(offset != null){
			offsetInt = offset.intValue();
		}
		// Get the results from the cache
		SortedMap<String, UserGroupHeader> matched = userGroupHeadersCache.prefixMap(prefix);
		List<UserGroupHeader> fullList = new ArrayList<UserGroupHeader>(matched.values());
		EntityQueryResults<UserGroupHeader> eqr = new EntityQueryResults<UserGroupHeader>(fullList, limitInt, offsetInt);
		UserGroupHeaderResponsePage results = new UserGroupHeaderResponsePage();
		results.setChildren(eqr.getResults());
		results.setPrefixFilter(prefix);
		results.setTotalNumberOfResults(new Long(eqr.getTotalNumberOfResults()));
		return results;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.UserProfileService#populateCache()
	 */
	@Override
	public synchronized void refreshCache() throws DatastoreException, NotFoundException {		
		Trie<String, UserGroupHeader> tempCache = new PatriciaTrie<String, UserGroupHeader>(StringKeyAnalyzer.CHAR);

		UserGroupHeader header;
		List<UserProfile> userProfiles = userProfileManager.getInRange(null, 0, Long.MAX_VALUE).getResults();
		for (UserProfile profile : userProfiles) {
			if (profile .getDisplayName() != null) {
				header = convertUserProfileToHeader(profile);			
				tempCache.put(header.getDisplayName().toLowerCase(), header);
			}
		}
		Collection<UserGroup> userGroups = permissionsManager.getGroups();
		for (UserGroup group : userGroups) {
			if (group.getName() != null) {
				header = convertUserGroupToHeader(group);			
				tempCache.put(group.getName().toLowerCase(), header);
			}
		}
		userGroupHeadersCache = tempCache;
		cacheLastUpdated = System.currentTimeMillis();		
	}
	
	@Override
	public Long millisSinceLastCacheUpdate() {
		if (userGroupHeadersCache == null) {
			return null;
		}
		return System.currentTimeMillis() - cacheLastUpdated;
	}
	
	private UserGroupHeader convertUserProfileToHeader(UserProfile profile) {
		UserGroupHeader header = new UserGroupHeader();
		header.setDisplayName(profile.getDisplayName());
		header.setEmail(profile.getEmail());
		header.setFirstName(profile.getFirstName());
		header.setLastName(profile.getLastName());
		header.setOwnerId(profile.getOwnerId());
		header.setPic(profile.getPic());
		header.setIsIndividual(true);
		return header;
	}
	
	private UserGroupHeader convertUserGroupToHeader(UserGroup group) {
		UserGroupHeader header = new UserGroupHeader();
		header.setDisplayName(group.getName());
		header.setOwnerId(group.getId());
		header.setIsIndividual(group.getIsIndividual());
		return header;
	}
}
