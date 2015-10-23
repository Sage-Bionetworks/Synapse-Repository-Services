package org.sagebionetworks.auth.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.authutil.OpenIDInfo;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.AliasAndType;
import org.sagebionetworks.repo.manager.oauth.OAuthManager;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UserInfo;
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
import org.sagebionetworks.repo.web.NotFoundException;

public class AuthenticationServiceImplTest {

	private AuthenticationServiceImpl service;
	
	private UserManager mockUserManager;
	private AuthenticationManager mockAuthenticationManager;
	private MessageManager mockMessageManager;
	private OAuthManager mockOAuthManager;
	
	private LoginCredentials credential;
	private UserInfo userInfo;
	private static String username = "AuthServiceUser";
	private static String fullName = "Auth User";
	private static String password = "NeverUse_thisPassword";
	private static long userId = 123456789L;
	private static String sessionToken = "Some session token";
	
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
		when(mockAuthenticationManager.checkSessionToken(eq(sessionToken), eq(DomainType.SYNAPSE), eq(true)))
				.thenReturn(userId);
		
		mockMessageManager = Mockito.mock(MessageManager.class);
		
		service = new AuthenticationServiceImpl(mockUserManager, mockAuthenticationManager, mockMessageManager, mockOAuthManager);
	}
	
	@Test
	public void testRevalidateToU() throws Exception {
		when(mockAuthenticationManager.checkSessionToken(eq(sessionToken), eq(DomainType.SYNAPSE), eq(true)))
				.thenThrow(new TermsOfUseException());
		
		// A boolean flag should let us get past this call
		service.revalidate(sessionToken, DomainType.SYNAPSE, false);

		// But it should default to true
		try {
			service.revalidate(sessionToken, DomainType.SYNAPSE);
			fail();
		} catch (TermsOfUseException e) {
			// Expected
		}
	}
	
	@Test (expected=NotFoundException.class)
	public void testOpenIDAuthentication_newUser() throws Exception {
		// This user does not exist yet
		OpenIDInfo info = new OpenIDInfo();
		info.setEmail(username);
		info.setFullName(fullName);
		service.processOpenIDInfo(info, DomainType.SYNAPSE);
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
		when(mockUserManager.lookupPrincipalByAlias(info.getUsersVerifiedEmail())).thenReturn(alias);
		Session session = new Session();
		session.setSessionToken("token");
		when(mockAuthenticationManager.getSessionToken(userId, DomainType.SYNAPSE)).thenReturn(session);
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
		when(mockUserManager.lookupPrincipalByAlias(info.getUsersVerifiedEmail())).thenReturn(alias);
		Session session = new Session();
		session.setSessionToken("token");
		when(mockAuthenticationManager.getSessionToken(userId, DomainType.SYNAPSE)).thenReturn(session);
		//call under test
		Session result = service.validateOAuthAuthenticationCodeAndLogin(request);
		assertEquals(session, result);
	}
	
	@Test
	public void testValidateOAuthAuthenticationCodeNoMatch() throws NotFoundException{
		OAuthValidationRequest request = new OAuthValidationRequest();
		request.setAuthenticationCode("some code");
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl("https://domain.com");
		ProvidedUserInfo info = new ProvidedUserInfo();
		info.setUsersVerifiedEmail("first.last@domain.com");
		when(mockOAuthManager.validateUserWithProvider(request.getProvider(), request.getAuthenticationCode(), request.getRedirectUrl())).thenReturn(info);
		when(mockUserManager.lookupPrincipalByAlias(info.getUsersVerifiedEmail())).thenReturn(null);
		//call under test
		try {
			service.validateOAuthAuthenticationCodeAndLogin(request);
			fail("Should have failed");
		} catch (NotFoundException e) {
			assertEquals("Email should be the error when not found.",info.getUsersVerifiedEmail(), e.getMessage());
		}
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
	

}
