package org.sagebionetworks.repo.model;

import java.util.Objects;

public class UnsuccessfulLoginLockoutDTO {
	private final long userId;
	private	long unsuccessfulLoginCount;
	private long lockoutExpiration;


	public UnsuccessfulLoginLockoutDTO(final long userId){
		this.userId = userId;
	}

	/**
	 * Id of the user for which this lockout applies
	 * @return
	 */
	public long getUserId() {
		return userId;
	}

	/**
	 * Number of consecutive unsuccessful login attempts. This is usually used to derive a new lockout expiration on
	 * subsequent unsuccessful login attempts.
	 */
	public long getUnsuccessfulLoginCount() {
		return unsuccessfulLoginCount;
	}

	/**
	 * Number of consecutive unsuccessful login attempts. This is usually used to derive a new lockout expiration on
	 * subsequent unsuccessful login attempts.
	 * @param unsuccessfulLoginCount
	 */
	public void setUnsuccessfulLoginCount(long unsuccessfulLoginCount) {
		this.unsuccessfulLoginCount = unsuccessfulLoginCount;
	}

	/**
	 * Number of consecutive unsuccessful login attempts. This is usually used to derive a new lockout expiration on
	 * subsequent unsuccessful login attempts.
	 * @param unsuccessfulLoginCount
	 */
	public UnsuccessfulLoginLockoutDTO withUnsuccessfulLoginCount(long unsuccessfulLoginCount) {
		setUnsuccessfulLoginCount(unsuccessfulLoginCount);
		return this;
	}

	/**
	 * Unix timestamp, in milliseconds of when lockout should expire
	 * @return
	 */
	public long getLockoutExpiration() {
		return lockoutExpiration;
	}

	/**
	 * Unix timestamp, in milliseconds of when lockout should expire
	 * @return
	 */
	public void setLockoutExpiration(long lockoutExpiration) {
		this.lockoutExpiration = lockoutExpiration;
	}

	/**
	 * Unix timestamp, in milliseconds of when lockout should expire
	 * @return
	 */
	public UnsuccessfulLoginLockoutDTO withLockoutExpiration(long lockoutExpiration) {
		setLockoutExpiration(lockoutExpiration);
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UnsuccessfulLoginLockoutDTO dto = (UnsuccessfulLoginLockoutDTO) o;
		return userId == dto.userId &&
				unsuccessfulLoginCount == dto.unsuccessfulLoginCount &&
				lockoutExpiration == dto.lockoutExpiration;
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, unsuccessfulLoginCount, lockoutExpiration);
	}
}
