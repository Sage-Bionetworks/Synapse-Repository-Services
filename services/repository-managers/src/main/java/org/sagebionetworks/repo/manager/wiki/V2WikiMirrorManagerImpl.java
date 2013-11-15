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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.V2WikiPageMirrorDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class V2WikiMirrorManagerImpl implements V2WikiMirrorManager {
	@Autowired
	V2WikiPageMirrorDao v2WikiPageMirrorDao;
	@Autowired
	FileHandleDao fileMetadataDao;
	@Autowired
	AuthorizationManager authorizationManager;
	
	
	static private Log log = LogFactory.getLog(V2WikiManagerImpl.class);	
	private static final String USER_IS_NOT_AUTHORIZED_TEMPLATE = "User is not authorized to '%1$s' a WikiPage with an onwerId: '%2$s' of type: '%3$s'";
	private static final String USER_IS_NOT_AUTHORIZED_FILE_HANDLE_TEMPLATE = "Only the creator of a FileHandle id: '%1$s' is authorized to assgin it to an object";
	
	/**
	 * Default for Spring!
	 */
	public V2WikiMirrorManagerImpl(){}
	
	@Override
	public V2WikiPage createWikiPage(UserInfo user, String objectId,
			ObjectType objectType, V2WikiPage toCreate)
			throws NotFoundException, UnauthorizedException {
		if(user == null) throw new IllegalArgumentException("user cannot be null");
		if(objectId == null) throw new IllegalArgumentException("objectId cannot be null");
		if(objectType == null) throw new IllegalArgumentException("objectType cannot be null");
		if(toCreate == null) throw new IllegalArgumentException("wikiPage cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, objectId,	objectType, ACCESS_TYPE.CREATE)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.CREATE.name(), objectId, objectType.name()));
		}

		// First build up the map of names to FileHandles
		Map<String, FileHandle> nameToHandleMap = buildFileNameMap(toCreate);
		
		// For a create the user must be the creator of each file handle used.
		List<String> newFileHandlesToInsert = new ArrayList<String>();
		for(FileHandle handle: nameToHandleMap.values()){
			newFileHandlesToInsert.add(handle.getId());
		}
		
		// Validate that the user can assign all file handles
		for(FileHandle handle: nameToHandleMap.values()){
			// the user must have access to the raw FileHandle to assign it to an object.
			if(!authorizationManager.canAccessRawFileHandleByCreator(user, handle.getCreatedBy())){
				throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_FILE_HANDLE_TEMPLATE, handle.getId()));
			}
		}
		// pass to the DAO
		return v2WikiPageMirrorDao.create(toCreate, nameToHandleMap, objectId, objectType, newFileHandlesToInsert);
	}

	@Override
	public V2WikiPage updateWikiPage(UserInfo user, String objectId,
			ObjectType objectType, V2WikiPage toUpdate, String etagToUpdate)
			throws NotFoundException, UnauthorizedException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(objectId == null) throw new IllegalArgumentException("ObjectType cannot be null");
		if(objectType == null) throw new IllegalArgumentException("ObjectType cannot be null");
		if(toUpdate == null) throw new IllegalArgumentException("wikiPage cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, objectId,	objectType, ACCESS_TYPE.UPDATE)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.UPDATE.name(), objectId, objectType.name()));
		}
		// Before we can update the Wiki we need to lock.
		String currentEtag = v2WikiPageMirrorDao.lockForUpdate(toUpdate.getId());
		if(!currentEtag.equals(toUpdate.getEtag())){
			throw new ConflictingUpdateException("ObjectId: "+objectId+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		
		// First build up the complete map of names to FileHandles of the wiki's attachments
		Map<String, FileHandle> nameToHandleMap = buildFileNameMap(toUpdate);
		
		// Determine which attachments are new to this wiki and need to be inserted. Keep track of the ids and pass them in.
		List<Long> allFileHandles = v2WikiPageMirrorDao.getFileHandleReservationForWiki(new WikiPageKey(objectId, objectType, toUpdate.getId()));
		Set<Long> fileHandleReservation = new HashSet<Long>(allFileHandles);
		
		// Validate that the user can assign all file handles that are new.  
		// Compare all stored attachments for a given wiki to the list of attachments for this updated version
		List<String> newFileHandlesToInsert = new ArrayList<String>();
		for(FileHandle handle: nameToHandleMap.values()){
			// If this file handle is not in the wiki's reservation of attachments, check permissions
			if(!fileHandleReservation.contains(Long.valueOf(handle.getId()))){
				// the user must have access to the raw FileHandle to assign it to an object.
				if(!authorizationManager.canAccessRawFileHandleByCreator(user, handle.getCreatedBy())){
					throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_FILE_HANDLE_TEMPLATE, handle.getId()));
				}
				// Add this to the list of file handles to insert for this update
				newFileHandlesToInsert.add(handle.getId());
			}
		}
		// Set etag to match the updated etag of the mirror V1 wiki
		toUpdate.setEtag(etagToUpdate);
		// Pass to the DAO
		return v2WikiPageMirrorDao.update(toUpdate, nameToHandleMap, objectId, objectType, newFileHandlesToInsert);
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
		v2WikiPageMirrorDao.delete(key);
	}
	
	/**
	 * Build up the Map of FileHandle.fileNames to FileHandles  If there are duplicate names in the input list,
	 * then then the handle with the most recent creation date will be used.
	 * (From org.sagebionetworks.repo.manager.wiki.V2WikiManagerImpl)
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
				log.info("Duplicate attachment file name found for Wiki. The older FileHandle will be replaced with the newer FileHandle.  Old FileHandle: "+old );
			}
		}
		return results;
	}

}
