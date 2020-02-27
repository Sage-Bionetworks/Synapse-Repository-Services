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
	 * 
	 * @return the Date of consent of null if no consent was given
	 */
	public Date lookupAuthorizationConsent(Long userId, Long clientId, String scopeHash);
	
	/**
	 * Delete the record of user consent.
	 * 
	 * @param userId the ID of the user who gave their consent
	 * @param clientId the ID of the OAuth 2.0 client which was authorized
	 * @param scopeHash the hash of the scope to which the user consented
	 */
	public void deleteAuthorizationConsent(Long userId, Long clientId, String scopeHash);

}
