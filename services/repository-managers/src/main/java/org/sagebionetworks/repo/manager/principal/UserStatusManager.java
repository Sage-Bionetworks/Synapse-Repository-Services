package org.sagebionetworks.repo.manager.principal;

public interface UserStatusManager {

	// Number of days of inactivity before a user is disabled
	int INACTIVITY_DAYS = 370;

	// Number of days in the future before a warned user is getting disabled
	int WARNING_PERIOD_LENGTH = 14;

	// Number of days of inactivity before the user receives a warning email (14 days before disable)
	int INACTIVITY_WARNING_DAYS = INACTIVITY_DAYS - WARNING_PERIOD_LENGTH;

	int disableInactiveUsers(int maxBatchSize);

	/**
	 * Sends a warning email to users who have been inactive for at least
	 * INACTIVITY_WARNING_DAYS days but have not yet been warned. Returns
	 * the number of users warned in this batch.
	 */
	int warnInactiveUsers(int maxBatchSize);

	void resetUserStatusToEnabled(Long targetUserId);

}
