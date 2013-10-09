package org.sagebionetworks.auth.services;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.SendMail;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.RegistrationInfo;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AuthenticationServiceImpl implements AuthenticationService {
	
	private Log log = LogFactory.getLog(AuthenticationServiceImpl.class);
	private static final String PORTAL_USER_NAME = StackConfiguration.getPortalUsername();

	@Autowired
	private UserManager userManager;

	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	private AuthenticationManager authManager;
	
	public AuthenticationServiceImpl() {}
	
	public AuthenticationServiceImpl(UserManager userManager, UserProfileManager userProfileManager, AuthenticationManager authManager) {
		this.userManager = userManager;
		this.userProfileManager = userProfileManager;
		this.authManager = authManager;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Session authenticate(String username, NewUser credential, boolean validatePassword, boolean validateToU) 
			throws NotFoundException {
		if (credential.getEmail() == null) {
			throw new UnauthorizedException("Username may not be null");
		}

		// Password checking is disabled for the user corresponding to the portal
		// Also, the ToU does not prevent the portal user from getting a session token
		boolean isPortalUser = PORTAL_USER_NAME.equals(username);
		
		// Fetch the user's session token, checking the password if required
		Session session;
		if (validatePassword && !isPortalUser) {
			if (credential.getPassword() == null) {
				throw new UnauthorizedException("Password may not be null");
			}
			session = authManager.authenticate(credential.getEmail(), credential.getPassword());
		} else {
			session = authManager.authenticate(credential.getEmail(), null);
		}
		
		if (validateToU) {
			// The ToU field might not be explicitly specified in the credential object
			if (credential.getAcceptsTermsOfUse() == null) {
				UserInfo userInfo = userManager.getUserInfo(credential.getEmail());
				credential.setAcceptsTermsOfUse(userInfo.getUser().isAgreesToTermsOfUse());
			}
			
			// Check for ToU acceptance
			if (!credential.getAcceptsTermsOfUse() && !isPortalUser) {
				throw new UnauthorizedException(ServiceConstants.TERMS_OF_USE_ERROR_MESSAGE);
			}
			
			// If the user is accepting the terms in this request, save the time of acceptance
			if (credential.getAcceptsTermsOfUse()) {
				UserInfo user = userManager.getUserInfo(credential.getEmail());
				if (!user.getUser().isAgreesToTermsOfUse()) {
					userProfileManager.agreeToTermsOfUse(user);
				}
			}
		}
		return session;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String revalidate(String sessionToken) throws NotFoundException {
		Long userId = authManager.checkSessionToken(sessionToken);
		if (!hasUserAcceptedTermsOfUse(userId.toString())) {
			throw new UnauthorizedException(ServiceConstants.TERMS_OF_USE_ERROR_MESSAGE);
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
	public void createUser(NewUser user) throws AuthenticationException {
		if (user == null || user.getEmail() == null) {
			throw new IllegalArgumentException("Required fields are missing for user creation");
		}
		try {
			userManager.createUser(user);
		} catch (DatastoreException e) {
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "User '" + user.getEmail() + "' already exists", e);
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
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(username)) {
			throw new NotFoundException("No user info for " + AuthorizationConstants.ANONYMOUS_USER_ID);
		}
		return userManager.getUserInfo(username);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void changePassword(String username, String newPassword) throws NotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
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
	public void updateEmail(String oldUserId, String newUserId) 
			throws DatastoreException, NotFoundException, XPathExpressionException, IOException, AuthenticationException {
		UserInfo userInfo = userManager.getUserInfo(oldUserId);
		userManager.updateEmail(userInfo, newUserId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateEmail(String oldEmail, RegistrationInfo registrationInfo) throws NotFoundException {
		
		// The mapping between usernames and user IDs is currently done on a one-to-one basis.
		// This means that changing the email associated with an ID in the UserGroup table 
		//   introduces an inconsistency between the UserGroup table and ID Generator table.
		// Until the Named ID Generator supports a one-to-many mapping, this method is disabled.   
		throw new NotFoundException("This service is currently unavailable");
		
		/*
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
		changePassword(realUsername, registrationInfo.getPassword());
		
		// Update the pre-existing user to the new email address
		UserInfo userInfo = userManager.getUserInfo(oldEmail);
		userManager.updateEmail(userInfo, realUsername);
		
		invalidateSessionToken(sessionToken);
		*/
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
}
