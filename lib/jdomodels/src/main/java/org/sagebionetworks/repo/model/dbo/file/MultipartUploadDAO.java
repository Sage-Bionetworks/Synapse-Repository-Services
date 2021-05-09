package org.sagebionetworks.repo.model.dbo.file;

import java.util.List;

import org.sagebionetworks.repo.model.file.PartErrors;
import org.sagebionetworks.repo.model.file.PartMD5;


/**
 * DAO to for metadata persisted for a multi-part upload.
 *
 */
public interface MultipartUploadDAO {
	
	/**
	 * Get the upload status for a file given a userId and upload hash.
	 * @param userId
	 * @param hash
	 * @return
	 */
	CompositeMultipartUploadStatus getUploadStatus(Long userId, String hash);
	
	/**
	 * Get the upload status for a file given an upload id.
	 * @param id
	 * @return
	 */
	CompositeMultipartUploadStatus getUploadStatus(String id);
	
	/**
	 * Get the JSON string for the original request of a multi-part upload.
	 * @param id
	 * @return
	 */
	String getUploadRequest(String id);
	
	/**
	 * Deletes the records for the given upload id
	 * 
	 * @param uploadId The upload id
	 */
	void deleteUploadStatus(String uploadId);
	
	/**
	 * Updates the hash of the file upload request for the given user and hash.
	 * 
	 * @param userId
	 * @param hash
	 */
	void setUploadStatusHash(long userId, String oldHash, String newHash);
	
	/**
	 * Create a new upload status from a request.
	 * @param userId
	 * @param hash
	 * @param request
	 * @return
	 */
	CompositeMultipartUploadStatus createUploadStatus(CreateMultipartRequest createRequest);
	
	/**
	 * Add a part to a multipart upload.
	 * 
	 * @param uploadId
	 * @param partNumber
	 * @param partMD5Hex
	 */
	void addPartToUpload(String uploadId, int partNumber, String partMD5Hex);
	
	/**
	 * Set the state of a part to failed.
	 * 
	 * @param uploadId
	 * @param partNumber
	 * @param errorDetails
	 */
	void setPartToFailed(String uploadId, int partNumber, String errorDetails);

	/**
	 * Lookup the parts string from the database.
	 * 
	 * @param uploadId
	 * @return
	 */
	String getPartsState(String uploadId, int numberOfParts);
	
	/**
	 * For each part added to an upload get the part MD5Hex
	 * @param uploadId
	 * @return
	 */
	List<PartMD5> getAddedPartMD5s(String uploadId);
	
	/**
	 * For each part with an error get the errors.
	 * @param uploadId
	 * @return
	 */
	List<PartErrors> getPartErrors(String uploadId);

	/**
	 * Set the given file upload to be complete.
	 * @param uploadId
	 * @param fileHandleId
	 * @return The final status of the file.
	 */
	CompositeMultipartUploadStatus setUploadComplete(String uploadId, String fileHandleId);
	
	/**
	 * Fetch a batch of upload ids that were last modified before the given number of days
	 * 
	 * @param numberOfDays The number of days
	 * @param batchSize The max amount of upload ids to fetch
	 * @return A batch of upload ids that were modified before the given instant, ordered by the modifiedOn ASC 
	 */
	List<String> getUploadsModifiedBefore(int numberOfDays, long batchSize);
	
	// For testing
	
	/**
	 * @param batchSize
	 * @return A batch of upload ids ordered by updated on
	 */
	List<String> getUploadsOrderByUpdatedOn(long batchSize);
	

	/**
	 * Remove all data for all users.
	 */
	void truncateAll();

}
