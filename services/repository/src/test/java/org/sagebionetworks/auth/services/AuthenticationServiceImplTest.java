package org.sagebionetworks.auth.services;

import static org.junit.Assert.assertEquals;

import java.net.URLEncoder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UserInfo;

public class AuthenticationServiceImplTest {

	AuthenticationServiceImpl service = new AuthenticationServiceImpl();
	UserManager mockUserManager;
	@Before
	public void setUp() throws Exception {
		mockUserManager = mock(UserManager.class);
		UserInfo testUserInfo = new UserInfo(false);
		when(mockUserManager.getUserInfo(anyString())).thenReturn(testUserInfo);
		service.setUserManager(mockUserManager);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test 
	public void testUpdateEmail() throws Exception {
		service.updateEmail("oldEmail@sagebase.org", "newEmail@sagebase.org");
		//it queried for the user
		verify(mockUserManager).getUserInfo(anyString());
		//and used the user manager to update the email address
		verify(mockUserManager).updateEmail(any(UserInfo.class), anyString());
	}
}
