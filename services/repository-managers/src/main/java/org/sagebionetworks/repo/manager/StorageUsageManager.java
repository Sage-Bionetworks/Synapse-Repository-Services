package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;

public interface StorageUsageManager {

	StorageUsageSummaryList getUsage(List<StorageUsageDimension> dimensionList);

	StorageUsageSummaryList getUsageForUser(String userId, List<StorageUsageDimension> dimensionList);

	StorageUsageSummaryList getCount(List<StorageUsageDimension> dimensionList);

	StorageUsageSummaryList getCountForUser(String userId, List<StorageUsageDimension> dimensionList);

	QueryResults<StorageUsage> getUsageInRangeForUser(String userId, Integer offset, Integer limit);

	StorageUsageSummaryList getUsageByUserInRange(Integer offset, Integer limit);

	StorageUsageSummaryList getUsageByNodeInRange(Integer offset, Integer limit);

	StorageUsageSummaryList getCountByUserInRange(Integer offset, Integer limit);

	StorageUsageSummaryList getCountByNodeInRange(Integer offset, Integer limit);
}
