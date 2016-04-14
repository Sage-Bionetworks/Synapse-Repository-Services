package org.sagebionetworks.auth.services;

import java.io.IOException;

import org.openid4java.message.ParameterList;
import org.sagebionetworks.authutil.OpenIDConsumerUtils;
import org.sagebionetworks.authutil.OpenIDInfo;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.AliasAndType;
import org.sagebionetworks.repo.manager.oauth.OAuthManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.ChangePasswordRequest;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class AuthenticationServiceImpl implements AuthenticationService {

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
	@Autowired
	private OAuthManager oauthManager;
	
	@Autowired
	private MessageManager messageManager;
	
	public AuthenticationServiceImpl() {}
	
	public AuthenticationServiceImpl(
			UserManager userManager, 
			AuthenticationManager authManager, 
			MessageManager messageManager, 
			OAuthManager oauthManager) {
		this.userManager = userManager;
		this.authManager = authManager;
		this.messageManager = messageManager;
		this.oauthManager = oauthManager;
	}

	@Override
	@WriteTransaction
	public Session authenticate(LoginCredentials credential, DomainType domain) throws NotFoundException {
		if (credential.getEmail() == null) {
			throw new UnauthenticatedException("Username may not be null");
		}
		if (credential.getPassword() == null) {
			throw new UnauthenticatedException("Password may not be null");
		}
		if (domain == null) {
			throw new UnauthenticatedException("Domain must be declared");
		}
		// Lookup the user.
		PrincipalAlias pa = lookupUserForAuthentication(credential.getEmail());
		if(pa == null) throw new NotFoundException("Did not find a user with alias: "+credential.getEmail());;
		
		// Fetch the user's session token
		return authManager.authenticate(pa.getPrincipalId(), credential.getPassword(), domain);
	}

	@Override
	@WriteTransaction
	public Long revalidate(String sessionToken, DomainType domain) throws NotFoundException {
		return revalidate(sessionToken, domain, true);
	}
	
	@Override
	@WriteTransaction
	public Long revalidate(String sessionToken, DomainType domain, boolean checkToU) throws NotFoundException {
		if (sessionToken == null) {
			throw new IllegalArgumentException("Session token may not be null");
		}
		return authManager.checkSessionToken(sessionToken, domain, checkToU);
	}

	@Override
	@WriteTransaction
	public void invalidateSessionToken(String sessionToken) {
		if (sessionToken == null) {
			throw new IllegalArgumentException("Session token may not be null");
		}
		authManager.invalidateSessionToken(sessionToken);
	}
	
	@Override
	@WriteTransaction
	public void createUser(NewUser user, DomainType domain) {
		if (user == null || user.getEmail() == null) {
			throw new IllegalArgumentException("Email must be specified");
		}
		
		Long userid = userManager.createUser(user);
		try {
			sendPasswordEmail(userid, domain);
		} catch (NotFoundException e) {
			throw new DatastoreException("Could not find user that was just created", e);
		}
	}
	
	@Override
	@WriteTransaction
	public void sendPasswordEmail(Long principalId, DomainType domain) throws NotFoundException {
		if (principalId == null) {
			throw new IllegalArgumentException("PrincipalId may not be null");
		}
		if (domain == null) {
			throw new IllegalArgumentException("OriginatingClient may not be null");
		}
		
		// Get the user's session token (which is refreshed)
		String sessionToken = authManager.getSessionToken(principalId, domain).getSessionToken();
		
		// Send the email
		messageManager.sendPasswordResetEmail(principalId, domain, sessionToken);
	}
	
	@Override
	@WriteTransaction
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
	@WriteTransaction
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
	@WriteTransaction
	public void deleteSecretKey(Long principalId) throws NotFoundException {
		authManager.changeSecretKey(principalId);
	}

	
	@Override
	public boolean hasUserAcceptedTermsOfUse(Long userId, DomainType domain) throws NotFoundException {
		return authManager.hasUserAcceptedTermsOfUse(userId, domain);
	}
	
	@Override
	@WriteTransaction
	public Session authenticateViaOpenID(ParameterList parameters) throws NotFoundException {
		// Verify that the OpenID request is valid
		OpenIDInfo openIDInfo;
		try {
			openIDInfo = OpenIDConsumerUtils.verifyResponse(parameters);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (openIDInfo == null) {
			throw new UnauthenticatedException("OpenID is not valid");
		}
			
		String domainParam = parameters.getParameterValue(OpenIDInfo.ORIGINATING_CLIENT_PARAM_NAME);
		DomainType domain = DomainType.valueOf(domainParam);
		
		return processOpenIDInfo(openIDInfo, domain);
	}
	
	/**
	 * Returns the session token of the user described by the OpenID information
	 */
	@WriteTransaction
	public Session processOpenIDInfo(OpenIDInfo info, DomainType domain) throws NotFoundException {
		// Get some info about the user
		String email = info.getEmail();
		if (email == null) {
			throw new UnauthenticatedException("An email must be returned from the OpenID provider");
		}
		if (domain == null) {
			throw new IllegalArgumentException("DomainType may not be null");
		}
		
		// First try to lookup the user by their OpenId
		boolean isOpenIDBound = false;
		PrincipalAlias alias = lookupUserForAuthentication(info.getIdentifier());
		if(alias == null){
			// Try to lookup the user by their email if we fail to look them up by OpenId
			alias = lookupUserForAuthentication(email);
		}else{
			// This open ID is already bound to this user.
			isOpenIDBound = true;
		}
		if(alias == null){
			throw new NotFoundException("Failed to find a user with OpenId: "+info.getIdentifier());
		}
		// Open ID is successful
		Session sesion = authManager.getSessionToken(alias.getPrincipalId(), domain);
		
		/**
		 * Binding the OpenID here is temporary and should be removed when PLFM-2437 is resolved.
		 */
		if(!isOpenIDBound){
			// Create the alias
			PrincipalAlias openIdAlias = new PrincipalAlias();
			openIdAlias.setType(AliasType.USER_OPEN_ID);
			openIdAlias.setAlias(info.getIdentifier());
			openIdAlias.setPrincipalId(alias.getPrincipalId());
			principalAliasDAO.bindAliasToPrincipal(openIdAlias);
		}
		
		return sesion;
	}

	@Override
	public PrincipalAlias lookupUserForAuthentication(String alias) {
		// Lookup the user
		PrincipalAlias pa = userManager.lookupPrincipalByAlias(alias);
		if(pa == null) return null;
		if(AliasType.TEAM_NAME.equals(pa.getType())) throw new UnauthenticatedException("Cannot authenticate as team. Only users can authenticate");
		return pa;
	}

	@Override
	public Long getUserId(String username) throws NotFoundException {
		PrincipalAlias pa = lookupUserForAuthentication(username);
		if(pa == null) throw new NotFoundException("Did not find a user with alias: "+username);
		return pa.getPrincipalId();
	}

	@Override
	public void sendPasswordEmail(String email, DomainType domain) throws NotFoundException {
		PrincipalAlias pa = lookupUserForAuthentication(email);
		if(pa == null) throw new NotFoundException("Did not find a user with alias: "+email);
		sendPasswordEmail(pa.getPrincipalId(), domain);
		
	}

	@Override
	public OAuthUrlResponse getOAuthAuthenticationUrl(OAuthUrlRequest request) {
		String url = oauthManager.getAuthorizationUrl(request.getProvider(), request.getRedirectUrl());
		OAuthUrlResponse response = new OAuthUrlResponse();
		response.setAuthorizationUrl(url);
		return response;
	}

	@Override
	public Session validateOAuthAuthenticationCodeAndLogin(
			OAuthValidationRequest request) throws NotFoundException {
		// Use the authentication code to lookup the user's information.
		ProvidedUserInfo providedInfo = oauthManager.validateUserWithProvider(
				request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		if(providedInfo.getUsersVerifiedEmail() == null){
			throw new IllegalArgumentException("OAuthProvider: "+request.getProvider().name()+" did not provide a user email");
		}
		// This is the ID of the user within the provider's system.
		PrincipalAlias emailAlias = userManager.lookupPrincipalByAlias(providedInfo.getUsersVerifiedEmail());
		if(emailAlias == null){
			// Let the caller know we did not find the user
			throw new NotFoundException(providedInfo.getUsersVerifiedEmail());
		}
		// Return the user's session token
		return authManager.getSessionToken(emailAlias.getPrincipalId(), DomainType.SYNAPSE);
	}
	
	@Override
	public PrincipalAlias bindExternalID(Long userId, OAuthValidationRequest validationRequest) {
		if (AuthorizationUtils.isUserAnonymous(userId)) throw new UnauthorizedException("User ID is required.");
		AliasAndType providersUserId = oauthManager.retrieveProvidersId(
				validationRequest.getProvider(), 
				validationRequest.getAuthenticationCode(),
				validationRequest.getRedirectUrl());
		// now bind the ID to the user account
		return userManager.bindAlias(providersUserId.getAlias(), providersUserId.getType(), userId);
	}
	
	@Override
	public void unbindExternalID(Long userId, OAuthProvider provider, String aliasName) {
		if (AuthorizationUtils.isUserAnonymous(userId)) throw new UnauthorizedException("User ID is required.");
		AliasType aliasType = oauthManager.getAliasTypeForProvider(provider);
		userManager.unbindAlias(aliasName, aliasType, userId);
	}
}
