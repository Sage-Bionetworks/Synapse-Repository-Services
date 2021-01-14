package org.sagebionetworks.repo.manager;

import java.util.Collections;
import java.util.Date;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.loginlockout.UnsuccessfulLoginLockoutException;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.LockoutInfo;
import org.sagebionetworks.repo.model.auth.LoginLockoutStatusDao;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.beans.factory.annotation.Autowired;


public class UserCredentialValidatorImpl implements UserCredentialValidator {
	public static final String LOGIN_FAIL_ATTEMPT_METRIC_UNIT = "Count";

	public static final double LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE = 1.0;

	public static final String LOGIN_FAIL_ATTEMPT_METRIC_NAME = "LoginFailAttemptExceedLimit";

	// 2^18 ~= 4.369 minutes. Once users have to wait that long, report lockout
	static final long REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD = 18;

	@Autowired
	AuthenticationDAO authDAO;

	@Autowired
	LoginLockoutStatusDao lockoutDao;

	@Autowired
	private Consumer consumer;

	private void logAttemptAfterAccountIsLocked(final long userId) {
		ProfileData loginFailAttemptExceedLimit = new ProfileData();
		loginFailAttemptExceedLimit.setNamespace(this.getClass().getName());
		loginFailAttemptExceedLimit.setName(LOGIN_FAIL_ATTEMPT_METRIC_NAME);
		loginFailAttemptExceedLimit.setValue(LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE);
		loginFailAttemptExceedLimit.setUnit(LOGIN_FAIL_ATTEMPT_METRIC_UNIT);
		loginFailAttemptExceedLimit.setTimestamp(new Date());
		loginFailAttemptExceedLimit.setDimension(Collections.singletonMap("UserId", "" + userId));
		consumer.addProfileData(loginFailAttemptExceedLimit);
	}

	/**
	 * Check username, password combination
	 * 
	 * @param userId
	 * @param password
	 */
	@Override
	public boolean checkPassword(final long userId, final String password) {
		byte[] salt = authDAO.getPasswordSalt(userId);
		String passHash = PBKDF2Utils.hashPassword(password, salt);
		return authDAO.checkUserCredentials(userId, passHash);
	}

	/**
	 * We intentionally excluded a transaction annotation from this method. In most
	 * cases, this method only reads data from the database. For cases where a
	 * database write is required, a new transaction will be created for the write.
	 */
	@Override
	public boolean checkPasswordWithThrottling(final long userId, final String password) {

		LockoutInfo lockoutInfo = lockoutDao.getLockoutInfo(userId);
		if (lockoutInfo.getRemainingMillisecondsToNextLoginAttempt() > 0) {
			if (lockoutInfo.getNumberOfFailedLoginAttempts() >= REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD) {
				logAttemptAfterAccountIsLocked(userId);
			}
			/*
			 * The user is currently locked out, so they cannot attempt another login at
			 * this time.
			 */
			throw new UnsuccessfulLoginLockoutException(lockoutInfo.getRemainingMillisecondsToNextLoginAttempt(),
					lockoutInfo.getNumberOfFailedLoginAttempts());
		}

		boolean credentialsCorrect = checkPassword(userId, password);
		boolean wasPreviouslyLockedOut = lockoutInfo.getNumberOfFailedLoginAttempts() > 0;

		if (!credentialsCorrect) {
			/*
			 * When the wrong credentials are provided the lockout information is
			 * incremented.
			 */
			lockoutDao.incrementLockoutInfoWithNewTransaction(userId);
		}else if(wasPreviouslyLockedOut) {
			/*
			 * The user was previously locked out but has now correctly authenticated so a
			 * one-time reset of the previous lockout information is needed.
			 */
			lockoutDao.resetLockoutInfoWithNewTransaction(userId);
		}
		return credentialsCorrect;
	}

	@Override
	public void forceResetLoginThrottle(final long userId) {
		lockoutDao.resetLockoutInfoWithNewTransaction(userId);
	}
}
