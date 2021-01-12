package org.sagebionetworks.repo.model.auth;

/**
 * 
 * The persistence of login lockout data.
 *
 */
public interface LoginLockoutStatusDao {

	/**
	 * Get the current lockout information for the given user.
	 * 
	 * @param userId
	 * @return
	 */
	public LockoutInfo getLockoutInfo(Long userId);

	/**
	 * Reset the lockout information for the given user. This will set the failed
	 * attempts to zero, and set the lockout time to zero. The database update will
	 * occur in a new transaction.
	 * 
	 * @param userId
	 */
	public void resetLockoutInfoWithNewTransaction(Long userId);

	/**
	 * Increment the number of failed logins for this user. This will increment the
	 * number of failed attempts by one, and increase the lockout time
	 * exponentially. The database update will occur in a new transaction.
	 * 
	 * @param userId
	 */
	public void incrementLockoutInfoWithNewTransaction(Long userId);
	
	/**
	 * Clear all of the data for this table.
	 */
	public void truncateAll();

}
