package org.sagebionetworks.repo.manager.file;

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
	public MultipartUploadStatus startOrResumeMultipartUpload(MultipartUploadRequest request);

}
