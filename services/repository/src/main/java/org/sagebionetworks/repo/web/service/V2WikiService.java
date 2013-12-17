package org.sagebionetworks.repo.web.service;

import java.net.URL;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

public interface V2WikiService {
	/**
	 * Create a new wiki page for a given owner.
	 * 
	 * @param userId
	 * @param objectId
	 * @param entity
	 * @param toCreate
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	V2WikiPage createWikiPage(String userId, String objectId, ObjectType entity, V2WikiPage toCreate) throws DatastoreException, NotFoundException;

	/**
	 * Get a wiki page.
	 * @param userId
	 * @param version TODO
	 * @param entityId
	 * @param entity
	 * @param wikiId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	V2WikiPage getWikiPage(String userId, WikiPageKey key, Long version) throws DatastoreException, NotFoundException;

	/**
	 * Update a wiki page.
	 * @param userId
	 * @param entityId
	 * @param entity
	 * @param toCreate
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	V2WikiPage updateWikiPage(String userId, String objectId, ObjectType objectType, V2WikiPage toUpdate) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userId
	 * @param objectId
	 * @param objectType
	 * @param wikiId
	 * @param version
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	V2WikiPage restoreWikipage(String userId, String objectId, ObjectType objectType, String wikiId, Long version) throws NotFoundException;
	
	/**
	 * Delete a wiki page.
	 * @param userId
	 * @param wikiPageKey
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void deleteWikiPage(String userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException;

	/**
	 * Get the WikiHeaderTree for an owner.
	 * @param userId
	 * @param ownerId
	 * @param entity
	 * @param limit
	 * @param offest
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	PaginatedResults<V2WikiHeader> getWikiHeaderTree(String userId,String ownerId, ObjectType type, Long limit , Long offest) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userId
	 * @param ownerId
	 * @param type
	 * @param limit
	 * @param offest
	 * @param wikiPageKey
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	PaginatedResults<V2WikiHistorySnapshot> getWikiHistory(String userId, String ownerId, ObjectType type, Long limit , Long offset, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException;
	
	/**
	 * Get all of the file handles of all attachments on the given wiki page.
	 * @param version TODO
	 * @param ownerId
	 * @param type
	 * @param wikiId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandleResults getAttachmentFileHandles(String userId, WikiPageKey wikiPageKey, Long version) throws DatastoreException, NotFoundException;

	/**
	 * Get the redirect URL for the wiki's markdown.
	 * @param userId
	 * @param wikiPageKey
	 * @param version TODO
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	URL getMarkdownRedirectURL(String userId, WikiPageKey wikiPageKey, Long version) throws DatastoreException, NotFoundException;

	/**
	 * Get the Redirect URL for a given attachment.
	 * @param userId
	 * @param wikiPageKey
	 * @param fileName
	 * @param version TODO
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	URL getAttachmentRedirectURL(String userId, WikiPageKey wikiPageKey, String fileName, Long version) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the redirect URL for a given Preview.
	 * @param userId
	 * @param wikiPageKey
	 * @param fileName
	 * @param version TODO
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	URL getAttachmentPreviewRedirectURL(String userId, WikiPageKey wikiPageKey, String fileName, Long version) throws DatastoreException, NotFoundException;

	/**
	 * Get the root wiki page.
	 * @param userId
	 * @param ownerId
	 * @param entity
	 * @return
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 */
	V2WikiPage getRootWikiPage(String userId, String ownerId, ObjectType type) throws UnauthorizedException, NotFoundException;

}
