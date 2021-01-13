package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.UserCredentialValidatorImpl.LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE;
import static org.sagebionetworks.repo.manager.UserCredentialValidatorImpl.LOGIN_FAIL_ATTEMPT_METRIC_NAME;
import static org.sagebionetworks.repo.manager.UserCredentialValidatorImpl.LOGIN_FAIL_ATTEMPT_METRIC_UNIT;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.loginlockout.UnsuccessfulLoginLockoutException;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.LockoutInfo;
import org.sagebionetworks.repo.model.auth.LoginLockoutStatusDao;
import org.sagebionetworks.securitytools.PBKDF2Utils;

@ExtendWith(MockitoExtension.class)
public class UserCredentialValidatorImplUnitTest {

	@Mock
	private LoginLockoutStatusDao mockLoginLockoutStatusDao;
	@Mock
	private AuthenticationDAO mockAuthDAO;
	@Mock
	private Consumer mockConsumer;
	@Mock
	private UnsuccessfulLoginLockoutException mockUnsuccessfulLoginLockoutException;

	@InjectMocks
	private UserCredentialValidatorImpl validator;

	final Long userId = 12345L;
	final byte[] salt = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	final String password = "gro.tset@reganaMhtuA";


	@Test
	public void testCheckPassword_RightPassword() {
		when(mockAuthDAO.getPasswordSalt(anyLong())).thenReturn(salt);
		when(mockAuthDAO.checkUserCredentials(anyLong(), any())).thenReturn(true);

		// method under test
		assertTrue(validator.checkPassword(userId, password));

		verify(mockAuthDAO).getPasswordSalt(userId);
		verify(mockAuthDAO).checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt));

	}

	@Test
	public void testCheckPassword_WrongPassword() {
		when(mockAuthDAO.getPasswordSalt(anyLong())).thenReturn(salt);
		when(mockAuthDAO.checkUserCredentials(anyLong(), any())).thenReturn(false);
		// method under test
		assertFalse(validator.checkPassword(userId, password));

		verify(mockAuthDAO).getPasswordSalt(userId);
		verify(mockAuthDAO).checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt));
	}

	@Test
	public void testCheckPasswordWithThrottlingWithNoLock() {
		when(mockLoginLockoutStatusDao.getLockoutInfo(any())).thenReturn(
				new LockoutInfo().withNumberOfFailedLoginAttempts(0L).withRemainingMillisecondsToNextLoginAttempt(0L));
		when(mockAuthDAO.getPasswordSalt(anyLong())).thenReturn(salt);
		when(mockAuthDAO.checkUserCredentials(anyLong(), any())).thenReturn(true);

		// call under test
		boolean result = validator.checkPasswordWithThrottling(userId, password);
		assertTrue(result);

		verify(mockLoginLockoutStatusDao).getLockoutInfo(userId);
		verify(mockLoginLockoutStatusDao, never()).incrementLockoutInfoWithNewTransaction(any());
		verify(mockLoginLockoutStatusDao, never()).resetLockoutInfoWithNewTransaction(any());
		verify(mockAuthDAO).checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt));
		verifyZeroInteractions(mockConsumer);
	}

	@Test
	public void testCheckPasswordWithThrottlingWithLockedNotExpired() {
		when(mockLoginLockoutStatusDao.getLockoutInfo(any())).thenReturn(
				new LockoutInfo().withNumberOfFailedLoginAttempts(1L).withRemainingMillisecondsToNextLoginAttempt(1L));

		String message = assertThrows(UnsuccessfulLoginLockoutException.class, () -> {
			// call under test
			validator.checkPasswordWithThrottling(userId, password);
		}).getMessage();
		assertEquals("You are locked out from making any additional login attempts for 1 milliseconds", message);

		verify(mockLoginLockoutStatusDao).getLockoutInfo(userId);
		verify(mockLoginLockoutStatusDao, never()).incrementLockoutInfoWithNewTransaction(any());
		verify(mockLoginLockoutStatusDao, never()).resetLockoutInfoWithNewTransaction(any());
		verify(mockAuthDAO, never()).checkUserCredentials(anyLong(), any());
		verifyZeroInteractions(mockConsumer);
	}

	@Test
	public void testCheckPasswordWithThrottlingWithLockedNotExpiredOverReportCount() {
		when(mockLoginLockoutStatusDao.getLockoutInfo(any())).thenReturn(new LockoutInfo()
				.withNumberOfFailedLoginAttempts(
						UserCredentialValidatorImpl.REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD + 1L)
				.withRemainingMillisecondsToNextLoginAttempt(1L));

		String message = assertThrows(UnsuccessfulLoginLockoutException.class, () -> {
			// call under test
			validator.checkPasswordWithThrottling(userId, password);
		}).getMessage();
		assertEquals("You are locked out from making any additional login attempts for 1 milliseconds", message);

		verify(mockLoginLockoutStatusDao).getLockoutInfo(userId);
		verify(mockLoginLockoutStatusDao, never()).incrementLockoutInfoWithNewTransaction(any());
		verify(mockLoginLockoutStatusDao, never()).resetLockoutInfoWithNewTransaction(any());
		verify(mockAuthDAO, never()).checkUserCredentials(anyLong(), any());
		// this event should get logged.
		ArgumentCaptor<ProfileData> captor = ArgumentCaptor.forClass(ProfileData.class);
		verify(mockConsumer).addProfileData(captor.capture());
		validateLoginFailAttemptMetricData(captor, userId);
	}

	@Test
	public void testCheckPasswordWithThrottlingWithLockedExpiredAndValidCredentials() {
		// negative remaining time indicates an expired lock
		when(mockLoginLockoutStatusDao.getLockoutInfo(any())).thenReturn(
				new LockoutInfo().withNumberOfFailedLoginAttempts(1L).withRemainingMillisecondsToNextLoginAttempt(-1L));
		when(mockAuthDAO.getPasswordSalt(anyLong())).thenReturn(salt);
		when(mockAuthDAO.checkUserCredentials(anyLong(), any())).thenReturn(true);

		// call under test
		boolean result = validator.checkPasswordWithThrottling(userId, password);
		assertTrue(result);

		verify(mockLoginLockoutStatusDao).getLockoutInfo(userId);
		verify(mockLoginLockoutStatusDao, never()).incrementLockoutInfoWithNewTransaction(any());
		// the lock should get reset.
		verify(mockLoginLockoutStatusDao).resetLockoutInfoWithNewTransaction(userId);
		verify(mockAuthDAO).checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt));
		verifyZeroInteractions(mockConsumer);
	}

	@Test
	public void testCheckPasswordWithThrottlingWithLockedExpiredAndInvalidCredentials() {
		// negative remaining time indicates an expired lock
		when(mockLoginLockoutStatusDao.getLockoutInfo(any())).thenReturn(
				new LockoutInfo().withNumberOfFailedLoginAttempts(1L).withRemainingMillisecondsToNextLoginAttempt(-1L));
		when(mockAuthDAO.getPasswordSalt(anyLong())).thenReturn(salt);
		when(mockAuthDAO.checkUserCredentials(anyLong(), any())).thenReturn(false);

		// call under test
		boolean result = validator.checkPasswordWithThrottling(userId, password);
		assertFalse(result);

		verify(mockLoginLockoutStatusDao).getLockoutInfo(userId);
		// should extend the lock
		verify(mockLoginLockoutStatusDao).incrementLockoutInfoWithNewTransaction(userId);
		verify(mockLoginLockoutStatusDao, never()).resetLockoutInfoWithNewTransaction(any());
		verify(mockAuthDAO).checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt));
		verifyZeroInteractions(mockConsumer);
	}

	private void validateLoginFailAttemptMetricData(ArgumentCaptor<ProfileData> captor, Long userId) {
		ProfileData arg = captor.getValue();
		assertEquals(UserCredentialValidatorImpl.class.getName(), arg.getNamespace());
		assertEquals(LOGIN_FAIL_ATTEMPT_METRIC_UNIT, arg.getUnit());
		assertEquals((Double) LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE, arg.getValue());
		assertEquals(LOGIN_FAIL_ATTEMPT_METRIC_NAME, arg.getName());
		assertEquals(userId.toString(), arg.getDimension().get("UserId"));
	}
	
	@Test
	public void testForceResetLoginThrottle() {
		// call under test
		validator.forceResetLoginThrottle(userId);
		verify(mockLoginLockoutStatusDao).resetLockoutInfoWithNewTransaction(userId);
	}
}