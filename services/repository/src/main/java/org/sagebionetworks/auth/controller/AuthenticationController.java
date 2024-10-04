package org.sagebionetworks.auth.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.authorize;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.auth.DeprecatedUtils;
import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationResponse;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.auth.AuthenticatedOn;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.SecretKey;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
import org.sagebionetworks.repo.model.auth.TermsOfServiceSignRequest;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.oauth.OAuthAccountCreationRequest;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.service.auth.AuthenticationService;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * <p>
 * Provides REST APIs for managing and obtaining the necessary credentials to access Synapse.
 * </p>
 * <p>
 * Authentication to Synapse services requires an access token passed in the HTTP Authorization
 * header, as per the <a href="https://tools.ietf.org/html/rfc6750#section-2.1">HTTP bearer authorization</a> standard.
 * The access token incorporates a set of 'scopes' and the documentation for each service requiring 
 * authorization lists the scopes which the access token must include in order to use that service.
 * <p/>
 * <p>
 * Synapse currently supports four modes of obtaining an access token:
 * </p>
 * <ul>
 * <li>Present username and password</li>
 * <li>Authentication via a whitelisted OAuth 2.0 provider</li>
 * <li>Authentication via a registered OAuth 2.0 client</li>
 * <li>Exchange an access token for Personal Access Token</li>
 * </ul>
 * <p>
 * The username and password are exchanged for an access token 
 * using <a href="${POST.login2}">POST /login2</a> service.
 * This method should only be used by Synapse itself.  No other 
 * application should prompt a user for their user name and password.
 * </p>
 * <p>
 * Synapse allows authentication via a white listed OAuth 2.0 provider.  Currently only Google is supported.
 * The final step is <a href="${POST.oauth2.session2}">POST /oauth2/session2</a> which returns an access token
 * which is included as a Bearer token in the Authorization header of 
 * subsequent requests as described above.  Only Synapse itself may use this service, as redirection back
 * from the OAuth2 provider is only allowed to the Synapse web portal.
 * </p>
 * <p>
 * A registered OAuth 2.0 client can use the <a href=
 * "#org.sagebionetworks.auth.controller.OpenIDConnectController">OAuth 2.0 services</a>
 * to authenticate, the final step of which is a request to the token endpoint:
 * <a href="${POST.oauth2.token}">POST /oauth2/token</a>
 * The returned access token is included as a Bearer token in the Authorization header of 
 * subsequent requests as described above.
 * </p>
 * <p>
 * A user may freely generate up to 100 Personal access tokens (PATs) with scoped access using 
 * <a href="${POST.personalAccessToken}">POST /personalAccessToken</a>. Unlike OAuth access tokens,
 * <ul>
 *     <li>PATs can be freely generated by a user alone and are not linked to any third party OAuth client and</li>
 *     <li>While ATs have a strict 24 hour lifetime, PATs are long lived, and will only expire if unused for 180 consecutive days</li>
 * </ul>
 * For these reasons, it is critical to treat PATs as sensitive credentials, like passwords. If a user creates more than 100 tokens, then
 * the least-recently used token(s) will be deleted until the user has no more than 100 tokens.
 * </p>
 * <p>
 * The returned personal access token is included as a Bearer token in the Authorization header of 
 * subsequent requests as described above.
 * </p>
 * 
 */
@ControllerInfo(displayName = "Authentication Services", path = "auth/v1")
@Controller
@RequestMapping(UrlHelpers.AUTH_PATH)
public class AuthenticationController {

	@Autowired
	private AuthenticationService authenticationService;

