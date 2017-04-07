package org.sagebionetworks.repo.manager.file;

import static org.sagebionetworks.downloadtools.FileUtils.DEFAULT_FILE_CHARSET;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.dao.ObjectRecordBatch;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.downloadtools.FileUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.audit.ObjectRecordQueue;
import org.sagebionetworks.repo.manager.file.transfer.TransferRequest;
import org.sagebionetworks.repo.manager.file.transfer.TransferUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.UploadDaemonStatusDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOStorageLocationDAOImpl;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyResult;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalS3UploadDestination;
import org.sagebionetworks.repo.model.file.ExternalUploadDestination;
import org.sagebionetworks.repo.model.file.FileDownloadRecord;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileHandleCopyRecord;
import org.sagebionetworks.repo.model.file.FileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.FileHandleCopyResult;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.FileResult;
import org.sagebionetworks.repo.model.file.FileResultFailureCode;
import org.sagebionetworks.repo.model.file.HasPreviewId;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandleInterface;
import org.sagebionetworks.repo.model.file.S3UploadDestination;
import org.sagebionetworks.repo.model.file.State;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.util.ContentTypeUtils;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.sagebionetworks.utils.MD5ChecksumHelper;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.HttpMethod;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import com.amazonaws.services.s3.model.CORSRule.AllowedMethods;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.util.BinaryUtils;
import com.google.common.collect.Lists;

/**
 * Basic implementation of the file upload manager.
 * 
 * @author John
 * 
 */
public class FileHandleManagerImpl implements FileHandleManager {

	private static final String FILE_DOWNLOAD_RECORD_TYPE = FileDownloadRecord.class.getSimpleName().toLowerCase();

	public static final String FILE_HANDLE_COPY_RECORD_TYPE = FileHandleCopyRecord.class.getSimpleName().toLowerCase();

	public static final String MUST_INCLUDE_EITHER = "Must include either FileHandles or pre-signed URLs or preview pre-signed URLs";

	public static final String UNAUTHORIZED_PROXY_FILE_HANDLE_MSG = "Only the creator of the ProxyStorageLocationSettings or a user with the 'create' permission on ProxyStorageLocationSettings.benefactorId can create a ProxyFileHandle using this storage location ID.";
	
	public static final long PRESIGNED_URL_EXPIRE_TIME_MS = 30 * 1000; // 30
																		// secs

	public static final int MAX_REQUESTS_PER_CALL = 100;

	public static final String MAX_REQUESTS_PER_CALL_MESSAGE = "Request exceeds the maximum number of objects per request: "+MAX_REQUESTS_PER_CALL;

	public static final String DUPLICATED_REQUEST_MESSAGE = "Request contains duplicated FileHandleId.";

	static private Log log = LogFactory.getLog(FileHandleManagerImpl.class);

	private static String FILE_TOKEN_TEMPLATE = "%1$s/%2$s/%3$s"; // userid/UUID/filename

	public static final String NOT_SET = "NOT_SET";
	
	private static final String DEFAULT_COMPRESSED_FILE_NAME = "compressed.txt.gz";
	
	private static final String GZIP_CONTENT_ENCODING = "gzip";

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

	@Autowired
	MultipartManager multipartManager;

	@Autowired
	ProjectSettingsManager projectSettingsManager;
	
	@Autowired
	StorageLocationDAO storageLocationDAO;

	@Autowired
	NodeManager nodeManager;
	
	@Autowired
	FileHandleAuthorizationManager fileHandleAuthorizationManager;
	
	@Autowired
	ObjectRecordQueue objectRecordQueue;

	@Autowired
	private IdGenerator idGenerator;

	/**
	 * This is the maximum amount of time the upload workers are allowed to take
	 * before timing out.
	 */
	long multipartUploadDaemonTimeoutMS;

	/**
	 * This is the maximum amount of time the upload workers are allowed to take
	 * before timing out. Injected via Spring
	 * 
	 * @param multipartUploadDaemonTimeoutMS
	 */
	public void setMultipartUploadDaemonTimeoutMS(
			long multipartUploadDaemonTimeoutMS) {
		this.multipartUploadDaemonTimeoutMS = multipartUploadDaemonTimeoutMS;
	}

	/**
	 * Used by spring
	 */
	public FileHandleManagerImpl() {
		super();
	}

	/**
	 * Get the User's ID
	 * 
	 * @param userInfo
	 * @return
	 */
	public String getUserId(UserInfo userInfo) {
		return userInfo.getId().toString();
	}


	/**
	 * Build up the S3FileMetadata.
	 * 
	 * @param contentType
	 * @param userId
	 * @param fileName
	 * @return
	 */
	public static TransferRequest createRequest(String contentType,
			String userId, String fileName, InputStream inputStream) {
		// Create a token for this file
		TransferRequest request = new TransferRequest();
		request.setContentType(ContentTypeUtils.getContentType(contentType,
				fileName));
		request.setS3bucketName(StackConfiguration.getS3Bucket());
		request.setS3key(createNewKey(userId, fileName));
		request.setFileName(fileName);
		request.setInputStream(inputStream);
		return request;
	}

	/**
	 * Create a new key
	 * 
	 * @param userId
	 * @param fileName
	 * @return
	 */
	private static String createNewKey(String userId, String fileName) {
		return String.format(FILE_TOKEN_TEMPLATE, userId, UUID.randomUUID()
				.toString(), fileName);
	}

	@Override
	public FileHandle getRawFileHandle(UserInfo userInfo, String handleId)
			throws DatastoreException, NotFoundException {
		if (userInfo == null)
			throw new IllegalArgumentException("UserInfo cannot be null");
		if (handleId == null)
			throw new IllegalArgumentException("FileHandleId cannot be null");
		// Get the file handle
		FileHandle handle = fileHandleDao.get(handleId);
		// Only the user that created this handle is authorized to get it.
		AuthorizationManagerUtil
				.checkAuthorizationAndThrowException(authorizationManager
						.canAccessRawFileHandleByCreator(userInfo, handleId,
								handle.getCreatedBy()));
		return handle;
	}

