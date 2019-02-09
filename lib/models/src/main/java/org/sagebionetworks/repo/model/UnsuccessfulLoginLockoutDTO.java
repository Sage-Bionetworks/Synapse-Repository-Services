package org.sagebionetworks.repo.model;

public class UnsuccessfulLoginLockoutDTO {
	private long userId;
	private	long unsuccessfulLoginCount;
	private long lockoutExpiration;

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getUnsuccessfulLoginCount() {
		return unsuccessfulLoginCount;
	}

	public void setUnsuccessfulLoginCount(long unsuccessfulLoginCount) {
		this.unsuccessfulLoginCount = unsuccessfulLoginCount;
	}

	public long getLockoutExpiration() {
		return lockoutExpiration;
	}

	public void setLockoutExpiration(long lockoutExpiration) {
		this.lockoutExpiration = lockoutExpiration;
	}
}
