package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.UserCredentialValidatorImpl.LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE;
import static org.sagebionetworks.repo.manager.UserCredentialValidatorImpl.LOGIN_FAIL_ATTEMPT_METRIC_NAME;
import static org.sagebionetworks.repo.manager.UserCredentialValidatorImpl.LOGIN_FAIL_ATTEMPT_METRIC_UNIT;
import static org.sagebionetworks.repo.manager.UserCredentialValidatorImpl.REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.loginlockout.LoginAttemptResultReporter;
import org.sagebionetworks.repo.manager.loginlockout.LoginLockoutStatus;
import org.sagebionetworks.repo.manager.loginlockout.UnsuccessfulLoginLockoutException;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.securitytools.PBKDF2Utils;

@RunWith(MockitoJUnitRunner.class)
public class UserCredentialValidatorImplTest {

	@Mock
	private LoginAttemptResultReporter mockLoginAttemptResultReporter;
	@Mock
	private LoginLockoutStatus mockLoginLockoutStatus;
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
		when(mockLoginLockoutStatus.checkIsLockedOut(userId)).thenReturn(mockLoginAttemptResultReporter);
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
	public void testCheckPasswordWithThrottling_IsLockedOut_lessThanLogThreshold(){
		when(mockLoginLockoutStatus.checkIsLockedOut(userId)).thenThrow(mockUnsuccessfulLoginLockoutException);
		when(mockUnsuccessfulLoginLockoutException.getNumFailedAttempts()).thenReturn(REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD - 1);

		try{
			//method under test
			authenticationManagerUtil.checkPasswordWithThrottling(userId, password);
			fail("expected exception to be thrown");
		} catch (UnsuccessfulLoginLockoutException e) {
			//expected
		}

		verify(mockLoginLockoutStatus).checkIsLockedOut(userId);
		verifyZeroInteractions(mockLoginAttemptResultReporter);
		verifyZeroInteractions(mockConsumer);
	}

	@Test
	public void testCheckPasswordWithThrottling_IsLockedOut_GreaterThanEqualLogThreshold(){
		when(mockLoginLockoutStatus.checkIsLockedOut(userId)).thenThrow(mockUnsuccessfulLoginLockoutException);
		when(mockUnsuccessfulLoginLockoutException.getNumFailedAttempts()).thenReturn(REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD);

		try{
			//method under test
			authenticationManagerUtil.checkPasswordWithThrottling(userId, password);
			fail("expected exception to be thrown");
		} catch (UnsuccessfulLoginLockoutException e) {
			//expected
		}

		verify(mockLoginLockoutStatus).checkIsLockedOut(userId);
		verifyZeroInteractions(mockLoginAttemptResultReporter);
		ArgumentCaptor<ProfileData> captor = ArgumentCaptor.forClass(ProfileData.class);
		verify(mockConsumer).addProfileData(captor.capture());
		validateLoginFailAttemptMetricData(captor, userId);
	}

	@Test
	public void checkPasswordWithThrottling_RightPassword(){
		when(mockAuthDAO.checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt))).thenReturn(true);

		//method under test
		assertTrue(authenticationManagerUtil.checkPasswordWithThrottling(userId, password));

		verify(mockAuthDAO).checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt));
		verify(mockLoginAttemptResultReporter).reportSuccess();
		verify(mockLoginLockoutStatus).checkIsLockedOut(userId);
		verifyZeroInteractions(mockConsumer);
	}

	@Test
	public void checkPasswordWithThrottling_WrongPassword(){
		when(mockAuthDAO.checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt))).thenReturn(false);

		//method under test
		assertFalse(authenticationManagerUtil.checkPasswordWithThrottling(userId, password));

		verify(mockAuthDAO).checkUserCredentials(userId, PBKDF2Utils.hashPassword(password, salt));
		verify(mockLoginAttemptResultReporter).reportFailure();
		verify(mockLoginLockoutStatus).checkIsLockedOut(userId);
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