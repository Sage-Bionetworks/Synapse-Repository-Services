package org.sagebionetworks.repo.manager.wiki;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

public interface V2WikiMirrorManager {
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
	 * Update a wiki page if allowed.
	 * @param user
	 * @param objectId
	 * @param objectType
	 * @param toUpdate
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 */
	V2WikiPage updateWikiPage(UserInfo user, String objectId, ObjectType objectType, V2WikiPage toUpdate, String etagToUpdate) throws NotFoundException, UnauthorizedException;

	/**
	 * Delete a wiki page.
	 * @param user
	 * @param wikiPageKey
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void deleteWiki(UserInfo user, WikiPageKey wikiPageKey) throws UnauthorizedException, DatastoreException, NotFoundException;
	
}
