package org.sagebionetworks.repo.manager.wiki;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for the Wiki manager.
 * @author John
 *
 */
public interface WikiManager {

	/**
	 * Create a Wiki page for a given object.
	 * @param user
	 * @param objectId
	 * @param objectType
	 * @param toCreate
	 * @return
	 * @throws NotFoundException 
	 */
	WikiPage createWikiPage(UserInfo user, String objectId,	ObjectType objectType, WikiPage toCreate) throws NotFoundException, UnauthorizedException;

	/**
	 * Get a wiki page for a given object.
	 * @param user
	 * @param objectId
	 * @param objectType
	 * @param wikiId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 */
	WikiPage getWikiPage(UserInfo user, WikiPageKey key) throws NotFoundException, UnauthorizedException;
	
	/**
	 * Get the root wiki page for an object.
	 * @param user
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	WikiPage getRootWikiPage(UserInfo user, String objectId, ObjectType objectType) throws NotFoundException, UnauthorizedException;

	/**
	 * Delete a wiki page.
	 * @param user
	 * @param wikiPageKey
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void deleteWiki(UserInfo user, WikiPageKey wikiPageKey) throws UnauthorizedException, DatastoreException, NotFoundException;

	/**
	 * Update a wiki page if allowed.
	 * @param user
	 * @param objectId
	 * @param objectType
	 * @param toUpdate
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 */
	WikiPage updateWikiPage(UserInfo user, String objectId,	ObjectType objectType, WikiPage toUpdate) throws NotFoundException, UnauthorizedException;

	/**
	 * 
	 * @param user
	 * @param ownerId
	 * @param type
	 * @param limit
	 * @param offest
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	PaginatedResults<WikiHeader> getWikiHeaderTree(UserInfo user, String ownerId, ObjectType type,	Long limit, Long offest) throws DatastoreException, NotFoundException;

	/**
	 * Get the attachment file handles for a give wiki page.
	 * @param user
	 * @param wikiPageKey
	 * @return
	 * @throws NotFoundException 
	 */
	FileHandleResults getAttachmentFileHandles(UserInfo user, WikiPageKey wikiPageKey) throws NotFoundException;
	
	/**
	 * Get the FileHandle ID for a given WikiPage and file name.
	 * 
	 * @param wikiPageKey
	 * @param fileName
	 * @return
	 */
	String getFileHandleIdForFileName(UserInfo user, WikiPageKey wikiPageKey, String fileName) throws NotFoundException, UnauthorizedException;

}
