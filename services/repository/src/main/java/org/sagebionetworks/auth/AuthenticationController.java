package org.sagebionetworks.auth;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.auth.services.AuthenticationService.PW_MODE;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ChangeUserPassword;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.RegistrationInfo;
import org.sagebionetworks.repo.model.auth.SecretKey;
import org.sagebionetworks.repo.model.auth.Session;
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
	
	/**
	 * This is a defines a 'back door' used for integration testing the authentication servlet.
	 * This should not be present in the production deployment.
	 * The behavior is as follows:
	 *   If passed to the user creation service, there is no confirmation email generated.
	 *   Instead the password is taken from the incoming request
	*/
	private String integrationTestUser = null;

	/**
	 * @return the integrationTestUser
	 */
	public String getIntegrationTestUser() {
		return integrationTestUser;
	}

	/**
	 * @param integrationTestUser the integrationTestUser to set
	 */
	public void setIntegrationTestUser(String integrationTestUser) {
		this.integrationTestUser = integrationTestUser;
	}

	public AuthenticationController() {
        // optional, only used for testing
        setIntegrationTestUser(StackConfiguration.getIntegrationTestUserThreeName());
	}
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/session", method = RequestMethod.POST)
	public @ResponseBody
	Session authenticate(@RequestBody NewUser credentials,
			HttpServletRequest request) throws Exception {
		return authenticationService.authenticate(credentials, true, true);
	}
	
	// this is just for testing
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/sso", method = RequestMethod.GET)
	public
	void redirectTarget(
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		PrintWriter pw = response.getWriter();
		pw.println(request.getRequestURI());
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/session", method = RequestMethod.PUT)
	public void revalidate(@RequestBody Session session) throws Exception {
		authenticationService.revalidate(session.getSessionToken());
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/session", method = RequestMethod.DELETE)
	public void deauthenticate(HttpServletRequest request) throws Exception {
		String sessionToken = request.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		authenticationService.invalidateSessionToken(sessionToken);
	}
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/user", method = RequestMethod.POST)
	public void createUser(@RequestBody NewUser user) throws Exception {
		authenticationService.createUser(user);
		
		String itu = getIntegrationTestUser();
		boolean isITU = (itu != null && user.getEmail().equals(itu));
		if (!isITU) {
			authenticationService.sendUserPasswordEmail(user.getEmail(), PW_MODE.RESET_PW);
		}
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/user", method = RequestMethod.GET)
	public @ResponseBody User getUser(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username) throws Exception {
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(username)) {
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), 
					"No user info for " + AuthorizationConstants.ANONYMOUS_USER_ID, null);
		}
		return authenticationService.getUserInfo(username).getUser();
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/userPasswordEmail", method = RequestMethod.POST)
	public void sendChangePasswordEmail(@RequestBody NewUser credential) throws Exception {
		authenticationService.authenticate(credential, false, false);
		authenticationService.sendUserPasswordEmail(credential.getEmail(), PW_MODE.RESET_PW);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/apiPasswordEmail", method = RequestMethod.POST)
	public void sendSetAPIPasswordEmail(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username) throws Exception {
		authenticationService.sendUserPasswordEmail(username, PW_MODE.SET_API_PW);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/userPassword", method = RequestMethod.POST)
	public void setPassword(@RequestBody ChangeUserPassword changeUserPassword,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username) throws Exception {
		NewUser credential = new NewUser();
		credential.setEmail(username);
		credential.setPassword(changeUserPassword.getNewPassword());
		authenticationService.changePassword(credential);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/changeEmail", method = RequestMethod.POST)
	public void changeEmail(@RequestBody RegistrationInfo registrationInfo,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String newUsername) throws Exception {
		authenticationService.updateEmail(registrationInfo, newUsername);
	}
	

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/registeringUserPassword", method = RequestMethod.POST)
	public void setRegisteringUserPassword(@RequestBody RegistrationInfo registrationInfo,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username) throws Exception {
		String registrationToken = registrationInfo.getRegistrationToken();
		String sessionToken = registrationToken.substring(AuthorizationConstants.REGISTRATION_TOKEN_PREFIX.length());
		String realUserId = authenticationService.revalidate(sessionToken);
		String realUsername = authenticationService.getUserInfo(realUserId).getIndividualGroup().getName();

		// Set the password
		NewUser credential = new NewUser();
		credential.setEmail(realUsername);
		credential.setPassword(registrationInfo.getPassword());
		authenticationService.changePassword(credential);
		authenticationService.invalidateSessionToken(sessionToken);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/secretKey", method = RequestMethod.GET)
	public @ResponseBody SecretKey newSecretKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username) throws Exception {
		SecretKey secret = new SecretKey();
		secret.setSecretKey(authenticationService.getSecretKey(username));
		return secret;
	}
	

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/secretKey", method = RequestMethod.DELETE)
	public void invalidateSecretKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username) throws Exception {
		authenticationService.deleteSecretKey(username);
	}
}


