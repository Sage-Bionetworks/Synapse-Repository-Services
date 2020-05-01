package org.sagebionetworks.auth.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.AliasAndType;
import org.sagebionetworks.repo.manager.oauth.OAuthManager;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.ChangePasswordWithToken;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
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
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceImplTest {

	
	@Mock
	private UserManager mockUserManager;
	@Mock
	private AuthenticationManager mockAuthenticationManager;
	@Mock
	private MessageManager mockMessageManager;
	@Mock
	private OAuthManager mockOAuthManager;
	@Mock
	private OpenIDConnectManager mockOidcManager;
	@InjectMocks
	private AuthenticationServiceImpl service;
	
	private LoginCredentials credential;
	private UserInfo userInfo;
	private static String username = "AuthServiceUser";
	private static String password = "NeverUse_thisPassword";
	private static long userId = 123456789L;
	private static String sessionToken = "Some session token";
	
	String alias, aliasEmail;
	PrincipalAlias principalAlias, principalEmailAlias;
	
	LoginRequest loginRequest;
	LoginResponse loginResponse;
	
	@BeforeEach
	public void setUp() throws Exception {
		credential = new LoginCredentials();
		credential.setEmail(username);
		credential.setPassword(password);
		
		userInfo = new UserInfo(false);
		userInfo.setId(userId);
		

		alias = "alias";
		principalAlias = new PrincipalAlias();
		principalAlias.setPrincipalId(userId);
		principalAlias.setAlias(alias);
		principalAlias.setAliasId(2222L);
		principalAlias.setType(AliasType.USER_NAME);
		
		aliasEmail = "user_alternative@test.com";
		principalEmailAlias = new PrincipalAlias();
		principalEmailAlias.setPrincipalId(userId);
		principalEmailAlias.setAliasId(3333L);
		principalEmailAlias.setAlias(aliasEmail);
		principalEmailAlias.setType(AliasType.USER_EMAIL);
		
		loginRequest = new LoginRequest();
		loginRequest.setAuthenticationReceipt("receipt");
		loginRequest.setUsername("username");
		loginRequest.setPassword("password");
		
		loginResponse = new LoginResponse();
		loginResponse.setAcceptsTermsOfUse(true);
		loginResponse.setAuthenticationReceipt("newReceipt");
		loginResponse.setSessionToken("sessionToken");
		
	}
	
	@Test
	public void testRevalidateToU() throws Exception {
		// A boolean flag should let us get past this call
		service.revalidate(sessionToken, false);

		when(mockAuthenticationManager.checkSessionToken(eq(sessionToken), eq(true)))
		.thenThrow(new TermsOfUseException());

		// But it should default to true
		assertThrows(TermsOfUseException.class, ()->service.revalidate(sessionToken));
	}
	
	@Test
	public void testGetOAuthAuthenticationUrl(){
		OAuthUrlRequest request = new OAuthUrlRequest();
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl("http://domain.com");
		request.setState("some state");
		String authUrl = "https://auth.org";
		when(mockOAuthManager.getAuthorizationUrl(request.getProvider(), request.getRedirectUrl(), request.getState())).thenReturn(authUrl);
		OAuthUrlResponse response = service.getOAuthAuthenticationUrl(request);
		assertNotNull(response);
		assertEquals(authUrl, response.getAuthorizationUrl());
	}
	
	
	@Test
	public void testValidateOAuthAuthenticationCode() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl("https://domain.com");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setUsersVerifiedEmail("first.last@domain.com");
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		PrincipalAlias alias = new PrincipalAlias();
		long userId = 3456L;
		alias.setPrincipalId(userId);
		when(mockUserManager.lookupUserByUsernameOrEmail(info.getUsersVerifiedEmail())).thenReturn(alias);
		Session session = new Session();
		session.setSessionToken("token");
		when(mockAuthenticationManager.getSessionToken(userId)).thenReturn(session);
		//call under test
		Session result = service.validateOAuthAuthenticationCodeAndLogin(request);
		assertEquals(session, result);
	}
	
	@Test
	public void testCreateAccountViaOauth() throws NotFoundException{
		OAuthAccountCreationRequest request = new OAuthAccountCreationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl("https://domain.com");
		request.setUserName("uname");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setUsersVerifiedEmail("first.last@domain.com");
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		
		when(mockUserManager.createUser((NewUser)any())).thenReturn(userId);
		Session session = new Session();
		session.setSessionToken("token");
		when(mockAuthenticationManager.getSessionToken(userId)).thenReturn(session);
		
		//call under test
		Session result = service.createAccountViaOauth(request);
		assertEquals(session, result);
	}
	
	@Test
	public void testValidateOAuthAuthenticationCodeEmailNull() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl("https://domain.com");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setUsersVerifiedEmail(null);
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		PrincipalAlias alias = new PrincipalAlias();
		long userId = 3456L;
		alias.setPrincipalId(userId);
		Session session = new Session();
		session.setSessionToken("token");

		//call under test
		assertThrows(IllegalArgumentException.class, ()->service.validateOAuthAuthenticationCodeAndLogin(request));
	}
	
	@Test
	public void testBindExternalID() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.ORCID);
		request.setRedirectUrl("https://domain.com");
		String aliasName = "name";
		AliasType type = AliasType.USER_ORCID;
		Long principalId = 101L;
		PrincipalAlias principalAlias = new PrincipalAlias();
		principalAlias.setAlias(aliasName);
		principalAlias.setPrincipalId(principalId);
		principalAlias.setType(type);
		when(mockUserManager.bindAlias(aliasName, type, principalId)).thenReturn(principalAlias);
		AliasAndType aliasAndType = new AliasAndType(aliasName, AliasType.USER_ORCID);
		when(mockOAuthManager.retrieveProvidersId(
				request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(aliasAndType);

		PrincipalAlias result = service.bindExternalID(principalId, request);
		assertEquals(principalAlias, result);
	}
	
	@Test
	public void testBindExternalIDAnonymous() throws Exception {
		assertThrows(UnauthorizedException.class, ()->service.bindExternalID(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), null));
	}
	
	@Test
	public void testUnbindExternalID() throws NotFoundException{
		Long principalId = 101L;
		when(mockOAuthManager.getAliasTypeForProvider(OAuthProvider.ORCID)).thenReturn(AliasType.USER_ORCID);
		String aliasName = "name";

		service.unbindExternalID(principalId, OAuthProvider.ORCID, aliasName);
		
		verify(mockOAuthManager).getAliasTypeForProvider(OAuthProvider.ORCID);
		verify(mockUserManager).unbindAlias(aliasName, AliasType.USER_ORCID, principalId);
	}
	
	
	@Test
	public void testLogin() {
		when(mockAuthenticationManager.login(loginRequest)).thenReturn(loginResponse);

		// call under test
		LoginResponse response = service.login(loginRequest);
		assertNotNull(response);
		assertEquals(loginResponse, response);
	}
	
	/**
	 * Test for PLFM-3914
	 * 
	 */
	@Test
	public void testLoginNotFound() {
		// NotFoundException should be converted to UnauthenticatedException;
		try {
			// call under test
			service.login(loginRequest);
		} catch (UnauthenticatedException e) {
			assertEquals(UnauthenticatedException.MESSAGE_USERNAME_PASSWORD_COMBINATION_IS_INCORRECT, e.getMessage());
		}
	}

	@Test
	public void testChangePassword(){
		ChangePasswordWithToken changePassword = new ChangePasswordWithToken();
		when(mockAuthenticationManager.changePassword(changePassword)).thenReturn(userId);


		service.changePassword(changePassword);

		verify(mockAuthenticationManager).changePassword(changePassword);
		verify(mockMessageManager).sendPasswordChangeConfirmationEmail(userId);
	}

	@Test
	public void testSendPasswordResetEmail(){
		String email = "user@test.com";
		String passwordResetUrlPrefix = "synapse.org";
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		when(mockUserManager.lookupUserByUsernameOrEmail(email)).thenReturn(principalAlias);
		when(mockAuthenticationManager.createPasswordResetToken(principalAlias.getPrincipalId())).thenReturn(token);

		//method under test
		service.sendPasswordResetEmail(passwordResetUrlPrefix, email);

		verify(mockUserManager).lookupUserByUsernameOrEmail(email);
		verify(mockAuthenticationManager).createPasswordResetToken(principalAlias.getPrincipalId());
		verify(mockMessageManager).sendNewPasswordResetEmail(passwordResetUrlPrefix, token, principalAlias);
	}

	@Test
	public void testSendPasswordResetEmail_UserNotFound(){
		String email = "user@test.com";
		String passwordResetUrlPrefix = "synapse.org";
		when(mockUserManager.lookupUserByUsernameOrEmail(email)).thenThrow(NotFoundException.class);

		//method under test
		service.sendPasswordResetEmail(passwordResetUrlPrefix, email);

		verify(mockUserManager).lookupUserByUsernameOrEmail(email);
		//expect no errors to be throw but the token should never be generated and sent
		verify(mockAuthenticationManager, never()).createPasswordResetToken(anyLong());
		verify(mockMessageManager, never()).sendNewPasswordResetEmail(anyString(), any(), any());
	}
	
	@Test
	public void testSendPasswordResetEmailWithEmailAlias() {
		when(mockUserManager.lookupUserByUsernameOrEmail(aliasEmail)).thenReturn(principalEmailAlias);

		String passwordResetUrlPrefix = "synapse.org";
		
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		
		when(mockAuthenticationManager.createPasswordResetToken(principalEmailAlias.getPrincipalId())).thenReturn(token);
		
		service.sendPasswordResetEmail(passwordResetUrlPrefix, aliasEmail);
		
		verify(mockUserManager).lookupUserByUsernameOrEmail(aliasEmail);
		verify(mockAuthenticationManager).createPasswordResetToken(principalEmailAlias.getPrincipalId());
		verify(mockMessageManager).sendNewPasswordResetEmail(passwordResetUrlPrefix, token, principalEmailAlias);	
	}
	
	@Test
	public void testHasUserAcceptedTermsOfUseJWT() {
		when(mockAuthenticationManager.hasUserAcceptedTermsOfUse(userId)).thenReturn(true);
		
		// method under test
		assertTrue(service.hasUserAcceptedTermsOfUse(userId));
		
		verify(mockAuthenticationManager).hasUserAcceptedTermsOfUse(userId);

		
	}
	
}
