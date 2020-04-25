package org.sagebionetworks.repo.manager.wiki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiMarkdownVersion;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * V2 WikiManager implementation.
 * (Derived from org.sagebionetworks.repo.manager.wiki.WikiManagerImpl)
 * @author hso
 *
 */
public class V2WikiManagerImpl implements V2WikiManager {
	
	static private Log log = LogFactory.getLog(V2WikiManagerImpl.class);
	
	private static final String USER_IS_NOT_AUTHORIZED_TEMPLATE = "User is not authorized to '%1$s' a WikiPage with an ownerId: '%2$s' of type: '%3$s'";
	
	private static final Long MAX_WIKI_MARKDOWN_RANK = 100L;

	public static final long MAX_LIMIT = 50;
	
	@Autowired
	V2WikiPageDao wikiPageDao;
	
	@Autowired
	FileHandleDao fileMetadataDao;
	
	@Autowired
	AuthorizationManager authorizationManager;

	/**
	 * Default for Spring!
	 */
	public V2WikiManagerImpl(){}
	
	/**
	 * The IoC constructor.
	 * @param wikiPageDao
	 * @param authorizationManager
	 */
	public V2WikiManagerImpl(V2WikiPageDao wikiPageDao,
			AuthorizationManager authorizationManager, FileHandleDao fileMetadataDao) {
		super();
		this.wikiPageDao = wikiPageDao;
		this.authorizationManager = authorizationManager;
		this.fileMetadataDao = fileMetadataDao;
	}

	@WriteTransaction
	@Override
	public V2WikiPage createWikiPage(UserInfo user, String objectId, ObjectType objectType, V2WikiPage wikiPage) throws NotFoundException, UnauthorizedException{
		if(user == null) throw new IllegalArgumentException("user cannot be null");
		if(objectId == null) throw new IllegalArgumentException("objectId cannot be null");
		if(objectType == null) throw new IllegalArgumentException("objectType cannot be null");
		if(wikiPage == null) throw new IllegalArgumentException("wikiPage cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canCreateWiki(user, objectId, objectType).isAuthorized()){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.CREATE.name(), objectId, objectType.name()));
		}
		
		// Set created by and modified by
		wikiPage.setCreatedBy(user.getId().toString());
		wikiPage.setModifiedBy(wikiPage.getCreatedBy());

		// First build up the map of names to FileHandles
		Map<String, FileHandle> nameToHandleMap = buildFileNameMap(wikiPage);
		
		// For a create the user must be the creator of each file handle used.
		List<String> newFileHandlesToInsert = new ArrayList<String>();
		for(FileHandle handle: nameToHandleMap.values()){
			newFileHandlesToInsert.add(handle.getId());
		}
		
		FileHandle markdownFileHandle = fileMetadataDao.get(wikiPage.getMarkdownFileHandleId());
		newFileHandlesToInsert.add(markdownFileHandle.getId());

		// Create a list of file handles associated with this wiki page to check permissions
		List<FileHandle> fileHandlesToCheck = new ArrayList<FileHandle>();
		// Add markdown file handle
		fileHandlesToCheck.add(markdownFileHandle);
		// Add attachments
		for(FileHandle handle: nameToHandleMap.values()) {
			fileHandlesToCheck.add(handle);
		}
		
		// Validate that the user can assign all file handles
		for(FileHandle handle: fileHandlesToCheck){
			// the user must have access to the raw FileHandle to assign it to an object.
			authorizationManager.canAccessRawFileHandleByCreator(user, handle.getId(), handle.getCreatedBy()).checkAuthorizationOrElseThrow();
		}
		// pass to the DAO
		return wikiPageDao.create(wikiPage, nameToHandleMap, objectId, objectType, newFileHandlesToInsert);
	}
	
