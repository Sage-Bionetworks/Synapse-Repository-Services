package org.sagebionetworks.repo.model.dbo.file.google;

import java.util.List;

public interface AsyncGooglePartRangeDao {

	void addPartRange(String uploadId, PartRange part);

	void removePartRange(String uploadId, PartRange part);

	boolean doesPartRangeExist(String uploadId, PartRange part);

	/**
	 * Find contiguous parts that are ready to be merged with each other.
	 * 
	 * @param uploadId
	 * @param order
	 * @param limit
	 * @return
	 */
	List<Compose> findContiguousPartRanges(String uploadId, OrderBy order, int limit);

	/**
	 * Attempt to acquire a lock on all of the provided parts in a new transaction.
	 * If all locks can be acquired, the provided consumer will be called from
	 * within the transaction.
	 * 
	 * @param uploadId
	 * @param consumer
	 * @param parts
	 * @return True if all locks were acquired and the consumer was called. False if
	 *         one or more lock could not be acquired.
	 */
	boolean attemptToLockPartRanges(String uploadId, Runnable runner, PartRange... parts);

	/**
	 * List all of the parts for a given upload ID.
	 * 
	 * @param uploadeId
	 * @return
	 */
	List<PartRange> listAllPartRangesForUploadId(String uploadeId);

}
