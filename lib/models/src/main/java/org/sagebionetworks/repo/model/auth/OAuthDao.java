package org.sagebionetworks.repo.model.auth;

public interface OAuthDao {
	
	/**
	 * 
	 * @param userId the ID of the user who gave their consent
	 * @param clientId the ID of the OAuth 2.0 client which was authorized
	 * @param scope the list of oauth scopes to which the user consented
	 * @param claims the claims to which the user consented
	 */
	public void saveAuthorizationConsent(Long userId, String clientId, String scope, String claims);
	
	/**
	 * 
	 * @param userId the ID of the user who gave their consent
	 * @param clientId the ID of the OAuth 2.0 client which was authorized
	 * @param scope the list of oauth scopes to which the user consented
	 * @param claims the claims to which the user consented
	 * 
	 * @return
	 */
	public boolean lookupAuthorizationConsent(Long userId, String clientId, String scope, String claims);

}
