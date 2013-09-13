package org.sagebionetworks.repo.manager.wiki;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.HasPreviewId;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * WikiManager implementation.
 * 
 * @author John
 *
 */
public class WikiManagerImpl implements WikiManager {
	
	static private Log log = LogFactory.getLog(WikiManagerImpl.class);	
	
	private static final String USER_IS_NOT_AUTHORIZED_TEMPLATE = "User is not authorized to '%1$s' a WikiPage with an onwerId: '%2$s' of type: '%3$s'";
	
	private static final String USER_IS_NOT_AUTHORIZED_FILE_HANDLE_TEMPLATE = "Only the creator of a FileHandle id: '%1$s' is authorized to assgin it to an object";

	@Autowired
	WikiPageDao wikiPageDao;
	
	@Autowired
	FileHandleDao fileMetadataDao;
	
	@Autowired
	AuthorizationManager authorizationManager;

	/**
	 * Default for Spring!
	 */
	public WikiManagerImpl(){}
	
	/**
	 * The IoC constructor.
	 * @param wikiPageDao
	 * @param authorizationManager
	 */
	public WikiManagerImpl(WikiPageDao wikiPageDao,
			AuthorizationManager authorizationManager, FileHandleDao fileMetadataDao) {
		super();
		this.wikiPageDao = wikiPageDao;
		this.authorizationManager = authorizationManager;
		this.fileMetadataDao = fileMetadataDao;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage createWikiPage(UserInfo user, String objectId,	ObjectType objectType, WikiPage wikiPage) throws NotFoundException, UnauthorizedException{
		if(user == null) throw new IllegalArgumentException("user cannot be null");
		if(objectId == null) throw new IllegalArgumentException("objectId cannot be null");
		if(objectType == null) throw new IllegalArgumentException("objectType cannot be null");
		if(wikiPage == null) throw new IllegalArgumentException("wikiPage cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, objectId,	objectType, ACCESS_TYPE.CREATE)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.CREATE.name(), objectId, objectType.name()));
		}
		// For a create the user must be the creator of each file handle used.
		
		// Set created by and modified by
		wikiPage.setCreatedBy(user.getIndividualGroup().getId());
		wikiPage.setModifiedBy(wikiPage.getCreatedBy());
		// First build up the map of names to FileHandles
		Map<String, FileHandle> nameToHandleMap = buildFileNameMap(wikiPage);
		// Validate that the user can assign all file handles
		for(FileHandle handle: nameToHandleMap.values()){
			// the user must have access to the raw FileHandle to assign it to an object.
			if(!authorizationManager.canAccessRawFileHandleByCreator(user, handle.getCreatedBy())){
				throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_FILE_HANDLE_TEMPLATE, handle.getId()));
			}
		}
		// pass to the DAO
		return wikiPageDao.create(wikiPage, nameToHandleMap, objectId, objectType);
	}
	
	/**
	 * Build up the Map of FileHandle.fileNames to FileHandles  If there are duplicate names in the input list,
	 * then then the handle with the most recent creation date will be used.
	 * @param page
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	Map<String, FileHandle> buildFileNameMap(WikiPage page) throws DatastoreException, NotFoundException{
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
	public WikiPage getRootWikiPage(UserInfo user, String objectId,	ObjectType objectType) throws NotFoundException, UnauthorizedException {
		// Look up the root wiki
		Long rootWikiId = wikiPageDao.getRootWiki(objectId, objectType);
		WikiPageKey key = new WikiPageKey(objectId, objectType, rootWikiId.toString());
		// The security check is done in the default method.
		return getWikiPage(user, key);
	}

	@Override
	public WikiPage getWikiPage(UserInfo user, WikiPageKey key) throws NotFoundException, UnauthorizedException {
		// Validate that the user has read access
		validateReadAccess(user, key);
		// Pass to the DAO
		return wikiPageDao.get(key);
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
		if(!authorizationManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.READ)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.READ.name(), key.getOwnerObjectId(), key.getOwnerObjectType().name()));
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteWiki(UserInfo user, WikiPageKey key) throws UnauthorizedException, DatastoreException{
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(key == null) throw new IllegalArgumentException("WikiPageKey cannot be null");
		// Check that the user is allowed to perform this action
		try {
			if(!authorizationManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.DELETE)){
				throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.DELETE.name(), key.getOwnerObjectId(), key.getOwnerObjectType().name()));
			}
		} catch (NotFoundException e) {
			// If the owner does not exist then it is okay to delete the wiki
			log.info("Deleting a Wikipage where the owner does not exist: "+key.toString());
		}
		// Pass to the DAO
		wikiPageDao.delete(key);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage updateWikiPage(UserInfo user, String objectId,	ObjectType objectType, WikiPage wikiPage) throws NotFoundException, UnauthorizedException, ConflictingUpdateException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(objectId == null) throw new IllegalArgumentException("ObjectType cannot be null");
		if(objectType == null) throw new IllegalArgumentException("ObjectType cannot be null");
		if(wikiPage == null) throw new IllegalArgumentException("wikiPage cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, objectId,	objectType, ACCESS_TYPE.UPDATE)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.UPDATE.name(), objectId, objectType.name()));
		}
		// Before we can update the Wiki we need to lock.
		String currentEtag = wikiPageDao.lockForUpdate(wikiPage.getId());
		if(!currentEtag.equals(wikiPage.getEtag())){
			throw new ConflictingUpdateException("ObjectId: "+objectId+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Set modified by
		wikiPage.setModifiedBy(user.getIndividualGroup().getId());
		// First build up the map of names to FileHandles
		Map<String, FileHandle> nameToHandleMap = buildFileNameMap(wikiPage);
		// Validate that the user can assign all file handles that are new.  To do this we first must detect which file handles are already assgined to the wiki
		List<String> currentFileHandleId = wikiPageDao.getWikiFileHandleIds(new WikiPageKey(objectId, objectType, wikiPage.getId()));
		Set<String> currentHandleSet = new HashSet<String>(currentFileHandleId);
		for(FileHandle handle: nameToHandleMap.values()){
			// We need to check any new handle
			if(!currentHandleSet.contains(handle.getId())){
				// the user must have access to the raw FileHandle to assign it to an object.
				if(!authorizationManager.canAccessRawFileHandleByCreator(user, handle.getCreatedBy())){
					throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_FILE_HANDLE_TEMPLATE, handle.getId()));
				}
			}
		}
		// Pass to the DAO
		return wikiPageDao.updateWikiPage(wikiPage, nameToHandleMap, objectId, objectType, false);
	}

	@Override
	public PaginatedResults<WikiHeader> getWikiHeaderTree(UserInfo user, String ownerId, ObjectType type, Long limit, Long offset) throws DatastoreException, NotFoundException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(type == null) throw new IllegalArgumentException("ownerId cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user,ownerId, type, ACCESS_TYPE.READ)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.READ.name(), ownerId, type.name()));
		}
		// Limit and offset are currently ignored.
		List<WikiHeader> list = wikiPageDao.getHeaderTree(ownerId, type);
		return new PaginatedResults<WikiHeader>(list, list.size());
	}

	@Override
	public FileHandleResults getAttachmentFileHandles(UserInfo user, WikiPageKey key) throws NotFoundException {
		// Validate that the user has read access
		validateReadAccess(user, key);
		List<String> handleIds = wikiPageDao.getWikiFileHandleIds(key);
		return fileMetadataDao.getAllFileHandles(handleIds, true);
	}

	@Override
	public String getFileHandleIdForFileName(UserInfo user, WikiPageKey wikiPageKey, String fileName) throws NotFoundException, UnauthorizedException {
		// Validate that the user has read access
		validateReadAccess(user, wikiPageKey);
		// Look-up the fileHandle ID
		return wikiPageDao.getWikiAttachmentFileHandleForFileName(wikiPageKey, fileName);
	}

}
