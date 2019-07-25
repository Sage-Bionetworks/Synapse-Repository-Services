package org.sagebionetworks.repo.manager.file;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
import org.sagebionetworks.repo.model.file.ExternalFileHandleInterface;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;

/**
 * Manages uploading files.
 * 
 * @author John
 *
 */
public interface FileHandleManager {

	/**
	 * The ID used for the CORS rule on the data bucket.
	 * 
	 */
	public static final String AUTO_GENERATED_ALLOW_ALL_CORS_RULE_ID = "auto-generated-allow-all-CORS";
	
	/**
	 * Get a file handle for a user.
	 * Note: Only the creator of the FileHandle can access it.
	 * @param userInfo
	 * @param handleId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandle getRawFileHandle(UserInfo userInfo, String handleId) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the FileHandle ID of a preview associated with a file handle.
	 * @param handleId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	String getPreviewFileHandleId(String handleId) throws DatastoreException, NotFoundException;
	
	/**
	 * Clear the preview associated with a file handle
	 * @param userInfo
	 * @param handleId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void clearPreview(UserInfo userInfo, String handleId) throws DatastoreException, NotFoundException;
	
	/**
	 * Delete a file handle
	 * @param userInfo
	 * @param handleId
	 * @throws DatastoreException
	 */
	void deleteFileHandle(UserInfo userInfo, String handleId) throws DatastoreException;
	
	/**
	 * Get the redirect URL for the file handle provided in the given request. If no associate object is provided in the request
	 * only the user that created the file handle or the admin are allowed to get the URL.
	 * <p>
	 * If an associate object is present the user authorization is checked against the associate object.
	 * <p>
	 * To avoid performing the authorization check the url request should be explicitly set to bypass the authorization check 
	 * (See {@link FileHandleUrlRequest#withBypassAuthCheck(boolean)}.
	 * 
	 * @param urlRequest The request context to get the pre-signed URL
	 * @return A pre-signed URL for the handle specified in the given request that can be used to download the content
	 */
	String getRedirectURLForFileHandle(FileHandleUrlRequest urlRequest) throws DatastoreException, NotFoundException, UnauthorizedException;
	
	
	/**
	 * Get a batch of FileHandles and URL
	 * @param userInfo
	 * @param request
	 * @return
	 */
	BatchFileResult getFileHandleAndUrlBatch(UserInfo userInfo, BatchFileRequest request);


	/**
	 * Get all file handles on the list.
	 * @param idsList
	 * @param includePreviews - When true, preview file handles will be included in the resutls.
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandleResults getAllFileHandles(Iterable<String> idsList, boolean includePreviews) throws DatastoreException, NotFoundException;

	/**
	 * Get all file handles on the list in batches. A null id will return a null file handle
	 * 
	 * @param idsList
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	Map<String, FileHandle> getAllFileHandlesBatch(Iterable<String> idsList) throws DatastoreException, NotFoundException;

	/**
	 * Creates any implementation of ExternalFileHandleInterface
	 * @param userInfo
	 * @param fileHandle
	 * @return
	 */
	ExternalFileHandleInterface createExternalFileHandle(UserInfo userInfo, ExternalFileHandleInterface fileHandle);

	/**
	 * Create an external file handle.
	 * 
	 * @param userInfo
	 * @param fileHandle
	 * @return
	 */
	ExternalFileHandle createExternalFileHandle(UserInfo userInfo,	ExternalFileHandle fileHandle);

	/**
	 * Create an ExternalObjectStoreFileHandle.
	 *
	 * @param userInfo
	 * @param fileHandle
	 * @return
	 */
	ExternalObjectStoreFileHandle createExternalFileHandle(UserInfo userInfo, ExternalObjectStoreFileHandle fileHandle);

	/**
	 * Ge the bucket CORS settings
	 * @return
	 */
	BucketCrossOriginConfiguration getBucketCrossOriginConfiguration();

