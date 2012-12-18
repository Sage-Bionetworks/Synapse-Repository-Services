package org.sagebionetworks.repo.manager.file;

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
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.file.transfer.FileTransferStrategy;
import org.sagebionetworks.repo.manager.file.transfer.TransferRequest;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Basic implementation of the file upload manager.
 * 
 * @author John
 *
 */
public class FileUploadManagerImpl implements FileUploadManager {
	
	
	@Autowired
	FileMetadataDao fileMetadataDao;
	
	/**
	 * This is the first strategy we try to use.
	 */
	FileTransferStrategy primaryStrategy;
	/**
	 * When the primaryStrategy fails, we try fall-back strategy
	 */
	FileTransferStrategy fallbackStrategy;
	
	/**
	 * Used by spring
	 */
	public FileUploadManagerImpl(){
		super();
	}
	/**
	 * Used for unit tests.
	 * @param s3Client
	 * @param fileMetadataDao
	 */
	public FileUploadManagerImpl(FileMetadataDao fileMetadataDao) {
		super();
		this.fileMetadataDao = fileMetadataDao;
	}

	static private Log log = LogFactory.getLog(FileUploadManagerImpl.class);
	
	private static String FILE_TOKEN_TEMPLATE = "%1$s/%2$s/%3$s"; // userid/UUID/filename

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public FileUploadResults uploadfiles(UserInfo userInfo,	Set<String> expectedParams, FileItemIterator itemIterator, long contentLength) throws FileUploadException, IOException, ServiceUnavailableException {
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
				TransferRequest request = createRequest(fis.getContentType(), userInfo.getIndividualGroup().getId(), fis.getName());
				
				S3FileMetadata s3Meta = null;
				try{
					// Try the primary
					s3Meta = primaryStrategy.transferToS3(request);
				}catch(ServiceUnavailableException e){
					log.info("The primary file transfer strategy failed, attempting to use the fall-back strategy");
					// The primary strategy failed so try the fall-back.
					s3Meta = fallbackStrategy.transferToS3(request);
				}
				// If here then we succeeded
				results.getFiles().add(s3Meta);
			}
		}
		if(log.isDebugEnabled()){
			log.debug(results);
		}
		return results;
	}
	
	/**
	 * Build up the S3FileMetadata.
	 * @param contentType
	 * @param userId
	 * @param fileName
	 * @return
	 */
	public static TransferRequest createRequest(String contentType, String userId, String fileName){
		// Create a token for this file
		TransferRequest request = new TransferRequest();
		request.setContentType(contentType);
		request.setS3bucketName(StackConfiguration.getS3Bucket());
		request.setS3key(String.format(FILE_TOKEN_TEMPLATE, userId, UUID.randomUUID().toString(), fileName));
		request.setFileName(fileName);
		return request;
	}


}
