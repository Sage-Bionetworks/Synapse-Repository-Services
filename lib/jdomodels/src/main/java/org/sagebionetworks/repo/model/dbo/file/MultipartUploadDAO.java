package org.sagebionetworks.repo.model.dbo.file;

import java.util.List;


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
	public CompositeMultipartUploadStatus getUploadStatus(Long userId, String hash);
	
	/**
	 * Get the upload status for a file given an upload id.
	 * @param id
	 * @return
	 */
	public CompositeMultipartUploadStatus getUploadStatus(String id);
	
	/**
	 * Delete all data for a file upload given a userId and upload hash.
	 * @param userId
	 * @param hash
	 */
	public void deleteUploadStatus(long userId, String hash);
	
	/**
	 * Create a new upload status from a request.
	 * @param userId
	 * @param hash
	 * @param request
	 * @return
	 */
	public CompositeMultipartUploadStatus createUploadStatus(CreateMultipartRequest createRequest);
	
	/**
	 * Remove all data for all users.
	 */
	public void truncateAll();
	
	/**
	 * Add a part to a multipart upload.
	 * 
	 * @param uploadId
	 * @param partNumber
	 * @param partMD5Hex
	 */
	public void addPartToUpload(String uploadId, int partNumber, String partMD5Hex);
	
	/**
	 * Set the state of a part to failed.
	 * 
	 * @param uploadId
	 * @param partNumber
	 * @param errorDetails
	 */
	public void setPartToFailed(String uploadId, int partNumber, String errorDetails);

	/**
	 * Lookup the parts string from the database.
	 * 
	 * @param uploadId
	 * @return
	 */
	public String getPartsState(String uploadId, int numberOfParts);
	
	/**
	 * For each part added to an upload get the part MD5Hex
	 * @param uploadId
	 * @return
	 */
	public List<PartMD5> getAddedPartMD5s(String uploadId);
	
	/**
	 * For each part with an error get the errors.
	 * @param uploadId
	 * @return
	 */
	public List<PartErrors> getPartErrors(String uploadId);

}
