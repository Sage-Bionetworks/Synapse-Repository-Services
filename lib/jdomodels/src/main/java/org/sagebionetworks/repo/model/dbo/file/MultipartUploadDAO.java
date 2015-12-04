package org.sagebionetworks.repo.model.dbo.file;


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

}
