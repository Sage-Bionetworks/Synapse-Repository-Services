package org.sagebionetworks.repo.manager.file;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

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
import org.sagebionetworks.repo.manager.file.transfer.TransferUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.UploadDaemonStatusDao;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.HasPreviewId;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandleInterface;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.util.ContentTypeUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import com.amazonaws.services.s3.model.CORSRule.AllowedMethods;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.util.BinaryUtils;

/**
 * Basic implementation of the file upload manager.
 * 
 * @author John
 *
 */
public class FileHandleManagerImpl implements FileHandleManager {


	public static final long PRESIGNED_URL_EXPIRE_TIME_MS = 30*1000; // 30 secs
	
	static private Log log = LogFactory.getLog(FileHandleManagerImpl.class);
	
	private static String FILE_TOKEN_TEMPLATE = "%1$s/%2$s/%3$s"; // userid/UUID/filename
	
	public static final String NOT_SET = "NOT_SET";
	
	@Autowired
	FileHandleDao fileHandleDao;
	
	@Autowired
	AuthorizationManager authorizationManager;
	
	@Autowired
	AmazonS3Client s3Client;
	
	@Autowired
	UploadDaemonStatusDao uploadDaemonStatusDao;
	
	@Autowired
	ExecutorService uploadFileDaemonThreadPoolPrimary;
	
	@Autowired
	ExecutorService uploadFileDaemonThreadPoolSecondary;
	
	/**
	 * This is the first strategy we try to use.
	 */
	FileTransferStrategy primaryStrategy;
	/**
	 * When the primaryStrategy fails, we try fall-back strategy
	 * 
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
		String userId =  getUserId(userInfo);
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
	 * Get the User's ID
	 * @param userInfo
	 * @return
	 */
	public String getUserId(UserInfo userInfo) {
		return userInfo.getIndividualGroup().getId();
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
		request.setContentType(ContentTypeUtils.getContentType(contentType, fileName));
		request.setS3bucketName(StackConfiguration.getS3Bucket());
		request.setS3key(createNewKey(userId, fileName));
		request.setFileName(fileName);
		request.setInputStream(inputStream);
		return request;
	}

	/**
	 * Create a new key
	 * @param userId
	 * @param fileName
	 * @return
	 */
	private static String createNewKey(String userId, String fileName) {
		return String.format(FILE_TOKEN_TEMPLATE, userId, UUID.randomUUID().toString(), fileName);
	}
	
