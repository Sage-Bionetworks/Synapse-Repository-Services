package org.sagebionetworks.repo.model.dbo.dao;

public interface UnsuccessfulAttemptLockoutDAO {
	public long incrementNumFailedAttempts(String key);
	public void setExpiration(String key, long expirationInSeconds);
	public void removeLockout(String key);
	public Long isLockedOut(String key);
}
