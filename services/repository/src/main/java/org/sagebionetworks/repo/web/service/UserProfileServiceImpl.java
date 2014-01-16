package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.ardverk.collection.Trie;
import org.ardverk.collection.Tries;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.UserProfileManagerUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
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
	private EntityPermissionsManager entityPermissionsManager;
	
	@Autowired
	private ObjectTypeSerializer objectTypeSerializer;
	
	@Autowired
	private EntityManager entityManager;

	/**
	 * These member variables are declared volatile to enforce thread-safe
	 * cache access. Clients should fetch the latest cache objects for every
	 * request.
	 * 
	 * The cache objects may be *replaced* by new cache objects created in the
	 * refreshCache() method, but existing cache objects can NOT be modified.
	 * This is to avoid corruption of cache state during multithreaded read
	 * operations.
	 */
	private volatile Long cachesLastUpdated = 0L;
	private volatile Trie<String, Collection<UserGroupHeader>> userGroupHeadersNamePrefixCache;
	private volatile Map<Long, UserGroupHeader> userGroupHeadersIdCache;

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
		QueryResults<UserProfile >results = userProfileManager.getInRange(userInfo, offset, endExcl);
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserProfile updateUserProfile(Long userId, HttpHeaders header, HttpServletRequest request) 
			throws NotFoundException, ConflictingUpdateException, DatastoreException, InvalidModelException, UnauthorizedException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserProfile entity = (UserProfile) objectTypeSerializer.deserialize(request.getInputStream(), header, UserProfile.class, header.getContentType());
		return userProfileManager.updateUserProfile(userInfo, entity);
	}

	@Override
	public S3AttachmentToken createUserProfileS3AttachmentToken(Long userId, String profileId, 
			S3AttachmentToken token, HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.createS3UserProfileAttachmentToken(userInfo, profileId, token);
	}

	@Override
	public PresignedUrl getUserProfileAttachmentUrl(Long userId, String profileId,
			PresignedUrl url, HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		if(url == null) throw new IllegalArgumentException("A PresignedUrl must be provided");
		return userProfileManager.getUserProfileAttachmentUrl(userId, profileId, url.getTokenID());
	}
	
	@Override
	public UserGroupHeaderResponsePage getUserGroupHeadersByIds(Long userId, List<Long> ids) 
			throws DatastoreException, NotFoundException {		
		if (userGroupHeadersIdCache == null || userGroupHeadersIdCache.size() == 0)
			refreshCache();
		UserInfo userInfo;
		if(userId != null) {
			userInfo = userManager.getUserInfo(userId);
		} else {
			// request is anonymous			
			userInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		}
		List<UserGroupHeader> ugHeaders = new ArrayList<UserGroupHeader>();
		for (Long id : ids) {
			UserGroupHeader header = userGroupHeadersIdCache.get(id);
			if (header == null) {
				// Header not found in cache; attempt to fetch one from repo
				header = fetchNewHeader(userInfo, id);
			}
			if (header != null) {
				ugHeaders.add(header);
			}
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
		List<UserGroupHeader> fullList = PrefixCacheHelper.flatten(matched);
		QueryResults<UserGroupHeader> eqr = new QueryResults<UserGroupHeader>(fullList, limitInt, offsetInt);
		UserGroupHeaderResponsePage results = new UserGroupHeaderResponsePage();
		results.setChildren(eqr.getResults());
		results.setPrefixFilter(prefix);
		results.setTotalNumberOfResults(new Long(eqr.getTotalNumberOfResults()));
		return results;
	}
	
	@Override
	public void refreshCache() throws DatastoreException, NotFoundException {

		this.logger.info("refreshCache() started at time " + System.currentTimeMillis());

		// Create and populate local caches. Upon completion, swap them for the
		// singleton member variable caches.
		Trie<String, Collection<UserGroupHeader>> tempPrefixCache = new PatriciaTrie<String, Collection<UserGroupHeader>>(StringKeyAnalyzer.CHAR);
		Map<Long, UserGroupHeader> tempIdCache = new HashMap<Long, UserGroupHeader>();

		List<UserProfile> userProfiles = userProfileManager.getInRange(null, 0, Long.MAX_VALUE).getResults();
		this.logger.info("Loaded " + userProfiles.size() + " user profiles.");
		UserGroupHeader header;
		for (UserProfile profile : userProfiles) {
			UserProfileManagerUtils.clearPrivateFields(null, profile);
			header = convertUserProfileToHeader(profile);
			addToPrefixCache(tempPrefixCache, header);
			addToIdCache(tempIdCache, header);
		}
		// List all team names
		List<PrincipalAlias> teamNames = principalAliasDAO.listPrincipalAliases(AliasType.TEAM_NAME);
		
		this.logger.info("Loaded " + teamNames.size() + " user teams.");
		for (PrincipalAlias alais: teamNames) {
			header = convertUserGroupToHeader(alais);			
			addToPrefixCache(tempPrefixCache, header);
			addToIdCache(tempIdCache, header);
		}
		userGroupHeadersNamePrefixCache = Tries.unmodifiableTrie(tempPrefixCache);
		userGroupHeadersIdCache = Collections.unmodifiableMap(tempIdCache);
		cachesLastUpdated = System.currentTimeMillis();

		this.logger.info("refreshCache() completed at time " + System.currentTimeMillis());
	}
	
	@Override
	public Long millisSinceLastCacheUpdate() {
		if (userGroupHeadersNamePrefixCache == null) {
			return null;
		}
		return System.currentTimeMillis() - cachesLastUpdated;
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
		if(!entityPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, userInfo)) 
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
	
	
	/*
	 * Private Methods
	 */

	/**
	 * Fetches a UserProfile for a specified Synapse ID. Note that this does not
	 * check for a UserGroup with the specified ID.
	 */
	private UserGroupHeader fetchNewHeader(UserInfo userInfo, Long id) {
		UserProfile profile;
		try {
			profile = userProfileManager.getUserProfile(userInfo, id.toString());
			UserProfileManagerUtils.clearPrivateFields(userInfo, profile);
		} catch (NotFoundException e) {
			// Profile not found, so return null
			return null;
		}
		return convertUserProfileToHeader(profile);
	}

	private void addToPrefixCache(Trie<String, Collection<UserGroupHeader>> prefixCache, UserGroupHeader header) {
		//get the collection of prefixes that we want to associate to this UserGroupHeader
		List<String> prefixes = PrefixCacheHelper.getPrefixes(header);
		
		for (String prefix : prefixes) {
			if (!prefixCache.containsKey(prefix)) {
				// cache does not contain a user/group with that name
				Collection<UserGroupHeader> coll = new HashSet<UserGroupHeader>();
				coll.add(header);
				prefixCache.put(prefix, coll);
			} else {					
				// cache already contains a user/group with that name; add to the collection
				Collection<UserGroupHeader> coll = prefixCache.get(prefix);
				coll.add(header);
			}
		}
	}

	private void addToIdCache(Map<Long, UserGroupHeader> idCache, UserGroupHeader header) {
		idCache.put(Long.parseLong(header.getOwnerId()), header);
	}

	private UserGroupHeader convertUserProfileToHeader(UserProfile profile) {
		UserGroupHeader header = new UserGroupHeader();
		header.setFirstName(profile.getFirstName());
		header.setLastName(profile.getLastName());
		header.setOwnerId(profile.getOwnerId());
		header.setPic(profile.getPic());
		header.setIsIndividual(true);
		header.setUserName(profile.getUserName());
		return header;
	}

	private UserGroupHeader convertUserGroupToHeader(PrincipalAlias alias) {
		UserGroupHeader header = new UserGroupHeader();
		header.setUserName(alias.getAlias());
		header.setOwnerId(alias.getPrincipalId().toString());
		header.setIsIndividual(false);
		return header;
	}

	@Override
	public void setPrincipalAlaisDAO(PrincipalAliasDAO mockPrincipalAlaisDAO) {
		this.principalAliasDAO = mockPrincipalAlaisDAO;
	}

}
