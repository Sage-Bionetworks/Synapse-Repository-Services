package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.auth.Session;

/**
 * Note: These methods assume that all users have a row in the appropriate table, 
 *   presumably created by the UserGroupDAO
 */
public interface AuthenticationDAO {

	/**
	 * Checks to see if the username and password hash combination are valid
	 * @return The UserID of corresponding to the credentials
	 * @throws UnauthorizedException If the username or password are incorrect
	 */
	public Long checkEmailAndPassword(String email, String passHash) throws UnauthorizedException;
	
	/**
	 * Updates the timestamp associated with the user's session token
	 * It is the caller's responsibility to determine if the session token is still valid
	 */
	public void revalidateSessionToken(String principalId);
	
	/**
	 * Changes the user's session token to the specified string
	 * @param sessionToken If null, a random token is generated
	 *   To set the token to null, use deleteSessionToken()
	 * @return The session token that was set
	 */
	public String changeSessionToken(String principalId, String sessionToken);
	
	/** 
	 * Fetches a session token by username (email)
	 * If the token has expired, null is returned
	 * It is the caller's responsibility to make sure the token does not go into unauthorized hands
	 */
	public Session getSessionTokenIfValid(String username);
	
	/**
	 * Nullifies the session token
	 */
	public void deleteSessionToken(String sessionToken);
	
	/**
	 * Looks for the given session token
	 * @return The principal ID of the holder, or null if the token is invalid
	 */
	public Long getPrincipalIfValid(String sessionToken);
	
	/**
	 * Returns the salt used to hash the user's password
	 */
	public byte[] getPasswordSalt(String username);
	
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
	
	/**
	 * Replaces the user's secret key with the specified one
	 * This method should only be used by the CrowdMigratorService
	 */
	@Deprecated
	public void changeSecretKey(String id, String secretKey);

}
