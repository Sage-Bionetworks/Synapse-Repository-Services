package org.sagebionetworks.repo.manager.file;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.MultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;

/**
 * Version two of the multipart manager.
 *
 */
public interface MultipartManagerV2 {
	
	// 30 days before the multi part uploads are garbage collected
	int EXPIRE_PERIOD_DAYS = 30;
	
	/**
	 * Starts one of the supported ({@link MultipartUploadRequest} or {@link MultipartUploadCopyRequest}) multipart operations.
	 * 
	 * @param user
	 * @param request
	 * @param forceRestart
	 * @return
	 */
	MultipartUploadStatus startOrResumeMultipartOperation(UserInfo user, MultipartRequest request, boolean forceRestart);
	
	/**
	 * Start or resume an multi-part upload for a given file.
	 * @param request
	 * @param forceRestart
	 * @return
	 */
	MultipartUploadStatus startOrResumeMultipartUpload(UserInfo user, MultipartUploadRequest request, boolean forceRestart);
	
	/**
	 * Start or resume a multi-part upload copying from a given source.
	 * @param user
	 * @param request
	 * @param forceRestart
	 * @return
	 */
	MultipartUploadStatus startOrResumeMultipartUploadCopy(UserInfo user, MultipartUploadCopyRequest request, boolean forceRestart);
	
	/**
	 * Get batch of pre-signed upload URLs for multi-part upload.
	 * @param user
	 * @param request
	 * @return
	 */
	BatchPresignedUploadUrlResponse getBatchPresignedUploadUrls(UserInfo user, BatchPresignedUploadUrlRequest request);
	
	/**
	 * After an part has been PUT to a pre-signed URL, it must be added to the multipart upload.
	 * 
	 * @param user
	 * @param uploadId
	 * @param partNumber
	 * @param partMD5Hex
	 * @return
	 */
	AddPartResponse addMultipartPart(UserInfo user, String uploadId, Integer partNumber, String partMD5Hex);
	
	/**
	 * After all of the parts are uploaded, complete the multi-part upload and generate a file.
	 * @param user
	 * @param uploadId
	 * @return
	 */
	MultipartUploadStatus completeMultipartUpload(UserInfo user, String uploadId);
	
	/**
	 * Fetch all the multipart uploads ids that were modified before the given number of days
	 * 
	 * @param numberOfDays The number of days
	 * @param batchSize The max number of upload ids to fetch
	 * @return A batch upload ids that were modified before the given instant
	 */
	List<String> getUploadsModifiedBefore(int numberOfDays, long batchSize);
	
	/**
	 * Clear the temporary data for the given multipart upload and remove its records. Completed
	 * multipart uploads will retain the uploaded data and the respective file handle, uploads that in
	 * progress will be abported if possible.
	 * 
	 * @param uploadId The id of the upload to clear
	 */
	void clearMultipartUpload(String uploadId);
	
	// For testing

	/**
	 * Truncate all data
	 */
	void truncateAll();
	
	/**
	 * @param batchSize
	 * @return A batch of upload ids ordered by updated on
	 */
	List<String> getUploadsOrderByUpdatedOn(long batchSize);

}
