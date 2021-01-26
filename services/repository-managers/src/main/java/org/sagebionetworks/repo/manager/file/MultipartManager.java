package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.file.S3FileHandle;

/**
 * Abstraction for multi-part support.
 * @author jmhill
 *
 */
public interface MultipartManager {

	/**
	 * Upload local file to S3.
	 * 
	 * @param request
	 * @return
	 */
	S3FileHandle multipartUploadLocalFile(LocalFileUploadRequest request);
}
