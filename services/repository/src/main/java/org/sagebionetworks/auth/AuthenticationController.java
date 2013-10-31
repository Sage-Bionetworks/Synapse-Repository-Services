package org.sagebionetworks.auth;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.ParameterList;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.auth.services.AuthenticationService.PW_MODE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ChangeUserPassword;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.RegistrationInfo;
import org.sagebionetworks.repo.model.auth.SecretKey;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
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
 * <p>Provides REST APIs for managing and obtaining the necessary credentials to access Synapse.</p>
 * <p>Synapse currently supports four modes of authentication:</p>
 * <ul>
 *   <li>username and password</li>
 *   <li>OpenID from Google</li>
 *   <li>session token</li>
 *   <li>API key</li>
 * </ul>
 * <p>
 * Only the session token or API key can be used to authenticate the user outside of the 
 * authentication services.  Authentication via a username and password, or via OpenID, will
 * allow the user to retrieve a session token and/or API key for use in other requests.  
 * </p>
 */
@ControllerInfo(displayName="Authentication Services", path="auth/v1")
@Controller
public class AuthenticationController extends BaseController {
	
	private static Log log = LogFactory.getLog(AuthenticationController.class);
	
	@Autowired
	private AuthenticationService authenticationService;
	
	/**
	 * Retrieve a session token that will be usable for 24 hours or until invalidated.
	 * The user must accept the terms of use before a session token is issued.
	 * </br>
	 * The passed request body must contain an email and password.  
	 * Other fields will be ignored.  
	 * See the <a href="${org.sagebionetworks.repo.model.auth.NewUser}">JSON schema</a> for more information.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION, method = RequestMethod.POST)
	public @ResponseBody
	Session authenticate(@RequestBody NewUser credentials) throws NotFoundException {
		return authenticationService.authenticate(credentials);
	}

	/**
	 * Refresh a session token to render it usable for another 24 hours.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION, method = RequestMethod.PUT)
	public void revalidate(@RequestBody Session session) throws NotFoundException {
		authenticationService.revalidate(session.getSessionToken());
	}

	/**
	 * Deauthenticate a session token.  This will sign out all active sessions using the session token.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION, method = RequestMethod.DELETE)
	public void deauthenticate(HttpServletRequest request) {
		String sessionToken = request.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		authenticationService.invalidateSessionToken(sessionToken);
	}

	/**
	 * Create a new user.  An email will be sent regarding how to set a password for the account.    
	 * </br>
	 * Note: The passed request body must contain an email.  
	 * First, last, and full name are recommended but not required.
	 * All other fields will be ignored.  
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_USER, method = RequestMethod.POST)
	public void createUser(@RequestBody NewUser user) throws NotFoundException {
		authenticationService.createUser(user);
		authenticationService.sendUserPasswordEmail(user.getEmail(), PW_MODE.SET_PW);
	}
	
	/**
	 * Retrieve basic information about the current authenticated user.  
	 * Information includes the user's display name, email, and whether they have accepted the terms of use.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_USER, method = RequestMethod.GET)
	public @ResponseBody
	NewUser getUser(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException {
		UserInfo userInfo = authenticationService.getUserInfo(username);
		NewUser user = new NewUser();
		user.setAcceptsTermsOfUse(userInfo.getUser().isAgreesToTermsOfUse());
		user.setDisplayName(userInfo.getUser().getDisplayName());
		user.setEmail(userInfo.getIndividualGroup().getName());
		user.setFirstName(userInfo.getUser().getFname());
		user.setLastName(userInfo.getUser().getLname());
		return user;
	}
	
	/**
	 * Request a password change email.
	 * </br>
	 * Note: The passed request body must contain an email.  
	 * Other fields will be ignored.  
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_USER_PASSWORD_EMAIL, method = RequestMethod.POST)
	public void sendChangePasswordEmail(@RequestBody NewUser credential)
			throws NotFoundException {
		authenticationService.sendUserPasswordEmail(credential.getEmail(), PW_MODE.RESET_PW);
	}
	
	/**
	 * Request a password change email via an API key.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_API_PASSWORD_EMAIL, method = RequestMethod.POST)
	public void sendSetAPIPasswordEmail(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException {
		authenticationService.sendUserPasswordEmail(username, PW_MODE.SET_API_PW);
	}
	
	/**
	 * Change the current authenticated user's password.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_USER_PASSWORD, method = RequestMethod.POST)
	public void setPassword(
			@RequestBody ChangeUserPassword changeUserPassword,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
		authenticationService.changePassword(username, changeUserPassword.getNewPassword());
	}
	
	/**
	 * Change the current authenticated user's email.  
	 * Note: this service is temporarily disabled.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_CHANGE_EMAIL, method = RequestMethod.POST)
	public void changeEmail(
			@RequestBody RegistrationInfo registrationInfo,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
		authenticationService.updateEmail(username, registrationInfo);
	}

	/**
	 * Used by password reset emails to reset a user's password.
	 * Must be used within 24 hours of sending the email.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_REGISTERING_USER_PASSWORD, method = RequestMethod.POST)
	public void setRegisteringUserPassword(
			@RequestBody RegistrationInfo registrationInfo)
			throws NotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
		String registrationToken = registrationInfo.getRegistrationToken();
		String sessionToken = registrationToken.substring(AuthorizationConstants.REGISTRATION_TOKEN_PREFIX.length());
		
		// A registering user has not had a chance to accept the terms yet
		String realUserId = authenticationService.revalidate(sessionToken, false);
		String realUsername = authenticationService.getUsername(realUserId);

		// Set the password
		authenticationService.changePassword(realUsername, registrationInfo.getPassword());
		authenticationService.invalidateSessionToken(sessionToken);
	}
	
	/**
	 * Retrieves the API key associated with the current authenticated user.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_SECRET_KEY, method = RequestMethod.GET)
	public @ResponseBody
	SecretKey newSecretKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException {
		SecretKey secret = new SecretKey();
		secret.setSecretKey(authenticationService.getSecretKey(username));
		return secret;
	}
	
	/**
	 * Invalidates the API key associated with the current authenticated user.  
	 * It is not recommended to use this service unless your key has been compromised.  
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_SECRET_KEY, method = RequestMethod.DELETE)
	public void invalidateSecretKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException {
		authenticationService.deleteSecretKey(username);
	}
	
	/**
	 * To authenticate via OpenID, this service takes all URL parameters returned by the OpenID provider (i.e. Google)
	 * along with an optional parameter to explicitly accept the terms of use (org.sagebionetworks.acceptsTermsOfUse=true).
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_OPEN_ID_CALLBACK, method = RequestMethod.POST)
	public @ResponseBody Session getSessionTokenViaOpenID(HttpServletRequest request) throws Exception {
		log.trace("Got a request: " + request.getRequestURL());
		ParameterList parameters = new ParameterList(request.getParameterMap());
		log.trace("Query params are: " + request.getQueryString());
		
		// Pass the request information to the auth service for a session token
		Session session = authenticationService.authenticateViaOpenID(parameters);
		return session;
	}
}


