package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.web.NotFoundException;

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
public interface StorageUsageService {

	/**
	 * Retrieves the aggregated usage.
	 *
	 * @throws UnauthorizedException
	 *			When the current user is not an administrator.
	 */
	StorageUsageSummaryList getUsage(Long currentUserId, List<StorageUsageDimension> dimensionList)
			throws UnauthorizedException, DatastoreException;

	/**
	 * Retrieves the aggregated usage for the specified user.
	 *
	 * @throws UnauthorizedException
	 *			When the current user is not authorized to view the specified user's storage usage.
	 * @throws NotFoundException
	 *			When the user does not exist.
	 */
	StorageUsageSummaryList getUsageForUser(Long currentUserId, Long userId,
			List<StorageUsageDimension> dimensionList)
			throws UnauthorizedException, NotFoundException, DatastoreException;

	/**
	 * Retrieves detailed, itemized usage for the specified user.
	 *
	 * @throws UnauthorizedException
	 *			When the current user is not authorized to view the specified user's storage usage.
	 * @throws NotFoundException
	 *			When the user does not exist.
	 */
	PaginatedResults<StorageUsage> getUsageInRangeForUser(Long currentUserId, Long userId,
			Integer offset, Integer limit, String urlPath)
			throws UnauthorizedException, NotFoundException, DatastoreException;

	/**
	 * Retrieves size in bytes aggregated by user ID.
	 *
	 * @throws UnauthorizedException
	 *			When the current user is not an administrator.
	 */
	StorageUsageSummaryList getUsageByUserInRange(Long currentUserId, Integer offset, Integer limit)
			throws UnauthorizedException, DatastoreException;
}
