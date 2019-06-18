package org.sagebionetworks.repo.model.dbo.file;

import java.util.List;

import org.sagebionetworks.repo.model.upload.PartRange;


/**
 * DAO to for metadata persisted for a multi-part upload.
 *
 */
public interface MultipartUploadComposerDAO {

	/**
	 * Add a part to a multipart upload.
	 *
	 * @param uploadId
	 * @param lowerBound
	 * @param upperBound
	 */
	void addPartToUpload(String uploadId, long lowerBound, long upperBound);

	/**
	 * Get all the parts for a given upload ID
	 * @param uploadId
	 * @return
	 */
	List<DBOMultipartUploadComposerPartState> getAddedParts(Long uploadId);

	/**
	 * Get all the parts for a given upload ID between the provided bounds (inclusive) for update.
	 * @param uploadId
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	List<PartRange> getAddedPartRangesForUpdate(Long uploadId, Long lowerBound, Long upperBound);

	/**
	 * Deletes all of the parts contained in a given range (inclusive).
	 * For example, if the table contains parts with ranges [1, 1], [2, 2], [3, 3], [3, 4], and [2, 4], then a request
	 * to delete given a lowerBound=2 and upperBound=4 will delete all parts except [1, 1].
	 *
	 * @param uploadId The upload ID under which parts should be deleted
	 * @param lowerBound The lower bound of the range to be deleted
	 * @param upperBound The upper bound of the range to be deleted
	 */
	void deletePartsInRange(String uploadId, long lowerBound, long upperBound);

	/**
	 * Set the given file upload to be complete.
	 * @param uploadId
	 * @param fileHandleId
	 * @return The final status of the file.
	 */
	CompositeMultipartUploadStatus setUploadComplete(String uploadId, String fileHandleId);

	/**
	 * Clears all multipart upload parts from the parts composer table.
	 */
	void truncateAll();
}
