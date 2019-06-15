package org.sagebionetworks.upload.multipart;

import java.net.URL;

import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
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

	/**
	 * Create a pre-signed URL to A pre-signed URL to upload a part of multi-part file upload.
	 * @param bucket
	 * @param partKey
	 * @param contentType Optional parameter.  Sets the expected content-type of the request. The content-type is included in
     * the signature.
	 * @return
	 */
	public URL createPreSignedPutUrl(String bucket, String partKey, String contentType);

	/**
	 * Add a part to a multi-part upload.s
	 * @param bucket
	 * @param key
	 * @param partKey
	 * @param partMD5Hex
	 */
	public void addPart(AddPartRequest request);

	/**
	 * Delete an object for the given bucket and key.
	 * @param bucket
	 * @param key
	 */
	public void deleteObject(String bucket, String key);

	/**
	 * Complete a multi-part upload.
	 * @param request
	 * @return The size of the resulting file.
	 */
	public long completeMultipartUpload(CompleteMultipartRequest request);

}
