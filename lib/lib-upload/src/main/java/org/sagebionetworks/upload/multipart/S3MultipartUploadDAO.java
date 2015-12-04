package org.sagebionetworks.upload.multipart;

import org.sagebionetworks.repo.model.file.MultipartUploadRequest;

/**
 * Abstraction for S3 multi-part upload.
 *
 */
public interface S3MultipartUploadDAO {

	/**
	 * Start a multi-part upload.
	 * @param bucket
	 * @param key
	 * @param request
	 * @return
	 */
	public String initiateMultipartUpload(String bucket, String key, MultipartUploadRequest request);
}
