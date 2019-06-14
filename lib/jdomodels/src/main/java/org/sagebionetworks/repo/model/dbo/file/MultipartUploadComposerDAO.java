package org.sagebionetworks.repo.model.dbo.file;

import java.util.List;


/**
 * DAO to for metadata persisted for a multi-part upload.
 *
 */
public interface MultipartUploadComposerDAO {


	/**
	 * Gets the parts that immediately precede and follow the given part number.
	 * @param uploadId ID of the entire multipart upload
	 * @param partNumber
	 * @return A list of neighboring parts. If two parts are returned (a part before and after the given part number),
	 * then the first part is necessarily the predecessor and the second part is necessarily the successor.
	 * If only one part is returned, the caller should check the part ranges to determine if the part is the predecessor or
	 * successor.
	 */
	List<DBOMultipartUploadComposerPartState> getContiguousParts(String uploadId, int partNumber);

	/**
	 * Add a part to a multipart upload.
	 *
	 * @param uploadId
	 * @param lowerBound
	 * @param upperBound
	 */
	void addPartToUpload(String uploadId, int lowerBound, int upperBound);

	/**
	 * Get all the parts for a given upload ID
	 * @param uploadId
	 * @return
	 */
	List<DBOMultipartUploadComposerPartState> getAddedParts(String uploadId);

	/**
	 * Deletes all of the parts contained in a given range (inclusive).
	 * For example, if the table contains parts with ranges [1, 1], [2, 2], [3, 3], [3, 4], and [2, 4], then a request
	 * to delete given a lowerBound=2 and upperBound=4 will delete all parts except [1, 1].
	 *
	 * @param uploadId The upload ID under which parts should be deleted
	 * @param lowerBound The lower bound of the range to be deleted
	 * @param upperBound The upper bound of the range to be deleted
	 */
	void deletePartsInRange(String uploadId, int lowerBound, int upperBound);

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
