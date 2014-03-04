package org.sagebionetworks.auth.services;

import java.io.IOException;
import java.util.LinkedList;

import org.openid4java.message.ParameterList;
import org.sagebionetworks.authutil.OpenIDConsumerUtils;
import org.sagebionetworks.authutil.OpenIDInfo;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.ChangePasswordRequest;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
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
	private UserProfileManager userProfileManager;
	
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
	public Session authenticate(LoginCredentials credential, DomainType domain) throws NotFoundException {
		if (credential.getEmail() == null) {
			throw new UnauthorizedException("Username may not be null");
		}
		if (credential.getPassword() == null) {
			throw new UnauthorizedException("Password may not be null");
		}
		if (domain == null) {
			throw new UnauthorizedException("Domain must be declared");
		}
		// Lookup the user.
		PrincipalAlias pa = lookupUserForAuthenication(credential.getEmail());
		if(pa == null) throw new NotFoundException("Did not find a user with alias: "+credential.getEmail());;
		
		// Fetch the user's session token
		return authManager.authenticate(pa.getPrincipalId(), credential.getPassword(), domain);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Long revalidate(String sessionToken, DomainType domain) throws NotFoundException {
		return revalidate(sessionToken, domain, true);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Long revalidate(String sessionToken, DomainType domain, boolean checkToU) throws NotFoundException {
		if (sessionToken == null) {
			throw new IllegalArgumentException("Session token may not be null");
		}
		return authManager.checkSessionToken(sessionToken, domain, checkToU);
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
	public void createUser(NewUser user, DomainType originClient) {
		if (user == null || user.getEmail() == null) {
			throw new IllegalArgumentException("Email must be specified");
		}
		
		Long userid = userManager.createUser(user);
		try {
			sendPasswordEmail(userid, originClient);
		} catch (NotFoundException e) {
			throw new DatastoreException("Could not find user that was just created", e);
		}
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void sendPasswordEmail(Long principalId, DomainType domain) throws NotFoundException {
		if (principalId == null) {
			throw new IllegalArgumentException("PrincipalId may not be null");
		}
		if (domain == null) {
			throw new IllegalArgumentException("OriginatingClient may not be null");
		}
		
		// Get the user's session token (which is refreshed)
		String sessionToken = authManager.authenticate(principalId, null, domain).getSessionToken();
		
		// Send the email
		messageManager.sendPasswordResetEmail(principalId, domain, sessionToken);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void changePassword(ChangePasswordRequest request, DomainType domain) throws NotFoundException {
		if (request.getSessionToken() == null) {
			throw new IllegalArgumentException("Session token may not be null");
		}
		if (request.getPassword() == null) { 			
			throw new IllegalArgumentException("Password may not be null");
		}
		
		Long principalId = authManager.checkSessionToken(request.getSessionToken(), domain, false);
		authManager.changePassword(principalId, request.getPassword());
		authManager.invalidateSessionToken(request.getSessionToken());
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void signTermsOfUse(Session session, DomainType domain) throws NotFoundException {
		if (session.getSessionToken() == null) {
			throw new IllegalArgumentException("Session token may not be null");
		}
		if (session.getAcceptsTermsOfUse() == null) {
			throw new IllegalArgumentException("Terms of use acceptance may not be null");
		}
		
		Long principalId = authManager.checkSessionToken(session.getSessionToken(), domain, false);
		UserInfo userInfo = userManager.getUserInfo(principalId);
		
		// Save the state of acceptance
		if (!session.getAcceptsTermsOfUse().equals(authManager.hasUserAcceptedTermsOfUse(principalId, domain))) {
			authManager.setTermsOfUseAcceptance(userInfo.getId(), domain, session.getAcceptsTermsOfUse());
		}
	}
	
	@Override
	public String getSecretKey(Long principalId) throws NotFoundException {
		return authManager.getSecretKey(principalId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteSecretKey(Long principalId) throws NotFoundException {
		authManager.changeSecretKey(principalId);
	}

	
	@Override
	public boolean hasUserAcceptedTermsOfUse(Long userId, DomainType domain) throws NotFoundException {
		return authManager.hasUserAcceptedTermsOfUse(userId, domain);
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
			
		String originClientParam = parameters.getParameterValue(OpenIDInfo.ORIGINATING_CLIENT_PARAM_NAME);
		DomainType originClient = DomainType.valueOf(originClientParam);
		
		return processOpenIDInfo(openIDInfo, originClient);
	}
	
	/**
	 * Returns the session token of the user described by the OpenID information
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Session processOpenIDInfo(OpenIDInfo info, DomainType domain) throws NotFoundException {
		// Get some info about the user
		String email = info.getEmail();
		if (email == null) {
			throw new UnauthorizedException("An email must be returned from the OpenID provider");
		}
		if (domain == null) {
			throw new IllegalArgumentException("DomainType may not be null");
		}
		
		// First try to lookup the user by their OpenId
		boolean isOpenIDBound = false;
		PrincipalAlias alias = lookupUserForAuthenication(info.getIdentifier());
		if(alias == null){
			// Try to lookup the user by their email if we fail to look them up by OpenId
			alias = lookupUserForAuthenication(email);
		}else{
			// This open ID is already bound to this user.
			isOpenIDBound = true;
		}
		if(alias == null){
			throw new NotFoundException("Failed to find a user with OpenId: "+info.getIdentifier());
		}
		// Open ID is successful
		Session sesion = authManager.authenticate(alias.getPrincipalId(), null, domain);
		
		/**
		 * Binding the OpenID here is temporary and should be removed when PLFM-2437 is resolved.
		 */
		if(!isOpenIDBound){
			// Get the current UserProfile and added it
			UserInfo userInfo = userManager.getUserInfo(alias.getPrincipalId());
			UserProfile profile = userProfileManager.getUserProfile(userInfo, alias.getPrincipalId().toString());
			if(profile.getOpenIds() == null){
				profile.setOpenIds(new LinkedList<String>());
			}
			profile.getOpenIds().add(info.getIdentifier());
			// Update the user's profile.
			userProfileManager.updateUserProfile(userInfo, profile);
		}
		
		return sesion;
	}

	@Override
	public PrincipalAlias lookupUserForAuthenication(String alias) {
		// Lookup the user
		PrincipalAlias pa = userManager.lookupPrincipalByAlias(alias);
		if(pa == null) return null;
		if(AliasType.TEAM_NAME.equals(pa.getType())) throw new UnauthorizedException("Cannot authenticate as team. Only users can authenticate");
		return pa;
	}

	@Override
	public Long getUserId(String username) throws NotFoundException {
		PrincipalAlias pa = lookupUserForAuthenication(username);
		if(pa == null) throw new NotFoundException("Did not find a user with alias: "+username);
		return pa.getPrincipalId();
	}

	@Override
	public void sendPasswordEmail(String email, DomainType originClient) throws NotFoundException {
		PrincipalAlias pa = lookupUserForAuthenication(email);
		if(pa == null) throw new NotFoundException("Did not find a user with alias: "+email);
		sendPasswordEmail(pa.getPrincipalId(), originClient);
		
	}
}
