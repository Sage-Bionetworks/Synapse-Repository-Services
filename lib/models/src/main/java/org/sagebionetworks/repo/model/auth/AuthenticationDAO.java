package org.sagebionetworks.repo.model.auth;

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
	 * Check to see if this user's credentials match.
	 * @return true if the credentials are correct, false otherwise.
	 */
	public boolean checkUserCredentials(long principalId, String passHash);

	/**
	 * Updates the timestamp associated with the user's session token it needed.
	 * Unconditionally updating the timestamp of a session token was cuasing users to be
	 * locked out of their accounts (see PLFM-3206).  Now we only update the timestamp
	 * if it is past its half-life.
	 * 
	 * It is the caller's responsibility to determine if the session token is still valid
	 * @return true if the timstamp was reset. Returns false if an update was not needed.
	 */
	public boolean revalidateSessionTokenIfNeeded(long principalId);
	
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
	public Session getSessionTokenIfValid(long principalId);

	/**
	 * For testing purposes only
	 * Allows the current time to be spoofed for testing purposes
	 */
	public Session getSessionTokenIfValid(long userId, Date now);
	
	/**
	 * Find the time stamp when the session was validated
	 * @param principalId
	 * @return the validation time stamp or null, if there is no session
	 */
	public Date getAuthenticatedOn(long principalId) ;
	
	/**
	 * Nullifies the session token
	 */
	public void deleteSessionToken(String sessionToken);

	/**
	 * Nullifies the session token for a user. This is idempotent.
	 * @param principalId id of the user for which the session token will be nullified.
	 */
	public void deleteSessionToken(long principalId);

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
	public byte[] getPasswordSalt(long principalId) throws NotFoundException;

	/**
	 * Returns the password hash for a user
	 * @param principalId user's Id
	 * @return password hash for user
	 */
	public String getPasswordHash(long principalId);

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
