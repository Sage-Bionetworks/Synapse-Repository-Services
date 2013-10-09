package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserManager {

	/**
	 * Get the User and UserGroup information for the given user name.
	 * Has the side effect of creating permissions-related objects for the
	 * groups that the user is in.
	 * 
	 */
	public UserInfo getUserInfo(String userName) throws DatastoreException, NotFoundException;
	
	/**
	 * Get a default group
	 */
	public UserGroup getDefaultUserGroup(DEFAULT_GROUPS group) throws DatastoreException;

	/**
	 * Find a group
	 */
	public UserGroup findGroup(String name, boolean b) throws DatastoreException;
	
	/**
	 * Creates a new user
	 */
	public void createUser(NewUser user);
	
	/**
	 * Creates a new user
	 * To be replaced with createUser
	 */
	@Deprecated
	public String createPrincipal(String name, boolean isIndividual) throws DatastoreException;
	
	
	/**
	 * Does a principal with this name exist?
	 */
	public boolean doesPrincipalExist(String name);
	
	/**
	 * Delete a principal by name
	 */
	public boolean deletePrincipal(String name);

	/**
	 * @param principalId
	 * @return for a group, returns the group name, for a user returns the display name in the user's profile
	 */
	public String getDisplayName(Long principalId) throws NotFoundException;
	
	/**
	 * Returns the group name
	 */
	public String getGroupName(String principalId) throws NotFoundException;

	/**
	 * To be removed soon
	 */
	@Deprecated
	public void clearCache();
	
	/**
	 * Changes the user's email
	 */
	public void updateEmail(UserInfo userInfo, String newEmail) throws DatastoreException, NotFoundException;

	/**
	 * Get all non-individual user groups, including Public.
	 */
	public Collection<UserGroup> getGroups() throws DatastoreException;

	/**
	 * Get non-individual user groups (including Public) in range
	 **/
	public List<UserGroup> getGroupsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) throws DatastoreException, UnauthorizedException;
}
