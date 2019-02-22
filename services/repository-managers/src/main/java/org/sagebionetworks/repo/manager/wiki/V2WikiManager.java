package org.sagebionetworks.repo.manager.wiki;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
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
	 * @param version TODO
	 * @param objectId
	 * @param objectType
	 * @param wikiId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 */
	V2WikiPage getWikiPage(UserInfo user, WikiPageKey key, Long version) throws NotFoundException, UnauthorizedException;
	
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
	 * @param wikiId
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	V2WikiPage restoreWikiPage(UserInfo user, String objectId, ObjectType objectType, Long version, String wikiId) throws NotFoundException, UnauthorizedException;

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
	 * @param version TODO
	 * @return
	 * @throws NotFoundException 
	 */
	FileHandleResults getAttachmentFileHandles(UserInfo user, WikiPageKey wikiPageKey, Long version) throws NotFoundException;
	
	/**
	 * Get the FileHandle ID for a given WikiPage and file name.
	 * @param wikiPageKey
	 * @param fileName
	 * @param version TODO
	 * 
	 * @return
	 */
	String getFileHandleIdForFileName(UserInfo user, WikiPageKey wikiPageKey, String fileName, Long version) throws NotFoundException, UnauthorizedException;

	/**
	 * Get the markdown file handle for a wiki page.
	 * @param user
	 * @param wikiPageKey
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	String getMarkdownFileHandleId(UserInfo user, WikiPageKey wikiPageKey, Long version) throws NotFoundException, UnauthorizedException;
	
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

	/**
	 * Gets the order hint associated with the given object id and object type.
	 * @param user
	 * @param objectId
	 * @param objectType
	 * @return The order hint associated with the given object id and object type.
	 * @throws NotFoundException
	 */
	V2WikiOrderHint getOrderHint(UserInfo user, String objectId, ObjectType objectType) throws NotFoundException;
	
	/**
	 * Updates the given order hint.
	 * @param user
	 * @param orderHint
	 * @return The updated order hint.
	 * @throws NotFoundException
	 */
	V2WikiOrderHint updateOrderHint(UserInfo user, V2WikiOrderHint orderHint) throws NotFoundException;

	/**
	 * Get the root wiki page key.
	 * @param user
	 * @param ownerId
	 * @param type
	 * @return
	 * @throws NotFoundException 
	 */
	WikiPageKey getRootWikiKey(UserInfo user, String ownerId, ObjectType type) throws NotFoundException;
	
}
