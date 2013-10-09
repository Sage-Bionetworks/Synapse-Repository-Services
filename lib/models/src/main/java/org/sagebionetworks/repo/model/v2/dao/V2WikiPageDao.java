package org.sagebionetworks.repo.model.v2.dao;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
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
	public V2WikiPage create(V2WikiPage toCreate, Map<String, FileHandle> fileNameToFileHandleMap, String ownerId, ObjectType ownerType) throws NotFoundException;
	
	/**
	 * Update a wikipage.
	 * @param toUpdate
	 * @param fileNameToFileHandleMap - Maps the name of a file to its FileHandle.  Used to ensure file names are unique within a single WikiPage.
	 * @param ownerId
	 * @param ownerType
	 * @param keepEtag
	 * @return
	 * @throws NotFoundException
	 */
	public V2WikiPage updateWikiPage(V2WikiPage toUpdate, Map<String, FileHandle> fileNameToFileHandleMap, String ownerId, ObjectType ownerType, boolean keepEtag) throws NotFoundException;
		
	/**
	 * Get a wiki page.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 */
	public V2WikiPage get(WikiPageKey key) throws NotFoundException;
	
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
	 * Get history (markdown, attachments etc) of a wiki page.
	 * @param key
	 */
	public List<V2WikiPage> getWikiHistory(WikiPageKey key) throws NotFoundException;

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
