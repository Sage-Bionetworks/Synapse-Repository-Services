package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UserInfo;

public interface StorageQuotaManager {

	/**
	 * Sets the storage quota for specified user.
	 *
	 * @param currentUser  Must be an administrator to set the quota
	 * @param user         The user whose storage quota is to be set
	 * @param quotaInMb    Storage quota in MB
	 */
	void setQuotaForUser(UserInfo currentUser, UserInfo user, int quotaInMb);

	/**
	 * Gets the storage quota for specified user.
	 *
	 * @return Storage quota in MB
	 */
	int getQuotaForUser(UserInfo currentUser, UserInfo user);
}
