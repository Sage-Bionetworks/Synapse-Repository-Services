package org.sagebionetworks.repo.model;

public interface UnsuccessfulAttemptLockoutDAO {
	public long incrementNumFailedAttempts(String key);
	public long	setExpiration(long );
}
