package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserManager {

	/**
	 * Get the User and UserGroup information for the given user name.
	 * Has the side effect of creating permissions-related objects for the
	 * groups that the user is in.
	 * 
	 */
	public UserInfo getUserInfo(String userName) throws DatastoreException, NotFoundException;
	
	// for testing
	public void setUserDAO(UserDAO userDAO);
	
	/**
	 * Get a default group.
	 * @param group
	 * @return
	 * @throws DatastoreException
	 */
	public UserGroup getDefaultUserGroup(DEFAULT_GROUPS group) throws DatastoreException;
	
	/**
	 * Delete a user.
	 * @param id
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void deleteUser(String id) throws DatastoreException, NotFoundException;
	

	/**
	 * Find a group.
	 * @param name
	 * @param b
	 * @return
	 * @throws DatastoreException 
	 */
	public UserGroup findGroup(String name, boolean b) throws DatastoreException;
	
	/**
	 * Create a user
	 * @param toCreate
	 * @return
	 * @throws DatastoreException 
	 */
	public String createPrincipal(String name, boolean isIndividual) throws DatastoreException;
	
	/**
	 * Does a principal with this name exist?
	 * @param name
	 * @return
	 */
	public boolean doesPrincipalExist(String name);
	
	/**
	 * Delete a principal by name
	 * @param name
	 * @return
	 */
	public boolean deletePrincipal(String name);

	/**
	 * @param principalId
	 * @return for a group, returns the group name, for a user returns the display name in the user's profile
	 */
	public String getDisplayName(Long principalId) throws NotFoundException, DatastoreException;

	public void updateEmail(UserInfo userInfo, String newEmail) throws DatastoreException, NotFoundException, IOException, AuthenticationException, XPathExpressionException;
	
	public void clearCache();

	/**
	 * Get all non-individual user groups, including Public.
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Collection<UserGroup> getGroups() throws DatastoreException;

	/**
	 * get non-individual user groups (including Public) in range
	 * 
	 **/
	public List<UserGroup> getGroupsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) throws DatastoreException, UnauthorizedException;
}
