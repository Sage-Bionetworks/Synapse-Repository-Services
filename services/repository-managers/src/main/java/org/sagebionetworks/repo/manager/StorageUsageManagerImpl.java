package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.StorageUsageQueryDao;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.springframework.beans.factory.annotation.Autowired;

@Deprecated
public class StorageUsageManagerImpl implements StorageUsageManager {

	@Autowired
	private StorageUsageQueryDao storageUsageDao;

	@Deprecated
	@Override
	public StorageUsageSummaryList getUsage(
			List<StorageUsageDimension> dimensionList) {
		return storageUsageDao.getAggregatedUsage(dimensionList);
	}

	@Deprecated
	@Override
	public StorageUsageSummaryList getUsageForUser(Long userId,
			List<StorageUsageDimension> dimensionList) {
		return storageUsageDao.getAggregatedUsageForUser(userId, dimensionList);
	}

	@Deprecated
	@Override
	public QueryResults<StorageUsage> getUsageInRangeForUser(Long userId, Integer offset, Integer limit) {
		List<StorageUsage> storageUsageList = storageUsageDao.getUsageInRangeForUser(userId, offset, offset + limit);
		long totalCount = storageUsageDao.getTotalCountForUser(userId).longValue();
		QueryResults<StorageUsage> queryResults = new QueryResults<StorageUsage>(storageUsageList, totalCount);
		return queryResults;
	}

	@Deprecated
	@Override
	public StorageUsageSummaryList getUsageByUserInRange(Integer offset, Integer limit) {
		return storageUsageDao.getAggregatedUsageByUserInRange(offset, offset + limit);
	}
}
