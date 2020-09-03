package org.sagebionetworks.auth.services;

import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.authentication.PersonalAccessTokenManager;
import org.sagebionetworks.repo.manager.oauth.AliasAndType;
import org.sagebionetworks.repo.manager.oauth.OAuthManager;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationResponse;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.oauth.OAuthAccountCreationRequest;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
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
	private PersonalAccessTokenManager personalAccessTokenManager;

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
		return authManager.login(request);
	}

	@Override
	public PrincipalAlias lookupUserForAuthentication(String alias) {
		return userManager.lookupUserByUsernameOrEmail(alias);
	}

	@Override
	public AccessTokenGenerationResponse createPersonalAccessToken(Long userId, String accessToken, AccessTokenGenerationRequest request, String oauthEndpoint) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return personalAccessTokenManager.issueToken(userInfo, accessToken, request, oauthEndpoint);
	}

	@Override
	public AccessTokenRecordList getPersonalAccessTokenRecords(Long userId, String nextPageToken) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return personalAccessTokenManager.getTokenRecords(userInfo, nextPageToken);
	}

	@Override
	public AccessTokenRecord getPersonalAccessTokenRecord(Long userId, Long tokenId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return personalAccessTokenManager.getTokenRecord(userInfo, tokenId.toString());
	}

	@Override
	public void revokePersonalAccessToken(Long userId, Long tokenId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		personalAccessTokenManager.revokeToken(userInfo, tokenId.toString());
	}

}
