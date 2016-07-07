package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;

/**
 * Retrieves storage usage data.
 * 
 * Update about deprecation: This feature was built so that we can put a limit
 * on how much users can store in Synapse. We abort the project, and have no
 * supports on this feature. It's reporting the wrong number and is not useful.
 * Also, it put a heavy load on the DB and need to be removed.
 * 
 */
@Deprecated
public interface StorageUsageManager {

	StorageUsageSummaryList getUsage(List<StorageUsageDimension> dimensionList);

	StorageUsageSummaryList getUsageForUser(Long userId, List<StorageUsageDimension> dimensionList);

	QueryResults<StorageUsage> getUsageInRangeForUser(Long userId, Integer offset, Integer limit);

	StorageUsageSummaryList getUsageByUserInRange(Integer offset, Integer limit);
}
