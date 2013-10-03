package org.sagebionetworks.repo.manager;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.securitytools.PBKDF2Utils;

public class AuthenticationManagerImplUnitTest {
	
	private AuthenticationManager authManager;
	private AuthenticationDAO authDAO;
	private UserGroupDAO userGroupDAO;
	
	final String userId = "12345";
	final String username = "AuthManager@test.org";
	final String password = "gro.tset@reganaMhtuA";
	final String sessionToken = "qwertyuiop";
	final byte[] salt = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	
	@Before
	public void setUp() throws Exception {
		authDAO = Mockito.mock(AuthenticationDAO.class);
		Mockito.when(authDAO.getPasswordSalt(Mockito.eq(username))).thenReturn(salt);
		Mockito.when(authDAO.changeSessionToken(Mockito.eq(userId), Mockito.eq((String) null))).thenReturn(sessionToken);
		
		userGroupDAO = Mockito.mock(UserGroupDAO.class);
		UserGroup ug = new UserGroup();
		ug.setId(userId);
		Mockito.when(userGroupDAO.findGroup(Mockito.eq(username), Mockito.eq(true))).thenReturn(ug);
		
		authManager = new AuthenticationManagerImpl(authDAO, userGroupDAO);
	}

	@Test
	public void testAuthenticateWithPassword() throws Exception {
		Session session = authManager.authenticate(username, password);
		Assert.assertEquals(sessionToken, session.getSessionToken());
		
		String passHash = PBKDF2Utils.hashPassword(password, salt);
		Mockito.verify(authDAO, Mockito.times(1)).getPasswordSalt(Mockito.eq(username));
		Mockito.verify(authDAO, Mockito.times(1)).checkEmailAndPassword(Mockito.eq(username), Mockito.eq(passHash));
	}

	@Test
	public void testAuthenticateWithoutPassword() throws Exception {
		Session session = authManager.authenticate(username, null);
		Assert.assertEquals(sessionToken, session.getSessionToken());
		
		Mockito.verify(authDAO, Mockito.never()).getPasswordSalt(Mockito.any(String.class));
		Mockito.verify(authDAO, Mockito.never()).checkEmailAndPassword(Mockito.any(String.class), Mockito.any(String.class));
	}

	@Test
	public void testGetSessionToken() throws Exception {
		Session session = authManager.getSessionToken(username);
		Assert.assertEquals(sessionToken, session.getSessionToken());
		
		Mockito.verify(authDAO, Mockito.times(1)).getSessionTokenIfValid(Mockito.eq(username));
		Mockito.verify(userGroupDAO, Mockito.times(1)).findGroup(Mockito.eq(username), Mockito.eq(true));
		Mockito.verify(authDAO, Mockito.times(1)).changeSessionToken(Mockito.eq(userId), Mockito.eq((String) null));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCheckSessionToken() throws Exception {
		Mockito.when(authDAO.getPrincipalIfValid(Mockito.eq(sessionToken))).thenReturn(Long.parseLong(userId));
		String principalId = authManager.checkSessionToken(sessionToken).toString();
		Assert.assertEquals(userId, principalId);

		Mockito.when(authDAO.getPrincipalIfValid(Mockito.eq(sessionToken))).thenReturn(null);
		authManager.checkSessionToken(sessionToken).toString();
	}
}