	@WriteTransaction
	@Override
	public void deleteFileHandle(UserInfo userInfo, String handleId)
			throws DatastoreException {
		if (userInfo == null)
			throw new IllegalArgumentException("UserInfo cannot be null");
		if (handleId == null)
			throw new IllegalArgumentException("FileHandleId cannot be null");
		// Get the file handle
		try {
			FileHandle handle = fileHandleDao.get(handleId);
			// Is the user authorized?
			AuthorizationManagerUtil
					.checkAuthorizationAndThrowException(authorizationManager
							.canAccessRawFileHandleByCreator(userInfo,
									handleId, handle.getCreatedBy()));
			// If this file has a preview then we want to delete the preview as
			// well.
			if (handle instanceof HasPreviewId) {
				HasPreviewId hasPreview = (HasPreviewId) handle;
				if (hasPreview.getPreviewId() != null
						&& !handle.getId().equals(hasPreview.getPreviewId())) {
					// Delete the preview.
					deleteFileHandle(userInfo, hasPreview.getPreviewId());
				}
			}
			// Is this an S3 file?
			if (handle instanceof S3FileHandleInterface) {
				S3FileHandleInterface s3Handle = (S3FileHandleInterface) handle;
				// at this point, we need to note that multiple S3FileHandles can point to the same bucket/key. We need
				// to check if this is the last S3FileHandle to point to this S3 object
				if (fileHandleDao.getS3objectReferenceCount(s3Handle.getBucketName(), s3Handle.getKey()) <= 1) {
					// Delete the file from S3
					s3Client.deleteObject(s3Handle.getBucketName(), s3Handle.getKey());
				}
			}
			// Delete the handle from the DB
			fileHandleDao.delete(handleId);
		} catch (NotFoundException e) {
			// there is nothing to do if the handle does not exist.
			return;
		}

	}

	@Override
	public String getRedirectURLForFileHandle(String handleId)
			throws DatastoreException, NotFoundException {
		// First lookup the file handle
		FileHandle handle = fileHandleDao.get(handleId);
		return getURLForFileHandle(handle);
	}

