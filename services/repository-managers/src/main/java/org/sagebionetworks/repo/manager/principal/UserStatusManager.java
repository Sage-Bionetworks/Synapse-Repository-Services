package org.sagebionetworks.repo.manager.principal;

public interface UserStatusManager {
	
	// Number of days of inactivity before a user is disabled
	int INACTIVITY_DAYS = 180;

	int disableInactiveUsers(int maxBatchSize);

	int backfillUsersLastSeenOn(int maxCount);
	
}
