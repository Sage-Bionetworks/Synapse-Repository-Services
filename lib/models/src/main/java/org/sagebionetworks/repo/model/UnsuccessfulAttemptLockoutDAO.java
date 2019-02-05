package org.sagebionetworks.repo.model;

public interface UnsuccessfulAttemptLockoutDAO {
	public long incrementNumFailedAttempts(String key);
	public long getNumFailedAttempts(String key);
	public void setExpiration(String key, long expirationSecondsFromNow);
	public void removeLockout(String key);
	public Long getLockoutExpirationTimestamp(String key);
}
