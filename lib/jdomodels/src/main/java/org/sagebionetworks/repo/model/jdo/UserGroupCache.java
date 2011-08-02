package org.sagebionetworks.repo.model.jdo;

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
	 */
	public Long getIdForUserGroupName(String name);
	
	/**
	 * Given an ID get the username
	 * @param id
	 * @return
	 */
	public String getUserGroupNameForId(Long id);

	/**
	 * Remove an user for the cache.
	 * @param id
	 */
	public void delete(Long id);

}
