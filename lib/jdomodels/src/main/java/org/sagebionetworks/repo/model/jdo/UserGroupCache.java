package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Cache Usernames to IDs
 * @author jmhill
 *
 */
public interface UserGroupCache {
	
	/**
	 * Given a username get the ID.
	 * @param name
	 * @return
	 * @throws NotFoundException 
	 */
	public Long getIdForUserGroupName(String name) throws NotFoundException;
	
	/**
	 * Given an ID get the username
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 */
	public String getUserGroupNameForId(Long id) throws NotFoundException;

	/**
	 * Remove an user for the cache.
	 * @param id
	 */
	public void delete(Long id);

}
