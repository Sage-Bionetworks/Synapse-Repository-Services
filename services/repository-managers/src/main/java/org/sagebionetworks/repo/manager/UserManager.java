package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserManager {
	
	/**
	 * Get the User and UserGroup information for the given user ID. Assumes a Synapse 
	 * user (terms of use for example). Right now, it doesn't look like checking the UserInfo
	 * object for TOU status is very common (three places), it might be removed, as there's 
	 * nothing forcing domain to be provided to this method other than that. 
	 * 
	 * @param principalId the ID of the user of interest
	 */
	public UserInfo getUserInfo(Long principalId) throws NotFoundException;
	
	/**
	 * Get the User and UserGroup information for the given user ID, including information 
	 * about the TOU status of this user for the given domain.
	 * 
	 * @param principalId
	 * @param domain
	 * @return
	 * @throws NotFoundException
	 */
	public UserInfo getUserInfo(Long principalId, DomainType domain) throws NotFoundException;
	
	/**o
	 * Creates a new user
	 * 
	 * @return The ID of the user
	 */
	public long createUser(NewUser user);
	
	/**
	 * Creates a new user and initializes some fields as specified.
	 * Must be an admin to use this
	 */
	public UserInfo createUser(UserInfo adminUserInfo, NewUser user, DBOCredential credential) throws NotFoundException;
		
	/**
	 * Delete a principal by ID
	 * 
	 * For testing purposes only
	 */
	public void deletePrincipal(UserInfo adminUserInfo, Long principalId) throws NotFoundException;

	/**
	 * Get all non-individual user groups, including Public.
	 */
	public Collection<UserGroup> getGroups() throws DatastoreException;

	/**
	 * Get non-individual user groups (including Public) in range
	 **/
	public List<UserGroup> getGroupsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) throws DatastoreException, UnauthorizedException;
	
	
	/**
	 * Principals can have many aliases including a username, multiple email addresses, and OpenIds.
	 * This method will look a user by any of the aliases.
	 * @param alias
	 * @return
	 */
	public PrincipalAlias lookupPrincipalByAlias(String alias);
	
	
}
