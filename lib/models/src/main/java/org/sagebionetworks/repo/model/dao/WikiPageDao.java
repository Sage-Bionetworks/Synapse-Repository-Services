package org.sagebionetworks.repo.model.dao;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * The abstraction of the wikipage data access object.
 * @author jmhill
 *
 */
public interface WikiPageDao {

	/**
	 * Create a new wiki page.
	 * @param page
	 * @return
	 * @throws NotFoundException 
	 */
	public WikiPage create(WikiPage toCreate, String ownerId, ObjectType ownerType) throws NotFoundException;
	
	/**
	 * Update a wiki page.
	 * @param toUpdate
	 * @return
	 * @throws NotFoundException 
	 */
	public WikiPage updateWikiPage(WikiPage toUpdate, String ownerId, ObjectType ownerType, boolean keepEtag) throws NotFoundException;
		
	/**
	 * Get a wiki page.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 */
	public WikiPage get(WikiPageKey key) throws NotFoundException;
	
	/**
	 * Delete a wiki page.
	 * @param id
	 */
	public void delete(WikiPageKey key);

	/**
	 * Get the entire tree of wiki pages for a given owner.
	 * @param parentId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	List<WikiHeader> getHeaderTree(String ownerId, ObjectType ownerType) throws DatastoreException, NotFoundException;
	
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

}
