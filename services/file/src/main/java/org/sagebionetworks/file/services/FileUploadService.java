package org.sagebionetworks.file.services;

import java.io.IOException;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUploadException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

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
	 */
	void uploadFiles(String userName, FileItemIterator itemIterator) throws DatastoreException, NotFoundException, FileUploadException, IOException;

}