	/**
	 * @param handle
	 * @return
	 */
	public String getURLForFileHandle(FileHandle handle) {
		if (handle instanceof ExternalFileHandle) {
			ExternalFileHandle efh = (ExternalFileHandle) handle;
			return efh.getExternalURL();
		} else if(handle instanceof ProxyFileHandle){
			ProxyFileHandle proxyHandle = (ProxyFileHandle) handle;
			StorageLocationSetting storage = this.storageLocationDAO.get(proxyHandle.getStorageLocationId());
			if(!(storage instanceof ProxyStorageLocationSettings)){
				throw new IllegalArgumentException("ProxyFileHandle.storageLocation is not of type"+ProxyStorageLocationSettings.class.getName());
			}
			ProxyStorageLocationSettings proxyStorage = (ProxyStorageLocationSettings) storage;
			return ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle, proxyStorage, new Date(System.currentTimeMillis() + PRESIGNED_URL_EXPIRE_TIME_MS));
		} else if (handle instanceof S3FileHandleInterface) {
			S3FileHandleInterface s3File = (S3FileHandleInterface) handle;
			// Create a pre-signed url
			GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(s3File.getBucketName(), s3File.getKey(), HttpMethod.GET);
			request.setExpiration(new Date(System.currentTimeMillis() + PRESIGNED_URL_EXPIRE_TIME_MS));

			ResponseHeaderOverrides responseHeaderOverrides = new ResponseHeaderOverrides();

			String contentType = handle.getContentType();
			if (StringUtils.isNotEmpty(contentType) && !NOT_SET.equals(contentType)) {
				responseHeaderOverrides.setContentType(contentType);
			}
			String fileName = handle.getFileName();
			if (StringUtils.isNotEmpty(fileName) && !NOT_SET.equals(fileName)) {
				responseHeaderOverrides.setContentDisposition("attachment; filename=" + fileName);
			}

			request.setResponseHeaders(responseHeaderOverrides);
			return s3Client.generatePresignedUrl(request).toExternalForm();
		} else {
			throw new IllegalArgumentException("Unknown FileHandle class: "
					+ handle.getClass().getName());
		}
	}

	@Override
	public String getPreviewFileHandleId(String handleId)
			throws DatastoreException, NotFoundException {
		return fileHandleDao.getPreviewFileHandleId(handleId);
	}

	@Override
	public void clearPreview(UserInfo userInfo, String handleId)
			throws DatastoreException, NotFoundException {
		if (userInfo == null)
			throw new IllegalArgumentException("UserInfo cannot be null");
		if (handleId == null)
			throw new IllegalArgumentException("FileHandleId cannot be null");

		// Get the file handle
		FileHandle handle = fileHandleDao.get(handleId);
		// Is the user authorized?
		if (!authorizationManager.canAccessRawFileHandleByCreator(userInfo,
				handleId, handle.getCreatedBy()).getAuthorized()) {
			throw new UnauthorizedException(
					"Only the creator of a FileHandle can clear the preview");
		}

		// clear the preview id
		fileHandleDao.setPreviewId(handleId, null);
	}

	@Override
	public FileHandleResults getAllFileHandles(Iterable<String> idList, boolean includePreviews) throws DatastoreException, NotFoundException {
		return fileHandleDao.getAllFileHandles(idList, includePreviews);
	}

	@Override
	public Map<String, FileHandle> getAllFileHandlesBatch(Iterable<String> idsList)
			throws DatastoreException, NotFoundException {
		return fileHandleDao.getAllFileHandlesBatch(idsList);
	}

	@WriteTransaction
	@Override
	public ExternalFileHandle createExternalFileHandle(UserInfo userInfo,
			ExternalFileHandle fileHandle) {
		if (userInfo == null)
			throw new IllegalArgumentException("UserInfo cannot be null");
		if (fileHandle == null)
			throw new IllegalArgumentException("FileHandle cannot be null");
		if (fileHandle.getExternalURL() == null)
			throw new IllegalArgumentException("ExternalURL cannot be null");
		if (fileHandle.getFileName() == null) {
			fileHandle.setFileName(NOT_SET);
		}
		if (fileHandle.getContentType() == null) {
			fileHandle.setContentType(NOT_SET);
		}
		// The URL must be a URL
		ValidateArgument.validUrl(fileHandle.getExternalURL());
		// set this user as the creator of the file
		fileHandle.setCreatedBy(getUserId(userInfo));
		fileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fileHandle.setEtag(UUID.randomUUID().toString());
		// Save the file metadata to the DB.
		return (ExternalFileHandle) fileHandleDao.createFile(fileHandle);
	}

	/**
	 * Called by Spring when after the bean is created..
	 */
	public void initialize() {
		// We need to ensure that Cross-Origin Resource Sharing (CORS) is
		// enabled on the bucket
		String bucketName = StackConfiguration.getS3Bucket();
		BucketCrossOriginConfiguration bcoc = s3Client
				.getBucketCrossOriginConfiguration(bucketName);
		if (bcoc == null || bcoc.getRules() == null
				|| bcoc.getRules().size() < 1) {
			// Set the CORS
			resetBuckCORS(bucketName);
		} else {
			// There can only be on rule on the bucket
			if (bcoc.getRules().size() > 1) {
				// rest the
				resetBuckCORS(bucketName);
			} else {
				// Check the rule
				CORSRule currentRule = bcoc.getRules().get(0);
				if (!FileHandleManager.AUTO_GENERATED_ALLOW_ALL_CORS_RULE_ID
						.equals(currentRule.getId())) {
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
		log.debug("Setting the buck Cross-Origin Resource Sharing (CORS) on bucket: "
				+ bucketName + " for the first time...");
		// We need to add the rules
		BucketCrossOriginConfiguration bcoc = new BucketCrossOriginConfiguration();
		CORSRule allowAll = new CORSRule();
		allowAll.setId(FileHandleManager.AUTO_GENERATED_ALLOW_ALL_CORS_RULE_ID);
		allowAll.setAllowedOrigins("*");
		allowAll.setAllowedMethods(AllowedMethods.GET, AllowedMethods.PUT,
				AllowedMethods.POST, AllowedMethods.HEAD);
		allowAll.setMaxAgeSeconds(300);
		allowAll.setAllowedHeaders("*");
		bcoc.withRules(allowAll);
		s3Client.setBucketCrossOriginConfiguration(
				StackConfiguration.getS3Bucket(), bcoc);
		log.info("Set CORSRule on bucket: " + bucketName + " to be: "
				+ allowAll);
	}

	@Override
	public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration() {
		String bucketName = StackConfiguration.getS3Bucket();
		return s3Client.getBucketCrossOriginConfiguration(bucketName);
	}

	@Override
	public ChunkedFileToken createChunkedFileUploadToken(UserInfo userInfo, CreateChunkedFileTokenRequest ccftr) throws DatastoreException,
			NotFoundException {
		if (userInfo == null)
			throw new IllegalArgumentException("UserInfo cannot be null");
		String userId = getUserId(userInfo);
		return this.multipartManager.createChunkedFileUploadToken(ccftr, ccftr.getStorageLocationId(), userId);
	}

	@Deprecated
	@Override
	public URL createChunkedFileUploadPartURL(UserInfo userInfo, ChunkRequest cpr) throws DatastoreException, NotFoundException {
		if (cpr == null)
			throw new IllegalArgumentException(
					"ChunkedPartRequest cannot be null");
		if (cpr.getChunkedFileToken() == null)
			throw new IllegalArgumentException(
					"ChunkedPartRequest.chunkedFileToken cannot be null");
		if (cpr.getChunkNumber() == null)
			throw new IllegalArgumentException(
					"ChunkedPartRequest.chunkNumber cannot be null");
		ChunkedFileToken token = cpr.getChunkedFileToken();
		// first validate the token
		validateChunkedFileToken(userInfo, token);
		return multipartManager.createChunkedFileUploadPartURL(cpr, token.getStorageLocationId());
	}

	@Override
	public ChunkResult addChunkToFile(UserInfo userInfo, ChunkRequest cpr) throws DatastoreException, NotFoundException {
		if (cpr == null)
			throw new IllegalArgumentException(
					"ChunkedPartRequest cannot be null");
		if (cpr.getChunkedFileToken() == null)
			throw new IllegalArgumentException(
					"ChunkedPartRequest.chunkedFileToken cannot be null");
		if (cpr.getChunkNumber() == null)
			throw new IllegalArgumentException(
					"ChunkedPartRequest.chunkNumber cannot be null");
		ChunkedFileToken token = cpr.getChunkedFileToken();
		int partNumber = cpr.getChunkNumber().intValue();
		// first validate the token
		validateChunkedFileToken(userInfo, token);

		// The part number cannot be less than one
		if (partNumber < 1)
			throw new IllegalArgumentException(
					"partNumber cannot be less than one");
		ChunkResult result = this.multipartManager.copyPart(token, partNumber, token.getStorageLocationId());
		// Now delete the original file since we now have a copy
		String partkey = this.multipartManager.getChunkPartKey(token, partNumber);
		StorageLocationSetting storageLocationSettins = projectSettingsManager.getStorageLocationSetting(token.getStorageLocationId());
		String bucket = MultipartUtils.getBucket(storageLocationSettins);
		s3Client.deleteObject(bucket, partkey);
		return result;
	}

	@WriteTransaction
	@Override
	public S3FileHandle completeChunkFileUpload(UserInfo userInfo, CompleteChunkedFileRequest ccfr) throws DatastoreException,
			NotFoundException {
		if (ccfr == null)
			throw new IllegalArgumentException(
					"CompleteChunkedFileRequest cannot be null");
		ChunkedFileToken token = ccfr.getChunkedFileToken();
		// first validate the token
		validateChunkedFileToken(userInfo, token);
		String userId = getUserId(userInfo);
		// Complete the multi-part
		return this.multipartManager.completeChunkFileUpload(ccfr, token.getStorageLocationId(), userId);
	}

	/**
	 * Validate that the user owns the token
	 */
	void validateChunkedFileToken(UserInfo userInfo, ChunkedFileToken token) {
		if (userInfo == null)
			throw new IllegalArgumentException("UserInfo cannot be null");
		if (token == null)
			throw new IllegalArgumentException(
					"ChunkedFileToken cannot be null");
		if (token.getKey() == null)
			throw new IllegalArgumentException(
					"ChunkedFileToken.key cannot be null");
		if (token.getUploadId() == null)
			throw new IllegalArgumentException(
					"ChunkedFileToken.uploadId cannot be null");
		if (token.getFileName() == null)
			throw new IllegalArgumentException(
					"ChunkedFileToken.getFileName cannot be null");
		if (token.getContentType() == null)
			throw new IllegalArgumentException(
					"ChunkedFileToken.getFileContentType cannot be null");
		// The token key must start with the User's id (and the baseKey if any)
		String userId = getUserId(userInfo);
		if (!token.getKey().startsWith(userId)
				&& token.getKey().indexOf(
						MultipartUtils.FILE_TOKEN_TEMPLATE_SEPARATOR + userId + MultipartUtils.FILE_TOKEN_TEMPLATE_SEPARATOR) == -1)
			throw new UnauthorizedException("The ChunkedFileToken: " + token
					+ " does not belong to User: " + userId);
	}

	@Override
	public UploadDaemonStatus startUploadDeamon(UserInfo userInfo,
			CompleteAllChunksRequest cacf) throws DatastoreException,
			NotFoundException {
		if (cacf == null)
			throw new IllegalArgumentException(
					"CompleteAllChunksRequest cannot be null");
		validateChunkedFileToken(userInfo, cacf.getChunkedFileToken());
		String userId = getUserId(userInfo);
		// Start the daemon
		UploadDaemonStatus status = new UploadDaemonStatus();
		status.setPercentComplete(0.0);
		status.setStartedBy(getUserId(userInfo));
		status.setRunTimeMS(0l);
		status.setState(State.PROCESSING);
		status = uploadDaemonStatusDao.create(status);
		// Create a worker and add it to the pool.
		CompleteUploadWorker worker = new CompleteUploadWorker(uploadDaemonStatusDao, uploadFileDaemonThreadPoolSecondary, status, cacf,
				multipartManager, multipartUploadDaemonTimeoutMS, userId);
		// Get a new copy of the status so we are not returning the same
		// instance that we passed to the worker.
		status = uploadDaemonStatusDao.get(status.getDaemonId());
		// Add this worker the primary pool
		uploadFileDaemonThreadPoolPrimary.submit(worker);
		// Return the status to the caller.
		return status;
	}

	@Override
	public S3FileHandle multipartUploadLocalFile(UserInfo userInfo,
			File fileToUpload, String contentType, ProgressListener listener) {
		String userId = getUserId(userInfo);
		return multipartManager.multipartUploadLocalFile(null, userId,
				fileToUpload, contentType, listener);
	}

	@Override
	public UploadDaemonStatus getUploadDaemonStatus(UserInfo userInfo,
			String daemonId) throws DatastoreException, NotFoundException {
		if (userInfo == null)
			throw new IllegalArgumentException("UserInfo cannot be null");
		if (daemonId == null)
			throw new IllegalArgumentException("DaemonID cannot be null");
		UploadDaemonStatus status = uploadDaemonStatusDao.get(daemonId);
		// Only the user that started the daemon can see the status
		if (!authorizationManager.isUserCreatorOrAdmin(userInfo,
				status.getStartedBy())) {
			throw new UnauthorizedException(
					"Only the user that started the daemon may access the daemon status");
		}
		return status;
	}

	@Override
	public String getRedirectURLForFileHandle(UserInfo userInfo,
			String fileHandleId) throws DatastoreException, NotFoundException {
		if (userInfo == null) {
			throw new IllegalArgumentException("User cannot be null");
		}
		if (fileHandleId == null) {
			throw new IllegalArgumentException("FileHandleId cannot be null");
		}
		FileHandle handle = fileHandleDao.get(fileHandleId);
		// Only the user that created the FileHandle can get the URL directly.
		if (!authorizationManager.isUserCreatorOrAdmin(userInfo,
				handle.getCreatedBy())) {
			throw new UnauthorizedException(
					"Only the user that created the FileHandle can get the URL of the file.");
		}
		return getURLForFileHandle(handle);
	}

	@Override
	@Deprecated
	public List<UploadDestination> getUploadDestinations(UserInfo userInfo, String parentId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		List<UploadDestinationLocation> uploadDestinationLocations = getUploadDestinationLocations(userInfo, parentId);

		List<UploadDestination> destinations = Lists.newArrayListWithExpectedSize(4);
		for (UploadDestinationLocation uploadDestinationLocation : uploadDestinationLocations) {
			destinations.add(getUploadDestination(userInfo, parentId, uploadDestinationLocation.getStorageLocationId()));
		}
		return destinations;
	}

	@Override
	public List<UploadDestinationLocation> getUploadDestinationLocations(UserInfo userInfo, String parentId) throws DatastoreException,
			NotFoundException {
		UploadDestinationListSetting uploadDestinationsSettings = projectSettingsManager.getProjectSettingForNode(userInfo, parentId,
				ProjectSettingsType.upload, UploadDestinationListSetting.class);

		// make sure there is always one entry
		if (uploadDestinationsSettings == null || uploadDestinationsSettings.getLocations() == null
				|| uploadDestinationsSettings.getLocations().isEmpty()) {
			UploadDestinationLocation uploadDestinationLocation = new UploadDestinationLocation();
			uploadDestinationLocation.setStorageLocationId(DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID);
			uploadDestinationLocation.setUploadType(UploadType.S3);
			return Collections.<UploadDestinationLocation> singletonList(uploadDestinationLocation);
		} else {
			return projectSettingsManager.getUploadDestinationLocations(userInfo, uploadDestinationsSettings.getLocations());
		}
	}

	@Override
	public UploadDestination getUploadDestination(UserInfo userInfo, String parentId, Long storageLocationId) throws DatastoreException,
			NotFoundException {
		ValidateArgument.required(storageLocationId, "storageLocationId");
		// handle default case
		if (storageLocationId.equals(DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID)) {
			return DBOStorageLocationDAOImpl.getDefaultUploadDestination();
		}

		StorageLocationSetting storageLocationSetting = storageLocationDAO.get(storageLocationId);

		UploadDestination uploadDestination;

		if (storageLocationSetting instanceof S3StorageLocationSetting) {
			uploadDestination = new S3UploadDestination();
		} else if (storageLocationSetting instanceof ExternalS3StorageLocationSetting) {
			ExternalS3StorageLocationSetting externalS3StorageLocationSetting = (ExternalS3StorageLocationSetting) storageLocationSetting;
			ExternalS3UploadDestination externalS3UploadDestination = new ExternalS3UploadDestination();
			externalS3UploadDestination.setBucket(externalS3StorageLocationSetting.getBucket());
			externalS3UploadDestination.setBaseKey(externalS3StorageLocationSetting.getBaseKey());
			externalS3UploadDestination.setEndpointUrl(externalS3StorageLocationSetting.getEndpointUrl());
			uploadDestination = externalS3UploadDestination;
		} else if (storageLocationSetting instanceof ExternalStorageLocationSetting) {
			String filename = UUID.randomUUID().toString();
			List<EntityHeader> nodePath = nodeManager.getNodePath(userInfo, parentId);
			uploadDestination = createExternalUploadDestination((ExternalStorageLocationSetting) storageLocationSetting,
					nodePath, filename);
		} else {
			throw new IllegalArgumentException("Cannot handle upload destination location setting of type: "
					+ storageLocationSetting.getClass().getName());
		}

		uploadDestination.setStorageLocationId(storageLocationId);
		uploadDestination.setUploadType(storageLocationSetting.getUploadType());
		uploadDestination.setBanner(storageLocationSetting.getBanner());
		return uploadDestination;
	}

	@Override
	public UploadDestination getDefaultUploadDestination(UserInfo userInfo, String parentId) throws DatastoreException, NotFoundException {
		UploadDestinationListSetting uploadDestinationsSettings = 
				projectSettingsManager.getProjectSettingForNode(userInfo, parentId,
				ProjectSettingsType.upload, UploadDestinationListSetting.class);

		// make sure there is always one entry
		Long storageLocationId;
		if (uploadDestinationsSettings == null ||
				uploadDestinationsSettings.getLocations() == null ||
				uploadDestinationsSettings.getLocations().isEmpty() ||
				uploadDestinationsSettings.getLocations().get(0) == null) {
			storageLocationId = DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID;
		} else {
			storageLocationId = uploadDestinationsSettings.getLocations().get(0);
		}
		return getUploadDestination(userInfo, parentId, storageLocationId);
	}

	private UploadDestination createExternalUploadDestination(ExternalStorageLocationSetting externalUploadDestinationSetting,
			List<EntityHeader> nodePath, String filename) {
		return createExternalUploadDestination(externalUploadDestinationSetting.getUrl(),
				externalUploadDestinationSetting.getSupportsSubfolders(), nodePath, filename);
	}

	private UploadDestination createExternalUploadDestination(String baseUrl, Boolean supportsSubfolders, List<EntityHeader> nodePath,
			String filename) {
		StringBuilder url = new StringBuilder(baseUrl);
		if (url.length() == 0) {
			throw new IllegalArgumentException("The url for the external upload destination setting is empty");
		}
		if (url.charAt(url.length() - 1) != '/') {
			url.append('/');
		}
		// need to add subfolders here if supported
		if (BooleanUtils.isTrue(supportsSubfolders)) {
			if (nodePath.size() > 0) {
				// the first path in the node path is always "root". We don't
				// want that to show up in the file path
				nodePath = nodePath.subList(1, nodePath.size());
			}
			for (EntityHeader node : nodePath) {
				try {
					// we need to url encode, but r client does not like '+' for
					// space. So encode with java encoder and
					// then replace '+' with %20
					url.append(URLEncoder.encode(node.getName(), "UTF-8").replace("+", "%20")).append('/');
				} catch (UnsupportedEncodingException e) {
					// shouldn't happen
					throw new IllegalArgumentException(e.getMessage(), e);
				}
			}
		}
		url.append(filename);
		ExternalUploadDestination externalUploadDestination = new ExternalUploadDestination();
		externalUploadDestination.setUrl(url.toString());
		return externalUploadDestination;
	}

	/**
	 * Extract the file name from the keys
	 * 
	 * @param key
	 * @return
	 */
	public static String extractFileNameFromKey(String key) {
		if (key == null) {
			return null;
		}
		String[] slash = key.split("/");
		if (slash.length > 0) {
			return slash[slash.length - 1];
		}
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.file.FileHandleManager#createCompressedFileFromString(java.lang.String, java.util.Date, java.lang.String)
	 */
	@Override
	public S3FileHandle createCompressedFileFromString(String createdBy,
			Date modifiedOn, String fileContents) throws UnsupportedEncodingException, IOException {
		return createCompressedFileFromString(createdBy, modifiedOn, fileContents, "application/octet-stream");
	}	
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.file.FileHandleManager#createCompressedFileFromString(java.lang.String, java.util.Date, java.lang.String)
	 */
	@Override
	public S3FileHandle createCompressedFileFromString(String createdBy,
			Date modifiedOn, String fileContents, String mimeType) throws UnsupportedEncodingException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		FileUtils.writeString(fileContents, DEFAULT_FILE_CHARSET, /*gzip*/true, out);
		byte[] compressedBytes = out.toByteArray();
		ContentType contentType = ContentType.create(mimeType, DEFAULT_FILE_CHARSET);
		return createFileFromByteArray(createdBy, modifiedOn, compressedBytes, null, contentType, GZIP_CONTENT_ENCODING);
	}
	
	@Override
	public S3FileHandle createFileFromByteArray(String createdBy,
				Date modifiedOn, byte[] fileContents, String fileName, ContentType contentType, String contentEncoding) throws UnsupportedEncodingException, IOException {
		// Create the compress string
		ByteArrayInputStream in = new ByteArrayInputStream(fileContents);
		String md5 = MD5ChecksumHelper.getMD5ChecksumForByteArray(fileContents);
		String hexMd5 = BinaryUtils.toBase64(BinaryUtils.fromHex(md5));
		// Upload the file to S3
		if (fileName==null) fileName=DEFAULT_COMPRESSED_FILE_NAME;
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentType(contentType.toString());
		meta.setContentMD5(hexMd5);
		meta.setContentLength(fileContents.length);
		meta.setContentDisposition(TransferUtils.getContentDispositionValue(fileName));
		if (contentEncoding!=null) meta.setContentEncoding(contentEncoding);
		String key = MultipartUtils.createNewKey(createdBy, fileName, null);
		String bucket = StackConfiguration.getS3Bucket();
		s3Client.putObject(bucket, key, in, meta);
		// Create the file handle
		S3FileHandle handle = new S3FileHandle();
		handle.setBucketName(bucket);
		handle.setKey(key);
		handle.setContentMd5(md5);
		handle.setContentType(meta.getContentType());
		handle.setContentSize(meta.getContentLength());
		handle.setFileName(fileName);
		handle.setCreatedBy(createdBy);
		handle.setCreatedOn(modifiedOn);
		handle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle.setEtag(UUID.randomUUID().toString());
		return (S3FileHandle) fileHandleDao.createFile(handle);
	}
	
	/**
	 * Retrieves file, decompressing if Content-Encoding indicates that it's gzipped
	 * @param fileHandleId
	 * @return
	 */
	public String downloadFileToString(String fileHandleId) throws IOException {
		URL url;
		try {
			url = new URL(getRedirectURLForFileHandle(fileHandleId));
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		// Read the file
		try {
			InputStream in = null;
			try {
				URLConnection connection = url.openConnection();
				String contentEncoding = connection.getHeaderField(HttpHeaders.CONTENT_ENCODING);
				String contentTypeString = connection.getHeaderField(HttpHeaders.CONTENT_TYPE);
				Charset charset = ContentTypeUtil.getCharsetFromContentTypeString(contentTypeString);
				in = connection.getInputStream();
				if (contentEncoding!=null && GZIP_CONTENT_ENCODING.equals(contentEncoding)) {
					return FileUtils.readStreamAsString(in, charset, /*gunzip*/true);
				} else {
					return FileUtils.readStreamAsString(in, charset, /*gunzip*/false);
				}
			} finally {
				if (in != null) {
					in.close();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	@Override
	public S3FileHandle createExternalS3FileHandle(UserInfo userInfo,
			S3FileHandle fileHandle) {
		if (userInfo == null){
			throw new IllegalArgumentException("UserInfo cannot be null");
		}
		if (fileHandle == null){
			throw new IllegalArgumentException("FileHandle cannot be null");
		}
		if(fileHandle.getBucketName() == null){
			throw new IllegalArgumentException("FileHandle.bucket cannot be null");
		}
		if(fileHandle.getKey() == null){
			throw new IllegalArgumentException("FileHandle.key cannot be null");
		}
		if(fileHandle.getStorageLocationId() == null){
			throw new IllegalArgumentException("FileHandle.storageLocationId cannot be null");
		}
		if(fileHandle.getContentMd5() == null){
			throw new IllegalArgumentException("FileHandle.contentMd5 cannot be null");
		}
		if (fileHandle.getFileName() == null) {
			fileHandle.setFileName(NOT_SET);
		}
		if (fileHandle.getContentType() == null) {
			fileHandle.setContentType(NOT_SET);
		}
		// Lookup the storage location
		StorageLocationSetting sls = storageLocationDAO.get(fileHandle.getStorageLocationId());
		ExternalS3StorageLocationSetting esls = null;
		if(!(sls instanceof ExternalS3StorageLocationSetting)){
			throw new IllegalArgumentException("StorageLocationSetting.id="+fileHandle.getStorageLocationId()+" was not of the expected type: "+ExternalS3StorageLocationSetting.class.getName());
		}
		esls = (ExternalS3StorageLocationSetting) sls;
		if(!fileHandle.getBucketName().equals(esls.getBucket())){
			throw new IllegalArgumentException("The bucket for ExternalS3StorageLocationSetting.id="+fileHandle.getStorageLocationId()+" does not match the provided bucket: "+fileHandle.getBucketName());
		}
		
		/*
		 *  The creation of the ExternalS3StorageLocationSetting already validates that the user has
		 *  permission to update the bucket. So the creator of the storage location is
		 *  the only one that can create an S3FileHandle using that storage location Id. 
		 */
		if(!esls.getCreatedBy().equals(userInfo.getId())){
			throw new UnauthorizedException("Only the creator of ExternalS3StorageLocationSetting.id="+fileHandle.getStorageLocationId()+" can create an external S3FileHandle with storagaeLocationId = "+fileHandle.getStorageLocationId());
		}
		try {
			ObjectMetadata summary = s3Client.getObjectMetadata(fileHandle.getBucketName(), fileHandle.getKey());
			if (fileHandle.getContentSize() == null) {
				fileHandle.setContentSize(summary.getContentLength());
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to access the file at bucket: "+fileHandle.getBucketName()+" key: "+fileHandle.getKey()+".", e);
		} 
		// set this user as the creator of the file
		fileHandle.setCreatedBy(getUserId(userInfo));
		fileHandle.setCreatedOn(new Date());
		fileHandle.setEtag(UUID.randomUUID().toString());
		fileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		// Save the file metadata to the DB.
		return (S3FileHandle) fileHandleDao.createFile(fileHandle);
	}
	
	@Override
	public ProxyFileHandle createExternalProxyFileHandle(UserInfo userInfo,
			ProxyFileHandle proxyFileHandle) {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(proxyFileHandle, "ProxyFileHandle");
		ValidateArgument.required(proxyFileHandle.getContentMd5(), "ProxyFileHandle.contentMd5");
		ValidateArgument.required(proxyFileHandle.getContentSize(), "ProxyFileHandle.contentSize");
		ValidateArgument.required(proxyFileHandle.getContentType(), "ProxyFileHandle.contentType");
		ValidateArgument.required(proxyFileHandle.getFileName(), "ProxyFileHandle.fileName");
		ValidateArgument.required(proxyFileHandle.getFilePath(), "ProxyFileHandle.filePath");
		ValidateArgument.required(proxyFileHandle.getStorageLocationId(), "ProxyFileHandle.storageLocationId");
		StorageLocationSetting sls = storageLocationDAO.get(proxyFileHandle.getStorageLocationId());
		if(!(sls instanceof ProxyStorageLocationSettings)){
			throw new IllegalArgumentException("ProxyFileHandle.storageLocationId must refer to a valid ProxyStorageLocationSettings.");
		}
		ProxyStorageLocationSettings proxyLocation = (ProxyStorageLocationSettings) sls;
		// If the user is not the creator of the location they must have 'create' on the benefactor.
		if (!userInfo.getId().equals(proxyLocation.getCreatedBy())) {
			// If the benefactor is not set.
			if (proxyLocation.getBenefactorId() == null
					|| !authorizationManager.canAccess(userInfo,
							proxyLocation.getBenefactorId(), ObjectType.ENTITY,
							ACCESS_TYPE.CREATE).getAuthorized()) {
				throw new UnauthorizedException(
						UNAUTHORIZED_PROXY_FILE_HANDLE_MSG);
			}
		}
		// set this user as the creator of the file
		proxyFileHandle.setCreatedBy(getUserId(userInfo));
		proxyFileHandle.setCreatedOn(new Date());
		proxyFileHandle.setEtag(UUID.randomUUID().toString());
		proxyFileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		// Save the file metadata to the DB.
		return (ProxyFileHandle) fileHandleDao.createFile(proxyFileHandle);
	}

	@Override
	public S3FileHandle createS3FileHandleCopy(UserInfo userInfo, String handleIdToCopyFrom, String fileName, String contentType) {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(handleIdToCopyFrom, "handleIdToCopyFrom");
		ValidateArgument.requirement(StringUtils.isNotEmpty(fileName) || StringUtils.isNotEmpty(contentType),
				"Either the fileName or the contentType needs to be set");

		FileHandle originalFileHandle = fileHandleDao.get(handleIdToCopyFrom);
		ValidateArgument.requireType(originalFileHandle, S3FileHandle.class, "file handle to copy from");
		S3FileHandle newS3FileHandle = (S3FileHandle) originalFileHandle;

		/*
		 * Only the creator of the original file handle can create a copy
		 */
		// Is the user authorized?
		if (!authorizationManager.canAccessRawFileHandleByCreator(userInfo, handleIdToCopyFrom, originalFileHandle.getCreatedBy())
				.getAuthorized()) {
			throw new UnauthorizedException("Only the creator of a file handle can create a copy. File handle id=" + handleIdToCopyFrom
					+ " is not owned by you");
		}

		newS3FileHandle.setId(null);
		newS3FileHandle.setCreatedBy(getUserId(userInfo));
		newS3FileHandle.setCreatedOn(new Date());

		boolean needsNewPreview = false;
		if (StringUtils.isNotEmpty(fileName)) {
			if (!StringUtils.equals(FilenameUtils.getExtension(fileName), FilenameUtils.getExtension(newS3FileHandle.getFileName()))) {
				needsNewPreview = true;
			}
			newS3FileHandle.setFileName(fileName);
		}
		if (StringUtils.isNotEmpty(contentType)) {
			if (!StringUtils.equals(contentType, newS3FileHandle.getContentType())) {
				needsNewPreview = true;
			}
			newS3FileHandle.setContentType(contentType);
		}
		if (needsNewPreview) {
			newS3FileHandle.setPreviewId(null);
		}
		newS3FileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		newS3FileHandle.setEtag(UUID.randomUUID().toString());
		// Save the file metadata to the DB.
		return (S3FileHandle) fileHandleDao.createFile(newS3FileHandle);
	}

	@Override
	public String getRedirectURLForFileHandle(UserInfo userInfo,
			String fileHandleId, FileHandleAssociateType fileAssociateType,
			String fileAssociateId) {
		FileHandleAssociation fileHandleAssociation = new FileHandleAssociation();
		fileHandleAssociation.setFileHandleId(fileHandleId);
		fileHandleAssociation.setAssociateObjectType(fileAssociateType);
		fileHandleAssociation.setAssociateObjectId(fileAssociateId);
		List<FileHandleAssociation> associations = Collections.singletonList(fileHandleAssociation);
		List<FileHandleAssociationAuthorizationStatus> authResults = fileHandleAuthorizationManager.canDownLoadFile(userInfo, associations);
		if (authResults.size()!=1) throw new IllegalStateException("Expected one result but found "+authResults.size());
		AuthorizationStatus authStatus = authResults.get(0).getStatus();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authStatus);
		FileHandle fileHandle = fileHandleDao.get(fileHandleId);
		return getURLForFileHandle(fileHandle);
	}

	@Override
	public BatchFileResult getFileHandleAndUrlBatch(UserInfo userInfo,
			BatchFileRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getRequestedFiles(), "requestedFiles");
		String userId = userInfo.getId().toString();
		long now = System.currentTimeMillis();
		if (request.getIncludeFileHandles() == null) {
			request.setIncludeFileHandles(false);
		}
		if (request.getIncludePreSignedURLs() == null) {
			request.setIncludePreSignedURLs(false);
		}
		if (request.getIncludePreviewPreSignedURLs() == null) {
			request.setIncludePreviewPreSignedURLs(false);
		}
		if(!request.getIncludeFileHandles() && !request.getIncludePreSignedURLs() && !request.getIncludePreviewPreSignedURLs()){
			throw new IllegalArgumentException(MUST_INCLUDE_EITHER);
		}
		if(request.getRequestedFiles().size() > MAX_REQUESTS_PER_CALL){
			throw new IllegalArgumentException(MAX_REQUESTS_PER_CALL_MESSAGE);
		}
		
		// Determine which files the user can download
		List<FileHandleAssociationAuthorizationStatus> authResults = fileHandleAuthorizationManager.canDownLoadFile(userInfo, request.getRequestedFiles());
		List<FileResult> requestedFiles = new LinkedList<FileResult>();
		Set<String> fileHandleIdsToFetch = new HashSet<String>();
		Map<String, FileHandleAssociation> idToFileHandleAssociation = new HashMap<String, FileHandleAssociation>(request.getRequestedFiles().size());
		List<ObjectRecord> downloadRecords = new LinkedList<ObjectRecord>();
		for(FileHandleAssociationAuthorizationStatus fhas: authResults){
			FileResult result = new FileResult();
			idToFileHandleAssociation.put(fhas.getAssociation().getFileHandleId(), fhas.getAssociation());
			result.setFileHandleId(fhas.getAssociation().getFileHandleId());
			if(!fhas.getStatus().getAuthorized()){
				result.setFailureCode(FileResultFailureCode.UNAUTHORIZED);
			}else{
				fileHandleIdsToFetch.add(fhas.getAssociation().getFileHandleId());
			}
			requestedFiles.add(result);
		}
		if(!fileHandleIdsToFetch.isEmpty()){
			// lookup the file handles.
			Map<String, FileHandle> fileHandles = fileHandleDao.getAllFileHandlesBatch(fileHandleIdsToFetch);
			Map<String, FileHandle> previewFileHandles = new HashMap<String, FileHandle>();
			if (request.getIncludePreviewPreSignedURLs()) {
				HashSet<String> previewFileHandleIdsToFetch = new HashSet<String>();
				for (String fileHandleId : fileHandles.keySet()) {
					FileHandle fh = fileHandles.get(fileHandleId);
					if (fh instanceof HasPreviewId) {
						HasPreviewId hasPreview = (HasPreviewId) fh;
						if (hasPreview.getPreviewId() != null) {
							previewFileHandleIdsToFetch.add(hasPreview.getPreviewId());
						}
					}
				}
				if (!previewFileHandleIdsToFetch.isEmpty()) {
					previewFileHandles = fileHandleDao.getAllFileHandlesBatch(previewFileHandleIdsToFetch);
				}
			}
			
			// add the fileHandles to the results
			for(FileResult fr: requestedFiles){
				if(fr.getFailureCode() == null){
					FileHandle handle = fileHandles.get(fr.getFileHandleId());
					if(handle == null){
						fr.setFailureCode(FileResultFailureCode.NOT_FOUND);
					}else{
						// keep the file handle if requested
						if(request.getIncludeFileHandles()){
							fr.setFileHandle(handle);
						}
						if(request.getIncludePreSignedURLs()){
							String url = getURLForFileHandle(handle);
							fr.setPreSignedURL(url);
							FileHandleAssociation association = idToFileHandleAssociation.get(fr.getFileHandleId());
							ObjectRecord record = createObjectRecord(userId, association, now);
							downloadRecords.add(record);
						}
						if (request.getIncludePreviewPreSignedURLs()) {
							if (handle instanceof HasPreviewId) {
								HasPreviewId hasPreview = (HasPreviewId) handle;
								String previewId = hasPreview.getPreviewId();
								if (previewFileHandles.containsKey(previewId)) {
									String previewURL = getURLForFileHandle(previewFileHandles.get(previewId));
									fr.setPreviewPreSignedURL(previewURL);
								}
							}
						}
					}
				}
			}
		}
		// record the downloads for the audit
		if(!downloadRecords.isEmpty()){
			// Push the records to queue
			objectRecordQueue.pushObjectRecordBatch(new ObjectRecordBatch(downloadRecords, FILE_DOWNLOAD_RECORD_TYPE));
		}
		BatchFileResult batch = new BatchFileResult();
		batch.setRequestedFiles(requestedFiles);
		return batch;
	}
	
	/**
	 * Build an ObjectRecord for a file download.
	 * 
	 * @param userId
	 * @param association
	 * @param nowMs
	 * @return
	 */
	static ObjectRecord createObjectRecord(String userId, FileHandleAssociation association, long nowMs){
		FileDownloadRecord record = new FileDownloadRecord();
		record.setDownloadedFile(association);
		record.setUserId(userId);
		return ObjectRecordBuilderUtils.buildObjectRecord(record, nowMs);
	}

	@WriteTransactionReadCommitted
	@Override
	public BatchFileHandleCopyResult copyFileHandles(UserInfo userInfo, BatchFileHandleCopyRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getCopyRequests(), "BatchFileHandleCopyRequest.copyRequests");
		ValidateArgument.requirement(request.getCopyRequests().size() <= MAX_REQUESTS_PER_CALL, MAX_REQUESTS_PER_CALL_MESSAGE);
		List<FileHandleAssociation> requestedFiles = FileHandleCopyUtils.getOriginalFiles(request);
		ValidateArgument.requirement(!FileHandleCopyUtils.hasDuplicates(requestedFiles), DUPLICATED_REQUEST_MESSAGE);

		// Determine which files the user can download
		List<FileHandleAssociationAuthorizationStatus> authResults = fileHandleAuthorizationManager.canDownLoadFile(userInfo, requestedFiles);
		List<FileHandleCopyResult> copyResults = new LinkedList<FileHandleCopyResult>();
		Set<String> fileHandleIdsToFetch = new HashSet<String>();
		for(FileHandleAssociationAuthorizationStatus fhas: authResults){
			FileHandleCopyResult result = new FileHandleCopyResult();
			result.setOriginalFileHandleId(fhas.getAssociation().getFileHandleId());
			if(!fhas.getStatus().getAuthorized()){
				result.setFailureCode(FileResultFailureCode.UNAUTHORIZED);
			}else{
				fileHandleIdsToFetch.add(fhas.getAssociation().getFileHandleId());
			}
			copyResults.add(result);
		}
		BatchFileHandleCopyResult result = new BatchFileHandleCopyResult();
		result.setCopyResults(copyResults);
		if(fileHandleIdsToFetch.isEmpty()){
			return result;
		}

		String userId = userInfo.getId().toString();
		long now = System.currentTimeMillis();
		Map<String, FileHandleCopyRequest> map = FileHandleCopyUtils.getRequestMap(request);
		List<FileHandle> toCreate = new ArrayList<FileHandle>();
		List<ObjectRecord> copyRecords = new LinkedList<ObjectRecord>();
		// lookup the file handles.
		Map<String, FileHandle> fileHandles = fileHandleDao.getAllFileHandlesBatch(fileHandleIdsToFetch);

		for(FileHandleCopyResult fhcr: copyResults){
			if(fhcr.getFailureCode() == null){
				FileHandle original = fileHandles.get(fhcr.getOriginalFileHandleId());
				if(original == null){
					fhcr.setFailureCode(FileResultFailureCode.NOT_FOUND);
				}else{
					FileHandle newFileHandle = FileHandleCopyUtils.createCopy(userId, original, map.get(fhcr.getOriginalFileHandleId()), idGenerator.generateNewId(IdType.FILE_IDS).toString());
					toCreate.add(newFileHandle);
					fhcr.setNewFileHandle(newFileHandle);
					// capture the data for audit
					FileHandleCopyRecord fileHandleCopyRecord = FileHandleCopyUtils.createCopyRecord(userId, newFileHandle.getId(), map.get(fhcr.getOriginalFileHandleId()).getOriginalFile());
					ObjectRecord record = ObjectRecordBuilderUtils.buildObjectRecord(fileHandleCopyRecord, now);
					copyRecords.add(record);
				}
			}
		}
		if (!toCreate.isEmpty()) {
			fileHandleDao.createBatch(toCreate);
		}
		// for audit
		if(!copyRecords.isEmpty()){
			// Push the records to queue
			objectRecordQueue.pushObjectRecordBatch(new ObjectRecordBatch(copyRecords, FILE_HANDLE_COPY_RECORD_TYPE));
		}

		return result;
	}
}
