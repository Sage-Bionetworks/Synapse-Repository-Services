package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.auth.Credential;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;


public interface AuthenticationManager {

	/**
	 * Authenticates a user/password combination, returning a session token if valid
	 */
	public Session authenticate(Credential credential, boolean validatePassword)
			throws NotFoundException;
	
	/**
	 * Looks for the given session token
	 * @return The principal ID of the holder, or null if the token is not used
	 */
	public Long checkSessionToken(String sessionToken);
	
	/**
	 * Deletes the given session token, thereby invalidating it
	 */
	public void invalidateSessionToken(String sessionToken);
	
	/**
	 * Changes a user's password
	 */
	public void changePassword(String id, String passHash);
	
	/** 
	 * Gets the user's secret key
	 */
	public String getSecretKey(String id);
	
	/**
	 * Replaces the user's secret key with a new one
	 */
	public void changeSecretKey(String id);
	
	/**
	 * Returns the user's session token
	 */
	public Session getSessionToken(String username);
	
}
