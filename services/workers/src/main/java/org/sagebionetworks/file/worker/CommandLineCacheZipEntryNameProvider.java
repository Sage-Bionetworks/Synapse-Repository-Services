package org.sagebionetworks.file.worker;

/**
 * Creates zip entry names using: {fileHandleId modulo 1000}
 * /{fileHandleId}/{fileName}
 *
 */
public class CommandLineCacheZipEntryNameProvider implements ZipEntryNameProvider {

	public static final int FILE_HANDLE_ID_MODULO_DIVISOR = 1000;

	public static final String ZIP_ENTRY_TEMPLATE = "%d/%d/%s";

	@Override
	public String createZipEntryName(String fileName, Long fileHandleId) {
		long fileHandleModulus = fileHandleId % FILE_HANDLE_ID_MODULO_DIVISOR;
		return String.format(ZIP_ENTRY_TEMPLATE, fileHandleModulus, fileHandleId, fileName);
	}

}
