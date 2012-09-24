package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.web.NotFoundException;

public interface StorageUsageService {

	/**
	 * Retrieves the aggregated usage for the specified user.
	 *
	 * @throws UnauthorizedException
	 *			When the current user is not authorized to view the specified user's storage usage.
	 * @throws NotFoundException
	 *			When the user does not exist.
	 */
	StorageUsageSummaryList getStorageUsage(String currUserName, String userName, List<StorageUsageDimension> dList)
			throws UnauthorizedException, NotFoundException, DatastoreException;

	/**
	 * Retrieves detailed, itemized usage for the specified user.
	 *
	 * @throws UnauthorizedException
	 *			When the current user is not authorized to view the specified user's storage usage.
	 * @throws NotFoundException
	 *			When the user does not exist.
	 */
	PaginatedResults<StorageUsage> getStorageUsage(String currUserName, String userName,
			Integer offset, Integer limit, String urlPath)
			throws UnauthorizedException, NotFoundException, DatastoreException;
}
