package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.sagebionetworks.repo.manager.AuthenticationManagerImpl.*;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.password.InvalidPasswordException;
import org.sagebionetworks.repo.manager.password.PasswordValidatorImpl;
import org.sagebionetworks.repo.manager.unsuccessfulattemptlockout.AttemptResultReporter;
import org.sagebionetworks.repo.manager.unsuccessfulattemptlockout.UnsuccessfulAttemptLockout;
import org.sagebionetworks.repo.manager.unsuccessfulattemptlockout.UnsuccessfulAttemptLockoutException;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.LockedException;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnsuccessfulAttemptLockoutDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.auth.AuthenticationReceiptDAO;
import org.sagebionetworks.repo.model.semaphore.MemoryCountingSemaphore;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationManagerImplUnitTest {

	@InjectMocks
	private AuthenticationManagerImpl authManager;
	@Mock
	private AuthenticationDAO mockAuthDAO;
	@Mock
	private UserGroupDAO mockUserGroupDAO;
	@Mock
	private AuthenticationReceiptDAO mockAuthReceiptDAO;
	@Mock
	private Consumer mockConsumer;
	@Mock
	private PasswordValidatorImpl mockPassswordValidator;
	@Mock
	private UnsuccessfulAttemptLockout mockUnsuccessfulAttemptLockout;
	@Mock
	private AttemptResultReporter mockAttemptResultReporter;

	@Mock
	private UnsuccessfulAttemptLockoutException mockUnsuccessfulAttemptLockoutException;

	final Long userId = 12345L;
	//	final String username = "AuthManager@test.org";
	final String password = "gro.tset@reganaMhtuA";
	final String synapseSessionToken = "synapsesessiontoken";
	final byte[] salt = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		when(mockAuthDAO.getPasswordSalt(eq(userId))).thenReturn(salt);
		when(mockAuthDAO.changeSessionToken(eq(userId), eq((String) null))).thenReturn(synapseSessionToken);

		UserGroup ug = new UserGroup();
		ug.setId(userId.toString());
		ug.setIsIndividual(true);
		when(mockUserGroupDAO.get(userId)).thenReturn(ug);
		when(mockUnsuccessfulAttemptLockout.checkIsLockedOut(anyString())).thenReturn(mockAttemptResultReporter);
		when(mockAuthDAO.checkUserCredentials(anyLong(), anyString())).thenReturn(true);
	}
	//TODO: UNCOMMENT AND MOVE OUT

//	private void validateLoginFailAttemptMetricData(ArgumentCaptor<ProfileData> captor, Long userId) {
//		ProfileData arg = captor.getValue();
//		assertEquals(AuthenticationManagerImpl.class.getName(), arg.getNamespace());
//		assertEquals(LOGIN_FAIL_ATTEMPT_METRIC_UNIT, arg.getUnit());
//		assertEquals((Double) LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE, arg.getValue());
//		assertEquals(LOGIN_FAIL_ATTEMPT_METRIC_NAME, arg.getName());
//		assertEquals(userId.toString(), arg.getDimension().get("UserId"));
//	}

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
		} catch (TermsOfUseException e) {
		}

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

	@Test(expected = IllegalArgumentException.class)
	public void testUnseeTermsOfUse() throws Exception {
		authManager.setTermsOfUseAcceptance(userId, null);
	}

	@Test
	public void testChangePasswordWithInvalidPassword() {
		String bannedPassword = "password123";
		doThrow(InvalidPasswordException.class).when(mockPassswordValidator).validatePassword(bannedPassword);
		try {
			authManager.changePassword(userId, bannedPassword);
		} catch (InvalidPasswordException e) {
			verify(mockPassswordValidator).validatePassword(bannedPassword);
			verify(mockAuthDAO, never()).changePassword(anyLong(), anyString());
		}
	}

	@Test
	public void testChangePasswordWithValidPassword() {
		String validPassword = UUID.randomUUID().toString();
		authManager.changePassword(userId, validPassword);
		verify(mockPassswordValidator).validatePassword(validPassword);
		verify(mockAuthDAO).changePassword(anyLong(), anyString());
	}
