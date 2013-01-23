package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ObjectType;
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

}
