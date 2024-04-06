package org.sagebionetworks.repo.model.dbo.file.part;

import java.util.List;
import java.util.function.Consumer;

public interface AsyncMultipartUploadComposerDAO {

	void addPart(String uploadId, PartRange part);

	void removePart(String uploadId, PartRange part);

	boolean doesExist(String uploadId, PartRange part);

	/**
	 * Find contiguous parts that are ready to be merged with each other.
	 * 
	 * @param uploadId
	 * @param order
	 * @param limit
	 * @return
	 */
	List<Compose> findContiguousParts(String uploadId, OrderBy order, int limit);

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
	boolean attemptToLockParts(String uploadId, Consumer<List<PartRange>> consumer, PartRange... parts);

	/**
	 * List all of the parts for a given upload ID.
	 * 
	 * @param uploadeId
	 * @return
	 */
	List<PartRange> listAllPartsForUploadId(String uploadeId);

}
