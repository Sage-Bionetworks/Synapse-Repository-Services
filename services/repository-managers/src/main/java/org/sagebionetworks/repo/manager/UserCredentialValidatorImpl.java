package org.sagebionetworks.repo.manager;

import java.util.Collections;
import java.util.Date;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.loginlockout.LoginAttemptResultReporter;
import org.sagebionetworks.repo.manager.loginlockout.LoginLockoutStatus;
import org.sagebionetworks.repo.manager.loginlockout.UnsuccessfulLoginLockoutException;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * (PLFM-5355)
 * This is in its own class instead of being a part of AuthenticationManagerImpl because:
 *
 *
 * Methods that have Spring's Advice annotations (such as our DB Transaction annotations)
 * only work when called from an outside class because Spring proxies our classes.
 *
 * AuthenticationManagerImpl already uses an annotation to start a
 * Transaction for session tokens and Auth receipts
 *
 * Credential checks are in a separate class so that Spring can start a separate transaction that tracks the success/failure
 * of login attempts for users.
 *
 * If these methods were, instead, helper methods within AuthenticationManagerImpl,
 * there would only be a SINGLE transaction and any Exceptions thrown (e.g. exception stating wrong user/pass credentials)
 * in AuthenticationManagerImpl would trigger a total transaction rollback, thus discarding the tracking of login attempts.
 *
 */
public class UserCredentialValidatorImpl implements UserCredentialValidator {
	public static final String LOGIN_FAIL_ATTEMPT_METRIC_UNIT = "Count";

	public static final double LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE = 1.0;

	public static final String LOGIN_FAIL_ATTEMPT_METRIC_NAME = "LoginFailAttemptExceedLimit";

	//2^18 ~= 4.369 minutes. Once users have to wait that long, report lockout
	static final long REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD = 18;


	@Autowired
	AuthenticationDAO authDAO;

	@Autowired
	LoginLockoutStatus loginLockoutStatus;

	@Autowired
	private Consumer consumer;


	private void logAttemptAfterAccountIsLocked(final long userId) {
		ProfileData loginFailAttemptExceedLimit = new ProfileData();
		loginFailAttemptExceedLimit.setNamespace(this.getClass().getName());
		loginFailAttemptExceedLimit.setName(LOGIN_FAIL_ATTEMPT_METRIC_NAME);
		loginFailAttemptExceedLimit.setValue(LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE);
		loginFailAttemptExceedLimit.setUnit(LOGIN_FAIL_ATTEMPT_METRIC_UNIT);
		loginFailAttemptExceedLimit.setTimestamp(new Date());
		loginFailAttemptExceedLimit.setDimension(Collections.singletonMap("UserId", ""+userId));
		consumer.addProfileData(loginFailAttemptExceedLimit);
	}

	/**
	 * Check username, password combination
	 *  @param userId
	 * @param password
	 */
	@Override
	public boolean checkPassword(final long userId, final String password) {
		byte[] salt = authDAO.getPasswordSalt(userId);
		String passHash = PBKDF2Utils.hashPassword(password, salt);
		return authDAO.checkUserCredentials(userId, passHash);
	}

	@Override
	@NewWriteTransaction
	public boolean checkPasswordWithThrottling(final long userId, final String password){
		LoginAttemptResultReporter loginAttemptReporter;
		try {
			loginAttemptReporter = loginLockoutStatus.checkIsLockedOut(userId);
		} catch (UnsuccessfulLoginLockoutException e){
			//log to cloudwatch and rethrow exception if too many consecutive unsuccessful logins.
			if (e.getNumFailedAttempts() >= REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD){
				logAttemptAfterAccountIsLocked(userId);
			}
			throw e;
		}

		boolean credentialsCorrect = checkPassword(userId, password);
		// check credentials and report success/failure of check
		if(credentialsCorrect) {
			loginAttemptReporter.reportSuccess();
		} else {
			loginAttemptReporter.reportFailure();
		}

		return credentialsCorrect;
	}

	@Override
	public void forceResetLoginThrottle(final long userId){
		loginLockoutStatus.forceResetLockoutCount(userId);
	}
}