	/**
	 * This method only exists for backwards compatibility.
	 * Use {@link #login2(LoginRequest)}.
	 */
	@Deprecated
	@RequiredScope({})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION, method = RequestMethod.POST)
	public @ResponseBody
	Session authenticate(
			@RequestBody LoginCredentials credentials,
			UriComponentsBuilder uriComponentsBuilder)
			throws NotFoundException {
		LoginRequest request = DeprecatedUtils.createLoginRequest(credentials);
		LoginResponse loginResponseForLogin2 = authenticationService.login(request, EndpointHelper.getEndpoint(uriComponentsBuilder));
		return DeprecatedUtils.createSessionFromLogin2Response(loginResponseForLogin2);
	}
	
	/**
	 * This method only exists for backwards compatibility.
	 * Use {@link #login2(LoginRequest)}.
	 */
	@Deprecated
	@RequiredScope({})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_LOGIN, method = RequestMethod.POST)
	public @ResponseBody
	LoginResponse loginForSessionToken(@RequestBody LoginRequest request,
			UriComponentsBuilder uriComponentsBuilder) throws NotFoundException {
		return DeprecatedUtils.createLoginResponseFromLogin2Response(
				authenticationService.login(request, EndpointHelper.getEndpoint(uriComponentsBuilder))
		);
	}

	/**
	 * Retrieve an access token that will be usable for 24 hours. 
	 * The user must accept the terms of use before the access token
	 * can be used.
	 */
	@RequiredScope({})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_LOGIN_2, method = RequestMethod.POST)
	public @ResponseBody
	LoginResponse login(@RequestBody LoginRequest request,
			UriComponentsBuilder uriComponentsBuilder
			) throws NotFoundException {
		return authenticationService.login(request, EndpointHelper.getEndpoint(uriComponentsBuilder));
	}
	
	/**
	 * Retrieve the date/time the requesting user was last authenticated, either by presenting
	 * their password or by logging in with a recognized identity provider, like Google.
	 * 
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTHENTICATED_ON, method = RequestMethod.GET)
	public @ResponseBody
	AuthenticatedOn getAuthenticatedOn(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException {
		return authenticationService.getAuthenticatedOn(userId);
	}

	/**
	 * Sends an email for resetting a user's password. <br/>
	 * @param passwordResetEndpoint the Portal's url prefix for handling password resets.
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_USER_PASSWORD_RESET, method = RequestMethod.POST)
	public void sendPasswordResetEmail(
			@RequestParam(value = AuthorizationConstants.PASSWORD_RESET_PARAM, required = true) String passwordResetEndpoint,
			@RequestBody Username user){
		authenticationService.sendPasswordResetEmail(passwordResetEndpoint, user.getEmail());
	}

	/**
	 * Change the current user's password. This will invalidate existing session tokens.
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_USER_CHANGE_PASSWORD, method = RequestMethod.POST)
	public void changePassword(
			@RequestBody ChangePasswordInterface request)
			throws NotFoundException {
		authenticationService.changePassword(request);
	}

	/**
	 * Identifies a user by an access token and signs that user's terms of use
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_TERMS_OF_USE_V2, method = RequestMethod.POST)
	public void signTermsOfUse(@RequestBody TermsOfServiceSignRequest signRequest)
			throws NotFoundException {
		authenticationService.signTermsOfUse(signRequest);
	}
	
	@RequiredScope({})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_TERMS_OF_USE_V2 + "/info", method = RequestMethod.GET)
	public @ResponseBody TermsOfServiceInfo getTermsOfServiceInfo() {
		return authenticationService.getTermsOfUseInfo();
	}

	/**
	 * Retrieves the API key associated with the current authenticated user.
	 */
	@RequiredScope({modify,authorize})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_SECRET_KEY, method = RequestMethod.GET)
	public @ResponseBody
	SecretKey newSecretKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws NotFoundException {
		SecretKey secret = new SecretKey();
		secret.setSecretKey(authenticationService.getSecretKey(userId));
		return secret;
	}

	/**
	 * Invalidates the API key associated with the current authenticated user.
	 * It is not recommended to use this service unless your key has been
	 * compromised.
	 */
	@RequiredScope({modify,authorize})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_SECRET_KEY, method = RequestMethod.DELETE)
	public void invalidateSecretKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws NotFoundException {
		authenticationService.deleteSecretKey(userId);
	}

	/**
	 * The first step in OAuth authentication involves sending the user to
	 * authenticate on an OAuthProvider's web page. Use this method to get a
	 * properly formed URL to redirect the browser to an OAuthProvider's
	 * authentication page.
	 * 
	 * Upon successful authentication at the OAuthProvider's page, the provider
	 * will redirect the browser to the redirectURL. The provider will add a
	 * query parameter to the redirect URL named "code". The code parameter's
	 * value is an authorization code that must be provided to Synapse to
	 * validate a user.
	 * 
	 * Note:  The 'state' field in the request body is an arbitrary string
	 * that certain Oauth providers (including Google) will return as a request 
	 * parameter in the redirect URL. (The handling of 'state' is not prescribed by 
	 * the OAuth standard.)
	 * 
	 */
	@RequiredScope({})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_OAUTH_2_AUTH_URL, method = RequestMethod.POST)
	public @ResponseBody
	OAuthUrlResponse getRedirectURLForOAuth2Authentication(
			@RequestBody OAuthUrlRequest request) throws Exception {
		return authenticationService.getOAuthAuthenticationUrl(request);
	}

	/**
	 * After a user has been authenticated at an OAuthProvider's web page, the
	 * provider will redirect the browser to the provided redirectUrl. The
	 * provider will add a query parameter to the redirectUrl called "code" that
	 * represent the authorization code for the user. This method will use the
	 * authorization code to validate the user and fetch information about the
	 * user from the OAuthProvider. If Synapse can match the user's information
	 * to a Synapse user then an access token for the user will be returned.
	 * 
	 * Note: If Synapse cannot match the user's information to an existing
	 * Synapse user, then a status code of 404 (not found) will be returned. The
	 * user should be prompted to create an account.
	 * 
	 */
	@RequiredScope({})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_OAUTH_2_SESSION_V2, method = RequestMethod.POST)
	public @ResponseBody
	LoginResponse validateOAuthSessionAndReturnAccessToken(
			@RequestBody OAuthValidationRequest request,
			UriComponentsBuilder uriComponentsBuilder)
			throws Exception {
		return authenticationService.validateOAuthAuthenticationCodeAndLogin(request, EndpointHelper.getEndpoint(uriComponentsBuilder));
	}

	/**
	 * After a user has been authenticated at an OAuth provider's web page, the
	 * provider will redirect the browser to the provided redirectUrl. The
	 * provider will add a query parameter to the redirectUrl called "code" that
	 * represents the authorization code for the user. This method will use the
	 * authorization code to fetch the provider's ID for the user.  The provider's
	 * ID will then be bound to the user's account as a new 'alias'.  Note:  Some
	 * alias types (like ORCID) allow just one value per account.  For such aliases, 
	 * an error will be returned if the user tries to bind more than one such alias.
	 * 
	 * @param request
	 * @param userId
	 * @return
	 * @throws Exception
	 */
	@RequiredScope({})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_OAUTH_2_ALIAS, method = RequestMethod.POST)
	public @ResponseBody
	PrincipalAlias bindExternalIdToAccount(@RequestBody OAuthValidationRequest request,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws Exception {
		return authenticationService.bindExternalID(userId, request);
	}
	
	/**
	 * After a user has been authenticated at an OAuthProvider's web page, the
	 * provider will redirect the browser to the provided redirectUrl. The
	 * provider will add a query parameter to the redirectUrl called "code" that
	 * represent the authorization code for the user. This method will use the
	 * authorization code to validate the user and fetch the user's email address
	 * from the OAuthProvider. If there is no existing account using the email address
	 * from the provider then a new account will be created, the user will be authenticated,
	 * and an access token will be returned.
	 * 
	 * If the email address from the provider is already associated with an account or
	 * if the passed user name is used by another account then the request will
	 * return HTTP Status 409 Conflict.
	 * 
	 * @param request
	 * @return 
	 * @throws Exception
	 */
	@RequiredScope({})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_OAUTH_2_ACCOUNT_V2, method = RequestMethod.POST)
	public @ResponseBody
	LoginResponse createAccountViaOAuth2(@RequestBody OAuthAccountCreationRequest request,
			UriComponentsBuilder uriComponentsBuilder)
			throws Exception {
		return authenticationService.createAccountViaOauth(request, EndpointHelper.getEndpoint(uriComponentsBuilder));
	}
	
	/**
	 * Remove an alias associated with an account via the OAuth mechanism.
	 * 
	 * @param userId
	 * @param provider the OAuth provider through which the alias was associated
	 * @param alias the alias for the user given by the provider
	 * @throws Exception
	 */
	@RequiredScope({modify,authorize})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_OAUTH_2_ALIAS, method = RequestMethod.DELETE)
	public void unbindExternalAliasFromAccount(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required=true) String provider,
			@RequestParam(required=true) String alias
			) throws Exception {
		OAuthProvider providerEnum = OAuthProvider.valueOf(provider);
		authenticationService.unbindExternalID(userId, providerEnum, alias);
	}


	/**
	 * Issues a personal access token to authorize scoped access to Synapse resources. 
	 * <br/>
	 * <br/>
	 * Note:  The scope of the personal access token is the intersection of the request scope
	 * and the scope of the access token used to authorize the request and also omits `authorize` scope.
	 * That is, the returned token cannot have `authorize` scope and cannot have greater scope than
	 * the token used to authorize this request.
	 * <br/>
	 * <br/>
	 * To use the token to authorize other requests to Synapse, use the HTTP
	 * header `Authorization: Bearer &lt;token&gt;`. The token will expire if unused for 180 days.
	 * The token cannot be re-retrieved after the initial creation.
	 * @param userId
	 * @param request
	 * @param uriComponentsBuilder
	 * @return
	 */
	@RequiredScope({authorize})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_PERSONAL_ACCESS_TOKEN, method = RequestMethod.POST)
	public @ResponseBody AccessTokenGenerationResponse createPersonalAccessToken(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestHeader(value = AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME, required=false) String authorizationHeader,
			@RequestBody(required=true) AccessTokenGenerationRequest request,
			UriComponentsBuilder uriComponentsBuilder
	) {
		String accessToken = HttpAuthUtil.getBearerTokenFromAuthorizationHeader(authorizationHeader);
		return authenticationService.createPersonalAccessToken(userId, accessToken, request, EndpointHelper.getEndpoint(uriComponentsBuilder));
	}

	/**
	 * Retrieve metadata for all personal access tokens issued for the requesting user. Metadata for active and expired
	 * tokens will be returned in reverse-chronological order by creation date. Metadata for revoked tokens cannot be returned.
	 * @param userId
	 * @param nextPageToken
	 * @return
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_PERSONAL_ACCESS_TOKEN, method = RequestMethod.GET)
	public @ResponseBody
	AccessTokenRecordList getPersonalAccessTokens(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.NEXT_PAGE_TOKEN_PARAM, required=false) String nextPageToken
	) {
		return authenticationService.getPersonalAccessTokenRecords(userId, nextPageToken);
	}

	/**
	 * Retrieve metadata for a particular personal access token. Metadata for revoked tokens cannot be retrieved.
	 * @param userId
	 * @param id The unique ID of the token, which is the unique ID (the "jti" claim) contained in the JWT
	 * @return
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_PERSONAL_ACCESS_TOKEN_ID, method = RequestMethod.GET)
	public @ResponseBody
	AccessTokenRecord getPersonalAccessToken(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) Long id
	) {
		return authenticationService.getPersonalAccessTokenRecord(userId, id);
	}

	/**
	 * Revoke a personal access token. The token cannot be re-enabled after being revoked.
	 * @param userId
	 * @param id The unique ID of the token, which is the unique ID (the "jti" claim) contained in the JWT
	 */
	@RequiredScope({modify, authorize})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_PERSONAL_ACCESS_TOKEN_ID, method = RequestMethod.DELETE)
	public void revokePersonalAccessToken(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) Long id
	) {
		authenticationService.revokePersonalAccessToken(userId, id);

	}
	
	/**
	 * Revokes the access token used to perform the request. E.g Can be used to logout the current user.
	 */
	@RequiredScope({modify, authorize})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION_ACCESS_TOKEN, method = RequestMethod.DELETE)
	public void revokeAccessToken(@RequestHeader(value = AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME, required=false) String authorizationHeader) {
		String accessToken = HttpAuthUtil.getBearerTokenFromAuthorizationHeader(authorizationHeader);
		
		authenticationService.revokeSessionAccessToken(accessToken);
	}
	
	/**
	 * Revokes any access token issued for the given user. If the target user id does not match the user in the access token used to perform the request the operation
	 * is limited to admin users. 
	 * 
	 * @param userId
	 * @param targetUserId The user 
	 */
	@RequiredScope({modify, authorize})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_USER + "/{targetUserId}" + UrlHelpers.AUTH_SESSION_ACCESS_TOKEN + "/all", method = RequestMethod.DELETE)
	public void revokeAllAccessTokens(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "targetUserId") Long targetUserId) {
		
		authenticationService.revokeAllSessionAccessTokens(userId, targetUserId);
	}

}
