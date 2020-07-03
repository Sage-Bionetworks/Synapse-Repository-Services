package org.sagebionetworks.repo.model.auth;

import java.util.Date;

public interface OAuthDao {
	
	/**
	 * Record that the given user granted consent to the given OAuth client to
	 * access the given scope/claims on the given date
	 * 
	 * @param userId the ID of the user who gave their consent
	 * @param clientId the ID of the OAuth 2.0 client which was authorized
	 * @param scopeHash the hash of the scope to which the user consented
	 * @param date the date consent was given
	 */
	public void saveAuthorizationConsent(Long userId, Long clientId, String scopeHash, Date date);
	
	/**
	 * 
	 * @param userId the ID of the user who gave their consent
	 * @param clientId the ID of the OAuth 2.0 client which was authorized
	 * @param scopeHash the hash of the scope to which the user consented
	 * @param notBefore the earliest time stamo for which the consent is valid
	 * 
	 * @return true iff consent was given on or later than the given date
	 */
	public boolean lookupAuthorizationConsent(Long userId, Long clientId, String scopeHash, Date notBefore);
	
	/**
	 * Delete the record of user consent.
	 * 
	 * @param userId the ID of the user who gave their consent
	 * @param clientId the ID of the OAuth 2.0 client which was authorized
	 * @param scopeHash the hash of the scope to which the user consented
	 */
	public void deleteAuthorizationConsent(Long userId, Long clientId, String scopeHash);

	/**
	 * Delete all records of user consent for a specific client.
	 *
	 * @param userId the ID of the user who gave their consent
	 * @param clientId the ID of the OAuth 2.0 client which was authorized
	 */
	public void deleteAuthorizationConsentForClient(Long userId, Long clientId);

}
