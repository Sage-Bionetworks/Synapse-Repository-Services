package org.sagebionetworks.auth.services;

import org.sagebionetworks.authutil.SendMail;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.RegistrationInfo;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AuthenticationServiceImpl implements AuthenticationService {

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
	public Session authenticate(NewUser credential, boolean validatePassword, boolean validateToU) 
			throws NotFoundException {
		Session session;
		if (validatePassword) {
			if (credential.getPassword() == null) {
				throw new UnauthorizedException("Username or password may not be empty");
			}
			session = authManager.authenticate(credential.getEmail(), credential.getPassword());
		} else {
			session = authManager.authenticate(credential.getEmail(), null);
		}
		
		if (validateToU) {
			// Check for ToU acceptance
			if (!credential.getAcceptsTermsOfUse()) {
				throw new UnauthorizedException(ServiceConstants.TERMS_OF_USE_ERROR_MESSAGE);
			}
			
			// If the user is accepting the terms in this request, save the time of acceptance
			UserInfo user = userManager.getUserInfo(credential.getEmail());
			if (!user.getUser().isAgreesToTermsOfUse()) {
				userProfileManager.agreeToTermsOfUse(user);
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
	public void createUser(NewUser user) {
		userManager.createUser(user);
	}
	
	@Override
	public UserInfo getUserInfo(String username) throws NotFoundException {
		return userManager.getUserInfo(username);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void changePassword(NewUser credential) throws NotFoundException {
		if (credential.getEmail() == null) {
			throw new IllegalArgumentException("Username may not be null");
		}
		if (credential.getPassword() == null) { 			
			throw new IllegalArgumentException("Password may not be null");
		}
		
		UserInfo user = userManager.getUserInfo(credential.getEmail());
		authManager.changePassword(user.getIndividualGroup().getId(), credential.getPassword());
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateEmail(RegistrationInfo registrationInfo, String newEmail) throws NotFoundException {
		// User must be logged in to make this request
		if (newEmail == null) {
			throw new IllegalArgumentException("New email may not be null");
		}
		String registrationToken = registrationInfo.getRegistrationToken();
		if (registrationToken == null) { 
			throw new UnauthorizedException("Missing registration token");
		}
		
		String sessionToken = registrationToken.substring(AuthorizationConstants.CHANGE_EMAIL_TOKEN_PREFIX.length());
		String realUserId = revalidate(sessionToken);
		String realUsername = getUsername(realUserId);
		
		// Set the password
		NewUser credential = new NewUser();
		credential.setEmail(realUsername);
		credential.setPassword(registrationInfo.getPassword());
		changePassword(credential);
		
		// Update the pre-existing user to the new email address
		UserInfo userInfo = userManager.getUserInfo(realUsername);
		userManager.updateEmail(userInfo, newEmail);
		
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
		UserInfo user = userManager.getUserInfo(username);
		username = user.getIndividualGroup().getName();
		String sessionToken = authManager.getSessionToken(username).getSessionToken();
		
		Long principalId = authManager.checkSessionToken(sessionToken);
		if (principalId == null) {
			throw new UnauthorizedException("Session token (" + sessionToken + ") is invalid");
		}
		
		NewUser mailTarget = new NewUser();
		mailTarget.setDisplayName(user.getUser().getDisplayName());
		mailTarget.setEmail(user.getIndividualGroup().getName());
		mailTarget.setFirstName(user.getUser().getFname());
		mailTarget.setLastName(user.getUser().getLname());
		// now send the reset password email, filling in the user name and session token
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
