package org.sagebionetworks.repo.model.v2.dao;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiMarkdownVersion;
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
	 * Get a version of a wiki page.
	 * @param version TODO
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 */
	public V2WikiPage get(WikiPageKey key, Long version) throws NotFoundException;
	
	/**
	 * Get the markdown of a wiki page as a string.
	 * @param key
	 * @param version TODO
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public String getMarkdown(WikiPageKey key, Long version) throws IOException, NotFoundException;
	
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
	 * Gets a version of a wiki's title, markdown handle id, and list of attachments' file handle ids.
	 * @param key
	 * @param version
	 * @return
	 * @throws NotFoundException
	 */
	public V2WikiMarkdownVersion getVersionOfWikiContent(WikiPageKey key, Long version) throws NotFoundException;
	
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
	 * To look at ANY VERSION of a wiki's attachments: Get the handle ids of a version's attachments.
	 * If version is null, the current attachments of the wiki are returned.
	 * @param key
	 * @param version
	 * @return
	 * @throws NotFoundException 
	 */
	List<String> getWikiFileHandleIds(WikiPageKey key, Long version) throws NotFoundException;
	
	/**
	 * Lookup the FileHandleId for an attachment, for a given WikiPage with the given name.
	 * @param fileName
	 * @param version TODO
	 * @param ownerId
	 * @param type
	 * 
	 * @return
	 */
	String getWikiAttachmentFileHandleForFileName(WikiPageKey key, String fileName, Long version) throws NotFoundException;

	/**
	 * To look at ANY VERSION of a wiki's markdown: Get the handle id of a version's markdown.
	 * If version is null, then the current markdown handle id is returned.
	 * @param key
	 * @param version
	 * @return
	 * @throws NotFoundException 
	 */
	String getMarkdownHandleId(WikiPageKey key, Long version) throws NotFoundException;
	
	/**
	 * Given a wiki id, lookup the key
	 * @param wikiId
	 * @return
	 */
	public WikiPageKey lookupWikiKey(String wikiId) throws NotFoundException;

	long getCount() throws DatastoreException;
}
