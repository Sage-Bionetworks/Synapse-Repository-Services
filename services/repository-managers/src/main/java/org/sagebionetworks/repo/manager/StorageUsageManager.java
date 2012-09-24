package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;

public interface StorageUsageManager {

	/**
	 * 
	 * @param userId
	 * @param dimensionList
	 * @return
	 */
	StorageUsageSummaryList getStorageUsage(String userId, List<StorageUsageDimension> dimensionList);

	/**
	 * 
	 * @param userId
	 * @param offset
	 * @param limit
	 * @return
	 */
	QueryResults<StorageUsage> getStorageUsage(String userId, Integer offset, Integer limit);
}
