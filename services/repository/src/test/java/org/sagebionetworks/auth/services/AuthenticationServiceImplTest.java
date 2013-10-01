package org.sagebionetworks.auth.services;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.Credential;

public class AuthenticationServiceImplTest {

	private AuthenticationServiceImpl service;
	
	private UserManager mockUserManager;
	private UserProfileManager mockUserProfileManager;
	private AuthenticationManager mockAuthenticationManager;
	
	private Credential credential;
	private UserInfo userInfo;
	
	@Before
	public void setUp() throws Exception {
		credential = new Credential();
		credential.setEmail("foo");
		credential.setPassHash("bar");
		
		userInfo = new UserInfo(false);
		userInfo.setUser(new User());
		userInfo.setIndividualGroup(new UserGroup());
		
		mockUserManager = Mockito.mock(UserManager.class);
		when(mockUserManager.getUserInfo(anyString())).thenReturn(userInfo);
		mockUserProfileManager = Mockito.mock(UserProfileManager.class);
		when(mockUserProfileManager.getUserProfile(any(UserInfo.class), anyString())).thenReturn(new UserProfile());
		mockAuthenticationManager = Mockito.mock(AuthenticationManager.class);
		
		service = new AuthenticationServiceImpl(mockUserManager, mockUserProfileManager, mockAuthenticationManager);
	}
	
	@Test
	public void testAuthenticate() throws Exception {
		// Don't do ToU checking
		service.authenticate(credential, true, false);
		verify(mockAuthenticationManager).authenticate(credential, true);
		verify(mockUserManager, times(0)).getUserInfo(anyString());
		
		// ToU checking passes
		credential.setAcceptsTermsOfUse(true);
		userInfo.getUser().setAgreesToTermsOfUse(true);
		service.authenticate(credential, false, true);
		verify(mockAuthenticationManager).authenticate(credential, false);
		verify(mockUserManager).getUserInfo(anyString());
		verify(mockUserProfileManager, times(0)).updateUserProfile(eq(userInfo), any(UserProfile.class));
		
		// ToU acceptance must be updated
		credential.setAcceptsTermsOfUse(true);
		userInfo.getUser().setAgreesToTermsOfUse(false);
		service.authenticate(credential, false, true);
		verify(mockUserProfileManager).agreeToTermsOfUse(eq(userInfo));
		
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testAuthenticateToUFail() throws Exception {
		// ToU checking fails
		credential.setAcceptsTermsOfUse(false);
		service.authenticate(credential, false, true);
	}
}
