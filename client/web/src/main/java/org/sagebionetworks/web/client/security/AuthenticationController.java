package org.sagebionetworks.web.client.security;

import org.sagebionetworks.web.client.security.user.UserData;

public interface AuthenticationController {
	
	/**
	 * Is the user logged in?
	 * @return
	 */
	public boolean isLoggedIn();
	
	/**
	 * Login the user
	 * @param username
	 * @param password
	 * @return
	 */
	public UserData loginUser(String username, String password) throws AuthenticationException;

}
