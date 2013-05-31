package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.springframework.beans.factory.annotation.Autowired;

public class StorageUsageManagerImpl implements StorageUsageManager {

	@Autowired
	private StorageLocationDAO storageUsageDao;

	@Override
	public StorageUsageSummaryList getUsage(
			List<StorageUsageDimension> dimensionList) {
		return storageUsageDao.getAggregatedUsage(dimensionList);
	}

	@Override
	public StorageUsageSummaryList getUsageForUser(String userId,
			List<StorageUsageDimension> dimensionList) {
		return storageUsageDao.getAggregatedUsageForUser(userId, dimensionList);
	}

	@Override
	public QueryResults<StorageUsage> getUsageInRangeForUser(String userId, Integer offset, Integer limit) {
		List<StorageUsage> storageUsageList = storageUsageDao.getUsageInRangeForUser(userId, offset, offset + limit);
		long totalCount = storageUsageDao.getTotalCountForUser(userId).longValue();
		QueryResults<StorageUsage> queryResults = new QueryResults<StorageUsage>(storageUsageList, totalCount);
		return queryResults;
	}

	@Override
	public StorageUsageSummaryList getUsageByUserInRange(Integer offset, Integer limit) {
		return storageUsageDao.getAggregatedUsageByUserInRange(offset, offset + limit);
	}
}
