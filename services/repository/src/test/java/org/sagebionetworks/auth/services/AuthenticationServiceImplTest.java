package org.sagebionetworks.auth.services;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.services.AuthenticationServiceImpl;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;

public class AuthenticationServiceImplTest {

	private AuthenticationServiceImpl service;
	
	private UserManager mockUserManager;
	private UserProfileManager mockUserProfileManager;
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
		
		mockUserProfileManager = Mockito.mock(UserProfileManager.class);
		when(mockUserProfileManager.getUserProfile(any(UserInfo.class), anyString())).thenReturn(new UserProfile());
		
		mockAuthenticationManager = Mockito.mock(AuthenticationManager.class);
		when(mockAuthenticationManager.checkSessionToken(eq(sessionToken))).thenReturn(userId);
		
		service = new AuthenticationServiceImpl(mockUserManager, mockUserProfileManager, mockAuthenticationManager);
	}
	
	@Test
	public void testAuthenticate() throws Exception {
		// Check password but not ToU
		service.authenticate(null, credential, true, false);
		verify(mockAuthenticationManager).authenticate(eq(username), eq(password));
		verify(mockUserManager, times(0)).getUserInfo(anyString());
		
		// Check ToU but not password
		credential.setAcceptsTermsOfUse(true);
		userInfo.getUser().setAgreesToTermsOfUse(true);
		service.authenticate(null, credential, false, true);
		verify(mockAuthenticationManager).authenticate(eq(username), eq((String) null));
		verify(mockUserManager).getUserInfo(anyString());
		verify(mockUserProfileManager, times(0)).updateUserProfile(eq(userInfo), any(UserProfile.class));
		
		// ToU acceptance must be updated
		credential.setAcceptsTermsOfUse(true);
		userInfo.getUser().setAgreesToTermsOfUse(false);
		service.authenticate(null, credential, false, true);
		verify(mockUserProfileManager).agreeToTermsOfUse(eq(userInfo));
		
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testAuthenticateToUFail() throws Exception {
		// ToU checking should fail
		credential.setAcceptsTermsOfUse(false);
		service.authenticate(null, credential, false, true);
	}
	
	@Test
	public void testAuthenticatePortalUser() throws Exception {
		// Password and ToU checking is disabled for the portal user
		userInfo.getUser().setAgreesToTermsOfUse(false);
		service.authenticate(StackConfiguration.getPortalUsername(), credential, true, true);
		verify(mockAuthenticationManager).authenticate(eq(username), eq((String) null));
		
		// But it is enabled for non-portal users
		userInfo.getUser().setAgreesToTermsOfUse(true);
		service.authenticate("Not the portal user", credential, true, true);
		verify(mockAuthenticationManager).authenticate(eq(username), eq(password));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testRevalidateToU() throws Exception {
		userInfo.getUser().setAgreesToTermsOfUse(true);
		Assert.assertTrue(service.hasUserAcceptedTermsOfUse("" + userId));

		userInfo.getUser().setAgreesToTermsOfUse(false);
		service.revalidate("Some session token");
	}
}
