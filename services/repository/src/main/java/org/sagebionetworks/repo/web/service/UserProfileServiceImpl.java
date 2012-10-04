package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import javax.servlet.http.HttpServletRequest;

import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.ardverk.collection.Trie;
import org.sagebionetworks.repo.manager.PermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class UserProfileServiceImpl implements UserProfileService {
	
	@Autowired
	UserProfileManager userProfileManager;	
	@Autowired
	UserManager userManager;	
	@Autowired
	PermissionsManager permissionsManager;	
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	private Long cachesLastUpdated = 0L;
	private Trie<String, Collection<UserGroupHeader>> userGroupHeadersNamePrefixCache;
	private Map<String, UserGroupHeader> userGroupHeadersIdCache;

	@Override
	public UserProfile getMyOwnUserProfile(String userId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getUserProfile(userInfo, userInfo.getIndividualGroup().getId());
	}
	
	@Override
	public UserProfile getUserProfileByOwnerId(String userId, String profileId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getUserProfile(userInfo, profileId);
	}
	
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
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

	@Override
	public S3AttachmentToken createUserProfileS3AttachmentToken(String userId, String profileId, 
			S3AttachmentToken token, HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.createS3UserProfileAttachmentToken(userInfo, profileId, token);
	}

	@Override
	public PresignedUrl getUserProfileAttachmentUrl(String userId, String profileId,
			PresignedUrl url, HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		if(url == null) throw new IllegalArgumentException("A PresignedUrl must be provided");
		// Pass it along.
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getUserProfileAttachmentUrl(userInfo, profileId, url.getTokenID());
	}
	
	@Override
	public UserGroupHeaderResponsePage getUserGroupHeadersByIds(List<String> ids) 
			throws DatastoreException, NotFoundException {		
		if (userGroupHeadersIdCache == null || userGroupHeadersIdCache.size() == 0)
			refreshCache();
		List<UserGroupHeader> ugHeaders = new ArrayList<UserGroupHeader>();
		for (String id : ids) {
			UserGroupHeader header = userGroupHeadersIdCache.get(id);
			if (header != null) ugHeaders.add(header);
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
		if (userGroupHeadersNamePrefixCache == null || userGroupHeadersNamePrefixCache.size() == 0)
			refreshCache();
		
		int limitInt = 10;
		if(limit != null){
			limitInt = limit.intValue();
		}
		int offsetInt = 0;
		if(offset != null){
			offsetInt = offset.intValue();
		}
		// Get the results from the cache
		SortedMap<String, Collection<UserGroupHeader>> matched = userGroupHeadersNamePrefixCache.prefixMap(prefix.toLowerCase());
		List<UserGroupHeader> fullList = flatten(matched);
		EntityQueryResults<UserGroupHeader> eqr = new EntityQueryResults<UserGroupHeader>(fullList, limitInt, offsetInt);
		UserGroupHeaderResponsePage results = new UserGroupHeaderResponsePage();
		results.setChildren(eqr.getResults());
		results.setPrefixFilter(prefix);
		results.setTotalNumberOfResults(new Long(eqr.getTotalNumberOfResults()));
		return results;
	}
	
	/**
	 * The Trie contains collections of UserGroupHeaders for a given name. This
	 * method flattens the collections into a single list of UserGroupHeaders.
	 * 
	 * @param prefixMap
	 * @return
	 */
	private List<UserGroupHeader> flatten (
			SortedMap<String, Collection<UserGroupHeader>> prefixMap) {
		List<UserGroupHeader> list = new ArrayList<UserGroupHeader>();
		for (Collection<UserGroupHeader> headersOfOneName : prefixMap.values()) {
			for (UserGroupHeader header : headersOfOneName) {
				list.add(header);
			}
		}
		return list;
	}

	@Override
	public synchronized void refreshCache() throws DatastoreException, NotFoundException {		
		Trie<String, Collection<UserGroupHeader>> tempPrefixCache = new PatriciaTrie<String, Collection<UserGroupHeader>>(StringKeyAnalyzer.CHAR);
		Map<String, UserGroupHeader> tempIdCache = new HashMap<String, UserGroupHeader>();
		
		UserGroupHeader header;
		List<UserProfile> userProfiles = userProfileManager.getInRange(null, 0, Long.MAX_VALUE, true).getResults();
		for (UserProfile profile : userProfiles) {
			if (profile.getDisplayName() != null) {
				header = convertUserProfileToHeader(profile);
				addToPrefixCache(tempPrefixCache, header);
				addToIdCache(tempIdCache, header);
			}
		}
		Collection<UserGroup> userGroups = permissionsManager.getGroups();
		for (UserGroup group : userGroups) {
			if (group.getName() != null) {
				header = convertUserGroupToHeader(group);			
				addToPrefixCache(tempPrefixCache, header);
				addToIdCache(tempIdCache, header);
			}
		}
		userGroupHeadersNamePrefixCache = tempPrefixCache;
		userGroupHeadersIdCache = tempIdCache;
		cachesLastUpdated = System.currentTimeMillis();		
	}
	
	private void addToPrefixCache(Trie<String, Collection<UserGroupHeader>> tempCache, UserGroupHeader header) {
		String name = header.getDisplayName().toLowerCase();
		if (!tempCache.containsKey(name)) {
			// cache does not contain a user/group with that name
			Collection<UserGroupHeader> coll = new HashSet<UserGroupHeader>();
			coll.add(header);
			tempCache.put(name, coll);
		} else {					
			// cache already contains a user/group with that name; add to the collection
			Collection<UserGroupHeader> coll = tempCache.get(name);
			coll.add(header);
		}
	}
	
	private void addToIdCache(Map<String, UserGroupHeader> tempCache, UserGroupHeader header) {
		tempCache.put(header.getOwnerId(), header);
	}
	
	@Override
	public Long millisSinceLastCacheUpdate() {
		if (userGroupHeadersNamePrefixCache == null) {
			return null;
		}
		return System.currentTimeMillis() - cachesLastUpdated;
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
	
	// setters for managers (for testing)
	@Override
	public void setObjectTypeSerializer(ObjectTypeSerializer objectTypeSerializer) {
		this.objectTypeSerializer = objectTypeSerializer;
	}

	@Override
	public void setPermissionsManager(PermissionsManager permissionsManager) {
		this.permissionsManager = permissionsManager;
	}

	@Override
	public void setUserManager(UserManager userManager) {
		this.userManager = userManager;
	}

	@Override
	public void setUserProfileManager(UserProfileManager userProfileManager) {
		this.userProfileManager = userProfileManager;
	}
}
