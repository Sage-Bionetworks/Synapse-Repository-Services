package org.sagebionetworks.repo.model;

import java.util.Date;

import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Note: These methods assume that all users have a row in the appropriate table, 
 *   presumably created by the UserGroupDAO
 */
public interface AuthenticationDAO {
	
	/**
	 * Creates a row in the Credentials table for the given principal
	 */
	public void createNew(long principalId);

	/**
	 * Checks to see if the username and password hash combination are valid
	 * @return The UserID of corresponding to the credentials
	 * @throws UnauthorizedException If the username or password are incorrect
	 */
	public Long checkEmailAndPassword(String email, String passHash);
	
	/**
	 * Updates the timestamp associated with the user's session token
	 * It is the caller's responsibility to determine if the session token is still valid
	 */
	public void revalidateSessionToken(long principalId);
	
	/**
	 * Changes the user's session token to the specified string
	 * @param sessionToken If null, a random token is generated
	 *   To set the token to null, use deleteSessionToken()
	 * @return The session token that was set
	 */
	public String changeSessionToken(long principalId, String sessionToken);
	
	/** 
	 * Fetches a session token by username (email)
	 * If the token has expired, null is returned
	 * It is the caller's responsibility to make sure the token does not go into unauthorized hands
	 */
	public Session getSessionTokenIfValid(String username);

	/**
	 * For testing purposes only
	 * Allows the current time to be spoofed for testing purposes
	 */
	public Session getSessionTokenIfValid(String username, Date now);
	
	/**
	 * Nullifies the session token
	 */
	public void deleteSessionToken(String sessionToken);
	
	/**
	 * Looks for the given session token
	 * @return The principal ID of the holder
	 */
	public Long getPrincipal(String sessionToken);
	
	/**
	 * Looks for the given valid session token
	 * @return The principal ID of the holder, or null if the token is invalid
	 */
	public Long getPrincipalIfValid(String sessionToken);
	
	/**
	 * Returns the salt used to hash the user's password
	 */
	public byte[] getPasswordSalt(String username) throws NotFoundException;
	
	/**
	 * Changes a user's password
	 */
	public void changePassword(long principalId, String passHash);
	
	/**
	 * Returns the user's secret key
	 */
	public String getSecretKey(long principalId) throws NotFoundException;
	
	/**
	 * Generates a new secret key for the user
	 */
	public void changeSecretKey(long principalId);
	
	/**
	 * Replaces the user's secret key with the specified one
	 */
	public void changeSecretKey(long principalId, String secretKey);
	
	/**
	 * Returns whether the user has accepted the terms of use
	 */
	public boolean hasUserAcceptedToU(long principalId) throws NotFoundException;
	
	/**
	 * Sets whether the user has accepted, rejected, or not seen the terms of use
	 */
	public void setTermsOfUseAcceptance(long principalId, Boolean acceptance);

	/**
	 * Ensure the bootstrap users have sufficient credentials to authenticate
	 */
	public void bootstrapCredentials() throws NotFoundException;
}
