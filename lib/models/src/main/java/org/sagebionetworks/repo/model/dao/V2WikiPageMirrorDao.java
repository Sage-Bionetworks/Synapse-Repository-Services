package org.sagebionetworks.repo.model.dao;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

public interface V2WikiPageMirrorDao {
	/**
	 * Creates a V2 WikiPage in the V2 DB to mirror a V1 Wiki
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
	 * Updates the V2 WikiPage to mirror its corresponding V1 Wiki
	 * @param wikiPage
	 * @param fileNameToFileHandleMap
	 * @param ownerId
	 * @param ownerType
	 * @param newFileHandleIds
	 * @return
	 * @throws NotFoundException
	 */
	public V2WikiPage update(V2WikiPage wikiPage, Map<String, FileHandle> fileNameToFileHandleMap, String ownerId, ObjectType ownerType, List<String> newFileHandleIds) throws NotFoundException;

	/**
	 * Delete a wiki page.
	 * @param id
	 */
	public void delete(WikiPageKey key);
	
	/**
	 * Lock for update, returning the current etag
	 * @param wikiId
	 * @return
	 */
	String lockForUpdate(String wikiId);
	
	/**
	 * Get ALL the file handle ids used (in the past/currently) for a wiki page.
	 * @param key
	 * @param version
	 * @return
	 * @throws NotFoundException 
	 */
	public List<Long> getFileHandleReservationForWiki(WikiPageKey key);
}
