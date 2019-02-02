package org.sagebionetworks.repo.model.dbo.dao;

import java.util.StringJoiner;

public interface UnsuccessfulAttemptLockoutDAO {
	public long incrementNumFailedAttempts(String key);
	public long	setExpiration(String key, long expirationInSeconds);
	public void removeLockout(String key);
	public Long isLockedOut(String key);
}
