package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.sagebionetworks.repo.manager.AuthenticationManagerImpl.*;

import org.apache.commons.lang.RandomStringUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.LockedException;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.auth.AuthenticationReceiptDAO;
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
	@Mock
	private AuthenticationReceiptDAO mockAuthReceiptDAO;
	@Mock
	private Consumer mockConsumer;
	
	final Long userId = 12345L;
//	final String username = "AuthManager@test.org";
	final String password = "gro.tset@reganaMhtuA";
	final String synapseSessionToken = "synapsesessiontoken";
	final byte[] salt = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		when(mockAuthDAO.getPasswordSalt(eq(userId))).thenReturn(salt);
		when(mockAuthDAO.changeSessionToken(eq(userId), eq((String) null))).thenReturn(synapseSessionToken);

		UserGroup ug = new UserGroup();
		ug.setId(userId.toString());
		ug.setIsIndividual(true);
		when(mockUserGroupDAO.get(userId)).thenReturn(ug);

		authManager = new AuthenticationManagerImpl();
		ReflectionTestUtils.setField(authManager, "authDAO", mockAuthDAO);
		ReflectionTestUtils.setField(authManager, "userGroupDAO", mockUserGroupDAO);
		ReflectionTestUtils.setField(authManager, "authenticationThrottleMemoryCountingSemaphore", mockUsernameThrottleGate);
		ReflectionTestUtils.setField(authManager, "authReceiptDAO", mockAuthReceiptDAO);
		ReflectionTestUtils.setField(authManager, "consumer", mockConsumer);
	}

	private void validateLoginFailAttemptMetricData(ArgumentCaptor<ProfileData> captor, Long userId) {
		ProfileData arg = captor.getValue();
		assertEquals(AuthenticationManagerImpl.class.getName(), arg.getNamespace());
		assertEquals(LOGIN_FAIL_ATTEMPT_METRIC_UNIT, arg.getUnit());
		assertEquals((Double)LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE, arg.getValue());
		assertEquals(LOGIN_FAIL_ATTEMPT_METRIC_NAME, arg.getName());
		assertEquals(userId, arg.getDimension().get("UserId"));
	}

	@Test
	public void testGetSessionToken() throws Exception {
		Session session = authManager.getSessionToken(userId);
		assertEquals(synapseSessionToken, session.getSessionToken());
		
		verify(mockAuthDAO, times(1)).getSessionTokenIfValid(eq(userId));
		verify(mockAuthDAO, times(1)).changeSessionToken(eq(userId), eq((String) null));
	}
	
	@Test
	public void testCheckSessionToken() throws Exception {
		when(mockAuthDAO.getPrincipalIfValid(eq(synapseSessionToken))).thenReturn(userId);
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(userId);
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId))).thenReturn(true);
		Long principalId = authManager.checkSessionToken(synapseSessionToken, true);
		assertEquals(userId, principalId);
		
		// Token matches, but terms haven't been signed
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId))).thenReturn(false);
		try {
			authManager.checkSessionToken(synapseSessionToken, true).toString();
			fail();
		} catch (TermsOfUseException e) { }

		// Nothing matches the token
		when(mockAuthDAO.getPrincipalIfValid(eq(synapseSessionToken))).thenReturn(null);
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(null);
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId))).thenReturn(true);
		try {
			authManager.checkSessionToken(synapseSessionToken, true).toString();
			fail();
		} catch (UnauthenticatedException e) {
			assertTrue(e.getMessage().contains("invalid"));
		}
		
		// Token matches, but has expired
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(userId);
		try {
			authManager.checkSessionToken(synapseSessionToken, true).toString();
			fail();
		} catch (UnauthenticatedException e) {
			assertTrue(e.getMessage().contains("expired"));
		}
	}
	
	@Test(expected=IllegalArgumentException.class) 
	public void testUnseeTermsOfUse() throws Exception {
		authManager.setTermsOfUseAcceptance(userId, null);
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

	@Test
	public void testLoginWithoutReceipt(){
		when(mockUsernameThrottleGate.attemptToAcquireLock(anyString(), anyLong(), anyInt())).thenReturn("lock");
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);
		authManager.login(userId, "fake password", null);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockUsernameThrottleGate).attemptToAcquireLock(""+userId, LOCK_TIMOUTE_SEC, MAX_CONCURRENT_LOCKS);
		verify(mockAuthReceiptDAO).createNewReceipt(userId);
		verify(mockUsernameThrottleGate).releaseLock(anyString(), anyString());
	}

	@Test
	public void testLoginWithInvalidReceipt(){
		when(mockUsernameThrottleGate.attemptToAcquireLock(anyString(), anyLong(), anyInt())).thenReturn("lock");
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);
		String receipt = "receipt";
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);
		authManager.login(userId, "fake password", receipt);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockUsernameThrottleGate).attemptToAcquireLock(""+userId, LOCK_TIMOUTE_SEC, MAX_CONCURRENT_LOCKS);
		verify(mockAuthReceiptDAO).createNewReceipt(userId);
		verify(mockUsernameThrottleGate).releaseLock(anyString(), anyString());
	}

	@Test
	public void testLoginWithValidReceipt(){
		when(mockUsernameThrottleGate.attemptToAcquireLock(anyString(), anyLong(), anyInt())).thenReturn("lock");
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);
		String receipt = "receipt";
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(true);
		authManager.login(userId, "fake password", receipt);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockUsernameThrottleGate, never()).attemptToAcquireLock(""+userId, LOCK_TIMOUTE_SEC, MAX_CONCURRENT_LOCKS);
		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
		verify(mockUsernameThrottleGate, never()).releaseLock(anyString(), anyString());
		verify(mockAuthReceiptDAO).replaceReceipt(userId, receipt);
	}

	@Test
	public void testLoginWithInvalidReceiptAndOverReceiptLimit(){
		when(mockUsernameThrottleGate.attemptToAcquireLock(anyString(), anyLong(), anyInt())).thenReturn("lock");
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(AUTHENTICATION_RECEIPT_LIMIT);
		String receipt = "receipt";
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);
		authManager.login(userId, "fake password", receipt);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockUsernameThrottleGate).attemptToAcquireLock(""+userId, LOCK_TIMOUTE_SEC, MAX_CONCURRENT_LOCKS);
		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
		verify(mockUsernameThrottleGate).releaseLock(anyString(), anyString());
		verify(mockAuthReceiptDAO, never()).replaceReceipt(userId, receipt);
	}

	@Test
	public void testLoginWithValidReceiptAndOverLimit(){
		when(mockUsernameThrottleGate.attemptToAcquireLock(anyString(), anyLong(), anyInt())).thenReturn("lock");
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(AUTHENTICATION_RECEIPT_LIMIT);
		String receipt = "receipt";
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(true);
		authManager.login(userId, "fake password", receipt);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockUsernameThrottleGate, never()).attemptToAcquireLock(""+userId, LOCK_TIMOUTE_SEC, MAX_CONCURRENT_LOCKS);
		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
		verify(mockUsernameThrottleGate, never()).releaseLock(anyString(), anyString());
		verify(mockAuthReceiptDAO, never()).replaceReceipt(userId, receipt);
	}

	@Test (expected=LockedException.class)
	public void testLoginWithInvalidReceiptAndOverFailAttemptLimit(){
		when(mockUsernameThrottleGate.attemptToAcquireLock(anyString(), anyLong(), anyInt())).thenReturn(null);
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(AUTHENTICATION_RECEIPT_LIMIT);
		String receipt = "receipt";
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);
		authManager.login(userId, "fake password", receipt);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockUsernameThrottleGate).attemptToAcquireLock(""+userId, LOCK_TIMOUTE_SEC, MAX_CONCURRENT_LOCKS);
		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
		verify(mockUsernameThrottleGate, never()).releaseLock(anyString(), anyString());
		verify(mockAuthReceiptDAO, never()).replaceReceipt(userId, receipt);
		ArgumentCaptor<ProfileData> captor = ArgumentCaptor.forClass(ProfileData.class);
		verify(mockConsumer).addProfileData(captor.capture());
		validateLoginFailAttemptMetricData(captor, userId);
	}

	@Test
	public void testLoginWithValidReceiptAndOverFailAttemptLimit(){
		when(mockUsernameThrottleGate.attemptToAcquireLock(anyString(), anyLong(), anyInt())).thenReturn(null);
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(AUTHENTICATION_RECEIPT_LIMIT);
		String receipt = "receipt";
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(true);
		authManager.login(userId, "fake password", receipt);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockUsernameThrottleGate, never()).attemptToAcquireLock(""+userId, LOCK_TIMOUTE_SEC, MAX_CONCURRENT_LOCKS);
		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
		verify(mockUsernameThrottleGate, never()).releaseLock(anyString(), anyString());
		verify(mockAuthReceiptDAO, never()).replaceReceipt(userId, receipt);
		verify(mockConsumer, never()).addProfileData(any(ProfileData.class));
	}
}
