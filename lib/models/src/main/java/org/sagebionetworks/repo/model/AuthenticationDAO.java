package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.auth.Credential;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;


public interface AuthenticationDAO {

	/**
	 * Checks a username/password combination
	 * @return A session token if valid, otherwise null
	 */
	public Session authenticate(Credential credential) throws NotFoundException;
	
	/** 
	 * Fetches a session token by username (email)
	 * It is the caller's responsibility to make sure the token does not go into unauthorized hands
	 */
	public Session getSessionToken(String username);
	
	/**
	 * Nullifies the session token
	 */
	public void deleteSessionToken(String sessionToken);
	
	/**
	 * Looks for the given session token
	 * @return The principal ID of the holder, or null if the token is not used
	 */
	public Long getPrincipal(String sessionToken);
	
	/**
	 * Makes an entry for the given user
	 */
	public void create(String id, String passHash);
	
	/**
	 * Changes a user's password
	 */
	public void changePassword(String id, String passHash);
	
	/**
	 * Returns the user's secret key
	 */
	public String getSecretKey(String id);
	
	/**
	 * Generates a new secret key for the user
	 */
	public void changeSecretKey(String id);

}
