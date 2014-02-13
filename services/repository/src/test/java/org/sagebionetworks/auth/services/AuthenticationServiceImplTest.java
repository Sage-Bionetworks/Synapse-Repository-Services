package org.sagebionetworks.auth.services;

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
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;

public class AuthenticationServiceImplTest {

	private AuthenticationServiceImpl service;
	
	private UserManager mockUserManager;
	private AuthenticationManager mockAuthenticationManager;
	private MessageManager mockMessageManager;
	
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
		when(mockUserManager.getUserInfo(eq(userId))).thenReturn(userInfo);
		when(mockUserManager.createUser(any(NewUser.class))).thenReturn(userId);
		
		mockAuthenticationManager = Mockito.mock(AuthenticationManager.class);
		when(mockAuthenticationManager.checkSessionToken(eq(sessionToken), eq(DomainType.SYNAPSE), eq(true)))
				.thenReturn(userId);
		
		mockMessageManager = Mockito.mock(MessageManager.class);
		
		service = new AuthenticationServiceImpl(mockUserManager, mockAuthenticationManager, mockMessageManager);
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
	
	
}
