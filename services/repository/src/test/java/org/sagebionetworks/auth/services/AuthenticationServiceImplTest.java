package org.sagebionetworks.auth.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
import org.sagebionetworks.repo.manager.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AccessToken;
import org.sagebionetworks.repo.model.auth.ChangePasswordWithToken;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalOidcBinding;
import org.sagebionetworks.repo.model.oauth.OAuthAccountCreationRequest;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
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
	private static String ACCESS_TOKEN = "Some access token";
	private static final String ISSUER = "https://repo-prod.sagebase.org/v1";
	
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
	public void testSignTermsOfUse() {
		AccessToken accessToken = new AccessToken();
		accessToken.setAccessToken(ACCESS_TOKEN);
		when(mockOidcManager.validateAccessToken(ACCESS_TOKEN)).thenReturn(""+userId);
		
		// method under test
		service.signTermsOfUse(accessToken);
		
		verify(mockOidcManager).validateAccessToken(ACCESS_TOKEN);
		verify(mockAuthenticationManager).setTermsOfUseAcceptance(userId, true);
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
		info.setSubject("abcd");
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		PrincipalAlias alias = new PrincipalAlias();
		long userId = 3456L;
		alias.setPrincipalId(userId);
		when(mockUserManager.lookupOidcBindingBySubject(any(), any())).thenReturn(Optional.of(new PrincipalOidcBinding().setUserId(userId).setAliasId(456L)));
		LoginResponse authMgrLoginResponse = new LoginResponse();
		authMgrLoginResponse.setAcceptsTermsOfUse(true);
		authMgrLoginResponse.setAccessToken(ACCESS_TOKEN);
		authMgrLoginResponse.setAuthenticationReceipt("authentication-receipt");
		
		when(mockAuthenticationManager.loginWithNoPasswordCheck(anyLong(), any())).thenReturn(authMgrLoginResponse);
		
		//call under test
		LoginResponse result = service.validateOAuthAuthenticationCodeAndLogin(request, ISSUER);
		
		assertEquals(authMgrLoginResponse, result);
		
		verify(mockOAuthManager).validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		verify(mockUserManager).lookupOidcBindingBySubject(request.getProvider(), info.getSubject());
		verifyNoMoreInteractions(mockUserManager);
		verify(mockAuthenticationManager).loginWithNoPasswordCheck(userId, ISSUER);
	}
	
	@Test
	public void testValidateOAuthAuthenticationCodeWithNoBoundAliasAndEmailMatch() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl("https://domain.com");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setUsersVerifiedEmail("first.last@domain.com");
		info.setSubject("abcd");
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		PrincipalAlias alias = new PrincipalAlias();
		long userId = 3456L;
		alias.setPrincipalId(userId);
		
		PrincipalOidcBinding oidcBinding = new PrincipalOidcBinding().setUserId(userId).setAliasId(null);
		
		when(mockUserManager.lookupOidcBindingBySubject(any(), any())).thenReturn(Optional.of(oidcBinding));
		when(mockUserManager.lookupUserByUsernameOrEmail(any())).thenReturn(alias);
		LoginResponse authMgrLoginResponse = new LoginResponse();
		authMgrLoginResponse.setAcceptsTermsOfUse(true);
		authMgrLoginResponse.setAccessToken(ACCESS_TOKEN);
		authMgrLoginResponse.setAuthenticationReceipt("authentication-receipt");
		
		when(mockAuthenticationManager.loginWithNoPasswordCheck(anyLong(), any())).thenReturn(authMgrLoginResponse);
		
		//call under test
		LoginResponse result = service.validateOAuthAuthenticationCodeAndLogin(request, ISSUER);
		
		assertEquals(authMgrLoginResponse, result);
		
		verify(mockOAuthManager).validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		verify(mockUserManager).lookupOidcBindingBySubject(request.getProvider(), info.getSubject());
		verify(mockUserManager).lookupUserByUsernameOrEmail(info.getUsersVerifiedEmail());
		verify(mockUserManager).setOidcBindingAlias(oidcBinding, alias);
		verifyNoMoreInteractions(mockUserManager);
		verify(mockAuthenticationManager).loginWithNoPasswordCheck(userId, ISSUER);
	}
	
	@Test
	public void testValidateOAuthAuthenticationCodeWithNoBoundAliasAndAliasMatch() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.ORCID);
		request.setRedirectUrl("https://domain.com");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setUsersVerifiedEmail("first.last@domain.com");
		info.setSubject("abcd");
		info.setAliasAndType(new AliasAndType("alias", AliasType.USER_ORCID));
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		PrincipalAlias alias = new PrincipalAlias();
		long userId = 3456L;
		alias.setPrincipalId(userId);
		
		PrincipalOidcBinding oidcBinding = new PrincipalOidcBinding().setUserId(userId).setAliasId(null);
		
		when(mockUserManager.lookupOidcBindingBySubject(any(), any())).thenReturn(Optional.of(oidcBinding));
		when(mockUserManager.lookupUserByAliasType(any(), any())).thenReturn(alias);
		LoginResponse authMgrLoginResponse = new LoginResponse();
		authMgrLoginResponse.setAcceptsTermsOfUse(true);
		authMgrLoginResponse.setAccessToken(ACCESS_TOKEN);
		authMgrLoginResponse.setAuthenticationReceipt("authentication-receipt");
		
		when(mockAuthenticationManager.loginWithNoPasswordCheck(anyLong(), any())).thenReturn(authMgrLoginResponse);
		
		//call under test
		LoginResponse result = service.validateOAuthAuthenticationCodeAndLogin(request, ISSUER);
		
		assertEquals(authMgrLoginResponse, result);
		
		verify(mockOAuthManager).validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		verify(mockUserManager).lookupOidcBindingBySubject(request.getProvider(), info.getSubject());
		verify(mockUserManager).lookupUserByUsernameOrEmail(info.getUsersVerifiedEmail());
		verify(mockUserManager).lookupUserByAliasType(AliasType.USER_ORCID, "alias");
		verify(mockUserManager).setOidcBindingAlias(oidcBinding, alias);
		verifyNoMoreInteractions(mockUserManager);
		verify(mockAuthenticationManager).loginWithNoPasswordCheck(userId, ISSUER);
	}
	
	@Test
	public void testValidateOAuthAuthenticationCodeWithNoBoundAliasAndNoAliasFound() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl("https://domain.com");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setUsersVerifiedEmail("first.last@domain.com");
		info.setSubject("abcd");
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
				
		PrincipalOidcBinding oidcBinding = new PrincipalOidcBinding().setBindingId(12345L).setUserId(123L).setAliasId(null);
		
		when(mockUserManager.lookupOidcBindingBySubject(any(), any())).thenReturn(Optional.of(oidcBinding));
		when(mockUserManager.lookupUserByUsernameOrEmail(any())).thenThrow(NotFoundException.class);
		LoginResponse authMgrLoginResponse = new LoginResponse();
		authMgrLoginResponse.setAcceptsTermsOfUse(true);
		authMgrLoginResponse.setAccessToken(ACCESS_TOKEN);
		authMgrLoginResponse.setAuthenticationReceipt("authentication-receipt");
		
		String result = assertThrows(NotFoundException.class, () -> {			
			//call under test
			service.validateOAuthAuthenticationCodeAndLogin(request, ISSUER);
		}).getMessage();
		
		assertEquals("Could not find a user matching the GOOGLE_OAUTH_2_0 provider information.", result);
		
		verify(mockOAuthManager).validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		verify(mockUserManager).lookupOidcBindingBySubject(request.getProvider(), info.getSubject());
		verify(mockUserManager).lookupUserByUsernameOrEmail(info.getUsersVerifiedEmail());
		verify(mockUserManager).deleteOidcBinding(oidcBinding.getBindingId());
		verifyNoMoreInteractions(mockUserManager);
		verifyNoMoreInteractions(mockAuthenticationManager);
	}
	
	@Test
	public void testValidateOAuthAuthenticationCodeWithNoBoundAliasAndUserMismatch() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl("https://domain.com");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setUsersVerifiedEmail("first.last@domain.com");
		info.setSubject("abcd");
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		PrincipalAlias alias = new PrincipalAlias();
		long userId = 3456L;
		alias.setAliasId(789L);
		alias.setPrincipalId(userId);
		
		PrincipalOidcBinding oidcBinding = new PrincipalOidcBinding().setBindingId(12345L).setUserId(123L).setAliasId(null);
		
		when(mockUserManager.lookupOidcBindingBySubject(any(), any())).thenReturn(Optional.of(oidcBinding));
		when(mockUserManager.lookupUserByUsernameOrEmail(any())).thenReturn(alias);
		LoginResponse authMgrLoginResponse = new LoginResponse();
		authMgrLoginResponse.setAcceptsTermsOfUse(true);
		authMgrLoginResponse.setAccessToken(ACCESS_TOKEN);
		authMgrLoginResponse.setAuthenticationReceipt("authentication-receipt");
		
		//call under test
		service.validateOAuthAuthenticationCodeAndLogin(request, ISSUER);
				
		verify(mockOAuthManager).validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		verify(mockUserManager).lookupOidcBindingBySubject(request.getProvider(), info.getSubject());
		verify(mockUserManager).lookupUserByUsernameOrEmail(info.getUsersVerifiedEmail());
		verify(mockUserManager).deleteOidcBinding(oidcBinding.getBindingId());
		verify(mockUserManager).bindUserToOidcSubject(alias, OAuthProvider.GOOGLE_OAUTH_2_0, "abcd");
		verifyNoMoreInteractions(mockUserManager);
		verify(mockAuthenticationManager).loginWithNoPasswordCheck(userId, ISSUER);
	}
	
	@Test
	public void testValidateOAuthAuthenticationCodeWithEmailFallback() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl("https://domain.com");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setUsersVerifiedEmail("first.last@domain.com");
		info.setSubject("abcd");
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		PrincipalAlias alias = new PrincipalAlias();
		long userId = 3456L;
		alias.setPrincipalId(userId);
		when(mockUserManager.lookupOidcBindingBySubject(any(), any())).thenReturn(Optional.empty());
		when(mockUserManager.lookupUserByUsernameOrEmail(any())).thenReturn(alias);
		when(mockUserManager.bindUserToOidcSubject(any(), any(), any())).thenReturn(new PrincipalOidcBinding().setUserId(userId).setAliasId(456L));
		LoginResponse authMgrLoginResponse = new LoginResponse();
		authMgrLoginResponse.setAcceptsTermsOfUse(true);
		authMgrLoginResponse.setAccessToken(ACCESS_TOKEN);
		authMgrLoginResponse.setAuthenticationReceipt("authentication-receipt");

		when(mockAuthenticationManager.loginWithNoPasswordCheck(anyLong(), any())).thenReturn(authMgrLoginResponse);
		
		//call under test
		LoginResponse result = service.validateOAuthAuthenticationCodeAndLogin(request, ISSUER);
		
		assertEquals(authMgrLoginResponse, result);
		
		verify(mockOAuthManager).validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		verify(mockUserManager).lookupOidcBindingBySubject(request.getProvider(), info.getSubject());
		verify(mockUserManager).lookupUserByUsernameOrEmail(info.getUsersVerifiedEmail());
		verify(mockUserManager).bindUserToOidcSubject(alias, request.getProvider(), info.getSubject());
		verify(mockAuthenticationManager).loginWithNoPasswordCheck(userId, ISSUER);
	}
	
	@Test
	public void testValidateOAuthAuthenticationCodeWithAliasFallback() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.ORCID);
		request.setRedirectUrl("https://domain.com");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setSubject("abcd");
		info.setAliasAndType(new AliasAndType("alias", AliasType.USER_ORCID));
		
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		PrincipalAlias alias = new PrincipalAlias();
		long userId = 3456L;
		alias.setPrincipalId(userId);
		when(mockUserManager.lookupOidcBindingBySubject(any(), any())).thenReturn(Optional.empty());
		when(mockUserManager.lookupUserByAliasType(any(), any())).thenReturn(alias);
		when(mockUserManager.bindUserToOidcSubject(any(), any(), any())).thenReturn(new PrincipalOidcBinding().setUserId(userId).setAliasId(456L));
		LoginResponse authMgrLoginResponse = new LoginResponse();
		authMgrLoginResponse.setAcceptsTermsOfUse(true);
		authMgrLoginResponse.setAccessToken(ACCESS_TOKEN);
		authMgrLoginResponse.setAuthenticationReceipt("authentication-receipt");

		when(mockAuthenticationManager.loginWithNoPasswordCheck(anyLong(), any())).thenReturn(authMgrLoginResponse);
		
		//call under test
		LoginResponse result = service.validateOAuthAuthenticationCodeAndLogin(request, ISSUER);
		
		assertEquals(authMgrLoginResponse, result);
		
		verify(mockOAuthManager).validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		verify(mockUserManager).lookupOidcBindingBySubject(request.getProvider(), info.getSubject());
		verify(mockUserManager).lookupUserByAliasType(AliasType.USER_ORCID, "alias");
		verify(mockUserManager).bindUserToOidcSubject(alias, request.getProvider(), info.getSubject());
		verify(mockAuthenticationManager).loginWithNoPasswordCheck(userId, ISSUER);
	}
	
	@Test
	public void testValidateOAuthAuthenticationCodeWithEmailNotFoundAndAliasFallback() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.ORCID);
		request.setRedirectUrl("https://domain.com");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setUsersVerifiedEmail("first.last@domain.com");
		info.setSubject("abcd");
		info.setAliasAndType(new AliasAndType("alias", AliasType.USER_ORCID));
		
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		PrincipalAlias alias = new PrincipalAlias();
		long userId = 3456L;
		alias.setPrincipalId(userId);
		when(mockUserManager.lookupOidcBindingBySubject(any(), any())).thenReturn(Optional.empty());
		when(mockUserManager.lookupUserByUsernameOrEmail(any())).thenThrow(NotFoundException.class);
		when(mockUserManager.lookupUserByAliasType(any(), any())).thenReturn(alias);
		when(mockUserManager.bindUserToOidcSubject(any(), any(), any())).thenReturn(new PrincipalOidcBinding().setUserId(userId).setAliasId(456L));
		LoginResponse authMgrLoginResponse = new LoginResponse();
		authMgrLoginResponse.setAcceptsTermsOfUse(true);
		authMgrLoginResponse.setAccessToken(ACCESS_TOKEN);
		authMgrLoginResponse.setAuthenticationReceipt("authentication-receipt");

		when(mockAuthenticationManager.loginWithNoPasswordCheck(anyLong(), any())).thenReturn(authMgrLoginResponse);
		
		//call under test
		LoginResponse result = service.validateOAuthAuthenticationCodeAndLogin(request, ISSUER);
		
		assertEquals(authMgrLoginResponse, result);
		
		verify(mockOAuthManager).validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		verify(mockUserManager).lookupOidcBindingBySubject(request.getProvider(), info.getSubject());
		verify(mockUserManager).lookupUserByUsernameOrEmail("first.last@domain.com");
		verify(mockUserManager).lookupUserByAliasType(AliasType.USER_ORCID, "alias");
		verify(mockUserManager).bindUserToOidcSubject(alias, request.getProvider(), info.getSubject());
		verify(mockAuthenticationManager).loginWithNoPasswordCheck(userId, ISSUER);
	}
	
	@Test
	public void testValidateOAuthAuthenticationCodeWithNoMatch() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.ORCID);
		request.setRedirectUrl("https://domain.com");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setUsersVerifiedEmail("first.last@domain.com");
		info.setSubject("abcd");
		info.setAliasAndType(new AliasAndType("alias", AliasType.USER_ORCID));
		
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		PrincipalAlias alias = new PrincipalAlias();
		long userId = 3456L;
		alias.setPrincipalId(userId);
		when(mockUserManager.lookupOidcBindingBySubject(any(), any())).thenReturn(Optional.empty());
		when(mockUserManager.lookupUserByUsernameOrEmail(any())).thenThrow(NotFoundException.class);
		when(mockUserManager.lookupUserByAliasType(any(), any())).thenThrow(NotFoundException.class);
		LoginResponse authMgrLoginResponse = new LoginResponse();
		authMgrLoginResponse.setAcceptsTermsOfUse(true);
		authMgrLoginResponse.setAccessToken(ACCESS_TOKEN);
		authMgrLoginResponse.setAuthenticationReceipt("authentication-receipt");

		String result = assertThrows(NotFoundException.class, () -> {			
			service.validateOAuthAuthenticationCodeAndLogin(request, ISSUER);
		}).getMessage();
		
		assertEquals("Could not find a user matching the ORCID provider information.", result);
				
		verify(mockOAuthManager).validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		verify(mockUserManager).lookupOidcBindingBySubject(request.getProvider(), info.getSubject());
		verify(mockUserManager).lookupUserByUsernameOrEmail("first.last@domain.com");
		verify(mockUserManager).lookupUserByAliasType(AliasType.USER_ORCID, "alias");
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
		info.setSubject("abcd");
		
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		
		when(mockUserManager.createUser((NewUser)any())).thenReturn(userId);
		LoginResponse authMgrLoginResponse = new LoginResponse();
		authMgrLoginResponse.setAcceptsTermsOfUse(true);
		authMgrLoginResponse.setAccessToken(ACCESS_TOKEN);
		authMgrLoginResponse.setAuthenticationReceipt("authentication-receipt");
		
		when(mockAuthenticationManager.loginWithNoPasswordCheck(anyLong(), any())).thenReturn(authMgrLoginResponse);
		
		//call under test
		LoginResponse result = service.createAccountViaOauth(request, ISSUER);
		assertEquals(authMgrLoginResponse, result);
		
		NewUser expectedUser = new NewUser();
		expectedUser.setEmail(info.getUsersVerifiedEmail());
		expectedUser.setFirstName(info.getFirstName());
		expectedUser.setLastName(info.getLastName());
		expectedUser.setUserName(request.getUserName());
		expectedUser.setOauthProvider(request.getProvider());
		expectedUser.setSubject(info.getSubject());
		
		verify(mockOAuthManager).validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl());
		verify(mockUserManager).createUser(expectedUser);
		verify(mockAuthenticationManager).loginWithNoPasswordCheck(userId, ISSUER);
	}
	
	@Test
	public void testValidateOAuthAuthenticationCodeEmailNull() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl("https://domain.com");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setUsersVerifiedEmail(null);
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), 
				request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);

		//call under test
		assertThrows(IllegalArgumentException.class, ()->service.validateOAuthAuthenticationCodeAndLogin(request, ISSUER));
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
