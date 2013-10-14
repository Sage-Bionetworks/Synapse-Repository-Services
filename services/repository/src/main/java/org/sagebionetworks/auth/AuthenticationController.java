package org.sagebionetworks.auth;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.servlet.http.HttpServletRequest;

import org.openid4java.message.ParameterList;
import org.sagebionetworks.StackConfiguration;
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

@ControllerInfo(displayName="Authentication Services", path="auth/v1")
@Controller
public class AuthenticationController extends BaseController {
	
	@Autowired
	private AuthenticationService authenticationService;
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION, method = RequestMethod.POST)
	public @ResponseBody
	Session authenticate(@RequestBody NewUser credentials) throws NotFoundException {
		return authenticationService.authenticate(credentials);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION, method = RequestMethod.PUT)
	public void revalidate(@RequestBody Session session) throws NotFoundException {
		authenticationService.revalidate(session.getSessionToken());
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_SESSION, method = RequestMethod.DELETE)
	public void deauthenticate(HttpServletRequest request) {
		String sessionToken = request.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		authenticationService.invalidateSessionToken(sessionToken);
	}

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AUTH_USER, method = RequestMethod.POST)
	public void createUser(@RequestBody NewUser user) throws NotFoundException {
		authenticationService.createUser(user);
		authenticationService.sendUserPasswordEmail(user.getEmail(), PW_MODE.RESET_PW);
	}
	
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
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_USER_PASSWORD_EMAIL, method = RequestMethod.POST)
	public void sendChangePasswordEmail(@RequestBody NewUser credential)
			throws NotFoundException {
		authenticationService.sendUserPasswordEmail(credential.getEmail(), PW_MODE.RESET_PW);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_API_PASSWORD_EMAIL, method = RequestMethod.POST)
	public void sendSetAPIPasswordEmail(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException {
		authenticationService.sendUserPasswordEmail(username, PW_MODE.SET_API_PW);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_USER_PASSWORD, method = RequestMethod.POST)
	public void setPassword(
			@RequestBody ChangeUserPassword changeUserPassword,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
		authenticationService.changePassword(username, changeUserPassword.getNewPassword());
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_CHANGE_EMAIL, method = RequestMethod.POST)
	public void changeEmail(
			@RequestBody RegistrationInfo registrationInfo,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
		authenticationService.updateEmail(username, registrationInfo);
	}
	

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_REGISTERING_USER_PASSWORD, method = RequestMethod.POST)
	public void setRegisteringUserPassword(
			@RequestBody RegistrationInfo registrationInfo)
			throws NotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
		String registrationToken = registrationInfo.getRegistrationToken();
		String sessionToken = registrationToken.substring(AuthorizationConstants.REGISTRATION_TOKEN_PREFIX.length());
		String realUserId = authenticationService.revalidate(sessionToken);
		String realUsername = authenticationService.getUsername(realUserId);

		// Set the password
		authenticationService.changePassword(realUsername, registrationInfo.getPassword());
		authenticationService.invalidateSessionToken(sessionToken);
	}
	
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
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.AUTH_SECRET_KEY, method = RequestMethod.DELETE)
	public void invalidateSecretKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException {
		authenticationService.deleteSecretKey(username);
	}
	

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.AUTH_OPEN_ID_CALLBACK, method = RequestMethod.POST)
	public Session getSessionTokenViaOpenID(HttpServletRequest request) throws Exception {
		ParameterList parameters = new ParameterList(request.getParameterMap());
		
		// Pass the request information to the auth service for a session token
		return authenticationService.authenticateViaOpenID(parameters);
	}
}


