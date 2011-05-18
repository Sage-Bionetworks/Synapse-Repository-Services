package org.sagebionetworks.repo.model;

import java.util.Collection;

import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Interface for getting user group information
 */
public interface GroupMembershipDAO {
	
	/**
	 * Get the groups that a user belongs to, INCLUDING the admin group (if a member)
	 * but excluding individual groups and the Public group (since all are members implicitly).
	 */
	Collection<String> getUserGroupNames(String userName) throws NotFoundException, DatastoreException;
}
