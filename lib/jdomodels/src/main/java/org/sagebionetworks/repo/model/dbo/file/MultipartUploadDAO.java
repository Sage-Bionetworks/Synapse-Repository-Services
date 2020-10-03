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
	 * Delete all data for a file upload given a userId and upload hash.
	 * @param userId
	 * @param hash
	 */
	void deleteUploadStatus(long userId, String hash);
	
	/**
	 * Create a new upload status from a request.
	 * @param userId
	 * @param hash
	 * @param request
	 * @return
	 */
	CompositeMultipartUploadStatus createUploadStatus(CreateMultipartRequest createRequest);
	
	/**
	 * Remove all data for all users.
	 */
	void truncateAll();
	
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

}
