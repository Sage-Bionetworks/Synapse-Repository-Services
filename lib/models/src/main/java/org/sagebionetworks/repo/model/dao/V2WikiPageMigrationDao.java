package org.sagebionetworks.repo.model.dao;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

public interface V2WikiPageMigrationDao {
	
	/**
	 * Creates a V2 WikiPage in the V2 DB
	 * @param wikiPage
	 * @param fileNameToFileHandleMap
	 * @param ownerId
	 * @param ownerType
	 * @param newFileHandleIds
	 * @return
	 * @throws NotFoundException
	 */
	public V2WikiPage create(V2WikiPage wikiPage, Map<String, FileHandle> fileNameToFileHandleMap, String ownerId, ObjectType ownerType, List<String> newFileHandleIds) throws NotFoundException;
	
	/**
	 * Returns whether a wiki's parent exists in the V2 DB already
	 * @param wikiId
	 * @return
	 */
	public boolean doesWikiExist(String wikiId);
	
	/**
	 * Returns the etag of the wiki specified by its wiki ID. If
	 * wiki is not found, null is returned.
	 * 
	 * @param wikiId
	 * @return
	 */
	public String getWikiEtag(String wikiId);
		
}
