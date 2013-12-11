package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserManager {

	/**
	 * Get the User and UserGroup information for the given user name.
	 * 
	 * @param userName the name (email address) of the user of interest
	 */
	@Deprecated
	public UserInfo getUserInfo(String userName) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the User and UserGroup information for the given user ID.
	 * 
	 * @param principalId the ID of the user of interest
	 */
	public UserInfo getUserInfo(Long principalId) throws DatastoreException, NotFoundException;

	/**
	 * Find a group
	 */
	@Deprecated
	public UserGroup findGroup(String name, boolean b) throws DatastoreException;
	
	/**
	 * Creates a new user
	 * 
	 * @return The ID of the user
	 */
	public long createUser(NewUser user);
	
	/**
	 * Creates a new user and initializes some fields as specified.
	 * Must be an admin to use this
	 */
	public UserInfo createUser(UserInfo adminUserInfo, String username, UserProfile profile, DBOCredential credential) throws NotFoundException;
	
	/**
	 * Does a principal with this name exist?
	 */
	public boolean doesPrincipalExist(String name);
	
	/**
	 * Delete a principal by ID
	 * 
	 * For testing purposes only
	 */
	public void deletePrincipal(UserInfo adminUserInfo, Long principalId) throws NotFoundException;

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
