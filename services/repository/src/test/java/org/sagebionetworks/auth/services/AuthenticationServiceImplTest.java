package org.sagebionetworks.auth.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.AliasAndType;
import org.sagebionetworks.repo.manager.oauth.OAuthManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
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
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationServiceImplTest {

	private AuthenticationServiceImpl service;
	
	@Mock
	private UserManager mockUserManager;
	@Mock
	private AuthenticationManager mockAuthenticationManager;
	@Mock
	private MessageManager mockMessageManager;
	@Mock
	private OAuthManager mockOAuthManager;
	
	private LoginCredentials credential;
	private UserInfo userInfo;
	private static String username = "AuthServiceUser";
	private static String fullName = "Auth User";
	private static String password = "NeverUse_thisPassword";
	private static long userId = 123456789L;
	private static String sessionToken = "Some session token";
	
	String alias;
	PrincipalAlias principalAlias;
	
	LoginRequest loginRequest;
	LoginResponse loginResponse;
	
	@Before
	public void setUp() throws Exception {
		credential = new LoginCredentials();
		credential.setEmail(username);
		credential.setPassword(password);
		
		userInfo = new UserInfo(false);
		userInfo.setId(userId);
		
		mockUserManager = Mockito.mock(UserManager.class);
		mockOAuthManager = Mockito.mock(OAuthManager.class);
		when(mockUserManager.getUserInfo(eq(userId))).thenReturn(userInfo);
		when(mockUserManager.createUser(any(NewUser.class))).thenReturn(userId);
		
		mockAuthenticationManager = Mockito.mock(AuthenticationManager.class);
		when(mockAuthenticationManager.checkSessionToken(eq(sessionToken), eq(true)))
				.thenReturn(userId);
		
		mockMessageManager = Mockito.mock(MessageManager.class);
		
		service = new AuthenticationServiceImpl();
		ReflectionTestUtils.setField(service, "userManager", mockUserManager);
		ReflectionTestUtils.setField(service, "authManager", mockAuthenticationManager);
		ReflectionTestUtils.setField(service, "messageManager", mockMessageManager);
		ReflectionTestUtils.setField(service, "oauthManager", mockOAuthManager);
		
		alias = "alias";
		principalAlias = new PrincipalAlias();
		principalAlias.setAlias(alias);
		principalAlias.setAliasId(2222L);
		principalAlias.setType(AliasType.USER_NAME);
		when(mockUserManager.lookupUserForAuthentication(alias)).thenReturn(principalAlias);
		
		loginRequest = new LoginRequest();
		loginRequest.setAuthenticationReceipt("receipt");
		loginRequest.setUsername("username");
		loginRequest.setPassword("password");
		
		loginResponse = new LoginResponse();
		loginResponse.setAcceptsTermsOfUse(true);
		loginResponse.setAuthenticationReceipt("newReceipt");
		loginResponse.setSessionToken("sessionToken");
		
		when(mockUserManager.lookupUserForAuthentication(loginRequest.getUsername())).thenReturn(principalAlias);
		when(mockAuthenticationManager.login(principalAlias.getPrincipalId(), loginRequest.getPassword(), loginRequest.getAuthenticationReceipt())).thenReturn(loginResponse);
	}
	
	@Test
	public void testRevalidateToU() throws Exception {
		when(mockAuthenticationManager.checkSessionToken(eq(sessionToken), eq(true)))
				.thenThrow(new TermsOfUseException());
		
		// A boolean flag should let us get past this call
		service.revalidate(sessionToken, false);

		// But it should default to true
		try {
			service.revalidate(sessionToken);
			fail();
		} catch (TermsOfUseException e) {
			// Expected
		}
	}
	
	@Test
	public void testGetOAuthAuthenticationUrl(){
		OAuthUrlRequest request = new OAuthUrlRequest();
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl("http://domain.com");
		String authUrl = "https://auth.org";
		when(mockOAuthManager.getAuthorizationUrl(request.getProvider(), request.getRedirectUrl())).thenReturn(authUrl);
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
		when(mockUserManager.lookupUserForAuthentication(info.getUsersVerifiedEmail())).thenReturn(alias);
		Session session = new Session();
		session.setSessionToken("token");
		when(mockAuthenticationManager.getSessionToken(userId)).thenReturn(session);
		//call under test
		Session result = service.validateOAuthAuthenticationCodeAndLogin(request);
		assertEquals(session, result);
	}
	
	@Test (expected=IllegalArgumentException.class)
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
		when(mockUserManager.lookupUserForAuthentication(info.getUsersVerifiedEmail())).thenReturn(alias);
		Session session = new Session();
		session.setSessionToken("token");
		when(mockAuthenticationManager.getSessionToken(userId)).thenReturn(session);
		//call under test
		Session result = service.validateOAuthAuthenticationCodeAndLogin(request);
		assertEquals(session, result);
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
	
	@Test(expected=UnauthorizedException.class)
	public void testBindExternalIDAnonymous() throws Exception {
		service.bindExternalID(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), null);
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
		when(mockUserManager.lookupUserForAuthentication(loginRequest.getUsername())).thenThrow(new NotFoundException("does not exist"));
		try {
			// call under test
			service.login(loginRequest);
		} catch (UnauthenticatedException e) {
			assertEquals(UnauthenticatedException.MESSAGE_USERNAME_PASSWORD_COMBINATION_IS_INCORRECT, e.getMessage());
		}
	}
}
