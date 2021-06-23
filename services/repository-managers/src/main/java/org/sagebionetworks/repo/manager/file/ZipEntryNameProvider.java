package org.sagebionetworks.repo.manager.file;

public interface ZipEntryNameProvider {

	/**
	 * Create a zip entry name for the given file name and file handle ID.
	 * 
	 * @param fileName
	 * @param fileHandleId
	 * @return
	 */
	public String createZipEntryName(String fileName, Long fileHandleId);
}
