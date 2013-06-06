package org.sagebionetworks.repo.model;

public interface StorageQuotaDao {

	/**
	 * Sets the storage quota in MB for the specified user.
	 */
	void setQuota(String userId, int quotaInMb);

	/**
	 * Gets the storage quota in MB for the specified user.
	 * If the quota is not found, null is returned.
	 */
	Integer getQuota(String userId);
}
