package org.sagebionetworks.repo.model.file;

public class FileConstants {
	
	public static final long MAX_FILE_SIZE_GB = 1L;

	/**
	 * The maximum file size for a bulk file download.s
	 */
	public static final long BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES = 1024 * 1024 * 1024*MAX_FILE_SIZE_GB; // 1 GB.
	
	/**
	 * The maximum size of a file that is eligible for packing.
	 */
	public static final long MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGEING = BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES/10L;
	
}
