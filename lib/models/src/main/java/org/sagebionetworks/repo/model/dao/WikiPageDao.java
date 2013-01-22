package org.sagebionetworks.repo.model.dao;

import java.util.List;

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
	 */
	public WikiPage create(WikiPage toCreate);
	
	/**
	 * Update a wiki page.
	 * @param toUpdate
	 * @return
	 * @throws NotFoundException 
	 */
	public WikiPage updateWikiPage(WikiPage toUpdate, boolean keepEtag) throws NotFoundException;
		
	/**
	 * Get a wiki page.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 */
	public WikiPage get(String id) throws NotFoundException;
	
	/**
	 * Delete a wiki page.
	 * @param id
	 */
	public void delete(String id);

	/**
	 * Get the children of a given wiki page.
	 * @param parentId
	 * @return
	 */
	List<WikiHeader> getChildrenHeaders(String parentId);

}
