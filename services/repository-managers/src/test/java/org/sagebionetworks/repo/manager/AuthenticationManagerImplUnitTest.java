package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.AuthenticationManagerImpl.*;

import org.apache.commons.lang.RandomStringUtils;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.LockedException;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.semaphore.MemoryCountingSemaphore;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.test.util.ReflectionTestUtils;

public class AuthenticationManagerImplUnitTest {

	private AuthenticationManager authManager;
	@Mock
	private AuthenticationDAO mockAuthDAO;
	@Mock
	private UserGroupDAO mockUserGroupDAO;
	@Mock
	private MemoryCountingSemaphore mockUsernameThrottleGate;
	
	final Long userId = 12345L;
//	final String username = "AuthManager@test.org";
	final String password = "gro.tset@reganaMhtuA";
	final String synapseSessionToken = "synapsesessiontoken";
	final byte[] salt = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		when(mockAuthDAO.getPasswordSalt(eq(userId))).thenReturn(salt);
		when(mockAuthDAO.changeSessionToken(eq(userId), eq((String) null), eq(DomainType.SYNAPSE))).thenReturn(synapseSessionToken);

		UserGroup ug = new UserGroup();
		ug.setId(userId.toString());
		ug.setIsIndividual(true);
		when(mockUserGroupDAO.get(userId)).thenReturn(ug);

		authManager = new AuthenticationManagerImpl();
		ReflectionTestUtils.setField(authManager, "authDAO", mockAuthDAO);
		ReflectionTestUtils.setField(authManager, "userGroupDAO", mockUserGroupDAO);
		ReflectionTestUtils.setField(authManager, "usernameThrottleGate", mockUsernameThrottleGate);
	}

	@Test
	public void testAuthenticateWithPassword() throws Exception {
		when(mockUsernameThrottleGate.attemptToAcquireLock(anyString(), anyLong(), anyInt())).thenReturn("fake token");
		Session session = authManager.authenticate(userId, password, DomainType.SYNAPSE);
		assertEquals(synapseSessionToken, session.getSessionToken());
		
		String passHash = PBKDF2Utils.hashPassword(password, salt);
		verify(mockAuthDAO, times(1)).getPasswordSalt(eq(userId));
		verify(mockAuthDAO, times(1)).checkUserCredentials(eq(userId), eq(passHash));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testAuthenticateWithoutPassword() throws Exception {
		authManager.authenticate(userId, null, DomainType.SYNAPSE);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testAuthenticateWithoutDomain() throws Exception {
		authManager.authenticate(userId, "password", null);
	}

	@Test
	public void testAuthenticateThrottleWithLimitAttempts() throws Exception {
		when(mockUsernameThrottleGate.attemptToAcquireLock(anyString(), anyLong(), anyInt())).thenReturn("0","1","2","3","4","5","6","7","8","9", null);
		for (int i = 0; i < MAX_CONCURRENT_LOCKS; i++) {
			authManager.authenticate(userId, "password", DomainType.SYNAPSE);
		}
	}

	@Test (expected=LockedException.class)
	public void testAuthenticateThrottleWithOverLimitAttempts() throws Exception {
		when(mockUsernameThrottleGate.attemptToAcquireLock(anyString(), anyLong(), anyInt())).thenReturn("0","1","2","3","4","5","6","7","8","9", null);
		for (int i = 0; i < MAX_CONCURRENT_LOCKS+1; i++) {
			authManager.authenticate(userId, "password", DomainType.SYNAPSE);
		}
	}

	@Test
	public void testGetSessionToken() throws Exception {
		Session session = authManager.getSessionToken(userId, DomainType.SYNAPSE);
		Assert.assertEquals(synapseSessionToken, session.getSessionToken());
		
		verify(mockAuthDAO, times(1)).getSessionTokenIfValid(eq(userId), eq(DomainType.SYNAPSE));
		verify(mockAuthDAO, times(1)).changeSessionToken(eq(userId), eq((String) null), eq(DomainType.SYNAPSE));
	}
	
	@Test
	public void testCheckSessionToken() throws Exception {
		when(mockAuthDAO.getPrincipalIfValid(eq(synapseSessionToken))).thenReturn(userId);
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(userId);
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId), eq(DomainType.SYNAPSE))).thenReturn(true);
		//when(authDAO.deriveDomainFromSessionToken(eq(sessionToken))).thenReturn(DomainType.SYNAPSE);
		Long principalId = authManager.checkSessionToken(synapseSessionToken, DomainType.SYNAPSE, true);
		Assert.assertEquals(userId, principalId);
		
		// Token matches, but terms haven't been signed
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId), eq(DomainType.SYNAPSE))).thenReturn(false);
		try {
			authManager.checkSessionToken(synapseSessionToken, DomainType.SYNAPSE, true).toString();
			fail();
		} catch (TermsOfUseException e) { }

		// Nothing matches the token
		when(mockAuthDAO.getPrincipalIfValid(eq(synapseSessionToken))).thenReturn(null);
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(null);
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId), eq(DomainType.SYNAPSE))).thenReturn(true);
		try {
			authManager.checkSessionToken(synapseSessionToken, DomainType.SYNAPSE, true).toString();
			fail();
		} catch (UnauthenticatedException e) {
			assertTrue(e.getMessage().contains("invalid"));
		}
		
		// Token matches, but has expired
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(userId);
		try {
			authManager.checkSessionToken(synapseSessionToken, DomainType.SYNAPSE, true).toString();
			fail();
		} catch (UnauthenticatedException e) {
			assertTrue(e.getMessage().contains("expired"));
		}
	}
	
	@Test(expected=IllegalArgumentException.class) 
	public void testUnseeTermsOfUse() throws Exception {
		authManager.setTermsOfUseAcceptance(userId, DomainType.SYNAPSE, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testChangePasswordWithInvalidPassword() {
		String invalidPassword = RandomStringUtils.randomAlphanumeric(PASSWORD_MIN_LENGTH-1);
		authManager.changePassword(userId, invalidPassword);
	}

	@Test
	public void testChangePasswordWithValidPassword() {
		String invalidPassword = RandomStringUtils.randomAlphanumeric(PASSWORD_MIN_LENGTH);
		authManager.changePassword(userId, invalidPassword);
		verify(mockAuthDAO).changePassword(anyLong(), anyString());
	}
}
