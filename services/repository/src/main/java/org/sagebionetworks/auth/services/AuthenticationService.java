package org.sagebionetworks.auth.services;

import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.AccessToken;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationResponse;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.Session;
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
	 * Revalidates a session token and checks if the user has accepted the terms of use.
	 * @return The principalId of the user holding the token
	 * @throws UnauthorizedException If the token has expired or is otherwise not valid.
	 * @throws TermsOfUseException If the user has not accepted the terms of use.
	 */
	public Long revalidate(String sessionToken) throws NotFoundException;
	
	/**
	 * Revalidates a session token
	 * See {@link #revalidate(String)}
	 * @param checkToU Should the check fail if the user has not accepted the terms of use?
	 */
	public Long revalidate(String sessionToken, boolean checkToU) throws NotFoundException;
	
	/**
	 * Invalidates a session token
	 */
	public void invalidateSessionToken(String sessionToken);

	/**
	 * Changes the password of the user
	 * Also invalidates the user's session token
	 */
	public void changePassword(ChangePasswordInterface request) throws NotFoundException;
	
	@Deprecated
	/**
	 * Identifies a user via session token and signs that user's terms of use
	 */
	public void signTermsOfUseSession(Session session) throws NotFoundException;
	
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

	@Deprecated
	public Session validateOAuthAuthenticationCodeAndLoginForSession(
			OAuthValidationRequest request) throws NotFoundException;
	
	public LoginResponse validateOAuthAuthenticationCodeAndLogin(
			OAuthValidationRequest request, String tokenIssuer) throws NotFoundException;
	
	@Deprecated
	public Session createAccountViaOauthForSession(OAuthAccountCreationRequest request) throws NotFoundException;

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
	public LoginResponse loginForSession(LoginRequest request);

	/**
	 * Authenticates username and password combination
	 * User can use an authentication receipt from previous login to skip extra security checks
	 * 
	 * @return a LoginResponse if username/password is valid
	 * @throws org.sagebionetworks.repo.model.UnauthenticatedException If the credentials are incorrect
	 */
	public LoginResponse login(LoginRequest request, String tokenIssuer);

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

}
