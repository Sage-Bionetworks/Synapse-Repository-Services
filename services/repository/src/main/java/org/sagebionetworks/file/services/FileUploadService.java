package org.sagebionetworks.file.services;

import java.net.URL;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyResult;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.DownloadOrder;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryRequest;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryResponse;
import org.sagebionetworks.repo.model.file.ExternalFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationList;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for the handling multi-part upload.
 * @author John
 *
 */
public interface FileUploadService {


	/**
	 * Get a file handle by ID.
	 * @param handleId
	 * @param accessToken
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandle getFileHandle(String handleId, String accessToken) throws DatastoreException, NotFoundException;

	/**
	 * Delete a file handle.
	 * @param handleId
	 * @param accessToken
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void deleteFileHandle(String handleId, String accessToken) throws DatastoreException, NotFoundException;

	/**
	 * Delete the preview associated with the given file handle (causes the preview generator worker to recreate).
	 * @param handleId
	 * @param accessToken
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	void clearPreview(String handleId, String accessToken) throws DatastoreException, NotFoundException;
	
	/**
	 * Create an implementation of ExternalFileHandleInterFace.
	 * @param accessToken
	 * @param fileHandle
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	ExternalFileHandleInterface createExternalFileHandle(String accessToken, ExternalFileHandleInterface fileHandle) throws DatastoreException, NotFoundException;

	/**
	 * Create a chunked file upload token that can be used to upload large files to S3.
	 * 
	 * @param accessToken
	 * @param ccftr
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Deprecated // replaced with multi-part upload V2
	ChunkedFileToken createChunkedFileUploadToken(String accessToken, CreateChunkedFileTokenRequest ccftr) throws DatastoreException, NotFoundException;
	
	/**
	 * Creates a pre-signed URL that can be used PUT file data to S3.
	 * @param accessToken
	 * @param cpr
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Deprecated // replaced with multi-part upload V2
	URL createChunkedFileUploadPartURL(String accessToken, ChunkRequest cpr) throws DatastoreException, NotFoundException;

	/**
	 * After upload a file chunk to a pre-signed URL, the part must be added to the final file.
	 * @param accessToken
	 * @param cpr
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Deprecated // replaced with multi-part upload V2
	ChunkResult addChunkToFile(String accessToken, ChunkRequest cpr) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param accessToken
	 * @param ccfr
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Deprecated // replaced with multi-part upload V2
	S3FileHandle completeChunkFileUpload(String accessToken, CompleteChunkedFileRequest ccfr) throws DatastoreException, NotFoundException;
	
	/**
	 * Start an asynchronous daemon that will add all chunks to the file upload and complete the file upload
	 * process. 
	 * @param userInfo
	 * @param cacf
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Deprecated // replaced with multi-part upload V2
	public UploadDaemonStatus startUploadDeamon(String accessToken, CompleteAllChunksRequest cacf) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the status of an asynchronous daemon stated with {@link #startUploadDeamon(UserInfo, CompleteAllChunksRequest)}
	 * @param userInfo
	 * @param daemonId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Deprecated // replaced with multi-part upload V2
	public UploadDaemonStatus getUploadDaemonStatus(String accessToken, String daemonId) throws DatastoreException, NotFoundException;

	/**
	 * Get a pre-signed URL for a FileHandle.
	 * 
	 * @param accessToken
	 * @param fileHandleId
	 * @return
	 * @throws NotFoundException 
	 */
	String getPresignedUrlForFileHandle(String accessToken, String fileHandleId) throws NotFoundException;

	/**
	 * Get a list of upload destinations for uploading a file with this entity as a parent
	 * 
	 * @param accessToken
	 * @param parentId
	 * @return the list of possible upload destinations. The first one in the list is the default.
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	@Deprecated
	List<UploadDestination> getUploadDestinations(String accessToken, String parentId) throws DatastoreException, UnauthorizedException,
			NotFoundException;

	/**
	 * Get the list of upload locations for a parent
	 * 
	 * @param accessToken
	 * @param parentId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	List<UploadDestinationLocation> getUploadDestinationLocations(String accessToken, String parentId) throws DatastoreException, NotFoundException;

	/**
	 * Get the upload location for an upload id
	 * 
	 * @param accessToken
	 * @param parentId
	 * @param uploadId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	UploadDestination getUploadDestination(String accessToken, String parentId, Long storageLocationId) throws DatastoreException, NotFoundException;

	/**
	 * Get the default upload location for a parent
	 * 
	 * @param accessToken
	 * @param parentId
	 * @param uploadId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	UploadDestination getDefaultUploadDestination(String accessToken, String parentId) throws DatastoreException, NotFoundException;

	/**
	 * Create an external S3 file handle.
	 * @param accessToken
	 * @param fileHandle
	 * @return
	 */
	S3FileHandle createExternalS3FileHandle(String accessToken, S3FileHandle fileHandle);

