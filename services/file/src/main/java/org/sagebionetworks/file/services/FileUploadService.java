package org.sagebionetworks.file.services;

import java.io.IOException;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUploadException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

/**
 * Abstraction for the handling multi-part upload.
 * @author John
 *
 */
public interface FileUploadService {

	
	/**
	 * Upload all files for this request.
	 * @param userName
	 * @param itemIterator
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 * @throws FileUploadException 
	 * @throws ServiceUnavailableException 
	 */
	FileHandleResults uploadFiles(String userName, FileItemIterator itemIterator) throws DatastoreException, NotFoundException, FileUploadException, IOException, ServiceUnavailableException;

	/**
	 * Get a file handle by ID.
	 * @param handleId
	 * @param userId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandle getFileHandle(String handleId, String userId) throws DatastoreException, NotFoundException;

	/**
	 * Delete a file handle.
	 * @param handleId
	 * @param userId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void deleteFileHandle(String handleId, String userId) throws DatastoreException, NotFoundException;

	/**
	 * Create an external file Handle.
	 * @param userId
	 * @param fileHandle
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	ExternalFileHandle createExternalFileHandle(String userId,	ExternalFileHandle fileHandle) throws DatastoreException, NotFoundException;

}
