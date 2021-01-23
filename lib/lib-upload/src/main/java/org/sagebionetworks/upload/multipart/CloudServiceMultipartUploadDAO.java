package org.sagebionetworks.upload.multipart;

import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.file.AbortMultipartRequest;
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
	 * Creates a pre-signed URL to copy a part of a multi-part file copy
	 * 
	 * @param upload
	 * @param partNumber
	 * @param contentType
	 * @return
	 */
	PresignedUrl createPartUploadCopyPresignedUrl(CompositeMultipartUploadStatus status, long partNumber, String contentType);
	
	/**
	 * Add a part to a multi-part upload. This call may delete the temporary part file, depending on implementation.
	 * @param request
	 */
	void validateAndAddPart(AddPartRequest request);
	
	/**
	 * Validates the added copy part
	 * 
	 * @param status
	 * @param partNumber
	 */
	void validatePartCopy(CompositeMultipartUploadStatus status, long partNumber, String partMD5Hex);

	/**
	 * Complete a multi-part upload.
	 * @param request
	 * @return The size of the resulting file.
	 */
	long completeMultipartUpload(CompleteMultipartRequest request);
	
	/**
	 * Tries to abort a multipart request, deleting any temporary key. If something goes wrong (e.g. no access, does not exists etc) does not throw.
	 * 
	 * @param request
	 */
	void tryAbortMultipartRequest(AbortMultipartRequest request);
	
	/**
	 * @param bucket
	 * @param key
	 * @return The etag assigned to the object in the given bucket and with the given key
	 */
	String getObjectEtag(String bucket, String key);

}
