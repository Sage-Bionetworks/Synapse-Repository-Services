package org.sagebionetworks.repo.manager.file;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
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
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.file.transfer.FileTransferStrategy;
import org.sagebionetworks.repo.manager.file.transfer.TransferRequest;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.HasPreviewId;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandleInterface;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * Basic implementation of the file upload manager.
 * 
 * @author John
 *
 */
public class FileHandleManagerImpl implements FileHandleManager {
	
	public static final long PRESIGNED_URL_EXPIRE_TIME_MS = 30*1000; // 30 secs
	
	static private Log log = LogFactory.getLog(FileHandleManagerImpl.class);
	
	private static String FILE_TOKEN_TEMPLATE = "%1$s/%2$s"; // userid/UUID
	
	@Autowired
	FileHandleDao fileHandleDao;
	
	@Autowired
	AuthorizationManager authorizationManager;
	
	@Autowired
	AmazonS3Client s3Client;
	
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
	public FileHandleManagerImpl(){
		super();
	}

	/**
	 * The IoC constructor.
	 * @param fileMetadataDao
	 * @param primaryStrategy
	 * @param fallbackStrategy
	 * @param authorizationManager
	 * @param s3Client
	 */
	public FileHandleManagerImpl(FileHandleDao fileMetadataDao,
			FileTransferStrategy primaryStrategy,
			FileTransferStrategy fallbackStrategy,
			AuthorizationManager authorizationManager, AmazonS3Client s3Client) {
		super();
		this.fileHandleDao = fileMetadataDao;
		this.primaryStrategy = primaryStrategy;
		this.fallbackStrategy = fallbackStrategy;
		this.authorizationManager = authorizationManager;
		this.s3Client = s3Client;
	}
	/**
	 * Inject the primary strategy.
	 * @param primaryStrategy
	 */
	public void setPrimaryStrategy(FileTransferStrategy primaryStrategy) {
		this.primaryStrategy = primaryStrategy;
	}
	/**
	 * Inject the fall-back strategy.
	 * @param fallbackStrategy
	 */
	public void setFallbackStrategy(FileTransferStrategy fallbackStrategy) {
		this.fallbackStrategy = fallbackStrategy;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public FileUploadResults uploadfiles(UserInfo userInfo,	Set<String> expectedParams, FileItemIterator itemIterator) throws FileUploadException, IOException, ServiceUnavailableException {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(expectedParams == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(itemIterator == null) throw new IllegalArgumentException("FileItemIterator cannot be null");
		if(primaryStrategy == null) throw new IllegalStateException("The primaryStrategy has not been set.");
		if(fallbackStrategy == null) throw new IllegalStateException("The fallbackStrategy has not been set.");
		FileUploadResults results = new FileUploadResults();
		String userId =  userInfo.getIndividualGroup().getId();
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
				S3FileHandle s3Meta = uploadFile(userId, fis);
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
	 * @param userId
	 * @param fis
	 * @return
	 * @throws IOException
	 * @throws ServiceUnavailableException
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public S3FileHandle uploadFile(String userId, FileItemStream fis)	throws IOException, ServiceUnavailableException {
		// Create a token for this file
		TransferRequest request = createRequest(fis.getContentType(),userId, fis.getName(), fis.openStream());
		S3FileHandle s3Meta = null;
		try{
			// Try the primary
			s3Meta = primaryStrategy.transferToS3(request);
		}catch(ServiceUnavailableException e){
			log.info("The primary file transfer strategy failed, attempting to use the fall-back strategy.");
			// The primary strategy failed so try the fall-back.
			s3Meta = fallbackStrategy.transferToS3(request);
		}
		// set this user as the creator of the file
		s3Meta.setCreatedBy(userId);
		// Save the file metadata to the DB.
		s3Meta= fileHandleDao.createFile(s3Meta);
		return s3Meta;
	}
	
	/**
	 * Build up the S3FileMetadata.
	 * @param contentType
	 * @param userId
	 * @param fileName
	 * @return
	 */
	public static TransferRequest createRequest(String contentType, String userId, String fileName, InputStream inputStream){
		// Create a token for this file
		TransferRequest request = new TransferRequest();
		request.setContentType(contentType);
		request.setS3bucketName(StackConfiguration.getS3Bucket());
		request.setS3key(String.format(FILE_TOKEN_TEMPLATE, userId, UUID.randomUUID().toString()));
		request.setFileName(fileName);
		request.setInputStream(inputStream);
		return request;
	}
	
	@Override
	public FileHandle getRawFileHandle(UserInfo userInfo, String handleId) throws DatastoreException, NotFoundException {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(handleId == null) throw new IllegalArgumentException("FileHandleId cannot be null");
		// Get the file handle
		FileHandle handle = fileHandleDao.get(handleId);
		// Only the user that created this handle is authorized to get it.
		if(!authorizationManager.canAccessRawFileHandle(userInfo, handle.getCreatedBy())){
			throw new UnauthorizedException("Only the creator of a FileHandle can access the raw FileHandle");
		}
		return handle;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteFileHandle(UserInfo userInfo, String handleId) throws DatastoreException {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(handleId == null) throw new IllegalArgumentException("FileHandleId cannot be null");
		// Get the file handle
		try {
			FileHandle handle = fileHandleDao.get(handleId);
			// Is the user authorized?
			if(!authorizationManager.canAccessRawFileHandle(userInfo, handle.getCreatedBy())){
				throw new UnauthorizedException("Only the creator of a FileHandle can delete the raw FileHandle");
			}
			// If this file has a preview then we want to delete the preview as well.
			if(handle instanceof HasPreviewId){
				HasPreviewId hasPreview = (HasPreviewId) handle;
				if(hasPreview.getPreviewId() != null){
					// Delete the preview.
					deleteFileHandle(userInfo, hasPreview.getPreviewId());
				}
			}
			// Is this an S3 file?
			if(handle instanceof S3FileHandleInterface){
				S3FileHandleInterface s3Handle = (S3FileHandleInterface) handle;
				// Delete the file from S3
				s3Client.deleteObject(s3Handle.getBucketName(), s3Handle.getKey());
			}
			// Delete the handle from the DB
			fileHandleDao.delete(handleId);
		} catch (NotFoundException e) {
			// there is nothing to do if the handle does not exist.
			return;
		}
		
	}

	@Override
	public URL getRedirectURLForFileHandle(String handleId) throws DatastoreException, NotFoundException {
		// First lookup the file handle
		FileHandle handle = fileHandleDao.get(handleId);
		if(handle instanceof ExternalFileHandle){
			ExternalFileHandle efh = (ExternalFileHandle) handle;
			try {
				return new URL(efh.getExternalURL());
			} catch (MalformedURLException e) {
				throw new DatastoreException(e);
			}
		}else if(handle instanceof S3FileHandleInterface){
			S3FileHandleInterface s3File = (S3FileHandleInterface) handle;
			// Create a pre-signed url
			return s3Client.generatePresignedUrl(s3File.getBucketName(), s3File.getKey(), new Date(System.currentTimeMillis()+PRESIGNED_URL_EXPIRE_TIME_MS), HttpMethod.GET);
		}else{
			throw new IllegalArgumentException("Unknown FileHandle class: "+handle.getClass().getName());
		}
	}

}
