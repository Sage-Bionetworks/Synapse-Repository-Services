package org.sagebionetworks.repo.model.v2.dao;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * The abstraction of the V2 wikipage data access object.
 * (Derived from org.sagebionetworks.repo.model.dao.WikiPageDao)
 * 
 * @author hso
 *
 */
public interface V2WikiPageDao {
	/**
	 * Create a new WikiPage.
	 * @param toCreate
	 * @param fileNameToFileHandleMap - Maps the name of a file to its FileHandle.  Used to ensure file names are unique within a single WikiPage.
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws NotFoundException
	 */
	public V2WikiPage create(V2WikiPage toCreate, Map<String, FileHandle> fileNameToFileHandleMap, String ownerId, ObjectType ownerType, List<String> newFileHandleIds) throws NotFoundException;
	
	/**
	 * Update a wikipage.
	 * @param toUpdate
	 * @param fileNameToFileHandleMap - Maps the name of a file to its FileHandle.  Used to ensure file names are unique within a single WikiPage/keeps track of which files this wiki contains
	 * @param ownerId
	 * @param ownerType
	 * @param newFileHandleIds - Identifies which files are to be inserted. Ensures that only new files are added to the database.
	 * @return
	 * @throws NotFoundException
	 */
	public V2WikiPage updateWikiPage(V2WikiPage toUpdate, Map<String, FileHandle> fileNameToFileHandleMap, String ownerId, ObjectType ownerType, List<String> newFileHandleIds) throws NotFoundException;		
	/**
	 * Get a wiki page.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 */
	public V2WikiPage get(WikiPageKey key) throws NotFoundException;
	
	/**
	 * To look at ANY VERSION of a wiki's markdown: Get the handle id of a version's markdown.
	 * @param key
	 * @param version
	 * @return
	 * @throws NotFoundException 
	 */
	public String getMarkdownHandleIdFromHistory(WikiPageKey key, Long version) throws NotFoundException;
	
	/**
	 * To look at ANY VERSION of a wiki's attachments: Get the handle ids of a version's attachments.
	 * @param key
	 * @param version
	 * @return
	 * @throws NotFoundException 
	 */
	public List<String> getWikiFileHandleIdsFromHistory(WikiPageKey key, Long version) throws NotFoundException;
	
	/**
	 * Get ALL the file handle ids used (in the past/currently) for a wiki page.
	 * @param key
	 * @param version
	 * @return
	 * @throws NotFoundException 
	 */
	public List<Long> getFileHandleReservationForWiki(WikiPageKey key);
	
	/**
	 * Get ALL file handle ids for a wiki's markdown
	 * @param key
	 * @return
	 */
	public List<Long> getMarkdownFileHandleIdsForWiki(WikiPageKey key);
	
	/**
	 * Get the ID of the root wiki page for a given Object.
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws NotFoundException
	 */
	public Long getRootWiki(String ownerId, ObjectType ownerType) throws NotFoundException;
	
	/**
	 * Delete a wiki page.
	 * @param id
	 */
	public void delete(WikiPageKey key);
	
	/**
	 * Get snapshots of a wiki page's history.
	 * @param key
	 * @param limit
	 * @param offset
	 */
	public List<V2WikiHistorySnapshot> getWikiHistory(WikiPageKey key, Long limit, Long offset) throws NotFoundException, DatastoreException;

	/**
	 * Get the entire tree of wiki pages for a given owner.
	 * @param parentId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	List<V2WikiHeader> getHeaderTree(String ownerId, ObjectType ownerType) throws DatastoreException, NotFoundException;
	
	/**
	 * Lock for update, returning the current etag
	 * @param wikiId
	 * @return
	 */
	String lockForUpdate(String wikiId);
	
	/**
	 * Get all of the FileHandleIds for a wiki page.
	 * @param key
	 * @return
	 * @throws NotFoundException 
	 */
	List<String> getWikiFileHandleIds(WikiPageKey key) throws NotFoundException;
	
	/**
	 * Lookup the FileHandleId for a given WikiPage with the given name.
	 * 
	 * @param ownerId
	 * @param type
	 * @param fileName
	 * @return
	 */
	String getWikiAttachmentFileHandleForFileName(WikiPageKey key, String fileName) throws NotFoundException;

	/**
	 * Given a wiki id, lookup the key
	 * @param wikiId
	 * @return
	 */
	public WikiPageKey lookupWikiKey(String wikiId) throws NotFoundException;

	long getCount() throws DatastoreException;
}
