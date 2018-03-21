package org.sagebionetworks.auth.services;

import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.AliasAndType;
import org.sagebionetworks.repo.manager.oauth.OAuthManager;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.ChangePasswordRequest;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class AuthenticationServiceImpl implements AuthenticationService {

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private OAuthManager oauthManager;
	
	@Autowired
	private MessageManager messageManager;

	@Override
	@WriteTransaction
	public Long revalidate(String sessionToken) throws NotFoundException {
		return revalidate(sessionToken, true);
	}
	
	@Override
	@WriteTransaction
	public Long revalidate(String sessionToken, boolean checkToU) throws NotFoundException {
		if (sessionToken == null) {
			throw new IllegalArgumentException("Session token may not be null");
		}
		return authManager.checkSessionToken(sessionToken, checkToU);
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
	public void createUser(NewUser user) {
		if (user == null || user.getEmail() == null) {
			throw new IllegalArgumentException("Email must be specified");
		}
		
		Long userid = userManager.createUser(user);
		try {
			sendPasswordEmail(userid);
		} catch (NotFoundException e) {
			throw new DatastoreException("Could not find user that was just created", e);
		}
	}
	
	@Override
	@WriteTransaction
	public void sendPasswordEmail(Long principalId) throws NotFoundException {
		if (principalId == null) {
			throw new IllegalArgumentException("PrincipalId may not be null");
		}
		
		// Get the user's session token (which is refreshed)
		String sessionToken = authManager.getSessionToken(principalId).getSessionToken();
		
		// Send the email
		messageManager.sendPasswordResetEmail(principalId, sessionToken);
	}
	
	@Override
	@WriteTransaction
	public void changePassword(ChangePasswordRequest request) throws NotFoundException {
		if (request.getSessionToken() == null) {
			throw new IllegalArgumentException("Session token may not be null");
		}
		if (request.getPassword() == null) { 			
			throw new IllegalArgumentException("Password may not be null");
		}
		
		Long principalId = authManager.checkSessionToken(request.getSessionToken(), false);
		authManager.changePassword(principalId, request.getPassword());
		authManager.invalidateSessionToken(request.getSessionToken());
	}
	
	@Override
	@WriteTransaction
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
		if (!session.getAcceptsTermsOfUse().equals(authManager.hasUserAcceptedTermsOfUse(principalId))) {
			authManager.setTermsOfUseAcceptance(userInfo.getId(), session.getAcceptsTermsOfUse());
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
	public boolean hasUserAcceptedTermsOfUse(Long userId) throws NotFoundException {
		return authManager.hasUserAcceptedTermsOfUse(userId);
	}

	@Override
	public Long getUserId(String username) throws NotFoundException {
		PrincipalAlias pa = userManager.lookupUserForAuthentication(username);
		return pa.getPrincipalId();
	}

	@Override
	public void sendPasswordEmail(String email) throws NotFoundException {
		PrincipalAlias pa = userManager.lookupUserForAuthentication(email);
		sendPasswordEmail(pa.getPrincipalId());
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
		PrincipalAlias emailAlias = userManager.lookupUserForAuthentication(providedInfo.getUsersVerifiedEmail());
		// Return the user's session token
		return authManager.getSessionToken(emailAlias.getPrincipalId());
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

	@Override
	public LoginResponse login(LoginRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getUsername(), "LoginRequest.username");
		ValidateArgument.required(request.getPassword(), "LoginRequest.password");
		try {
			// Lookup the user.
			PrincipalAlias pa = userManager.lookupUserForAuthentication(request.getUsername());

			// Fetch the user's session token
			return authManager.login(pa.getPrincipalId(), request.getPassword(), request.getAuthenticationReceipt());
		} catch (NotFoundException e) {
			// see PLFM-3914
			throw new UnauthenticatedException(UnauthenticatedException.MESSAGE_USERNAME_PASSWORD_COMBINATION_IS_INCORRECT, e);
		}
	}

	@Override
	public PrincipalAlias lookupUserForAuthentication(String alias) {
		return userManager.lookupUserForAuthentication(alias);
	}
}
