package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;

/**
 * Accesses database for storage locations.
 *
 * @author ewu
 */
public interface StorageLocationDAO {

	/**
	 * Replaces the storage locations for a given node. Storage locations are
	 * extracted from the annotations blobs.
	 */
	void replaceLocationData(StorageLocations locations) throws DatastoreException;

	/**
	 * Gets the total usage in bytes for the specified user.
	 */
	Long getTotalUsage(String userId) throws DatastoreException;

	/**
	 * Gets the aggregated totals in bytes for the specified user. Numbers are
	 * aggregated by the supplied dimension list. If the list of dimensions
	 * is empty, this will return the grand total.
	 *
	 * @throws InvalidModelException Dimensions have invalid column names
	 */
	StorageUsageSummaryList getAggregatedUsage(String userId, List<StorageUsageDimension> dimensionList)
			throws InvalidModelException, DatastoreException;

	/**
	 * Gets detailed, itemized storage usage for the specified user. Results are paged as
	 * specified by the begin (inclusive) and the end (exclusive) indices.
	 */
	List<StorageUsage> getStorageUsageInRange(String userId, long beginIncl, long endExcl)
			throws DatastoreException;

	/**
	 * Gets the count of storage items for the specified user.
	 */
	Long getCount(String userId) throws DatastoreException;
}
