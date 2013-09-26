package org.sagebionetworks.auth.services;

import org.sagebionetworks.authutil.SendMail;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.Credential;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AuthenticationServiceImpl implements AuthenticationService {

	@Autowired
	UserManager userManager;

	@Autowired
	UserProfileManager userProfileManager;
	
	@Autowired
	AuthenticationManager authManager;
	
	public AuthenticationServiceImpl(UserManager userManager, UserProfileManager userProfileManager, AuthenticationManager authManager) {
		this.userManager = userManager;
		this.userProfileManager = userProfileManager;
		this.authManager = authManager;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Session authenticate(Credential credential, boolean validatePassword, boolean validateToU) 
			throws NotFoundException {
		Session session = authManager.authenticate(credential, validatePassword);
		
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
	public String revalidateSessionToken(String sessionToken) {
		Long userId = authManager.checkSessionToken(sessionToken);
		if (userId == null) {
			throw new UnauthorizedException("The session token " + sessionToken + " is not valid");
		}
		return userId.toString();
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
	public void invalidateSessionToken(String sessionToken) {
		authManager.invalidateSessionToken(sessionToken);
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
	public void changePassword(Credential credential) throws NotFoundException {
		UserInfo user = userManager.getUserInfo(credential.getEmail());
		authManager.changePassword(user.getIndividualGroup().getId(), credential.getPassword());
	}
	
	@Override
	public void updateEmail(String oldEmail, String newEmail) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(oldEmail);
		userManager.updateEmail(userInfo, newEmail);
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
	public String getSessionTokenFromUserName(String userName) {
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
	public void sendUserPasswordEmail(String email, PW_MODE mode, String sessionToken) throws NotFoundException {
		Long principalId = authManager.checkSessionToken(sessionToken);
		if (principalId == null) {
			throw new UnauthorizedException("Session token (" + sessionToken + ") is invalid");
		}
		
		UserInfo user = userManager.getUserInfo(email);
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
