package org.sagebionetworks.repo.model;

public interface UnsuccessfulLoginLockoutDAO {

	/**
	 * Increments the number of failed attempts by 1 and returns the new value.
	 * @param key identifies the lockout
	 * @return the new number of failed attempts after incrementing by 1
	 */
	public long incrementNumFailedAttempts(String key);

	/**
	 * Get current number of failed attempts for this lockout
	 * @param key identifies the lockout
	 * @return number of failed attempts for this lockout
	 */
	public long getNumFailedAttempts(String key);

	/**
	 * Set a new expiration for the lockout of the given key
	 * @param key identifies the lockout
	 * @param expirationMillisecondsFromNow Milliseconds from the current timestamp, after which the lockout will expire.
	 */
	public void setExpiration(String key, long expirationMillisecondsFromNow);

	/**
	 * Removes the lockout for the specific key
	 * @param key identifies the lockout
	 */
	public void removeLockout(String key);

	/**
	 * Gets the Unix Timestamp (in milliseconds) for when the lockout of the key will expire
	 * @param key identifies the lockout
	 * @return null if the key's lockout has expired or does have a lockout at all
	 * a Long representing the Unix timestamp (in seconds) of when the lockout will expire
	 */
	public Long getUnexpiredLockoutTimestampMillis(String key);

	void truncateTable();
}
