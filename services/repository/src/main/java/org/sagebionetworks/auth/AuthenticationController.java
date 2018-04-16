package org.sagebionetworks.auth;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.auth.ChangePasswordRequest;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.SecretKey;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.BaseController;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>
 * Provides REST APIs for managing and obtaining the necessary credentials to
 * access Synapse.
 * </p>
 * <p>
 * Synapse currently supports four modes of authentication:
 * </p>
 * <ul>
 * <li>username and password</li>
 * <li>session token</li>
 * <li>API key</li>
 * </ul>
 * <p>
 * Only the session token or API key can be used to authenticate the user
 * outside of the authentication services. Authentication via a username and
 * password will allow the user to retrieve a session token
 * and/or API key for use in other requests.
 * </p>
 */
@ControllerInfo(displayName = "Authentication Services", path = "auth/v1")
@Controller
@RequestMapping(UrlHelpers.AUTH_PATH)
public class AuthenticationController extends BaseController {

	private static Log log = LogFactory.getLog(AuthenticationController.class);

	@Autowired
	private AuthenticationService authenticationService;

	/**
	 * This method only exists for backwards compatibility.
	 * Use {@link #login(LoginRequest)}.
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION, method = RequestMethod.POST)
	public @ResponseBody
	Session authenticate(
			@RequestBody LoginCredentials credentials)
			throws NotFoundException {
		LoginRequest request = DeprecatedUtils.createLoginRequest(credentials);
		LoginResponse loginResponse =  authenticationService.login(request);
		return DeprecatedUtils.createSession(loginResponse);
	}

	/**
	 * Retrieve a session token that will be usable for 24 hours or until
	 * invalidated. The user must accept the terms of use before a session token
	 * is issued.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_LOGIN, method = RequestMethod.POST)
	public @ResponseBody
	LoginResponse login(@RequestBody LoginRequest request) throws NotFoundException {
		return authenticationService.login(request);
	}

	/**
	 * Refresh a session token to render it usable for another 24 hours.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION, method = RequestMethod.PUT)
	public void revalidate(
			@RequestBody Session session)
			throws NotFoundException {
		authenticationService.revalidate(session.getSessionToken());
	}

	/**
	 * Deauthenticate a session token. This will sign out all active sessions
	 * using the session token.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION, method = RequestMethod.DELETE)
	public void deauthenticate(HttpServletRequest request) {
		String sessionToken = request
				.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		authenticationService.invalidateSessionToken(sessionToken);
	}

	/**
	 * Create a new user. An email will be sent regarding how to set a password for the account. <br/>
	 * The query parameter <code>domain</code> may be appended to this URI. If absent or set to "synapse", the service
	 * will send email specific to the Synapse application; <br/>
	 * Note: The passed request body must contain an email. First, last, and full name are recommended but not required.
	 * All other fields will be ignored.
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_USER, method = RequestMethod.POST)
	public void createUser(
			@RequestBody NewUser user) {
		authenticationService.createUser(user);
	}

	/**
	 * Sends an email for setting a user's password. <br/>
	 * The query parameter <code>domain</code> may be appended to this URI. If absent or set to "synapse", the service
	 * will send email specific to the Synapse application;
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_USER_PASSWORD_EMAIL, method = RequestMethod.POST)
	public void sendPasswordEmail(
			@RequestBody Username user)
			throws NotFoundException {
		authenticationService.sendPasswordEmail(user.getEmail());
	}

	/**
	 * Change the current authenticated user's password.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_USER_PASSWORD, method = RequestMethod.POST)
	public void changePassword(
			@RequestBody ChangePasswordRequest request)
			throws NotFoundException {
		authenticationService.changePassword(request);
	}

	/**
	 * Identifies a user by a session token and signs that user's terms of use
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_TERMS_OF_USE, method = RequestMethod.POST)
	public void signTermsOfUse(
			@RequestBody Session session)
			throws NotFoundException {
		authenticationService.signTermsOfUse(session);
	}

	/**
	 * Retrieves the API key associated with the current authenticated user.
	 */
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
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_OAUTH_2_AUTH_URL, method = RequestMethod.POST)
	public @ResponseBody
	OAuthUrlResponse getSessionTokenViaOAuth2(
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
	 * to a Synapse user then a session token for the user will be returned.
	 * 
	 * Note: If Synapse cannot match the user's information to an existing
	 * Synapse user, then a status code of 404 (not found) will be returned. The
	 * user should be prompted to create an account.
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_OAUTH_2_SESSION, method = RequestMethod.POST)
	public @ResponseBody
	Session validateOAuthSession(@RequestBody OAuthValidationRequest request)
			throws Exception {
		return authenticationService.validateOAuthAuthenticationCodeAndLogin(request);
	}

	/**
	 * After a user has been authenticated at an OAuthProvider's web page, the
	 * provider will redirect the browser to the provided redirectUrl. The
	 * provider will add a query parameter to the redirectUrl called "code" that
	 * represent the authorization code for the user. This method will use the
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
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_OAUTH_2_ALIAS, method = RequestMethod.POST)
	public @ResponseBody
	PrincipalAlias bindExternalIdToAccount(@RequestBody OAuthValidationRequest request,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws Exception {
		return authenticationService.bindExternalID(userId, request);
	}
	
	/**
	 * Remove an alias associated with an account via the OAuth mechanism.
	 * 
	 * @param userId
	 * @param provider the OAuth provider through which the alias was associated
	 * @param alias the alias for the user given by the provider
	 * @throws Exception
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_OAUTH_2_ALIAS, method = RequestMethod.DELETE)
	public @ResponseBody
	void unbindExternalAliasFromAccount(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required=true) String provider,
			@RequestParam(required=true) String alias
			) throws Exception {
		OAuthProvider providerEnum = OAuthProvider.valueOf(provider);
		authenticationService.unbindExternalID(userId, providerEnum, alias);
	}

}
