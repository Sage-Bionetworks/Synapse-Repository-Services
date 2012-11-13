package org.sagebionetworks.file.manager;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUploadException;
import org.sagebionetworks.repo.model.UserInfo;

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
	FileUploadResults uploadfiles(UserInfo userInfo, Set<String> expectedParams, FileItemIterator itemIterator) throws FileUploadException, IOException;

}
