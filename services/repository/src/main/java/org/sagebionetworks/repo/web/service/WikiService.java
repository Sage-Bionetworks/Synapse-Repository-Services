package org.sagebionetworks.repo.web.service;

import java.net.URL;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

public interface WikiService {

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
	WikiPage createWikiPage(String userId, String objectId, ObjectType entity,	WikiPage toCreate) throws DatastoreException, NotFoundException;

	/**
	 * Get a wiki page.
	 * @param userId
	 * @param entityId
	 * @param entity
	 * @param wikiId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	WikiPage getWikiPage(String userId, WikiPageKey key) throws DatastoreException, NotFoundException;

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
	WikiPage updateWikiPage(String userId, String objectId, ObjectType objectType,	WikiPage toUpdate) throws DatastoreException, NotFoundException;

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
	PaginatedResults<WikiHeader> getWikiHeaderTree(String userId,String ownerId, ObjectType type, Long limit , Long offest) throws DatastoreException, NotFoundException;

	/**
	 * Get all of the file handles of all attachments on the given wiki page.
	 * @param ownerId
	 * @param type
	 * @param wikiId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandleResults getAttachmentFileHandles(String userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the Redirect URL for a given attachment.
	 * @param userId
	 * @param wikiPageKey
	 * @param fileName
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	URL getAttachmentRedirectURL(String userId, WikiPageKey wikiPageKey, String fileName) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the redirect URL for a given Preview.
	 * @param userId
	 * @param wikiPageKey
	 * @param fileName
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	URL getAttachmentPreviewRedirectURL(String userId, WikiPageKey wikiPageKey, String fileName) throws DatastoreException, NotFoundException;

	/**
	 * Get the root wiki page.
	 * @param userId
	 * @param ownerId
	 * @param entity
	 * @return
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 */
	WikiPage getRootWikiPage(String userId, String ownerId, ObjectType type) throws UnauthorizedException, NotFoundException;

}
