package org.sagebionetworks.repo.manager.file;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.S3FileHandle;

/**
 * A data access object used to support bulk file download.  This abstraction contains 
 * 
 *
 */
public interface FileHandleSupport {

	/**
	 * Create a temporary file on the local machine.
	 * 
	 * @param prefix
	 * @param suffix
	 * @return
	 * @throws IOException 
	 */
	public File createTempFile(String prefix, String suffix) throws IOException;
	
	/**
	 * Create a ZipOutputStream using the given file.
	 * @param outFile
	 * @return
	 * @throws IOException 
	 */
	public ZipOutputStream createZipOutputStream(File outFile) throws IOException;
	
	/**
	 * Get the S3FileHandle for the given FileHandle.id.
	 * 
	 * @param fileHandleId
	 * @return
	 * @throws IllegalArgumentException If the requested FileHandle is not an S3FileHandle.
	 */
	S3FileHandle getS3FileHandle(String fileHandleId);
	
	/**
	 * Download the given FileHandle to the a local file.
	 * @param fileHandle
	 * @return
	 * @throws IOException 
	 */
	public File downloadToTempFile(S3FileHandle fileHandle) throws IOException;
	
	/**
	 * Add the given file to 
	 * @param zipOut
	 * @param toAdd
	 * @param entryName
	 * @throws IOException 
	 */
	public void addFileToZip(ZipOutputStream zipOut, File toAdd, String entryName) throws IOException;
	
	/**
	 * Packaged the requested files into a zip file and upload the file to S3 as a FileHandle.
	 * 
	 * @param user
	 * @param request
	 * @return
	 * @throws IOException
	 */
	public BulkFileDownloadResponse buildZip(UserInfo user,
			BulkFileDownloadRequest request) throws IOException;
	
	
}
