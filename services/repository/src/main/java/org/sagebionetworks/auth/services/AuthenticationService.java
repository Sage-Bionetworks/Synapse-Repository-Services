package org.sagebionetworks.auth.services;

import org.sagebionetworks.repo.model.auth.AccessToken;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationResponse;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.auth.AuthenticatedOn;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.TotpSecret;
import org.sagebionetworks.repo.model.auth.TotpSecretActivationRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthLoginRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthStatus;
import org.sagebionetworks.repo.model.oauth.OAuthAccountCreationRequest;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;


/**
 * Abstraction for the handling authentication
 */
public interface AuthenticationService {
	
	/**
	 * Changes the password of the user
	 */
	public void changePassword(ChangePasswordInterface request) throws NotFoundException;
	
	/**
	 * Identifies a user via access token and signs that user's terms of use
	 */
	public void signTermsOfUse(AccessToken accessToken) throws NotFoundException;
	
	/**
	 * Gets the current secret key of the user
	 */
	public String getSecretKey(Long principalId) throws NotFoundException;
	
	/** 
	 * Invalidates the user's secret key
	 */
	public void deleteSecretKey(Long principalId) throws NotFoundException;

	/**
	 * Principals can have many aliases including a username, multiple email addresses, and OpenIds.
	 * This method will look a user by any of the aliases.
	 * @param alias
	 * @return Null if the user does not exist.
	 * @throws {@link UnauthorizedException} If the alias belongs to a team.
	 */
	public PrincipalAlias lookupUserForAuthentication(String alias);
	
	/**
	 * Has the user accepted the terms of use?
	 */
	public boolean hasUserAcceptedTermsOfUse(Long userId) throws NotFoundException;

	/**
	 * Sends a password reset email to the user identified by the given alias (username or email)
	 * 
	 * @param passwordResetUrlPrefix The url prefix for the verification callback in the portal
	 * @param usernameOrEmail An alias identifier for the user, might be a username or email
	 */
	public void sendPasswordResetEmail(String passwordResetUrlPrefix, String usernameOrEmail);

	public OAuthUrlResponse getOAuthAuthenticationUrl(OAuthUrlRequest request);

	public LoginResponse validateOAuthAuthenticationCodeAndLogin(
			OAuthValidationRequest request, String tokenIssuer) throws NotFoundException;
	
	public LoginResponse createAccountViaOauth(OAuthAccountCreationRequest request, String tokenIssuer) throws NotFoundException;

	public PrincipalAlias bindExternalID(Long userId, OAuthValidationRequest validationRequest);

	void unbindExternalID(Long userId, OAuthProvider provider, String aliasName);

	/**
	 * Authenticates username and password combination
	 * User can use an authentication receipt from previous login to skip extra security checks
	 * 
	 * @return a LoginResponse if username/password is valid
	 * @throws org.sagebionetworks.repo.model.UnauthenticatedException If the credentials are incorrect
	 */
	public LoginResponse login(LoginRequest request, String tokenIssuer);
	
	/**
	 * Get the date/time when the user was authenticated
	 * @param userId
	 * @return
	 */
	public AuthenticatedOn getAuthenticatedOn(long userId);

	/**
	 * Creates a scoped personal access token for the requesting user.
	 * @param userId
	 * @param accessToken
	 * @param request
	 * @param oauthEndpoint
	 * @return
	 */
	public AccessTokenGenerationResponse createPersonalAccessToken(Long userId, String accessToken, AccessTokenGenerationRequest request, String oauthEndpoint);

	/**
	 * Retrieves the list of issued personal access token records (both active and expired tokens) for the requesting user.
	 * @param userId
	 * @param nextPageToken
	 * @return
	 */
	public AccessTokenRecordList getPersonalAccessTokenRecords(Long userId, String nextPageToken);

	/**
	 * Retrieves the record for an individual personal access tokens.
	 * @param userId
	 * @param tokenId
	 * @return
	 */
	public AccessTokenRecord getPersonalAccessTokenRecord(Long userId, Long tokenId);

	/**
	 * Revokes a personal access token
	 * @param userId
	 * @param tokenId
	 */
	public void revokePersonalAccessToken(Long userId, Long tokenId);
	
	/**
	 * Enroll the user into 2FA. Generates a new inactive secret.
	 * 
	 * @param userId
	 * @return A new totp secret
	 */
	TotpSecret enroll2Fa(Long userId);
	
	/**
	 * Activates the secret specified in the request for 2FA
	 * 
	 * @param userId
	 * @param request
	 * @return The 2FA status
	 */
	TwoFactorAuthStatus enable2Fa(Long userId, TotpSecretActivationRequest request);

	/**
	 * Fetch the 2FA status for the user
	 * 
	 * @param userId
	 * @return The 2FA status
	 */
	TwoFactorAuthStatus get2FaStatus(Long userId);
	
	/**
	 * Disables 2FA for the user
	 * 
	 * @param userId
	 * @return The 2FA status
	 */
	void disable2Fa(Long userId);
	
	/**
	 * Authenticates the user through 2FA
	 * 
	 * @param request
	 * @param issuer
	 * @return
	 * @throws org.sagebionetworks.repo.model.UnauthenticatedException If the token and/or code are invalid
	 */
	LoginResponse loginWith2Fa(TwoFactorAuthLoginRequest request, String issuer);

}
