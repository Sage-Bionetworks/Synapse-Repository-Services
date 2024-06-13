package org.sagebionetworks.repo.model.auth;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Note: These methods assume that all users have a row in the appropriate table, 
 *   presumably created by the UserGroupDAO
 */
public interface AuthenticationDAO {
	
	/**
	 * Creates a row in the Credentials table for the given principal
	 */
	void createNew(long principalId);

	/**
	 * Check to see if this user's credentials match.
	 * @return true if the credentials are correct, false otherwise.
	 */
	boolean checkUserCredentials(long principalId, String passHash);

	/**
	 * Set the time stamp at which the given user authenticated to the system
	 * @param principalId
	 * @param authTime
	 */
	void setAuthenticatedOn(long principalId, Date authTime);
	
	/**
	 * Find the time stamp when the session was validated
	 * @param principalId
	 * @return the validation time stamp or null, if there is no session
	 */
	Date getAuthenticatedOn(long principalId) ;
	
	/**
	 * Returns the salt used to hash the user's password
	 */
	byte[] getPasswordSalt(long principalId) throws NotFoundException;

	/**
	 * Returns the password hash for a user
	 * @param principalId user's Id
	 * @return password hash for user
	 */
	String getPasswordHash(long principalId);

	/**
	 * Changes a user's password
	 */
	void changePassword(long principalId, String passHash);
	
	/**
	 * Returns the user's secret key
	 */
	String getSecretKey(long principalId) throws NotFoundException;
	
	/**
	 * Generates a new secret key for the user
	 */
	void changeSecretKey(long principalId);
	
	/**
	 * Replaces the user's secret key with the specified one
	 */
	void changeSecretKey(long principalId, String secretKey);
	
	/**
	 * Returns whether the user has accepted the terms of use
	 */
	boolean hasUserAcceptedToU(long principalId) throws NotFoundException;
	
	/**
	 * Sets whether the user has accepted, rejected, or not seen the terms of use
	 */
	void setTermsOfUseAcceptance(long principalId, Boolean acceptance);
	
	/**
	 * Updates the state of 2fa for the given principal
	 * 
	 * @param principalId
	 * @param enabled
	 */
	void setTwoFactorAuthState(long principalId, boolean enabled);
	
	/**
	 * @param principalId
	 * @return True if the user has two fa enabled
	 */
	boolean isTwoFactorAuthEnabled(long principalId);
	
	/**
	 * @param principalIds
	 * @return A map containing the two factor authentication state for each of the principal ids included in the set
	 */
	Map<Long, Boolean> getTwoFactorAuthStateMap(Set<Long> principalIds);
	
	/**
	 * @param principalId
	 * @return The last modification date of the user password, if any
	 */
	Optional<Date> getPasswordModifiedOn(long principalId);
	
	/**
	 * @param principalId
	 * @return The expiration date for the user password, if any
	 */
	Optional<Date> getPasswordExpiresOn(long principalId);
	
	/**
	 * Ensure the bootstrap users have sufficient credentials to authenticate
	 */
	void bootstrapCredentials() throws NotFoundException;
}
