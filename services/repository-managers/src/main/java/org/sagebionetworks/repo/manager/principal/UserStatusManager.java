package org.sagebionetworks.repo.manager.principal;

public interface UserStatusManager {
	
	// Number of days of inactivity before a user is disabled
	int INACTIVITY_DAYS = 370;
	int WARNING_INACTIVITY_DAYS = 345;

	int disableInactiveUsers(int maxBatchSize);

	int warnSoonToBeInactiveUsers(int maxBatchSize);

	void resetUserStatusToEnabled(Long targetUserId);

}
