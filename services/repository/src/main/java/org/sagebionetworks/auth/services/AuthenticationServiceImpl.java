package org.sagebionetworks.auth.services;

import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.AliasAndType;
import org.sagebionetworks.repo.manager.oauth.OAuthManager;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.oauth.OAuthAccountCreationRequest;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.mchange.v1.lang.BooleanUtils;

public class AuthenticationServiceImpl implements AuthenticationService {


	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private OAuthManager oauthManager;
	
	@Autowired
	private MessageManager messageManager;

	@Autowired
	private OpenIDConnectManager oidcManager;

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

	@WriteTransaction
	@Override
	public void changePassword(ChangePasswordInterface request) throws NotFoundException {
		final long userId = authManager.changePassword(request);
		messageManager.sendPasswordChangeConfirmationEmail(userId);
	}

	@Override
	@WriteTransaction
	public void signTermsOfUse(String accessToken) throws NotFoundException {
		
		UserInfo userInfo = oidcManager.getUserAuthorization(accessToken); 
		if (!userInfo.getScopes().contains(OAuthScope.modify)) throw new UnauthorizedException();
		
		// Save the state of acceptance
		Boolean alreadyAcceptedTOU = authManager.hasUserAcceptedTermsOfUse(userInfo.getId());
		if (alreadyAcceptedTOU==null || !alreadyAcceptedTOU) {
			authManager.setTermsOfUseAcceptance(userInfo.getId(), true);
		}
	}
	
	@Override
	public String getSecretKey(String accessToken) throws NotFoundException {
		UserInfo userAuthorization = oidcManager.getUserAuthorization(accessToken);
		// TODO authorization check
		return authManager.getSecretKey(userAuthorization.getId());
	}
	
	@Override
	@WriteTransaction
	public void deleteSecretKey(String accessToken) throws NotFoundException {
		UserInfo userAuthorization = oidcManager.getUserAuthorization(accessToken);
		// TODO authorization check
		authManager.changeSecretKey(userAuthorization.getId());
	}
	
	@Override
	public boolean hasUserAcceptedTermsOfUse(String accessToken) throws NotFoundException {
		String userId = oidcManager.getUserId(accessToken);
		return authManager.hasUserAcceptedTermsOfUse(Long.parseLong(userId));
	}

	@Override
	public void sendPasswordResetEmail(String passwordResetUrlPrefix, String usernameOrEmail) {
		try {
			PrincipalAlias principalAlias = userManager.lookupUserByUsernameOrEmail(usernameOrEmail);
			PasswordResetSignedToken passwordRestToken = authManager.createPasswordResetToken(principalAlias.getPrincipalId());
			messageManager.sendNewPasswordResetEmail(passwordResetUrlPrefix, passwordRestToken, principalAlias);
		} catch (NotFoundException e) {
			// should not indicate that a email/user could not be found
		}
	}

	@Override
	public OAuthUrlResponse getOAuthAuthenticationUrl(OAuthUrlRequest request) {
		String url = oauthManager.getAuthorizationUrl(request.getProvider(), request.getRedirectUrl(), request.getState());
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
		PrincipalAlias emailAlias = userManager.lookupUserByUsernameOrEmail(providedInfo.getUsersVerifiedEmail());
		// Return the user's session token
		return authManager.getSessionToken(emailAlias.getPrincipalId());
	}
	
	@WriteTransaction
	public Session createAccountViaOauth(OAuthAccountCreationRequest request) {
		// Use the authentication code to lookup the user's information.
		ProvidedUserInfo providedInfo = oauthManager.validateUserWithProvider(
				request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		if(providedInfo.getUsersVerifiedEmail() == null){
			throw new IllegalArgumentException("OAuthProvider: "+request.getProvider().name()+" did not provide a user email");
		}
		// create account with the returned user info.
		NewUser newUser = new NewUser();
		newUser.setEmail(providedInfo.getUsersVerifiedEmail());
		newUser.setFirstName(providedInfo.getFirstName());
		newUser.setLastName(providedInfo.getLastName());
		newUser.setUserName(request.getUserName());
		long newPrincipalId = userManager.createUser(newUser);
		
		Session session = authManager.getSessionToken(newPrincipalId);
		return session;
	}
	
	@Override
	public PrincipalAlias bindExternalID(String accessToken, OAuthValidationRequest validationRequest) {
		UserInfo userAuthorization = oidcManager.getUserAuthorization(accessToken);
		if (AuthorizationUtils.isUserAnonymous(userAuthorization.getId())) throw new UnauthorizedException("User ID is required.");
		// TODO authorization check
		AliasAndType providersUserId = oauthManager.retrieveProvidersId(
				validationRequest.getProvider(), 
				validationRequest.getAuthenticationCode(),
				validationRequest.getRedirectUrl());
		// now bind the ID to the user account
		return userManager.bindAlias(providersUserId.getAlias(), providersUserId.getType(), userAuthorization.getId());
	}
	
	@Override
	public void unbindExternalID(String accessToken, OAuthProvider provider, String aliasName) {
		UserInfo userAuthorization = oidcManager.getUserAuthorization(accessToken);
		if (AuthorizationUtils.isUserAnonymous(userAuthorization.getId())) throw new UnauthorizedException("User ID is required.");
		// TODO authorization check
		AliasType aliasType = oauthManager.getAliasTypeForProvider(provider);
		userManager.unbindAlias(aliasName, aliasType, userAuthorization.getId());
	}

	@Override
	public LoginResponse login(LoginRequest request) {
		return authManager.login(request);
	}

	@Override
	public PrincipalAlias lookupUserForAuthentication(String alias) {
		return userManager.lookupUserByUsernameOrEmail(alias);
	}
	
}
