package org.sagebionetworks.auth;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.servlet.http.HttpServletRequest;

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
			HttpServletRequest request) throws NotFoundException {
		return authenticationService.authenticate(credentials, true, true);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/session", method = RequestMethod.PUT)
	public void revalidate(@RequestBody Session session) throws NotFoundException {
		authenticationService.revalidate(session.getSessionToken());
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/session", method = RequestMethod.DELETE)
	public void deauthenticate(HttpServletRequest request) {
		String sessionToken = request.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		authenticationService.invalidateSessionToken(sessionToken);
	}

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/user", method = RequestMethod.POST)
	public void createUser(@RequestBody NewUser user) throws NotFoundException {
		authenticationService.createUser(user);
		
		// Don't send a password email for integration testing
		String itu = getIntegrationTestUser();
		boolean isITU = (itu != null && user.getEmail().equals(itu));
		if (!isITU) {
			authenticationService.sendUserPasswordEmail(user.getEmail(), PW_MODE.RESET_PW);
		}
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/user", method = RequestMethod.GET)
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
	@RequestMapping(value = "/userPasswordEmail", method = RequestMethod.POST)
	public void sendChangePasswordEmail(@RequestBody NewUser credential)
			throws NotFoundException {
		authenticationService.sendUserPasswordEmail(credential.getEmail(), PW_MODE.RESET_PW);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/apiPasswordEmail", method = RequestMethod.POST)
	public void sendSetAPIPasswordEmail(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException {
		authenticationService.sendUserPasswordEmail(username, PW_MODE.SET_API_PW);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/userPassword", method = RequestMethod.POST)
	public void setPassword(
			@RequestBody ChangeUserPassword changeUserPassword,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
		authenticationService.changePassword(username, changeUserPassword.getNewPassword());
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/changeEmail", method = RequestMethod.POST)
	public void changeEmail(
			@RequestBody RegistrationInfo registrationInfo,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String newUsername)
			throws NotFoundException {
		authenticationService.updateEmail(registrationInfo, newUsername);
	}
	

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/registeringUserPassword", method = RequestMethod.POST)
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
	@RequestMapping(value = "/secretKey", method = RequestMethod.GET)
	public @ResponseBody
	SecretKey newSecretKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException {
		SecretKey secret = new SecretKey();
		secret.setSecretKey(authenticationService.getSecretKey(username));
		return secret;
	}
	

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/secretKey", method = RequestMethod.DELETE)
	public void invalidateSecretKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String username)
			throws NotFoundException {
		authenticationService.deleteSecretKey(username);
	}
}


