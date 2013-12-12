package org.sagebionetworks.auth.services;

import java.io.IOException;

import org.openid4java.message.ParameterList;
import org.sagebionetworks.authutil.OpenIDConsumerUtils;
import org.sagebionetworks.authutil.OpenIDInfo;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.OriginatingClient;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.ChangePasswordRequest;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AuthenticationServiceImpl implements AuthenticationService {

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private MessageManager messageManager;
	
	public AuthenticationServiceImpl() {}
	
	public AuthenticationServiceImpl(UserManager userManager, AuthenticationManager authManager, MessageManager messageManager) {
		this.userManager = userManager;
		this.authManager = authManager;
		this.messageManager = messageManager;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Session authenticate(LoginCredentials credential) throws NotFoundException {
		if (credential.getEmail() == null) {
			throw new UnauthorizedException("Username may not be null");
		}
		if (credential.getPassword() == null) {
			throw new UnauthorizedException("Password may not be null");
		}
		
		// Fetch the user's session token
		return authManager.authenticate(credential.getEmail(), credential.getPassword());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String revalidate(String sessionToken) throws NotFoundException {
		return revalidate(sessionToken, true);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String revalidate(String sessionToken, boolean checkToU) throws NotFoundException {
		if (sessionToken == null) {
			throw new IllegalArgumentException("Session token may not be null");
		}
		return authManager.checkSessionToken(sessionToken, checkToU).toString();
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
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void createUser(NewUser user, OriginatingClient originClient) {
		if (user == null || user.getEmail() == null) {
			throw new IllegalArgumentException("Email must be specified");
		}
		
		userManager.createUser(user);
		try {
			sendPasswordEmail(user.getEmail(), originClient);
		} catch (NotFoundException e) {
			throw new DatastoreException("Could not find user that was just created", e);
		}
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void sendPasswordEmail(String username, OriginatingClient originClient) throws NotFoundException {
		if (username == null) {
			throw new IllegalArgumentException("Username may not be null");
		}
		if (originClient == null) {
			throw new IllegalArgumentException("OriginatingClient may not be null");
		}
		
		// Get the user's info and session token (which is refreshed)
		UserInfo user = userManager.getUserInfo(username);
		username = user.getIndividualGroup().getName();
		String sessionToken = authManager.authenticate(username, null).getSessionToken();
		
		// Send the email
		messageManager.sendPasswordResetEmail(user.getIndividualGroup().getId(), originClient, sessionToken);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void changePassword(ChangePasswordRequest request) throws NotFoundException {
		if (request.getSessionToken() == null) {
			throw new IllegalArgumentException("Session token may not be null");
		}
		if (request.getPassword() == null) { 			
			throw new IllegalArgumentException("Password may not be null");
		}
		
		Long principalId = authManager.checkSessionToken(request.getSessionToken(), false);
		authManager.changePassword(principalId.toString(), request.getPassword());
		authManager.invalidateSessionToken(request.getSessionToken());
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void signTermsOfUse(Session session) throws NotFoundException {
		if (session.getSessionToken() == null) {
			throw new IllegalArgumentException("Session token may not be null");
		}
		if (session.getAcceptsTermsOfUse() == null) {
			throw new IllegalArgumentException("Terms of use acceptance may not be null");
		}
		
		Long principalId = authManager.checkSessionToken(session.getSessionToken(), false);
		UserInfo userInfo = userManager.getUserInfo(principalId);
		
		// Save the state of acceptance
		if (session.getAcceptsTermsOfUse() != userInfo.getUser().isAgreesToTermsOfUse()) {
			authManager.setTermsOfUseAcceptance(userInfo.getIndividualGroup().getId(), session.getAcceptsTermsOfUse());
		}
	}
	
	@Override
	public String getSecretKey(String username) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		return authManager.getSecretKey(userInfo.getIndividualGroup().getId());
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteSecretKey(String username) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		authManager.changeSecretKey(userInfo.getIndividualGroup().getId());
	}
	
	@Override
	public String getUsername(String principalId) throws NotFoundException {
		return userManager.getGroupName(principalId);
	}
	
	@Override
	public boolean hasUserAcceptedTermsOfUse(String username) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		return authManager.hasUserAcceptedTermsOfUse(userInfo.getIndividualGroup().getId());
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Session authenticateViaOpenID(ParameterList parameters) throws NotFoundException {
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
		
		// Dig out a createUser boolean from the request
		// Defaults to null (different from false)
		String createParam = parameters.getParameterValue(OpenIDInfo.CREATE_USER_IF_NECESSARY_PARAM_NAME);
		Boolean shouldCreateUser = createParam == null ? null : new Boolean(createParam);
		
		String originClientParam = parameters.getParameterValue(OpenIDInfo.ORIGINATING_CLIENT_PARAM_NAME);
		OriginatingClient originClient = OriginatingClient.getClientFromOriginClientParam(originClientParam);
		
		return processOpenIDInfo(openIDInfo, shouldCreateUser, originClient);
	}
	
	/**
	 * Returns the session token of the user described by the OpenID information
	 */
	protected Session processOpenIDInfo(OpenIDInfo info, boolean createUserIffNecessary,
			OriginatingClient originClient) throws NotFoundException {
		// Get some info about the user
		String email = info.getEmail();
		String fname = info.getFirstName();
		String lname = info.getLastName();
		String fullName = info.getFullName();
		if (email == null) {
			throw new UnauthorizedException("An email must be returned from the OpenID provider");
		}
		if (originClient == null) {
			throw new IllegalArgumentException("OriginatingClient may not be null");
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
				UserInfo justCreated = userManager.getUserInfo(email);
				
				// Send a welcome message
				messageManager.sendWelcomeEmail(justCreated.getIndividualGroup().getId(), originClient);
			} else {
				throw new NotFoundException(email);
			}
		}
		
		// Open ID is successful
		return authManager.authenticate(email, null);
	}
}
