package org.sagebionetworks.web.client.security;

import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.user.client.rpc.AsyncCallback;

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
	public void loginUser(String username, String password, AsyncCallback<UserData> callback);
	
	/**
	 * Terminates the session of the current user
	 */
	public void logoutUser();
	
	/**
	 * Get the currently logged in user, if any.
	 * @return the current user
	 */
	public UserData getLoggedInUser();

}
