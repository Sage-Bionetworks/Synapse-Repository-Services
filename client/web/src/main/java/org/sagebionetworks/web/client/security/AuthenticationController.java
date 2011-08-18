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
	 * Logs in the user represented by the token
	 * @param token
	 */
	public void loginUser(String token, AsyncCallback<UserData> callback);
	
	/**
	 * Sets the current user 
	 * @param displayName
	 * @param token
	 */
	public void loginUserSSO(String token, AsyncCallback<UserData> callback);
	
	/**
	 * Terminates the session of the current user
	 */
	public void logoutUser();
	
	/**
	 * Get the currently logged in user, if any.
	 * @return the current user
	 */
	public UserData getLoggedInUser();
	
	
	/**
	 * Saves the show demo flag into a cookie
	 */
	public void saveShowDemo();
	
	public void loadShowDemo();
}
