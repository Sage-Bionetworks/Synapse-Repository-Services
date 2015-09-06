package org.sagebionetworks.file.worker;

import org.sagebionetworks.repo.model.file.FileDownloadCode;

/**
 * Failure of a single file in a bulk file download.
 *
 */
public class BulkFileException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	FileDownloadCode failureCode;
	
	public BulkFileException(String message, FileDownloadCode failureCode) {
		super(message);
		this.failureCode = failureCode;
	}

	public FileDownloadCode getFailureCode() {
		return failureCode;
	}
	
}
