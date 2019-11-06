package org.sagebionetworks.upload.multipart;

import java.net.URL;

import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;

/**
 * Defines the general functions needed to coordinate steps in a multi-part upload with a specific cloud provider.
 */
public interface CloudServiceMultipartUploadDAO {

	/**
	 * Initiates a multi-part upload. Returns an access token, if applicable for the implementation.
	 * @param bucket The bucket to upload to
	 * @param key The key to upload to
	 * @param request
	 * @return an access token, if needed for the multi-part upload
	 */
	public String initiateMultipartUpload(String bucket, String key, MultipartUploadRequest request);
	
	/**
	 * Create a pre-signed URL to upload a part of multi-part file upload.
	 * @param bucket
	 * @param partKey
	 * @param contentType Optional parameter.  Sets the expected content-type of the request. The content-type is included in
     * the signature.
	 * @return
	 */
	public URL createPreSignedPutUrl(String bucket, String partKey, String contentType);

	/**
	 * Add a part to a multi-part upload. This call may delete the temporary part file, depending on implementation.
	 * @param request
	 */
	public void validateAndAddPart(AddPartRequest request);

	/**
	 * Complete a multi-part upload.
	 * @param request
	 * @return The size of the resulting file.
	 */
	public long completeMultipartUpload(CompleteMultipartRequest request);

}
