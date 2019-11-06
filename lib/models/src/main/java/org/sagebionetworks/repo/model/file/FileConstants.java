package org.sagebionetworks.repo.model.file;

public class FileConstants {
	
	public static final long MAX_FILE_SIZE_GB = 1L;

	/**
	 * The maximum file size for a bulk file download.s
	 */
	public static final long BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES = 1024 * 1024 * 1024*MAX_FILE_SIZE_GB; // 1 GB.
	
}
