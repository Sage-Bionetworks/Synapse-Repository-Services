package org.sagebionetworks.auth.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.ParameterList;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.authutil.OpenIDConsumerUtils;
import org.sagebionetworks.authutil.OpenIDInfo;
import org.sagebionetworks.authutil.SendMail;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.RegistrationInfo;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.dao.AuthorizationUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AuthenticationServiceImpl implements AuthenticationService {
	
	private static Log log = LogFactory.getLog(AuthenticationServiceImpl.class);

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthenticationManager authManager;
	
	public AuthenticationServiceImpl() {}
	
	public AuthenticationServiceImpl(UserManager userManager, AuthenticationManager authManager) {
		this.userManager = userManager;
		this.authManager = authManager;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Session authenticate(NewUser credential) throws NotFoundException {
		if (credential.getEmail() == null) {
			throw new UnauthorizedException("Username may not be null");
		}
		
		// Fetch the user's session token
			if (credential.getPassword() == null) {
				throw new UnauthorizedException("Password may not be null");
			}
		Session session = authManager.authenticate(credential.getEmail(), credential.getPassword());
		
		// Only hand back the session token if ToU has been accepted
		handleTermsOfUse(credential.getEmail(), credential.getAcceptsTermsOfUse());
		return session;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String revalidate(String sessionToken) throws NotFoundException {
		Long userId = authManager.checkSessionToken(sessionToken);
		return userId.toString();
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void invalidateSessionToken(String sessionToken) {
		if (sessionToken == null) {
			throw new IllegalArgumentException("Session token may not be null");
		}
		authManager.invalidateSessionToken(sessionToken);
	}

	@Override
	public boolean hasUserAcceptedTermsOfUse(String id)
			throws NotFoundException {
		String username = userManager.getGroupName(id);
		UserInfo user = userManager.getUserInfo(username);
		return user.getUser().isAgreesToTermsOfUse();
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void createUser(NewUser user) throws UnauthorizedException {
		if (user == null || user.getEmail() == null) {
			throw new IllegalArgumentException("Required fields are missing for user creation");
		}
		try {
			userManager.createUser(user);
		} catch (DatastoreException e) {
			throw new UnauthorizedException("User '" + user.getEmail() + "' already exists", e);
		}
		
		// For integration test to confirm that a user can be created
		if (!StackConfiguration.isProductionStack()) {
			try {
				UserInfo userInfo = userManager.getUserInfo(user.getEmail());
				authManager.changePassword(userInfo.getIndividualGroup().getId(), user.getPassword());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public UserInfo getUserInfo(String username) throws NotFoundException {
		if (AuthorizationUtils.isUserAnonymous(username)) {
			throw new NotFoundException("No user info for " + AuthorizationConstants.ANONYMOUS_USER_ID);
		}
		return userManager.getUserInfo(username);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void changePassword(String username, String newPassword) throws NotFoundException {
		if (username == null) {
			throw new IllegalArgumentException("Username may not be null");
		}
		if (newPassword == null) { 			
			throw new IllegalArgumentException("Password may not be null");
		}
		
		UserInfo user = userManager.getUserInfo(username);
		authManager.changePassword(user.getIndividualGroup().getId(), newPassword);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateEmail(String oldEmail, RegistrationInfo registrationInfo) throws NotFoundException {
		// User must be logged in to make this request
		if (oldEmail == null) {
			throw new UnauthorizedException("Not authorized");
		}
		String registrationToken = registrationInfo.getRegistrationToken();
		if (registrationToken == null) { 
			throw new UnauthorizedException("Missing registration token");
		}
		
		String sessionToken = registrationToken.substring(AuthorizationConstants.CHANGE_EMAIL_TOKEN_PREFIX.length());
		String realUserId = revalidate(sessionToken);
		String realUsername = getUsername(realUserId);
		
		// Set the password
		if (registrationInfo.getPassword() != null) {
			changePassword(realUsername, registrationInfo.getPassword());
		}
		
		// Update the pre-existing user to the new email address
		UserInfo userInfo = userManager.getUserInfo(oldEmail);
		userManager.updateEmail(userInfo, realUsername);
		
		invalidateSessionToken(sessionToken);
	}
	
	@Override
	public String getSecretKey(String username) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		return authManager.getSecretKey(userInfo.getIndividualGroup().getId());
	}
	
	@Override
	public void deleteSecretKey(String username) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		authManager.changeSecretKey(userInfo.getIndividualGroup().getId());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String getSessionTokenFromUserName(String userName) throws NotFoundException {
		Session session = authManager.getSessionToken(userName);
		return session.getSessionToken();
	}
	
	@Override
	public boolean isAdmin(String username) throws NotFoundException {
		UserInfo user = userManager.getUserInfo(username);
		return user.isAdmin();
	}
	
	@Override
	public String getUsername(String principalId) throws NotFoundException {
		return userManager.getGroupName(principalId);
	}

	@Override
	public void sendUserPasswordEmail(String username, PW_MODE mode) throws NotFoundException {
		if (username == null) {
			throw new IllegalArgumentException("Username may not be null");
		}
		
		// Get the user's info and session token (which is refreshed)
		UserInfo user = userManager.getUserInfo(username);
		username = user.getIndividualGroup().getName();
		String sessionToken = authManager.authenticate(username, null).getSessionToken();

		// Send the password email with username and session token info
		NewUser mailTarget = new NewUser();
		mailTarget.setDisplayName(user.getUser().getDisplayName());
		mailTarget.setEmail(user.getIndividualGroup().getName());
		mailTarget.setFirstName(user.getUser().getFname());
		mailTarget.setLastName(user.getUser().getLname());
		
		// Don't spam emails for integration tests
		if (!StackConfiguration.isProductionStack()) {
			log.debug("Prevented " + mode + " email from being sent to " + mailTarget + " with session token " + sessionToken);
			return;
		}
		
		SendMail sendMail = new SendMail();
		switch (mode) {
			case SET_PW:
				sendMail.sendSetPasswordMail(mailTarget, sessionToken);
				break;
			case RESET_PW:
				sendMail.sendResetPasswordMail(mailTarget, sessionToken);
				break;
			case SET_API_PW:
				sendMail.sendSetAPIPasswordMail(mailTarget, sessionToken);
				break;
		}
	}
	
	/**
	 * Checks to see if the given user has accepted the terms of use
	 * 
	 * Note: Could be made into a public service sometime in the future
	 * @param acceptsTermsOfUse Will check stored data on user if set to null or false
	 */
	private void handleTermsOfUse(String username, Boolean acceptsTermsOfUse) 
			throws NotFoundException, TermsOfUseException {
		UserInfo userInfo = userManager.getUserInfo(username);
		
		// The ToU field might not be explicitly specified or false
		if (acceptsTermsOfUse == null || !acceptsTermsOfUse) {
			acceptsTermsOfUse = userInfo.getUser().isAgreesToTermsOfUse();
		}
		
		// Check for ToU acceptance
		if (!acceptsTermsOfUse) {
			throw new TermsOfUseException();
		}
		
		// If the user is accepting the terms in this request, save the time of acceptance
		if (acceptsTermsOfUse) {
			if (!userInfo.getUser().isAgreesToTermsOfUse()) {
				authManager.setTermsOfUseAcceptance(userInfo.getIndividualGroup().getId(), acceptsTermsOfUse);
			}
		}
	}
	
	@Override
	public Session authenticateViaOpenID(ParameterList parameters) throws NotFoundException, UnauthorizedException {
		// Verify that the OpenID request is valid
		OpenIDInfo openIDInfo;
		try {
			openIDInfo = OpenIDConsumerUtils.verifyResponse(parameters);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (openIDInfo == null) {
			throw new UnauthorizedException("OpenID is not valid");
		}
		
		// Dig out a ToU boolean from the request
		String toUParam = parameters.getParameterValue(OpenIDInfo.ACCEPTS_TERMS_OF_USE_PARAM_NAME);
		Boolean acceptsTermsOfUse = new Boolean(toUParam);
		
		return processOpenIDInfo(openIDInfo, acceptsTermsOfUse);
	}
	
	/**
	 * Returns the session token of the user described by the OpenID information
	 */
	protected Session processOpenIDInfo(OpenIDInfo info, Boolean acceptsTermsOfUse) throws NotFoundException {
		// Get some info about the user
		Map<String, List<String>> mappings = info.getMap();
		List<String> emails = mappings.get(OpenIDConsumerUtils.AX_EMAIL);
		List<String> fnames = mappings.get(OpenIDConsumerUtils.AX_FIRST_NAME);
		List<String> lnames = mappings.get(OpenIDConsumerUtils.AX_LAST_NAME);
		String email = (emails == null || emails.size() < 1 ? null : emails.get(0));
		String fname = (fnames == null || fnames.size() < 1 ? null : fnames.get(0));
		String lname = (lnames == null || lnames.size() < 1 ? null : lnames.get(0));

		if (email == null) {
			throw new UnauthorizedException("Unable to authenticate");
		}
		
		if (!userManager.doesPrincipalExist(email)) {
			// A new user must be created
			NewUser user = new NewUser();
			user.setEmail(email);
			user.setFirstName(fname);
			user.setLastName(lname);
			if (fname != null && lname != null) {
				user.setDisplayName(fname + " " + lname);
			}
			userManager.createUser(user);
		}
		
		// The user does not need to accept the terms of use to get a session token via OpenID
		//TODO This should not be the case
		try {
			handleTermsOfUse(email, acceptsTermsOfUse);
		} catch (UnauthorizedException e) { }
		
		// Open ID is successful
		return authManager.authenticate(email, null);
	}
}
