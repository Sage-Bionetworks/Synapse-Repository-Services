package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;

public interface StorageUsageService {

	/**
	 * Retrieves the aggregated usage for the specified user.
	 *
	 * @throws UnauthorizedException
	 *			When the current user is not authorized to view the specified user's storage usage.
	 */
	StorageUsageSummaryList getStorageUsage(String currUserId, String userId, List<StorageUsageDimension> dList)
			throws UnauthorizedException, DatastoreException;

	/**
	 * Retrieves detailed, itemized usage for the specified user.
	 *
	 * @throws UnauthorizedException
	 *			When the current user is not authorized to view the specified user's storage usage.
	 */
	PaginatedResults<StorageUsage> getStorageUsage(String currUserId, String userId,
			Integer offset, Integer limit, String urlPath)
			throws UnauthorizedException, DatastoreException;
}
