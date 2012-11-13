package org.sagebionetworks.file.manager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.UserInfo;

/**
 * Basic implementation of the file upload manager.
 * 
 * @author John
 *
 */
public class FileUploadManagerImpl implements FileUploadManager {
	
	static private Log log = LogFactory.getLog(FileUploadManagerImpl.class);
	
	private static String FILE_TOKEN_TEMPLATE = "%1$s/%2$s/%3$s"; // userid/UUID/filename

	@Override
	public FileUploadResults uploadfiles(UserInfo userInfo,	Set<String> expectedParams, FileItemIterator itemIterator) throws FileUploadException, IOException {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(expectedParams == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(itemIterator == null) throw new IllegalArgumentException("FileItemIterator cannot be null");
		FileUploadResults results = new FileUploadResults();
		// Upload all of the files
		// Before we try to read any files make sure we have all of the expected parameters.
		Set<String> expectedCopy = new HashSet<String>(expectedParams);
		while(itemIterator.hasNext()){
			FileItemStream fis = itemIterator.next();
			if(fis.isFormField()){
				// This is a parameter
				// By removing it from the set we indicate that it was found.
				expectedCopy.remove(fis.getFieldName());
				// Map parameter in the results
				results.getParameters().put(fis.getFieldName(), Streams.asString(fis.openStream()));
			}else{
				// This is a file
				if(!expectedCopy.isEmpty()){
					// We are missing some required parameters
					throw new IllegalArgumentException("Missing one or more of the expected form fields: "+expectedCopy);
				}
				// Create a token for this file
				FileData fileData = new FileData();
				fileData.setContentType(fis.getContentType());
				fileData.setFileName(fis.getName());
				fileData.setFileToken(String.format(FILE_TOKEN_TEMPLATE, userInfo.getIndividualGroup().getId(), UUID.randomUUID().toString(), fis.getName()));
				results.getFiles().add(fileData);
			}
		}
		if(log.isDebugEnabled()){
			log.debug(results);
		}
		return results;
	}

}
