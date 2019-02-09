package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.UserCredentialValidatorImpl.LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE;
import static org.sagebionetworks.repo.manager.UserCredentialValidatorImpl.LOGIN_FAIL_ATTEMPT_METRIC_NAME;
import static org.sagebionetworks.repo.manager.UserCredentialValidatorImpl.LOGIN_FAIL_ATTEMPT_METRIC_UNIT;
import static org.sagebionetworks.repo.manager.UserCredentialValidatorImpl.REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD;
import static org.sagebionetworks.repo.manager.UserCredentialValidatorImpl.UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.loginlockout.AttemptResultReporter;
import org.sagebionetworks.repo.manager.loginlockout.UnsuccessfulLoginLockout;
import org.sagebionetworks.repo.manager.loginlockout.UnsuccessfulLoginLockoutException;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.securitytools.PBKDF2Utils;

@RunWith(MockitoJUnitRunner.class)
public class UserCredentialValidatorImplTest {

	@Mock
	private AttemptResultReporter mockAttemptResultReporter;
	@Mock
	private UnsuccessfulLoginLockout mockUnsuccessfulLoginLockout;
	@Mock
	private AuthenticationDAO mockAuthDAO;
	@Mock
	private Consumer mockConsumer;

	@Mock
	private UnsuccessfulLoginLockoutException mockUnsuccessfulLoginLockoutException;

	@InjectMocks
	private UserCredentialValidatorImpl authenticationManagerUtil;

	final Long userId = 12345L;
	final byte[] salt = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	final String password = "gro.tset@reganaMhtuA";


	@Before
	public void setUp() throws Exception {
		when(mockAuthDAO.getPasswordSalt(eq(userId))).thenReturn(salt);


		when(mockAuthDAO.checkUserCredentials(anyLong(), anyString())).thenReturn(true);
		when(mockUnsuccessfulLoginLockout.checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId)).thenReturn(mockAttemptResultReporter);
	}

	@Test
	public void testCheckPassword_RightPassword(){
		when(mockAuthDAO.checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt))).thenReturn(true);

		//method under test
		assertTrue(authenticationManagerUtil.checkPassword(userId, password));

		verify(mockAuthDAO).checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt));

	}

	@Test
	public void testCheckPassword_WrongPassword(){
		when(mockAuthDAO.checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt))).thenReturn(false);
		//method under test
		assertFalse(authenticationManagerUtil.checkPassword(userId, password));

		verify(mockAuthDAO).checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt));
	}

	@Test
	public void testCheckPasswordWithLock_IsLockedOut_lessThanLogThreshold(){
		when(mockUnsuccessfulLoginLockout.checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId)).thenThrow(mockUnsuccessfulLoginLockoutException);
		when(mockUnsuccessfulLoginLockoutException.getNumFailedAttempts()).thenReturn(REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD - 1);

		try{
			//method under test
			authenticationManagerUtil.checkPasswordWithLock(userId, password);
			fail("expected exception to be thrown");
		} catch (UnsuccessfulLoginLockoutException e) {
			//expected
		}

		verify(mockUnsuccessfulLoginLockout).checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId);
		verifyZeroInteractions(mockAttemptResultReporter);
		verifyZeroInteractions(mockConsumer);
	}

	@Test
	public void testCheckPasswordWithLock_IsLockedOut_GreaterThanEqualLogThreshold(){
		when(mockUnsuccessfulLoginLockout.checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId)).thenThrow(mockUnsuccessfulLoginLockoutException);
		when(mockUnsuccessfulLoginLockoutException.getNumFailedAttempts()).thenReturn(REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD);

		try{
			//method under test
			authenticationManagerUtil.checkPasswordWithLock(userId, password);
			fail("expected exception to be thrown");
		} catch (UnsuccessfulLoginLockoutException e) {
			//expected
		}

		verify(mockUnsuccessfulLoginLockout).checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId);
		verifyZeroInteractions(mockAttemptResultReporter);
		ArgumentCaptor<ProfileData> captor = ArgumentCaptor.forClass(ProfileData.class);
		verify(mockConsumer).addProfileData(captor.capture());
		validateLoginFailAttemptMetricData(captor, userId);
	}

	@Test
	public void checkPasswordWithLock_RightPassword(){
		when(mockAuthDAO.checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt))).thenReturn(true);

		//method under test
		assertTrue(authenticationManagerUtil.checkPasswordWithLock(userId, password));

		verify(mockAuthDAO).checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt));
		verify(mockAttemptResultReporter).reportSuccess();
		verify(mockUnsuccessfulLoginLockout).checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId);
		verifyZeroInteractions(mockConsumer);
	}

	@Test
	public void checkPasswordWithLock_WrongPassword(){
		when(mockAuthDAO.checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt))).thenReturn(false);

		//method under test
		assertFalse(authenticationManagerUtil.checkPasswordWithLock(userId, password));

		verify(mockAuthDAO).checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt));
		verify(mockAttemptResultReporter).reportFailure();
		verify(mockUnsuccessfulLoginLockout).checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + userId);
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
}