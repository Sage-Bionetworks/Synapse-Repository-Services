package org.sagebionetworks.file.services;

import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUploadException;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileUploadManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of the file upload service.
 * @author John
 *
 */
public class FileUploadServiceImp implements FileUploadService {
	
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	FileUploadManager fileUploadManager;

	@Override
	public void uploadFiles(String username, FileItemIterator itemIterator, long contentLength) throws DatastoreException, NotFoundException, FileUploadException, IOException {
		if(username == null) throw new IllegalArgumentException("Username cannot be null");
		if(itemIterator == null) throw new IllegalArgumentException("FileItemIterator cannot be null");
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(username);
		
		fileUploadManager.uploadfiles(userInfo, new HashSet<String>(0), itemIterator, contentLength);

	}

}
