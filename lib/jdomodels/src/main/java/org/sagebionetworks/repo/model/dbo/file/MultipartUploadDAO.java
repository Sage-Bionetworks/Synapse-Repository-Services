package org.sagebionetworks.repo.model.dbo.file;

import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;

public interface MultipartUploadDAO {
	
	/**
	 * Start of continue a multi-part upload.
	 * @param request
	 * @return
	 */
	public MultipartUploadStatus startOrContinueUpload(MultipartUploadRequest request);

}
