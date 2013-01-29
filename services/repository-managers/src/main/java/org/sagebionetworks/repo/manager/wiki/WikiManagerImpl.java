package org.sagebionetworks.repo.manager.wiki;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.HasPreviewId;
import org.sagebionetworks.repo.model.message.ObjectType;
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

	@Autowired
	WikiPageDao wikiPageDao;
	
	@Autowired
	FileMetadataDao fileMetadataDao;
	
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
			AuthorizationManager authorizationManager, FileMetadataDao fileMetadataDao) {
		super();
		this.wikiPageDao = wikiPageDao;
		this.authorizationManager = authorizationManager;
		this.fileMetadataDao = fileMetadataDao;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage createWikiPage(UserInfo user, String objectId,	ObjectType objectType, WikiPage toCreate) throws NotFoundException, UnauthorizedException{
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(objectId == null) throw new IllegalArgumentException("objectId cannot be null");
		if(objectType == null) throw new IllegalArgumentException("ObjectType cannot be null");
		if(toCreate == null) throw new IllegalArgumentException("WikiPage cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, objectId,	objectType, ACCESS_TYPE.CREATE)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.CREATE.name(), objectId, objectType.name()));
		}
		// Set created by and modified by
		toCreate.setCreatedBy(user.getIndividualGroup().getId());
		toCreate.setModifiedBy(toCreate.getCreatedBy());
		// pass to the DAO
		return wikiPageDao.create(toCreate, objectId, objectType);
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
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.CREATE.name(), key.getOwnerObjectId(), key.getOwnerObjectType().name()));
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
	public WikiPage updateWikiPage(UserInfo user, String objectId,	ObjectType objectType, WikiPage toUpdate) throws NotFoundException, UnauthorizedException, ConflictingUpdateException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(objectId == null) throw new IllegalArgumentException("ObjectType cannot be null");
		if(objectType == null) throw new IllegalArgumentException("ObjectType cannot be null");
		if(toUpdate == null) throw new IllegalArgumentException("WikiPage cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, objectId,	objectType, ACCESS_TYPE.UPDATE)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.UPDATE.name(), objectId, objectType.name()));
		}
		// Before we can update the Wiki we need to lock.
		String currentEtag = wikiPageDao.lockForUpdate(toUpdate.getId());
		if(!currentEtag.equals(toUpdate.getEtag())){
			throw new ConflictingUpdateException("ObjectId: "+objectId+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Set modified by
		toUpdate.setModifiedBy(user.getIndividualGroup().getId());
		// Pass to the DAO
		return wikiPageDao.updateWikiPage(toUpdate, objectId, objectType, false);
	}

	@Override
	public PaginatedResults<WikiHeader> getWikiHeaderTree(UserInfo user, String ownerId, ObjectType type, Long limit, Long offset) throws DatastoreException, NotFoundException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(type == null) throw new IllegalArgumentException("ownerId cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user,ownerId, type, ACCESS_TYPE.READ)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.CREATE.name(), ownerId, type.name()));
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
		List<FileHandle> handles = new LinkedList<FileHandle>();
		if(handleIds != null){
			for(String handleId: handleIds){
				// Look up each handle
				FileHandle handle = fileMetadataDao.get(handleId);
				handles.add(handle);
				// If this handle has a preview then we fetch that as well.
				if(handle instanceof HasPreviewId){
					String previewId = ((HasPreviewId)handle).getPreviewId();
					if(previewId != null){
						FileHandle preview = fileMetadataDao.get(previewId);
						handles.add(preview);
					}
				}
			}
		}
		FileHandleResults results = new FileHandleResults();
		results.setList(handles);
		return results;
	}

}
