package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;

/**
 * Version two of the multipart manager.
 *
 */
public interface MultipartManagerV2 {
	
	/**
	 * Start or resume an multi-part upload for a given file.
	 * @param request
	 * @param forceRestart
	 * @return
	 */
	public MultipartUploadStatus startOrResumeMultipartUpload(UserInfo user, MultipartUploadRequest request, boolean forceRestart);
	
	/**
	 * Get batch of pre-signed upload URLs for multi-part upload.
	 * @param user
	 * @param request
	 * @return
	 */
	public BatchPresignedUploadUrlResponse getBatchPresignedUploadUrls(UserInfo user, BatchPresignedUploadUrlRequest request);
	
	/**
	 * After an part has been PUT to a pre-signed URL, it must be added to the multipart upload.
	 * 
	 * @param user
	 * @param uploadId
	 * @param partNumber
	 * @param partMD5Hex
	 * @return
	 */
	public AddPartResponse addMultipartPart(UserInfo user, String uploadId, Integer partNumber, String partMD5Hex);
	
	/**
	 * After all of the parts are uploaded, complete the multi-part upload and generate a file.
	 * @param user
	 * @param uploadId
	 * @return
	 */
	public MultipartUploadStatus completeMultipartUpload(UserInfo user, String uploadId);
	
	/**
	 * Get the original request for a given multi-part file upload.
	 * @param uploadId
	 * @return
	 */
	public MultipartUploadRequest getRequestForUpload(String uploadId);
	
	/**
	 * Create a filehandle for a multi-part upload.
	 * @param fileSize
	 * @param composite
	 * @param request
	 * @return
	 */
	public CloudProviderFileHandleInterface createFileHandle(long fileSize, CompositeMultipartUploadStatus composite, MultipartUploadRequest request);

	/**
	 * Truncate all data
	 */
	public void truncateAll();
	


}
