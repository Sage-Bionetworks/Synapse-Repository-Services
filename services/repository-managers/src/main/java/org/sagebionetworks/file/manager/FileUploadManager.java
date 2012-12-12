package org.sagebionetworks.file.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUploadException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileMetadata;

import com.amazonaws.services.s3.internal.S3ErrorResponseHandler;

/**
 * Manages uploading files.
 * 
 * @author John
 *
 */
public interface FileUploadManager {

	/**
	 * Upload all of the files found in a request and capture all of the parameters and meta-data.
	 * 
	 * @param userInfo - The User's information.
	 * @param expectedParams - If any of the expected parameters are missing by the time we get to a file then an IllegalArgumentException will be thrown.
	 * @param itemIterator - Iterates over all of the parts.
	 * @return FileUploadResults
	 * @throws IOException 
	 * @throws FileUploadException 
	 */
	FileUploadResults uploadfiles(UserInfo userInfo, Set<String> expectedParams, FileItemIterator itemIterator, long contentLength) throws FileUploadException, IOException;
	
	/**
	 * Upload the given file input stream as a multi-part S3 upload.
	 * @param metadata
	 * @param in
	 * @param bufferSize
	 * @throws IOException
	 */
	public void uploadFileAsMultipart(S3FileMetadata metadata, InputStream in, int bufferSize) throws IOException;

}
