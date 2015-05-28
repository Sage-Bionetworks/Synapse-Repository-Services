package org.sagebionetworks.auth;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.ParameterList;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.auth.ChangePasswordRequest;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.SecretKey;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.BaseController;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
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
 * <li>OpenID from Google</li>
 * <li>session token</li>
 * <li>API key</li>
 * </ul>
 * <p>
 * Only the session token or API key can be used to authenticate the user
 * outside of the authentication services. Authentication via a username and
 * password, or via OpenID, will allow the user to retrieve a session token
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
	 * Retrieve a session token that will be usable for 24 hours or until
	 * invalidated. The user must accept the terms of use before a session token
	 * is issued.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION, method = RequestMethod.POST)
	public @ResponseBody
	Session authenticate(
			@RequestBody LoginCredentials credentials,
			@RequestParam(value = AuthorizationConstants.DOMAIN_PARAM, required = false) String client)
			throws NotFoundException {
		DomainType domain = DomainTypeUtils.valueOf(client);
		return authenticationService.authenticate(credentials, domain);
	}

	/**
	 * Refresh a session token to render it usable for another 24 hours.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION, method = RequestMethod.PUT)
	public void revalidate(
			@RequestBody Session session,
			@RequestParam(value = AuthorizationConstants.DOMAIN_PARAM, required = false) String client)
			throws NotFoundException {
		DomainType domain = DomainTypeUtils.valueOf(client);
		authenticationService.revalidate(session.getSessionToken(), domain);
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
			@RequestBody NewUser user,
			@RequestParam(value = AuthorizationConstants.DOMAIN_PARAM, required = false) String client) {
		DomainType domain = DomainTypeUtils.valueOf(client);
		authenticationService.createUser(user, domain);
	}

	/**
	 * Sends an email for setting a user's password. <br/>
	 * The query parameter <code>domain</code> may be appended to this URI. If absent or set to "synapse", the service
	 * will send email specific to the Synapse application;
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_USER_PASSWORD_EMAIL, method = RequestMethod.POST)
	public void sendPasswordEmail(
			@RequestBody Username user,
			@RequestParam(value = AuthorizationConstants.DOMAIN_PARAM, required = false) String client)
			throws NotFoundException {
		DomainType domain = DomainTypeUtils.valueOf(client);
		authenticationService.sendPasswordEmail(user.getEmail(), domain);
	}

	/**
	 * Change the current authenticated user's password.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_USER_PASSWORD, method = RequestMethod.POST)
	public void changePassword(
			@RequestBody ChangePasswordRequest request,
			@RequestParam(value = AuthorizationConstants.DOMAIN_PARAM, required = false) String client)
			throws NotFoundException {
		DomainType domain = DomainTypeUtils.valueOf(client);
		authenticationService.changePassword(request, domain);
	}

	/**
	 * Identifies a user by a session token and signs that user's terms of use
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_TERMS_OF_USE, method = RequestMethod.POST)
	public void signTermsOfUse(
			@RequestBody Session session,
			@RequestParam(value = AuthorizationConstants.DOMAIN_PARAM, required = false) String client)
			throws NotFoundException {
		DomainType domain = DomainTypeUtils.valueOf(client);
		authenticationService.signTermsOfUse(session, domain);
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
	 * To authenticate via OpenID, this service takes all URL parameters
	 * returned by the OpenID provider (i.e. Google) along with an optional
	 * parameter to explicitly accept the terms of use
	 * (org.sagebionetworks.acceptsTermsOfUse=true) and an optional parameter to
	 * create a user account if the OpenID is not registered in Synapse
	 * (org.sagebionetworks.createUserIfNecessary=true). If
	 * org.sagebionetworks.createUserIfNecessary is not set to true, and if the
	 * email address returned by the OpenID provider is not registered, then the
	 * service returns a 404.
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_OPEN_ID_CALLBACK, method = RequestMethod.POST)
	public @ResponseBody
	Session getSessionTokenViaOpenID(HttpServletRequest request)
			throws Exception {
		log.trace("Got a request: " + request.getRequestURL());
		ParameterList parameters = new ParameterList(request.getParameterMap());
		log.trace("Query params are: " + request.getQueryString());

		// Pass the request information to the auth service for a session token
		Session session = authenticationService
				.authenticateViaOpenID(parameters);
		return session;
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
		return authenticationService.validateOAuthAuthenticationCode(request);
	}

}
