package org.sagebionetworks.auth.services;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.authutil.OpenIDConsumerUtils;
import org.sagebionetworks.authutil.OpenIDInfo;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.RegistrationInfo;
import org.sagebionetworks.repo.model.auth.Session;

public class AuthenticationServiceImplTest {

	private AuthenticationServiceImpl service;
	
	private UserManager mockUserManager;
	private AuthenticationManager mockAuthenticationManager;
	
	private NewUser credential;
	private UserInfo userInfo;
	private static String username = "AuthServiceUser";
	private static String password = "NeverUse_thisPassword";
	private static long userId = 123456789L;
	private static String sessionToken = "Some session token";
	
	@Before
	public void setUp() throws Exception {
		credential = new NewUser();
		credential.setEmail(username);
		credential.setPassword(password);
		
		userInfo = new UserInfo(false);
		userInfo.setUser(new User());
		userInfo.getUser().setDisplayName(username);
		userInfo.getUser().setId("" + userId);
		userInfo.setIndividualGroup(new UserGroup());
		userInfo.getIndividualGroup().setName(username);
		userInfo.getIndividualGroup().setId("" + userId);
		
		mockUserManager = Mockito.mock(UserManager.class);
		when(mockUserManager.getUserInfo(eq(username))).thenReturn(userInfo);
		when(mockUserManager.getGroupName(anyString())).thenReturn(username);
		
		mockAuthenticationManager = Mockito.mock(AuthenticationManager.class);
		when(mockAuthenticationManager.checkSessionToken(eq(sessionToken))).thenReturn(userId);
		
		service = new AuthenticationServiceImpl(mockUserManager, mockAuthenticationManager);
	}
	
	@Test
	public void testAuthenticate() throws Exception {
		// Check password and update the ToU acceptance
		credential.setAcceptsTermsOfUse(true);
		userInfo.getUser().setAgreesToTermsOfUse(false);
		
		service.authenticate(credential);
		verify(mockAuthenticationManager).authenticate(eq(username), eq(password));
		verify(mockUserManager).getUserInfo(anyString());
		verify(mockAuthenticationManager).setTermsOfUseAcceptance(eq("" + userId), eq(true));
		
	}
	
	@Test(expected=TermsOfUseException.class)
	public void testAuthenticateToUFail() throws Exception {
		// ToU checking should fail
		credential.setAcceptsTermsOfUse(false);
		service.authenticate(credential);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testRevalidateToU() throws Exception {
		userInfo.getUser().setAgreesToTermsOfUse(true);
		Assert.assertTrue(service.hasUserAcceptedTermsOfUse("" + userId));

		userInfo.getUser().setAgreesToTermsOfUse(false);
		service.revalidate("Some session token");
	}
	
	@Test
	public void testOpenIDAuthentication_newUser() throws Exception {
		// This user does not exist yet
		when(mockUserManager.doesPrincipalExist(eq(username))).thenReturn(false);
		userInfo.getUser().setAgreesToTermsOfUse(false);
		
		OpenIDInfo info = new OpenIDInfo();
		info.setMap(new HashMap<String, List<String>>());
		info.getMap().put(OpenIDConsumerUtils.AX_EMAIL, Arrays.asList(new String[] { username }));
		
		service.processOpenIDInfo(info, null);
		
		// The user should be created
		verify(mockUserManager).createUser(any(NewUser.class));
		verify(mockAuthenticationManager).authenticate(eq(username), eq((String) null));
	}
	
	@Test
	public void testOpenIDAuthentication_acceptToU() throws Exception {
		// This user is returning to accept the ToU
		when(mockUserManager.doesPrincipalExist(eq(username))).thenReturn(true);
		userInfo.getUser().setAgreesToTermsOfUse(false);
		
		OpenIDInfo info = new OpenIDInfo();
		info.setMap(new HashMap<String, List<String>>());
		info.getMap().put(OpenIDConsumerUtils.AX_EMAIL, Arrays.asList(new String[] { username }));
		
		service.processOpenIDInfo(info, true);
		
		// User should not be created, ToU should be updated
		verify(mockUserManager, times(0)).createUser(any(NewUser.class));
		verify(mockAuthenticationManager).authenticate(eq(username), eq((String) null));
		verify(mockUserManager).getUserInfo(anyString());
		verify(mockAuthenticationManager).setTermsOfUseAcceptance(eq("" + userId), eq(true));
	}
	
	@Test
	public void testChangeEmail() throws Exception {
		userInfo.getUser().setAgreesToTermsOfUse(true);
		
		RegistrationInfo registrationInfo = new RegistrationInfo();
		registrationInfo.setPassword(password);
		registrationInfo.setRegistrationToken(AuthorizationConstants.CHANGE_EMAIL_TOKEN_PREFIX + sessionToken);
		service.updateEmail(username, registrationInfo);
		verify(mockUserManager, times(3)).getUserInfo(eq(username));
		verify(mockUserManager).updateEmail(eq(userInfo), eq(username));
	}
	
	@Test
	public void testAcceptTermsOfUse() throws Exception {
		userInfo.getUser().setAgreesToTermsOfUse(false);
		Session session = new Session();
		session.setSessionToken(sessionToken);
		
		service.acceptTermsOfUse(session);
		verify(mockAuthenticationManager).setTermsOfUseAcceptance(eq("" + userId), eq(true));
	}
}
