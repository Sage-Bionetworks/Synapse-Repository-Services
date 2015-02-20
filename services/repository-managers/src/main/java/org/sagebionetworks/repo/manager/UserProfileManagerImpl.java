package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.IdSet;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class UserProfileManagerImpl implements UserProfileManager {
	
	static private Logger log = LogManager.getLogger(EntityTypeConverterImpl.class);
	
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
	private AmazonS3Client s3Client;
	@Autowired
	private FileHandleDao fileHandleDao;
	

	public UserProfileManagerImpl() {
	}

	/**
	 * Used by unit tests
	 */
	public UserProfileManagerImpl(UserProfileDAO userProfileDAO, UserGroupDAO userGroupDAO, FavoriteDAO favoriteDAO, PrincipalAliasDAO principalAliasDAO,
			AuthorizationManager authorizationManager,
			AmazonS3Client s3Client,
			FileHandleDao fileHandleDao) {
		super();
		this.userProfileDAO = userProfileDAO;
		this.favoriteDAO = favoriteDAO;
		this.principalAliasDAO = principalAliasDAO;
		this.authorizationManager = authorizationManager;
		this.s3Client = s3Client;
		this.fileHandleDao = fileHandleDao;
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
		userProfile = convertAttachemtns(userProfile);
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

	/**
	 *  Convert old style attachment pictures to files handles as needed.
	 * @param profiles
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws ConflictingUpdateException
	 * @throws NotFoundException
	 */
	private List<UserProfile> convertAttachemtns(List<UserProfile> profiles) throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException{
		List<UserProfile> list = new LinkedList<UserProfile>();
		for(UserProfile profile: profiles){
			list.add(convertAttachemtns(profile));
		}
		return list;
	}
	
	/**
	 * Convert old style attachment pictures to files handles as needed.
	 * @param profile
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws ConflictingUpdateException
	 * @throws NotFoundException
	 */
	private UserProfile convertAttachemtns(UserProfile profile) throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException{
		if(profile.getPic() == null){
			// nothing do to here.
			return profile;
		}
		// We need to convert from an attachment to an S3FileHandle
		S3FileHandle handle = createFileHandleFromAttachment(profile.getOwnerId(), profile.getPic());
		// clear the pic, set the filehandle.
		profile.setPic(null);
		if(handle != null){
			profile.setProfilePicureFileHandleId(handle.getId());
		}
		return userProfileDAO.update(profile);
	}
	

	@Override
	public S3FileHandle createFileHandleFromAttachment(String createdBy, AttachmentData attachment) {
		if(attachment == null){
			return null;
		}
		if(attachment.getTokenId() == null){
			return null;
		}
		// The keys do not start with "/"
		String key = attachment.getTokenId();
		String bucket = StackConfiguration.getS3Bucket();
		// Can we find this object with the key?
		try {
			ObjectMetadata meta = s3Client.getObjectMetadata(bucket, key);
			S3FileHandle handle = new S3FileHandle();
			handle.setBucketName(bucket);
			handle.setKey(key);
			handle.setContentType(meta.getContentType());
			handle.setContentMd5(meta.getContentMD5());
			handle.setContentSize(meta.getContentLength());
			handle.setFileName(extractFileNameFromKey(key));
			if(attachment.getName() != null){
				handle.setFileName(attachment.getName());
			}
			if(attachment.getMd5() != null){
				handle.setContentMd5(attachment.getMd5());
			}
			if(attachment.getContentType() != null){
				handle.setContentType(attachment.getContentType());
			}
			handle.setCreatedBy(createdBy);
			handle.setCreatedOn(new Date(System.currentTimeMillis()));
			handle = fileHandleDao.createFile(handle);
			return handle;
		} catch (Exception e) {
			log.error("Cannot find profile picture in S3. Key: "+key);
			return null;
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
	public QueryResults<UserProfile> getInRange(UserInfo userInfo, long startIncl, long endExcl) throws DatastoreException, NotFoundException{
		List<UserProfile> userProfiles = userProfileDAO.getInRange(startIncl, endExcl);
		userProfiles = convertAttachemtns(userProfiles);
		addAliasesToProfiles(userProfiles);
		long totalNumberOfResults = userProfileDAO.getCount();
		QueryResults<UserProfile> result = new QueryResults<UserProfile>(userProfiles, (int)totalNumberOfResults);
		return result;
	}
	/**
	 * List the UserProfiles for the given IDs
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public ListWrapper<UserProfile> list(IdSet ids) throws DatastoreException, NotFoundException {
		List<UserProfile> userProfiles = userProfileDAO.list(ids.getSet());
		userProfiles = convertAttachemtns(userProfiles);
		addAliasesToProfiles(userProfiles);
		return ListWrapper.wrap(userProfiles, UserProfile.class);
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
	public PaginatedResults<ProjectHeader> getProjects(UserInfo userInfo, UserInfo userToGetInfoFor, Team teamToFetch, ProjectListType type,
			ProjectListSortColumn sortColumn, SortDirection sortDirection, Integer limit, Integer offset) throws DatastoreException,
			InvalidModelException, NotFoundException {
		PaginatedResults<ProjectHeader> projectHeaders = nodeDao.getProjectHeaders(userInfo, userToGetInfoFor, teamToFetch, type, sortColumn,
				sortDirection, limit, offset);
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
