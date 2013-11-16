package org.sagebionetworks.auth.services;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.authutil.OpenIDInfo;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.OriginatingClient;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.NewUser;

public class AuthenticationServiceImplTest {

	private AuthenticationServiceImpl service;
	
	private UserManager mockUserManager;
	private AuthenticationManager mockAuthenticationManager;
	
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
		when(mockAuthenticationManager.checkSessionToken(eq(sessionToken), eq(true))).thenReturn(userId);
		
		service = new AuthenticationServiceImpl(mockUserManager, mockAuthenticationManager);
	}
	
	@Test
	public void testRevalidateToU() throws Exception {
		when(mockAuthenticationManager.checkSessionToken(eq(sessionToken), eq(true))).thenThrow(new TermsOfUseException());
		
		// A boolean flag should let us get past this call
		userInfo.getUser().setAgreesToTermsOfUse(false);
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
	public void testOpenIDAuthentication_newUser() throws Exception {
		// This user does not exist yet
		when(mockUserManager.doesPrincipalExist(eq(username))).thenReturn(false);
		userInfo.getUser().setAgreesToTermsOfUse(false);
		
		OpenIDInfo info = new OpenIDInfo();
		info.setEmail(username);
		info.setFullName(fullName);
		
		service.processOpenIDInfo(info, null, true, OriginatingClient.SYNAPSE);
		
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
		info.setEmail(username);
		info.setFullName(fullName);
		
		service.processOpenIDInfo(info, true, false, OriginatingClient.SYNAPSE);
		
		// User should not be created, ToU should be updated
		verify(mockUserManager, times(0)).createUser(any(NewUser.class));
		verify(mockAuthenticationManager).authenticate(eq(username), eq((String) null));
		verify(mockUserManager).getUserInfo(anyString());
		verify(mockAuthenticationManager).setTermsOfUseAcceptance(eq("" + userId), eq(true));
	}
	
}