//
//	@Test
//	public void testLoginWithoutReceipt() {
//		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);
//		authManager.login(userId, "fake password", null);
//		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
//		verify(mockUnsuccessfulAttemptLockout).checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId);
//		verify(mockAttemptResultReporter).reportSuccess();
//		verify(mockAuthReceiptDAO).createNewReceipt(userId);
//		verify(mockAuthReceiptDAO, never()).replaceReceipt(anyLong(), anyString());
//	}
//
//	@Test
//	public void testLoginWithInvalidReceipt() {
//		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);
//		String receipt = "receipt";
//		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);
//		authManager.login(userId, "fake password", receipt);
//		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
//		verify(mockUnsuccessfulAttemptLockout).checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId);
//		verify(mockAttemptResultReporter).reportSuccess();
//		verify(mockAuthReceiptDAO).createNewReceipt(userId);
//		verify(mockAuthReceiptDAO, never()).replaceReceipt(anyLong(), anyString());
//	}
//
//	@Test
//	public void testLoginWithInvalidReceiptAndWrongPassword() {
//		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);
//		when(mockAuthDAO.checkUserCredentials(anyLong(), anyString())).thenReturn(false);
//		String receipt = "receipt";
//		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);
//		try {
//			authManager.login(userId, "fake password", receipt);
//			fail("expected exception to be thrown");
//		} catch (UnauthenticatedException e) {
//			//expected the exception to be thrown
//		}
//		verify(mockAttemptResultReporter).reportFailure();
//
//		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
//		verify(mockUnsuccessfulAttemptLockout).checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId);
//		verify(mockAuthReceiptDAO, never()).createNewReceipt(anyLong());
//		verify(mockAuthReceiptDAO, never()).replaceReceipt(anyLong(), anyString());
//
//	}
//
//	@Test
//	public void testLoginWithValidReceipt() {
//		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);
//		String receipt = "receipt";
//		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(true);
//		authManager.login(userId, "fake password", receipt);
//		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
//		verifyZeroInteractions(mockUnsuccessfulAttemptLockout);
//		verifyZeroInteractions(mockAttemptResultReporter);
//		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
//		verify(mockAuthReceiptDAO).replaceReceipt(userId, receipt);
//	}
//
//	@Test
//	public void testLoginWithInvalidReceiptAndOverReceiptLimit() {
//		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(AUTHENTICATION_RECEIPT_LIMIT);
//		String receipt = "receipt";
//		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);
//		authManager.login(userId, "fake password", receipt);
//		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
//		verify(mockUnsuccessfulAttemptLockout).checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId);
//		verify(mockAttemptResultReporter).reportSuccess();
//		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
//		verify(mockAuthReceiptDAO, never()).replaceReceipt(userId, receipt);
//	}
//
//	@Test
//	public void testLoginWithInvalidReceiptAndWithinLockoutPeriod() {
//		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(AUTHENTICATION_RECEIPT_LIMIT);
//		when(mockUnsuccessfulAttemptLockout.checkIsLockedOut(anyString())).thenThrow(mockUnsuccessfulAttemptLockoutException);
//		when(mockUnsuccessfulAttemptLockoutException.getNumFailedAttempts()).thenReturn(REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD);
//		String receipt = "receipt";
//		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);
//		try {
//			authManager.login(userId, "fake password", receipt);
//			fail("expected exception to be thrown");
//		} catch (UnsuccessfulAttemptLockoutException e) {
//			//expected
//		}
//		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
//		verify(mockUnsuccessfulAttemptLockout).checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId);
//		verifyZeroInteractions(mockAttemptResultReporter);
//		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
//		verify(mockAuthReceiptDAO, never()).replaceReceipt(userId, receipt);
//		ArgumentCaptor<ProfileData> captor = ArgumentCaptor.forClass(ProfileData.class);
//		verify(mockConsumer).addProfileData(captor.capture());
//		validateLoginFailAttemptMetricData(captor, userId);
//	}
//
//	@Test
//	public void testLoginWithInvalidReceiptAndWithinLockoutPeriodBelowCloudwatchReportThreshold() {
//		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(AUTHENTICATION_RECEIPT_LIMIT);
//		when(mockUnsuccessfulAttemptLockout.checkIsLockedOut(anyString())).thenThrow(mockUnsuccessfulAttemptLockoutException);
//		when(mockUnsuccessfulAttemptLockoutException.getNumFailedAttempts()).thenReturn(REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD - 1);
//
//		String receipt = "receipt";
//		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);
//		try {
//			authManager.login(userId, "fake password", receipt);
//			fail("expected exception to be thrown");
//		} catch (UnsuccessfulAttemptLockoutException e) {
//			//expected
//		}
//		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
//		verify(mockUnsuccessfulAttemptLockout).checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId);
//		verifyZeroInteractions(mockAttemptResultReporter);
//		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
//		verify(mockAuthReceiptDAO, never()).replaceReceipt(userId, receipt);
//		//verify never logged on cloudwatch
//		verifyZeroInteractions(mockConsumer);
//	}

}
