package org.sagebionetworks.auth.services;

import java.io.IOException;

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
	public String revalidate(String sessionToken) 
			throws NotFoundException, UnauthorizedException, TermsOfUseException {
		return revalidate(sessionToken, true);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String revalidate(String sessionToken, boolean checkToU) 
			throws NotFoundException, UnauthorizedException, TermsOfUseException {
		Long userId;
		try {
			userId = authManager.checkSessionToken(sessionToken);
		} catch (TermsOfUseException e) {
			if (checkToU) {
				throw e;
			}
			userId = authManager.getPrincipalId(sessionToken);
		}
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
		
		SendMail sendMail = new SendMail();
		switch (mode) {
			case SET_PW:
				sendMail.sendSetPasswordMail(mailTarget, AuthorizationConstants.REGISTRATION_TOKEN_PREFIX + sessionToken);
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
		// Defaults to false
		String toUParam = parameters.getParameterValue(OpenIDInfo.ACCEPTS_TERMS_OF_USE_PARAM_NAME);
		boolean acceptsTermsOfUse = new Boolean(toUParam);
		
		// Dig out a createUser boolean from the request
		// Defaults to false
		String createParam = parameters.getParameterValue(OpenIDInfo.CREATE_USER_IF_NECESSARY_PARAM_NAME);
		boolean shouldCreateUser = new Boolean(createParam);
		
		return processOpenIDInfo(openIDInfo, acceptsTermsOfUse, shouldCreateUser);
	}
	
	/**
	 * Returns the session token of the user described by the OpenID information
	 */
	protected Session processOpenIDInfo(OpenIDInfo info, Boolean acceptsTermsOfUse, Boolean createUserIffNecessary) throws NotFoundException {
		// Get some info about the user
		String email = info.getEmail();
		String fname = info.getFirstName();
		String lname = info.getLastName();
		String fullName = info.getFullName();
		if (email == null) {
			throw new UnauthorizedException("An email must be returned from the OpenID provider");
		}
		
		if (!userManager.doesPrincipalExist(email)) {
			if (createUserIffNecessary) {
				// A new user must be created
				NewUser user = new NewUser();
				user.setEmail(email);
				user.setFirstName(fname);
				user.setLastName(lname);
				user.setDisplayName(fullName);
				userManager.createUser(user);
				
				// Send the user a welcoming message
				SendMail sendMail = new SendMail();
				sendMail.sendWelcomeMail(user);
			} else {
				throw new NotFoundException(email);
			}
		}
		
		// The user does not need to accept the terms of use to get a session token via OpenID
		//TODO This should not be the case
		try {
			handleTermsOfUse(email, acceptsTermsOfUse);
		} catch (TermsOfUseException e) { }
		
		// Open ID is successful
		return authManager.authenticate(email, null);
	}
}
