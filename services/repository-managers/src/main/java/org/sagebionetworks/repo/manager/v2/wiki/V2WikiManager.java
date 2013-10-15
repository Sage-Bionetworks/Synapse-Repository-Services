package org.sagebionetworks.repo.manager.v2.wiki;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for the V2 Wiki manager.
 * (Derived from org.sagebionetworks.repo.manager.wiki.WikiManager) 
 * @author hso
 *
 */
public interface V2WikiManager {

	/**
	 * Create a Wiki page for a given object.
	 * @param user
	 * @param objectId
	 * @param objectType
	 * @param toCreate
	 * @return
	 * @throws NotFoundException 
	 */
	V2WikiPage createWikiPage(UserInfo user, String objectId,	ObjectType objectType, V2WikiPage toCreate) throws NotFoundException, UnauthorizedException;

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
	V2WikiPage getWikiPage(UserInfo user, WikiPageKey key) throws NotFoundException, UnauthorizedException;
	
	/**
	 * Get the root wiki page for an object.
	 * @param user
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	V2WikiPage getRootWikiPage(UserInfo user, String objectId, ObjectType objectType) throws NotFoundException, UnauthorizedException;

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
	V2WikiPage updateWikiPage(UserInfo user, String objectId, ObjectType objectType, V2WikiPage toUpdate) throws NotFoundException, UnauthorizedException;

	/**
	 * 
	 * @param user
	 * @param objectId
	 * @param objectType
	 * @param version
	 * @param current
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	V2WikiPage restoreWikiPage(UserInfo user, String objectId, ObjectType objectType, Long version, V2WikiPage current) throws NotFoundException, UnauthorizedException;

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
	PaginatedResults<V2WikiHeader> getWikiHeaderTree(UserInfo user, String ownerId, ObjectType type,	Long limit, Long offest) throws DatastoreException, NotFoundException;

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

	/**
	 * @param user
	 * @param ownerId
	 * @param type
	 * @param wikiPageKey
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException
	 */
	PaginatedResults<V2WikiHistorySnapshot> getWikiHistory(UserInfo user, String ownerId, ObjectType type, WikiPageKey wikiPageKey, Long limit, Long offset) throws NotFoundException, DatastoreException;

}
