package org.sagebionetworks.repo.model.dao;

import org.sagebionetworks.repo.model.ObjectType;

/**
 * It takes a three part key to access a wiki page.
 * @author John
 *
 */
public class WikiPageKeyHelper {
	
	public static WikiPageKey createWikiPageKey(String ownerObjectId, ObjectType ownerObjectType, String wikiPageId) {
		WikiPageKey result = new WikiPageKey();
		result.setOwnerObjectId(ownerObjectId);
		result.setOwnerObjectType(ownerObjectType);
		result.setWikiPageId(wikiPageId);
		return result;
	}
}
