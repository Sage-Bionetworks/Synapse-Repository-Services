package org.sagebionetworks.repo.model.dbo.file;

import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;

public interface MultipartUploadDAO {
	
	/**
	 * Get the upload status for a file given a userId and upload hash.
	 * @param userId
	 * @param hash
	 * @return
	 */
	public MultipartUploadStatus getUploadStatus(long userId, String hash);
	
	/**
	 * Get the upload status for a file given an upload id.
	 * @param id
	 * @return
	 */
	public MultipartUploadStatus getUploadStatus(String id);
	
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
	public MultipartUploadStatus createUploadStatus(long userId, String hash, String providerId, MultipartUploadRequest request);

}