	/**
	 * Build up the Map of FileHandle.fileNames to FileHandles  If there are duplicate names in the input list,
	 * then then the handle with the most recent creation date will be used.
	 * @param page
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	Map<String, FileHandle> buildFileNameMap(V2WikiPage page) throws DatastoreException, NotFoundException{
		Map<String, FileHandle> results = new HashMap<String, FileHandle>();
		// First lookup each FileHandle
		List<FileHandle> handles = new LinkedList<FileHandle>();
		if(page.getAttachmentFileHandleIds() != null){
			for(String id: page.getAttachmentFileHandleIds()){
				FileHandle handle = fileMetadataDao.get(id);
				handles.add(handle);
			}
		}
		// Now sort the list by createdOn
		Collections.sort(handles,  new Comparator<FileHandle>(){
			@Override
			public int compare(FileHandle one, FileHandle two) {
				return one.getCreatedOn().compareTo(two.getCreatedOn());
			}});
		// Now process the results
		for(FileHandle handle: handles){
			FileHandle old = results.put(handle.getFileName(), handle);
			if(old != null){
				// Log the duplicates
				log.info("Duplicate attachment file name found for WikiPage. The older FileHandle will be replaced with the newer FileHandle.  Old FileHandle: "+old );
			}
		}
		return results;
	}
	
	@Override
	public V2WikiPage getRootWikiPage(UserInfo user, String objectId,	ObjectType objectType) throws NotFoundException, UnauthorizedException {
		// Look up the root wiki
		Long rootWikiId = wikiPageDao.getRootWiki(objectId, objectType);
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(objectId, objectType, rootWikiId.toString());
		// The security check is done in the default method.
		return getWikiPage(user, key, null);
	}

	@Override
	public V2WikiPage getWikiPage(UserInfo user, WikiPageKey key, Long version) throws NotFoundException, UnauthorizedException {
		// Validate that the user has read access
		validateReadAccess(user, key);
		// Pass to the DAO
		return wikiPageDao.get(key, version);
	}

	/**
	 * Validate that the user has read access to the owner object.
	 * @param user
	 * @param key
	 * @throws NotFoundException
	 */
	private void validateReadAccess(UserInfo user, WikiPageKey key)
			throws NotFoundException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(key == null) throw new IllegalArgumentException("WikiPageKey cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.READ).isAuthorized()){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.READ.name(), key.getOwnerObjectId(), key.getOwnerObjectType().name()));
		}
	}
	
	/**
	 * Validate that the user has update access to the owner object.
	 * @param user
	 * @param key
	 * @throws NotFoundException
	 */
	private void validateUpdateAccess(UserInfo user, WikiPageKey key)
			throws NotFoundException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(key == null) throw new IllegalArgumentException("WikiPageKey cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.UPDATE).isAuthorized()){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.READ.name(), key.getOwnerObjectId(), key.getOwnerObjectType().name()));
		}
	}

	@WriteTransaction
	@Override
	public void deleteWiki(UserInfo user, WikiPageKey key) throws UnauthorizedException, DatastoreException{
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(key == null) throw new IllegalArgumentException("WikiPageKey cannot be null");
		// Check that the user is allowed to perform this action
		try {
			if(!authorizationManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.DELETE).isAuthorized()){
				throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.DELETE.name(), key.getOwnerObjectId(), key.getOwnerObjectType().name()));
			}
		} catch (NotFoundException e) {
			// If the owner does not exist then it is okay to delete the wiki
			log.info("Deleting a Wikipage where the owner does not exist: "+key.toString());
		}
		// Pass to the DAO
		wikiPageDao.delete(key);
	}

	@WriteTransaction
	@Override
	public V2WikiPage updateWikiPage(UserInfo user, String objectId, ObjectType objectType, V2WikiPage wikiPage) throws NotFoundException, UnauthorizedException, ConflictingUpdateException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(objectId == null) throw new IllegalArgumentException("ObjectType cannot be null");
		if(objectType == null) throw new IllegalArgumentException("ObjectType cannot be null");
		if(wikiPage == null) throw new IllegalArgumentException("wikiPage cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, objectId,	objectType, ACCESS_TYPE.UPDATE).isAuthorized()){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.UPDATE.name(), objectId, objectType.name()));
		}
		
		// Before we can update the Wiki we need to lock.
		String currentEtag = wikiPageDao.lockForUpdate(wikiPage.getId());
		if(!currentEtag.equals(wikiPage.getEtag())){
			throw new ConflictingUpdateException("ObjectId: "+objectId+" was updated since you last fetched it, retrieve it again and reapply the update");
		}

		// Delete Wiki versions for rank > MAX_RANK
		Long versionAtMaxRank = wikiPageDao.getWikiVersionByRank(WikiPageKeyHelper.createWikiPageKey(objectId, objectType, wikiPage.getId()), MAX_WIKI_MARKDOWN_RANK);
		wikiPageDao.deleteWikiVersions(WikiPageKeyHelper.createWikiPageKey(objectId, objectType, wikiPage.getId()), versionAtMaxRank);
		
		// Set modified by
		wikiPage.setModifiedBy(user.getId().toString());
		
		// First build up the complete map of names to FileHandles of the wiki's attachments
		Map<String, FileHandle> nameToHandleMap = buildFileNameMap(wikiPage);
		
		// Get all the file handles used for this wiki in its history
		List<Long> allFileHandles = wikiPageDao.getFileHandleReservationForWiki(WikiPageKeyHelper.createWikiPageKey(objectId, objectType, wikiPage.getId()));
		Set<Long> existingFileHandleIds = new HashSet<Long>(allFileHandles);
		List<Long> allMarkdownHandles = wikiPageDao.getMarkdownFileHandleIdsForWiki(WikiPageKeyHelper.createWikiPageKey(objectId, objectType, wikiPage.getId()));
		for(Long markdownId: allMarkdownHandles) {
			existingFileHandleIds.add(markdownId);
		}

		// Get all the file handles associated with this wiki that we have to check
		FileHandle markdownFileHandle = fileMetadataDao.get(wikiPage.getMarkdownFileHandleId());
		List<FileHandle> fileHandlesToCheck = new ArrayList<FileHandle>();
		fileHandlesToCheck.add(markdownFileHandle);
		for(FileHandle handle: nameToHandleMap.values()) {
			fileHandlesToCheck.add(handle);
		}

		// Validate that the user can assign all file handles that are new.  
		// Compare all stored attachments for a given wiki to the list of attachments for this updated version
		List<String> newFileHandlesToInsert = new ArrayList<String>();
		for(FileHandle handle: fileHandlesToCheck) {
			// If this file handle is not in the wiki's reservation of attachments, check permissions
			if(!existingFileHandleIds.contains(Long.valueOf(handle.getId()))){
				// the user must have access to the raw FileHandle to assign it to an object.
				authorizationManager.canAccessRawFileHandleByCreator(user, handle.getId(), handle.getCreatedBy()).checkAuthorizationOrElseThrow();
				// Add this to the list of file handles to insert for this update
				newFileHandlesToInsert.add(handle.getId());
			}
		}
		// Set etag
		wikiPage.setEtag(UUID.randomUUID().toString());
		// Pass to the DAO
		return wikiPageDao.updateWikiPage(wikiPage, nameToHandleMap, objectId, objectType, newFileHandlesToInsert);
	}

	@Override
	public PaginatedResults<V2WikiHeader> getWikiHeaderTree(UserInfo user, String ownerId, ObjectType type, Long limit, Long offset) throws DatastoreException, NotFoundException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(type == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(limit == null){
			limit = MAX_LIMIT;
		}
		if(limit > MAX_LIMIT){
			throw new IllegalArgumentException("Limit cannot exceed: "+MAX_LIMIT);
		}
		if(offset == null){
			offset = 0L;
		}
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user,ownerId, type, ACCESS_TYPE.READ).isAuthorized()){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.READ.name(), ownerId, type.name()));
		}
		List<V2WikiHeader> list = wikiPageDao.getHeaderTree(ownerId, type, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(list, limit, offset);
	}
	
	@Override
	public V2WikiOrderHint getOrderHint(UserInfo user, String objectId,	ObjectType objectType) throws NotFoundException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(objectId == null) throw new IllegalArgumentException("ObjectId cannot be null");
		if(objectType == null) throw new IllegalArgumentException("ObjectType cannot be null");
		
		// Look up the root wiki
		Long rootWikiId = wikiPageDao.getRootWiki(objectId, objectType);
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(objectId, objectType, rootWikiId.toString());
		
		// Check that user has read access.
		validateReadAccess(user, key);
		
		return wikiPageDao.getWikiOrderHint(key);
	}
	
	@WriteTransaction
	@Override
	public V2WikiOrderHint updateOrderHint(UserInfo user, V2WikiOrderHint orderHint) throws NotFoundException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if (orderHint == null) throw new IllegalArgumentException("OrderHint cannot be null");
		
		// Look up the root wiki
		Long rootWikiId = wikiPageDao.getRootWiki(orderHint.getOwnerId(), orderHint.getOwnerObjectType());
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(orderHint.getOwnerId(), orderHint.getOwnerObjectType(), rootWikiId.toString());
		
		// Check that user has update access.
		validateUpdateAccess(user, key);
		
		// Before we can update the Wiki we need to lock.
		String currentEtag = wikiPageDao.lockWikiOwnersForUpdate(key.getWikiPageId());
		if(!currentEtag.equals(orderHint.getEtag())) {
			throw new ConflictingUpdateException("ObjectId: "+orderHint.getOwnerId()+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		
		orderHint.setEtag(UUID.randomUUID().toString());
		
		return wikiPageDao.updateOrderHint(orderHint, key);
	}


	@Override
	public FileHandleResults getAttachmentFileHandles(UserInfo user, WikiPageKey key, Long version) throws NotFoundException {
		// Validate that the user has read access
		validateReadAccess(user, key);
		List<String> handleIds = wikiPageDao.getWikiFileHandleIds(key, version);
		return fileMetadataDao.getAllFileHandles(handleIds, true);
	}

	@Override
	public String getFileHandleIdForFileName(UserInfo user, WikiPageKey wikiPageKey, String fileName, Long version) throws NotFoundException, UnauthorizedException {
		// Validate that the user has read access
		validateReadAccess(user, wikiPageKey);
		// Look-up the fileHandle ID
		return wikiPageDao.getWikiAttachmentFileHandleForFileName(wikiPageKey, fileName, version);
	}

	@WriteTransaction
	@Override
	public V2WikiPage restoreWikiPage(UserInfo user, String objectId,
			ObjectType objectType, Long version, String wikiId) throws NotFoundException,
			UnauthorizedException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(objectId == null) throw new IllegalArgumentException("ObjectType cannot be null");
		if(objectType == null) throw new IllegalArgumentException("ObjectType cannot be null");
		if(wikiId == null) throw new IllegalArgumentException("Wiki id cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, objectId, objectType, ACCESS_TYPE.UPDATE).isAuthorized()){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.UPDATE.name(), objectId, objectType.name()));
		}
		
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(objectId, objectType, wikiId);
		// Get the most recent version of the wiki page for its etag
		V2WikiPage wiki = wikiPageDao.get(key, null);
		
		V2WikiMarkdownVersion versionOfContents = wikiPageDao.getVersionOfWikiContent(key, version);
		
		// Set up a new V2 WikiPage
		V2WikiPage newWikiVersion = new V2WikiPage();
		newWikiVersion.setId(wikiId);
		newWikiVersion.setEtag(wiki.getEtag());
		newWikiVersion.setParentWikiId(wiki.getParentWikiId());
		//Preserve creation metadata
		newWikiVersion.setCreatedBy(wiki.getCreatedBy());
		newWikiVersion.setCreatedOn(wiki.getCreatedOn());
		// Assign restored content to the wiki page
		newWikiVersion.setMarkdownFileHandleId(versionOfContents.getMarkdownFileHandleId());
		newWikiVersion.setAttachmentFileHandleIds(versionOfContents.getAttachmentFileHandleIds());
		newWikiVersion.setTitle(versionOfContents.getTitle());
		// Update the page with these changes
		return updateWikiPage(user, objectId, objectType, newWikiVersion);
	}

	@Override
	public PaginatedResults<V2WikiHistorySnapshot> getWikiHistory(
			UserInfo user, String ownerId, ObjectType type, WikiPageKey wikiPageKey, Long limit, Long offset)
			throws NotFoundException, DatastoreException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(type == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(limit == null) throw new IllegalArgumentException("limit cannot be null");
		if(offset == null) throw new IllegalArgumentException("offset cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user,ownerId, type, ACCESS_TYPE.READ).isAuthorized()){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.READ.name(), ownerId, type.name()));
		}
		List<V2WikiHistorySnapshot> snapshots = wikiPageDao.getWikiHistory(wikiPageKey, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(snapshots, limit, offset);
	}
	
	@Override
	public String getMarkdownFileHandleId(UserInfo user,
			WikiPageKey wikiPageKey, Long version) throws NotFoundException,
			UnauthorizedException {
		// Validate that the user has read access
		validateReadAccess(user, wikiPageKey);
		// Look-up the fileHandle ID
		return wikiPageDao.getMarkdownHandleId(wikiPageKey, version);
	}

	@Override
	public WikiPageKey getRootWikiKey(UserInfo user, String ownerId,
			ObjectType type) throws NotFoundException {
		Long rootWikiId = wikiPageDao.getRootWiki(ownerId, type);
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, type, rootWikiId.toString());
		// Validate that the user has read access
		validateReadAccess(user, key);
		return key;
	}
}
