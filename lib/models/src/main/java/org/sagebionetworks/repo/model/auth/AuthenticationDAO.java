package org.sagebionetworks.repo.model.auth;

import java.util.Date;

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
	 * Set the time stamp at which the given user authenticated to the system
	 * @param principalId
	 * @param authTime
	 */
	public void setAuthenticatedOn(long principalId, Date authTime);
	
	/**
	 * Find the time stamp when the session was validated
	 * @param principalId
	 * @return the validation time stamp or null, if there is no session
	 */
	public Date getAuthenticatedOn(long principalId) ;
	
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
