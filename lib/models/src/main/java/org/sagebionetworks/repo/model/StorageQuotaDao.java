package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.storage.StorageQuota;

public interface StorageQuotaDao {

	/**
	 * Sets the storage quota in MB for the specified user.
	 */
	void setQuota(StorageQuota quota);

	/**
	 * Gets the storage quota in MB for the specified user.
	 * If the quota is not found, null is returned.
	 */
	StorageQuota getQuota(String userId);
}
