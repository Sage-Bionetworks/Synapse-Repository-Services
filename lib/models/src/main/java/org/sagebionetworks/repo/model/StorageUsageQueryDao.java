package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;

/**
 * Accesses database for storage locations.
 * 
 * Update about deprecation: This feature was built so that we can put a limit
 * on how much users can store in Synapse. We abort the project, and have no
 * supports on this feature. It's reporting the wrong number and is not useful.
 * Also, it put a heavy load on the DB and need to be removed.
 */
@Deprecated
public interface StorageUsageQueryDao {

	/**
	 * Gets the total usage in bytes.
	 */
	Long getTotalSize() throws DatastoreException;

	/**
	 * Gets the total usage in bytes for the specified user.
	 */
	Long getTotalSizeForUser(Long userId) throws DatastoreException;

	/**
	 * Gets the count of storage items.
	 */
	Long getTotalCount() throws DatastoreException;

	/**
	 * Gets the count of storage items for the specified user.
	 */
	Long getTotalCountForUser(Long userId) throws DatastoreException;

	/**
	 * Gets the aggregated usage in bytes. Numbers are aggregated by the supplied
	 * dimension list. If the list of dimensions is empty, this will return the grand total.
	 *
	 * @throws InvalidModelException Dimensions have invalid column names
	 */
	StorageUsageSummaryList getAggregatedUsage(List<StorageUsageDimension> dimensionList)
			throws DatastoreException, InvalidModelException;

	/**
	 * Gets the aggregated totals in bytes for the specified user. Numbers are
	 * aggregated by the supplied dimension list. If the list of dimensions
	 * is empty, this will return the grand total.
	 *
	 * @throws InvalidModelException Dimensions have invalid column names
	 */
	StorageUsageSummaryList getAggregatedUsageForUser(Long userId,
			List<StorageUsageDimension> dimensionList)
			throws InvalidModelException, DatastoreException;

	/**
	 * Gets detailed, itemized storage usage for the specified user. Results are paged as
	 * specified by the begin (inclusive) and the end (exclusive) indices.
	 */
	List<StorageUsage> getUsageInRangeForUser(Long userId, long beginIncl, long endExcl)
			throws DatastoreException;

	/**
	 * Size in bytes aggregated by user ID.
	 */
	StorageUsageSummaryList getAggregatedUsageByUserInRange(long beginIncl, long endExcl);
}
