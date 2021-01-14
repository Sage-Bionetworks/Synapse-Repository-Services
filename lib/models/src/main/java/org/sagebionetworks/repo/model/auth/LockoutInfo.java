package org.sagebionetworks.repo.model.auth;

import java.util.Objects;

/**
 * Information about the current lockout status for a given user.
 * 
 *
 */
public class LockoutInfo {

	/**
	 * The number of failed login attempts for this user since the last successful
	 * login.
	 */
	private Long numberOfFailedLoginAttempts;

	/**
	 * The number of milliseconds remaining until the user can attempt another
	 * login. If this is one or greater then the user is currently locked out and
	 * may not login.
	 */
	private Long remainingMillisecondsToNextLoginAttempt;

	/**
	 * @return the numberOfFailedLoginAttempts
	 */
	public Long getNumberOfFailedLoginAttempts() {
		return numberOfFailedLoginAttempts;
	}

	/**
	 * @param numberOfFailedLoginAttempts the numberOfFailedLoginAttempts to set
	 */
	public LockoutInfo withNumberOfFailedLoginAttempts(Long numberOfFailedLoginAttempts) {
		this.numberOfFailedLoginAttempts = numberOfFailedLoginAttempts;
		return this;
	}

	/**
	 * @return the remainingMillisecondsToNextLoginAttempt
	 */
	public Long getRemainingMillisecondsToNextLoginAttempt() {
		return remainingMillisecondsToNextLoginAttempt;
	}

	/**
	 * @param remainingMillisecondsToNextLoginAttempt the remainingMillisecondsToNextLoginAttempt to set
	 */
	public LockoutInfo withRemainingMillisecondsToNextLoginAttempt(Long remainingMillisecondsToNextLoginAttempt) {
		this.remainingMillisecondsToNextLoginAttempt = remainingMillisecondsToNextLoginAttempt;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(numberOfFailedLoginAttempts, remainingMillisecondsToNextLoginAttempt);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof LockoutInfo)) {
			return false;
		}
		LockoutInfo other = (LockoutInfo) obj;
		return Objects.equals(numberOfFailedLoginAttempts, other.numberOfFailedLoginAttempts) && Objects
				.equals(remainingMillisecondsToNextLoginAttempt, other.remainingMillisecondsToNextLoginAttempt);
	}

	@Override
	public String toString() {
		return "LockoutInfo [numberOfFailedLoginAttempts=" + numberOfFailedLoginAttempts
				+ ", remainingMillisecondsToNextLoginAttempt=" + remainingMillisecondsToNextLoginAttempt + "]";
	}

}