	/**
	 * This is the first step in uploading a file as multiple chunks. The returned file token must be provide in all
	 * subsequent chunk calls.
	 * 
	 * @param userInfo
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public ChunkedFileToken createChunkedFileUploadToken(UserInfo userInfo, CreateChunkedFileTokenRequest ccftr) throws DatastoreException,
			NotFoundException;
	
	/**
	 * Create a pre-signed URL that can be used to PUT a single chunk of file data to S3.
	 * 
	 * @param token
	 * @param partNumber
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public URL createChunkedFileUploadPartURL(UserInfo userInfo, ChunkRequest cpr) throws DatastoreException, NotFoundException;
	
	/**
	 * After uploading a file chunk to the pre-signed URL add it to the larger file. This must be called for each chunk.
	 * 
	 * @param userInfo
	 * @param token
	 * @param partNumber
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public ChunkResult addChunkToFile(UserInfo userInfo, ChunkRequest cpr) throws DatastoreException, NotFoundException;

	/**
	 * The final step of a chunked file upload. This is where an {@link S3FileHandle} is created.
	 * 
	 * @param userInfo
	 * @param token
	 * @param partList
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public S3FileHandle completeChunkFileUpload(UserInfo userInfo, CompleteChunkedFileRequest ccfr) throws DatastoreException,
			NotFoundException;
	
	/**
	 * Start an asynchronous daemon that will add all chunks to the file upload and complete the file upload
	 * process. 
	 * @param userInfo
	 * @param cacf
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public UploadDaemonStatus startUploadDeamon(UserInfo userInfo, CompleteAllChunksRequest cacf) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the status of an asynchronous daemon stated with {@link #startUploadDeamon(UserInfo, CompleteAllChunksRequest)}
	 * @param userInfo
	 * @param daemonId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public UploadDaemonStatus getUploadDaemonStatus(UserInfo userInfo, String daemonId) throws DatastoreException, NotFoundException;

	/**
	 * Multi-part upload a local file to S3.  This is used by workers.
	 * 
	 * @param request
	 * @return
	 */
	S3FileHandle multipartUploadLocalFile(LocalFileUploadRequest request);

	/**
	 * Get the list of upload destinations for this parent
	 * 
	 * @param userInfo
	 * @param parentId
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	@Deprecated
	List<UploadDestination> getUploadDestinations(UserInfo userInfo, String parentId) throws DatastoreException, UnauthorizedException,
			NotFoundException;

	/**
	 * get the list of upload locations
	 * 
	 * @param userInfo
	 * @param parentId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	List<UploadDestinationLocation> getUploadDestinationLocations(UserInfo userInfo, String parentId) throws DatastoreException,
			NotFoundException;

	/**
	 * get the upload location for an uploadId
	 * 
	 * @param userInfo
	 * @param parentId
	 * @param storageLocationId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	UploadDestination getUploadDestination(UserInfo userInfo, String parentId, Long storageLocationId) throws DatastoreException,
			NotFoundException;

	/**
	 * get the default upload location for a parent
	 * 
	 * @param userInfo
	 * @param parentId
	 * @param uploadId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	UploadDestination getDefaultUploadDestination(UserInfo userInfo, String parentId) throws DatastoreException, NotFoundException;

	/**
	 * Create a file handle with the given contents gzipped, using application/octet-stream as the mime type.
	 * @param createdBy
	 * @param modifiedOn
	 * @param markDown
	 * @return
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 */
	S3FileHandle createCompressedFileFromString(String createdBy,
			Date modifiedOn, String fileContents) throws UnsupportedEncodingException, IOException;
	
	/**
	 * 
	 * @param createdBy
	 * @param modifiedOn
	 * @param fileContents
	 * @param fileName set to null for default name
	 * @param contentType
	 * @param contentEncoding
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	S3FileHandle createFileFromByteArray(String createdBy,
			Date modifiedOn, byte[] fileContents, String fileName, ContentType contentType, String contentEncoding) throws UnsupportedEncodingException, IOException;
	
	/**
	 * Create a file handle with the given contents gzipped, using the specified mime-type.
	 * @param createdBy
	 * @param modifiedOn
	 * @param markDown
	 * @param mimeType
	 * @return
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 */
	S3FileHandle createCompressedFileFromString(String createdBy,
			Date modifiedOn, String fileContents, String mimeType) throws UnsupportedEncodingException, IOException;
	
	/**
	 * Retrieves file, decompressing if Content-Encoding indicates that it's gzipped
	 * @param fileHandleId
	 * @return
	 */
	String downloadFileToString(String fileHandleId) throws IOException;

	/**
	 * 
	 * @param userInfo
	 * @param fileHandle
	 * @return
	 */
	S3FileHandle createExternalS3FileHandle(UserInfo userInfo,
			S3FileHandle fileHandle);

	/**
	 * Create a copy of a file handle with a new name and content type
	 * 
	 * @param userInfo
	 * @param handleIdToCopyFrom
	 * @param fileName
	 * @param contentType
	 * @return
	 */
	S3FileHandle createS3FileHandleCopy(UserInfo userInfo, String handleIdToCopyFrom, String fileName, String contentType);

	/**
	 * Create an external ProxyFileHandle.
	 * @param userInfo
	 * @param fileHandle
	 * @return
	 */
	ProxyFileHandle createExternalFileHandle(UserInfo userInfo, ProxyFileHandle fileHandle);

	/**
	 * Make copy of a batch of FileHandles
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	BatchFileHandleCopyResult copyFileHandles(UserInfo userInfo, BatchFileHandleCopyRequest request);
}