	@Override
	public FileHandle getRawFileHandle(UserInfo userInfo, String handleId) throws DatastoreException, NotFoundException {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(handleId == null) throw new IllegalArgumentException("FileHandleId cannot be null");
		// Get the file handle
		FileHandle handle = fileHandleDao.get(handleId);
		// Only the user that created this handle is authorized to get it.
		if(!authorizationManager.canAccessRawFileHandleByCreator(userInfo, handle.getCreatedBy())){
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
			if(!authorizationManager.canAccessRawFileHandleByCreator(userInfo, handle.getCreatedBy())){
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

	@Override
	public String getPreviewFileHandleId(String handleId) throws DatastoreException, NotFoundException {
		return fileHandleDao.getPreviewFileHandleId(handleId);
	}

	@Override
	public FileHandleResults getAllFileHandles(List<String> idList, boolean includePreviews) throws DatastoreException, NotFoundException {
		return fileHandleDao.getAllFileHandles(idList, includePreviews);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ExternalFileHandle createExternalFileHandle(UserInfo userInfo,ExternalFileHandle fileHandle) {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(fileHandle == null) throw new IllegalArgumentException("FileHandle cannot be null");
		if(fileHandle.getExternalURL() == null) throw new IllegalArgumentException("ExternalURL cannot be null");
		if(fileHandle.getFileName() == null){
			fileHandle.setFileName(NOT_SET);
		}
		if(fileHandle.getContentType() == null){
			fileHandle.setContentType(NOT_SET);
		}
		// The URL must be a URL
		try{
			URL url = new URL(fileHandle.getExternalURL());
		}catch(MalformedURLException e){
			throw new IllegalArgumentException("The ExternalURL is malformed: "+e.getMessage());
		}
		// set this user as the creator of the file
		fileHandle.setCreatedBy(getUserId(userInfo));
		// Save the file metadata to the DB.
		return fileHandleDao.createFile(fileHandle);
	}
	
	/**
	 * Called by Spring when after the bean is created..
	 */
	public void initialize(){
		// We need to ensure that Cross-Origin Resource Sharing (CORS) is enabled on the bucket
		String bucketName = StackConfiguration.getS3Bucket();
		BucketCrossOriginConfiguration bcoc = s3Client.getBucketCrossOriginConfiguration(bucketName);
		if(bcoc == null || bcoc.getRules() == null || bcoc.getRules().size() < 1){
			// Set the CORS
			resetBuckCORS(bucketName);
		}else{
			// There can only be on rule on the bucket
			if(bcoc.getRules().size() > 1){
				// rest the 
				resetBuckCORS(bucketName);
			}else{
				// Check the rule
				CORSRule currentRule = bcoc.getRules().get(0);
				if(!FileHandleManager.AUTO_GENERATED_ALLOW_ALL_CORS_RULE_ID.equals(currentRule.getId())){
					// rest the rule
					resetBuckCORS(bucketName);
				}
			}
		}
	}

	/**
	 * Reset the bucket's Cross-Origin Resource Sharing (CORS).
	 * 
	 * @param bucketName
	 */
	private void resetBuckCORS(String bucketName) {
		log.debug("Setting the buck Cross-Origin Resource Sharing (CORS) on bucket: "+bucketName+" for the first time...");
		// We need to add the rules
		BucketCrossOriginConfiguration bcoc = new BucketCrossOriginConfiguration();
		CORSRule allowAll = new CORSRule();
		allowAll.setId(FileHandleManager.AUTO_GENERATED_ALLOW_ALL_CORS_RULE_ID);
		allowAll.setAllowedOrigins("*");
		allowAll.setAllowedMethods(AllowedMethods.GET, AllowedMethods.PUT, AllowedMethods.POST, AllowedMethods.HEAD);
		allowAll.setMaxAgeSeconds(300);
		allowAll.setAllowedHeaders("*");
		bcoc.withRules(allowAll);
		s3Client.setBucketCrossOriginConfiguration(StackConfiguration.getS3Bucket(), bcoc);
		log.info("Set CORSRule on bucket: "+bucketName+" to be: "+allowAll);
	}

	@Override
	public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration() {
		String bucketName = StackConfiguration.getS3Bucket();
		return s3Client.getBucketCrossOriginConfiguration(bucketName);
	}

	@Override
	public ChunkedFileToken createChunkedFileUploadToken(UserInfo userInfo, CreateChunkedFileTokenRequest ccftr) {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(ccftr == null) throw new IllegalArgumentException("CreateChunkedFileTokenRequest cannot be null");
		if(ccftr.getFileName() == null) throw new IllegalArgumentException("CreateChunkedFileTokenRequest.fileName cannot be null");
		String contentType = ccftr.getContentType();
		if(contentType == null){
			contentType = "application/octet-stream";
		}
		String userId =  getUserId(userInfo);
		// Start a multi-file upload
		String key = createNewKey(userId, ccftr.getFileName());
		ObjectMetadata objMeta = new ObjectMetadata();
		objMeta.setContentType(contentType);
		objMeta.setContentDisposition(TransferUtils.getContentDispositionValue(ccftr.getFileName()));
		if(ccftr.getContentMD5() != null){
			// convert it from hex to base64.
			objMeta.setContentMD5(BinaryUtils.toBase64(BinaryUtils.fromHex(ccftr.getContentMD5())));
		}
		InitiateMultipartUploadResult imur = s3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(StackConfiguration.getS3Bucket(), key).withObjectMetadata(objMeta));
		// the token will be the ke
		ChunkedFileToken cft = new ChunkedFileToken();
		cft.setKey(key);
		cft.setUploadId(imur.getUploadId());
		cft.setFileName(ccftr.getFileName());
		cft.setContentType(contentType);
		cft.setContentMD5(ccftr.getContentMD5());
		return cft;
	}

	@Override
	public URL createChunkedFileUploadPartURL(UserInfo userInfo, ChunkRequest cpr) {
		if(cpr == null) throw new IllegalArgumentException("ChunkedPartRequest cannot be null");
		if(cpr.getChunkedFileToken() == null) throw new IllegalArgumentException("ChunkedPartRequest.chunkedFileToken cannot be null");
		if(cpr.getChunkNumber() == null) throw new IllegalArgumentException("ChunkedPartRequest.chunkNumber cannot be null");
		ChunkedFileToken token = cpr.getChunkedFileToken();
		int partNumber = cpr.getChunkNumber().intValue();
		// first validate the token
		validateChunkedFileToken(userInfo, token);
		// The part number cannot be less than one
		if(partNumber < 1) throw new IllegalArgumentException("partNumber cannot be less than one");
		String partKey = ChunkUtils.getChunkPartKey(token, partNumber);
		// For each block we want to create a pre-signed URL file.
		GeneratePresignedUrlRequest gpur = new GeneratePresignedUrlRequest(StackConfiguration.getS3Bucket(), partKey).withMethod(HttpMethod.PUT);
		if(cpr.getChunkedFileToken().getContentType() != null){
			gpur.setContentType(cpr.getChunkedFileToken().getContentType());
		}
		return  s3Client.generatePresignedUrl(gpur);
	}


	@Override
	public ChunkResult addChunkToFile(UserInfo userInfo, ChunkRequest cpr) {
		if(cpr == null) throw new IllegalArgumentException("ChunkedPartRequest cannot be null");
		if(cpr.getChunkedFileToken() == null) throw new IllegalArgumentException("ChunkedPartRequest.chunkedFileToken cannot be null");
		if(cpr.getChunkNumber() == null) throw new IllegalArgumentException("ChunkedPartRequest.chunkNumber cannot be null");
		ChunkedFileToken token = cpr.getChunkedFileToken();
		int partNumber = cpr.getChunkNumber().intValue();
		// first validate the token
		validateChunkedFileToken(userInfo, token);
		// The part number cannot be less than one
		if(partNumber < 1) throw new IllegalArgumentException("partNumber cannot be less than one");
		String bucket = StackConfiguration.getS3Bucket();
		String partKey = ChunkUtils.getChunkPartKey(token, partNumber);
		// copy this part to the larger file.
		CopyPartRequest copyPartRequest = new CopyPartRequest();
		copyPartRequest.setDestinationBucketName(bucket);
		copyPartRequest.setDestinationKey(token.getKey());
		copyPartRequest.setPartNumber(partNumber);
		copyPartRequest.setSourceBucketName(bucket);
		copyPartRequest.setSourceKey(partKey);
		copyPartRequest.setUploadId(token.getUploadId());
		// copy the part
		CopyPartResult result = s3Client.copyPart(copyPartRequest);
		// Now delete the original file since we now have a copy
		s3Client.deleteObject(bucket, partKey);
		ChunkResult cp = new ChunkResult();
		cp.setEtag(result.getETag());
		cp.setChunkNumber((long) result.getPartNumber());
		return cp;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public S3FileHandle completeChunkFileUpload(UserInfo userInfo, CompleteChunkedFileRequest ccfr) {
		if(ccfr == null) throw new IllegalArgumentException("CompleteChunkedFileRequest cannot be null");
		ChunkedFileToken token = ccfr.getChunkedFileToken();
		List<ChunkResult> chunkParts = ccfr.getChunkResults();
		// first validate the token
		validateChunkedFileToken(userInfo, token);
		if(chunkParts == null) throw new IllegalArgumentException("ChunkParts cannot be null");
		if(chunkParts.size() < 1) throw new IllegalArgumentException("ChunkParts.getList() must contain at least one ChunkPart");
		String bucket = StackConfiguration.getS3Bucket();
		String userId =  getUserId(userInfo);
		// Create the list of PartEtags
		List<PartETag> ptList = new LinkedList<PartETag>();
		for(ChunkResult cp: chunkParts){
			if(cp == null) 	throw new IllegalArgumentException("ChunkPart cannot be null");
			if(cp.getEtag() == null) throw new IllegalArgumentException("ChunkPart.getEtag() cannot be null");
			if(cp.getChunkNumber() == null) throw new IllegalArgumentException("ChunkPart.chunkNumber() cannot be null");
			PartETag pe = new PartETag(cp.getChunkNumber().intValue(), cp.getEtag());
			ptList.add(pe);
		}
		// We are now ready to complete the parts
		CompleteMultipartUploadResult cmp = s3Client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucket, token.getKey(), token.getUploadId(), ptList));
		// Update the metadata
		// The file is now in S3.
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setFileName(token.getFileName());
		fileHandle.setContentType(token.getContentType());
		fileHandle.setBucketName(bucket);
		fileHandle.setKey(token.getKey());
		fileHandle.setCreatedBy(userId);
		fileHandle.setCreatedOn(new Date(System.currentTimeMillis()));
		fileHandle.setEtag(UUID.randomUUID().toString());
		fileHandle.setContentMd5(token.getContentMD5());
		// Lookup the final file size
		ObjectMetadata current = s3Client.getObjectMetadata(bucket, token.getKey());
		// Capture the content length
		fileHandle.setContentSize(current.getContentLength());
		// Update the metadata
		// Save the file handle
		return fileHandleDao.createFile(fileHandle);
	}
	
	/**
	 * Validate that the user owns the token
	 */
	void validateChunkedFileToken(UserInfo userInfo, ChunkedFileToken token){
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(token == null) throw new IllegalArgumentException("ChunkedFileToken cannot be null");
		if(token.getKey() == null) throw new IllegalArgumentException("ChunkedFileToken.key cannot be null");
		if(token.getUploadId() == null) throw new IllegalArgumentException("ChunkedFileToken.uploadId cannot be null");
		if(token.getFileName() == null) throw new IllegalArgumentException("ChunkedFileToken.getFileName cannot be null");
		if(token.getContentType() == null) throw new IllegalArgumentException("ChunkedFileToken.getFileContentType cannot be null");
		// The token key must start with the User's id
		String userId =  getUserId(userInfo);
		if(!token.getKey().startsWith(userId)) throw new UnauthorizedException("The ChunkedFileToken: "+token+" does not belong to User: "+userId);
	}

	@Override
	public UploadDaemonStatus startUploadDeamon(UserInfo userInfo,	CompleteAllChunksRequest cacf) throws DatastoreException, NotFoundException {
		if(cacf == null) throw new IllegalArgumentException("CompleteAllChunksRequest cannot be null");
		validateChunkedFileToken(userInfo, cacf.getChunkedFileToken());
		// Start the daemon
		UploadDaemonStatus status = new UploadDaemonStatus();
		status.setPercentComplete(0.0);
		status.setStartedBy(getUserId(userInfo));
		status = uploadDaemonStatusDao.create(status);
		status = uploadDaemonStatusDao.get(status.getId());
		// Create a worker and add it to the pool.
		CompleteUploadWorker worker = new CompleteUploadWorker(uploadDaemonStatusDao, this, status, cacf);
		// Add this worker the primary pool
		uploadFileDaemonThreadPoolPrimary.submit(worker);
		// Return the status to the caller.
		return status;
	}

	@Override
	public UploadDaemonStatus getUploadDaemonStatus(UserInfo userInfo, String daemonId) throws DatastoreException, NotFoundException {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(daemonId == null) throw new IllegalArgumentException("DaemonID cannot be null");
		UploadDaemonStatus status = uploadDaemonStatusDao.get(daemonId);
		// Only the user that started the daemon can see the status
		if(!authorizationManager.isUserCreatorOrAdmin(userInfo, status.getStartedBy())){
			throw new UnauthorizedException("Only the user that started the daemon may access the daemon status");
		}
		return status;
	}

}
