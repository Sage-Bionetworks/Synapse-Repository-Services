package org.sagebionetworks.repo.service.auth;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.authentication.PersonalAccessTokenManager;
import org.sagebionetworks.repo.manager.authentication.TermsOfServiceManager;
import org.sagebionetworks.repo.manager.authentication.TwoFactorAuthManager;
import org.sagebionetworks.repo.manager.oauth.AliasAndType;
import org.sagebionetworks.repo.manager.oauth.OAuthManager;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenManager;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.manager.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationResponse;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.auth.AuthenticatedOn;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
import org.sagebionetworks.repo.model.auth.TermsOfServiceRequirements;
import org.sagebionetworks.repo.model.auth.TermsOfServiceSignRequest;
import org.sagebionetworks.repo.model.auth.TermsOfServiceStatus;
import org.sagebionetworks.repo.model.auth.TotpSecret;
import org.sagebionetworks.repo.model.auth.TotpSecretActivationRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthDisableRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthLoginRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthRecoveryCodes;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthResetRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthStatus;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalOidcBinding;
import org.sagebionetworks.repo.model.oauth.OAuthAccountCreationRequest;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class AuthenticationServiceImpl implements AuthenticationService {
	
	private static final Logger LOGGER = LogManager.getLogger(AuthenticationServiceImpl.class);
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private OAuthManager oauthManager;
	
	@Autowired
	private OpenIDConnectManager oidcManager;
	
	@Autowired
	private MessageManager messageManager;

	@Autowired
	private PersonalAccessTokenManager personalAccessTokenManager;
	
	@Autowired
	private OIDCTokenManager oidcTokenManager;
	
	@Autowired
	private TwoFactorAuthManager twoFaManager;
	
	@Autowired
	private TermsOfServiceManager tosManager;
	
	@WriteTransaction
	@Override
	public void changePassword(ChangePasswordInterface request) throws NotFoundException {
		final long userId = authManager.changePassword(request);
		messageManager.sendPasswordChangeConfirmationEmail(userId);
	}

	@Override
	@WriteTransaction
	public void signTermsOfService(TermsOfServiceSignRequest signRequest) throws NotFoundException {
		ValidateArgument.required(signRequest, "The request");
		ValidateArgument.required(signRequest.getAccessToken(), "Access token contents");
		
		Long principalId = Long.parseLong(oidcManager.validateAccessToken(signRequest.getAccessToken()));
		
		// Save the state of acceptance
		tosManager.signTermsOfService(principalId, signRequest.getTermsOfServiceVersion());
	}
	
	@Override
	public TermsOfServiceInfo getTermsOfServiceInfo() {
		return tosManager.getTermsOfServiceInfo();
	}
	
	@Override
	public TermsOfServiceStatus getUserTermsOfServiceStatus(Long userId) {
		return tosManager.getUserTermsOfServiceStatus(userId);
	}
	
	@Override
	public TermsOfServiceInfo updateTermsOfServiceRequirements(Long userId, TermsOfServiceRequirements request) {
		UserInfo user = userManager.getUserInfo(userId);
		return tosManager.updateTermsOfServiceRequirements(user, request);
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
	public boolean hasUserAcceptedTermsOfService(Long userId) throws NotFoundException {
		return tosManager.hasUserAcceptedTermsOfService(userId);
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
	public LoginResponse validateOAuthAuthenticationCodeAndLogin(
			OAuthValidationRequest request, String tokenIssuer) throws NotFoundException {
		// Use the authentication code to lookup the user's information.
		ProvidedUserInfo providedInfo = oauthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		
		if (providedInfo.getSubject() == null) {
			throw new IllegalArgumentException("OAuthProvider: " + request.getProvider().name() + " did not provide the user subject");
		}
		
		// We first lookup for a potential subject already bound for the given provider
		PrincipalOidcBinding oidcBinding = userManager.lookupOidcBindingBySubject(request.getProvider(), providedInfo.getSubject()).orElseGet(() -> {
			PrincipalAlias alias = findPrincipalAlias(request.getProvider(), providedInfo)
				.orElseThrow(() -> new NotFoundException("Could not find a user matching the " + request.getProvider() + " provider information."));
			
			// Finally, we also migrate the user to the oauth provider subject (See https://sagebionetworks.jira.com/browse/PLFM-7302)
			return userManager.bindUserToOidcSubject(alias, request.getProvider(), providedInfo.getSubject());
		});
		
		Long loggedInUserId = oidcBinding.getUserId();
				
		// In https://sagebionetworks.jira.com/browse/PLFM-8198 we added the alias FK and we need to backfill	
		if (oidcBinding.getAliasId() == null) {
						
			PrincipalAlias alias = findPrincipalAlias(request.getProvider(), providedInfo).orElseThrow(() -> {
				// If an alias is not found the user deleted the associated alias and the binding is not valid anymore
				userManager.deleteOidcBinding(oidcBinding.getBindingId());
				
				LOGGER.warn("A {} OIDC binding was found for user {} but no matching alias was found (The binding has been deleted)", request.getProvider(), oidcBinding.getUserId());
				
				// The not found exception will send the user to the registration page
				return new NotFoundException("Could not find a user matching the " + request.getProvider() + " provider information.");
			});

			if (!alias.getPrincipalId().equals(oidcBinding.getUserId())) {
				// If the matched alias is different than the user id the binding is associated with, then the user might have
				// an old binding that needs to be deleted (e.g. this is the case where they logged in and removed an alias in 
				// the past and added that alias to another account)
				userManager.deleteOidcBinding(oidcBinding.getBindingId());
				
				userManager.bindUserToOidcSubject(alias, request.getProvider(), providedInfo.getSubject());
				
				loggedInUserId = alias.getPrincipalId();
				
				LOGGER.warn("A {} OIDC binding was found for user {} but the alias {} belongs to user {} (The binding has been migrated)", request.getProvider(), oidcBinding.getUserId(), alias.getAliasId(), alias.getPrincipalId());
			} else {
				// See See https://sagebionetworks.jira.com/browse/PLFM-8198, we backfill the missing alias
				userManager.setOidcBindingAlias(oidcBinding, alias);
			}
			
		}
		
		// Return the user's access token
		return authManager.loginWithNoPasswordCheck(loggedInUserId, tokenIssuer);
	}
	
	private Optional<PrincipalAlias> findPrincipalAlias(OAuthProvider provider, ProvidedUserInfo providedInfo) {
		PrincipalAlias alias = null;
		
		// For backward compatibility we also lookup the user by the provider verified email
		if (providedInfo.getUsersVerifiedEmail() != null) {
			try {
				alias = userManager.lookupUserByUsernameOrEmail(providedInfo.getUsersVerifiedEmail());
			} catch (NotFoundException e) {
				LOGGER.warn("Could not match user (Provider: {}, Sub: {}, Verified email: {}): {}", provider, providedInfo.getSubject(), providedInfo.getUsersVerifiedEmail(), e.getMessage());
			}
		}
		
		// If the provider does not give a verified email or there is no match we try using the alias if present
		if (alias == null && providedInfo.getAliasAndType() != null) {
			AliasAndType aliasType = providedInfo.getAliasAndType();
			try {
				alias = userManager.lookupUserByAliasType(aliasType.getType(), aliasType.getAlias());
			} catch (NotFoundException e) {
				LOGGER.warn("Could not match user (Provider: {}, Sub: {}, Alias: {}): {}", provider, providedInfo.getSubject(), aliasType.getAlias(), e.getMessage());
			}
		}
		
		return Optional.ofNullable(alias);
	}
	
	@WriteTransaction
	public LoginResponse createAccountViaOauth(OAuthAccountCreationRequest request, String tokenIssuer) {
		// Use the authentication code to lookup the user's information.
		ProvidedUserInfo providedInfo = oauthManager.validateUserWithProvider(
				request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		
		if (providedInfo.getUsersVerifiedEmail() == null){
			throw new IllegalArgumentException("OAuthProvider: "+request.getProvider().name()+" did not provide a user email");
		}
		
		if (providedInfo.getSubject() == null) {
			throw new IllegalArgumentException("OAuthProvider: "+request.getProvider().name()+" did not provide the user subject");
		}
		
		// create account with the returned user info.
		NewUser newUser = new NewUser();
		
		newUser.setEmail(providedInfo.getUsersVerifiedEmail());
		newUser.setFirstName(providedInfo.getFirstName());
		newUser.setLastName(providedInfo.getLastName());
		newUser.setUserName(request.getUserName());
		newUser.setOauthProvider(request.getProvider());
		newUser.setSubject(providedInfo.getSubject());
		
		long newPrincipalId = userManager.createUser(newUser);

		return authManager.loginWithNoPasswordCheck(newPrincipalId, tokenIssuer);

	}
	
	@Override
	public PrincipalAlias bindExternalID(Long userId, OAuthValidationRequest validationRequest) {
		
		if (AuthorizationUtils.isUserAnonymous(userId)) {
			throw new UnauthorizedException("User ID is required.");
		}
		
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
	public LoginResponse login(LoginRequest request, String tokenIssuer) {
		return authManager.login(request, tokenIssuer);
	}

	@Override
	public AuthenticatedOn getAuthenticatedOn(long userId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return authManager.getAuthenticatedOn(userInfo);
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

	@Override
	public TotpSecret enroll2Fa(Long userId) {
		UserInfo user = userManager.getUserInfo(userId);
		return twoFaManager.init2Fa(user);
	}

	@Override
	public TwoFactorAuthStatus enable2Fa(Long userId, TotpSecretActivationRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		twoFaManager.enable2Fa(user, request);
		return twoFaManager.get2FaStatus(user);
	}

	@Override
	public void disable2Fa(Long userId) {
		UserInfo user = userManager.getUserInfo(userId);
		twoFaManager.disable2Fa(user);
	}

	@Override
	public TwoFactorAuthStatus get2FaStatus(Long userId) {
		UserInfo user = userManager.getUserInfo(userId);
		return twoFaManager.get2FaStatus(user);
	}
	
	@Override
	public TwoFactorAuthRecoveryCodes generate2faRecoveryCodes(Long userId) {
		UserInfo user = userManager.getUserInfo(userId);
		return twoFaManager.generate2FaRecoveryCodes(user);
	}
	
	@Override
	public LoginResponse loginWith2Fa(TwoFactorAuthLoginRequest request, String issuer) {
		return authManager.loginWith2Fa(request, issuer);
	}
	
	@Override
	public void send2FaResetNotification(TwoFactorAuthResetRequest request) {
		authManager.send2FaResetNotification(request);
	}
	
	@Override
	public void disable2FaWithToken(TwoFactorAuthDisableRequest request) {
		authManager.disable2FaWithToken(request);
	}
	
	@Override
	public void revokeSessionAccessToken(String token) {
		oidcTokenManager.revokeOIDCAccessToken(token);
	}
	
	@Override
	public void revokeAllSessionAccessTokens(Long userId, Long targetUserId) {
		
		if (!userId.equals(targetUserId) && !userManager.getUserInfo(userId).isAdmin()) {
			throw new UnauthorizedException("You are not authorized to perform this operation.");
		}
				
		oidcTokenManager.revokeOIDCAccessTokens(targetUserId);
	}

}
