package org.sagebionetworks.upload.multipart;

import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;

/**
 * Defines the general functions needed to coordinate steps in a multi-part upload with a specific cloud provider.
 */
public interface CloudServiceMultipartUploadDAO {
	
	/**
	 * Initiates a multi-part upload. Returns an access token, if applicable for the implementation.
	 * @param bucket The bucket to upload to
	 * @param key The key to upload to
	 * @param request The original upload request
	 * @return an access token, if needed for the multi-part upload
	 */
	String initiateMultipartUpload(String bucket, String key, MultipartUploadRequest request);
	
	/**
	 * Initiates a multi-part upload copying the data from a given source file.
	 * @param bucket The bucket to upload to
	 * @param key The key to upload to
	 * @param request The original copy request
	 * @param fileHandle The source file handle to copy
	 * @return an access token, if needed for the multi-part upload
	 */
	String initiateMultipartUploadCopy(String bucket, String key, MultipartUploadCopyRequest request, FileHandle fileHandle);
	
	/**
	 * Create a pre-signed URL to upload a part of multi-part file upload.
	 * @param bucket
	 * @param partKey
	 * @param contentType Optional parameter.  Sets the expected content-type of the request. The content-type is included in
     * the signature.
	 * @return
	 */
	PresignedUrl createPartUploadPreSignedUrl(String bucket, String partKey, String contentType);
	
	/**
	 * Add a part to a multi-part upload. This call may delete the temporary part file, depending on implementation.
	 * @param request
	 */
	void validateAndAddPart(AddPartRequest request);

	/**
	 * Complete a multi-part upload.
	 * @param request
	 * @return The size of the resulting file.
	 */
	long completeMultipartUpload(CompleteMultipartRequest request);

}