	/**
	 * Create an external Google Cloud file handle.
	 * @param accessToken
	 * @param fileHandle
	 * @return
	 */
	GoogleCloudFileHandle createExternalGoogleCloudFileHandle(String accessToken, GoogleCloudFileHandle fileHandle);

	/**
	 * Create a new file handle pointing to an existing s3 file
	 *
	 * @param accessToken
	 *
	 * @param handleIdToCopyFrom
	 * @param fileName
	 * @param contentType
	 * @return
	 */
	S3FileHandle createS3FileHandleCopy(String accessToken, String handleIdToCopyFrom, String fileName, String contentType);

	/**
	 * 
	 * @param accessToken
	 * @param fileHandleId
	 * @param fileAssociateType
	 * @param fileAssociateId
	 * @return
	 * @throws NotFoundException
	 */
	String getPresignedUrlForFileHandle(String accessToken, String fileHandleId,
			FileHandleAssociateType fileAssociateType, String fileAssociateId)
			throws NotFoundException;

	/**
	 * Start or resume a multi-part upload.
	 * @param accessToken
	 * @param request
	 * @param forceRestart
	 * @return
	 */
	MultipartUploadStatus startMultipartUpload(String accessToken,
			MultipartUploadRequest request, boolean forceRestart);

	/**
	 * Get a batch of pre-signed urls.
	 * @param accessToken
	 * @param request
	 * @return
	 */
	BatchPresignedUploadUrlResponse getMultipartPresignedUrlBatch(String accessToken,
			BatchPresignedUploadUrlRequest request);

	/**
	 * Add a part to a multi-part upload.
	 * @param accessToken
	 * @param uploadId
	 * @param partNumber
	 * @param partMD5Hex
	 * @return
	 */
	AddPartResponse addPart(String accessToken, String uploadId, Integer partNumber,
			String partMD5Hex);

	/**
	 * Complete a multi-part upload.
	 * @param accessToken
	 * @param uploadId
	 * @return
	 */
	MultipartUploadStatus completeMultipartUpload(String accessToken, String uploadId);

	/**
	 * Get a batch of FileHandles for the given batch of requests.
	 * 
	 * @param accessToken
	 * @param request
	 * @return
	 */
	BatchFileResult getFileHandleAndUrlBatch(String accessToken, BatchFileRequest request);

	/**
	 * Copy a batch of FileHandles.
	 * 
	 * @param accessToken
	 * @param request
	 * @return
	 */
	BatchFileHandleCopyResult copyFileHandles(String accessToken, BatchFileHandleCopyRequest request);

	/**
	 * Add the given files to the user's download list.
	 * @param accessToken
	 * @param request
	 * @return
	 */
	DownloadList addFilesToDownloadList(String accessToken, FileHandleAssociationList request);

	/**
	 * Remove the given list of files from the user's download list.
	 * 
	 * @param accessToken
	 * @param request
	 * @return
	 */
	DownloadList removeFilesFromDownloadList(String accessToken, FileHandleAssociationList request);

	/**
	 * Clear the user's download list.
	 * 
	 * @param accessToken
	 * @return
	 */
	DownloadList clearDownloadList(String accessToken);

	/**
	 * Create a DownloadOrder from the user's current download list.
	 * @param accessToken
	 * @param zipFileName 
	 * @return
	 */
	DownloadOrder createDownloadOrder(String accessToken, String zipFileName);

	/**
	 * Get a download order given its ID.
	 * @param accessToken
	 * @param orderId
	 * @return
	 */
	DownloadOrder getDownloadOrder(String accessToken, String orderId);

	/**
	 * Get a user's download history in reverse chronological order.
	 * @param accessToken
	 * @param request
	 * @return
	 */
	DownloadOrderSummaryResponse getDownloadOrderHistory(String accessToken, DownloadOrderSummaryRequest request);

	/**
	 * Get the user's current download list.
	 * 
	 * @param accessToken
	 * @return
	 */
	DownloadList getDownloadList(String accessToken);
}
