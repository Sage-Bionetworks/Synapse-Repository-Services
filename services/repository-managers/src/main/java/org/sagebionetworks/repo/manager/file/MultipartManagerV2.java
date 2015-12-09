package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;

/**
 * Version two of the multipart manager.
 *
 */
public interface MultipartManagerV2 {
	
	/**
	 * Start or resume an multi-part upload for a given file.
	 * @param request
	 * @return
	 */
	public MultipartUploadStatus startOrResumeMultipartUpload(UserInfo user, MultipartUploadRequest request, Boolean forceRestart);
	
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
	 * @param partNumbers
	 * @param partMD5Hex
	 * @return
	 */
	public AddPartResponse addMultipartPart(UserInfo user, String uploadId, Integer partNumber, String partMD5Hex);
	


}
