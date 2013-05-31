package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;

/**
 * Accesses database for storage locations.
 */
public interface StorageLocationDAO {

	/**
	 * Gets the total usage in bytes.
	 */
	Long getTotalSize() throws DatastoreException;

	/**
	 * Gets the total usage in bytes for the specified user.
	 */
	Long getTotalSizeForUser(String userId) throws DatastoreException;

	/**
	 * Gets the count of storage items.
	 */
	Long getTotalCount() throws DatastoreException;

	/**
	 * Gets the count of storage items for the specified user.
	 */
	Long getTotalCountForUser(String userId) throws DatastoreException;

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
	StorageUsageSummaryList getAggregatedUsageForUser(String userId,
			List<StorageUsageDimension> dimensionList)
			throws InvalidModelException, DatastoreException;

	/**
	 * Gets detailed, itemized storage usage for the specified user. Results are paged as
	 * specified by the begin (inclusive) and the end (exclusive) indices.
	 */
	List<StorageUsage> getUsageInRangeForUser(String userId, long beginIncl, long endExcl)
			throws DatastoreException;

	/**
	 * Size in bytes aggregated by user ID.
	 */
	StorageUsageSummaryList getAggregatedUsageByUserInRange(long beginIncl, long endExcl);
}
