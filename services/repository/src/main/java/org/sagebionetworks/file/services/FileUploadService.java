package org.sagebionetworks.file.services;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyResult;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.DownloadOrder;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryRequest;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryResponse;
import org.sagebionetworks.repo.model.file.ExternalFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationList;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.MultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.S3FileHandle;
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
	 * @param userId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandle getFileHandle(String handleId, Long userId) throws DatastoreException, NotFoundException;

	/**
	 * Delete a file handle.
	 * @param handleId
	 * @param userId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void deleteFileHandle(String handleId, Long userId) throws DatastoreException, NotFoundException;

	/**
	 * Delete the preview associated with the given file handle (causes the preview generator worker to recreate).
	 * @param handleId
	 * @param userId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	void clearPreview(String handleId, Long userId) throws DatastoreException, NotFoundException;
	
	/**
	 * Create an implementation of ExternalFileHandleInterFace.
	 * @param userId
	 * @param fileHandle
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	ExternalFileHandleInterface createExternalFileHandle(Long userId, ExternalFileHandleInterface fileHandle) throws DatastoreException, NotFoundException;

	/**
	 * Get a pre-signed URL for a FileHandle.
	 * 
	 * @param userId
	 * @param fileHandleId
	 * @return
	 * @throws NotFoundException 
	 */
	String getPresignedUrlForFileHandle(Long userId, String fileHandleId) throws NotFoundException;

	/**
	 * Get a list of upload destinations for uploading a file with this entity as a parent
	 * 
	 * @param userId
	 * @param parentId
	 * @return the list of possible upload destinations. The first one in the list is the default.
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	@Deprecated
	List<UploadDestination> getUploadDestinations(Long userId, String parentId) throws DatastoreException, UnauthorizedException,
			NotFoundException;

	/**
	 * Get the list of upload locations for a parent
	 * 
	 * @param userId
	 * @param parentId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	List<UploadDestinationLocation> getUploadDestinationLocations(Long userId, String parentId) throws DatastoreException, NotFoundException;

	/**
	 * Get the upload location for an upload id
	 * 
	 * @param userId
	 * @param parentId
	 * @param uploadId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	UploadDestination getUploadDestination(Long userId, String parentId, Long storageLocationId) throws DatastoreException, NotFoundException;

	/**
	 * Get the default upload location for a parent
	 * 
	 * @param userId
	 * @param parentId
	 * @param uploadId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	UploadDestination getDefaultUploadDestination(Long userId, String parentId) throws DatastoreException, NotFoundException;

	/**
	 * Create an external S3 file handle.
	 * @param userId
	 * @param fileHandle
	 * @return
	 */
	S3FileHandle createExternalS3FileHandle(Long userId, S3FileHandle fileHandle);

	/**
	 * Create an external Google Cloud file handle.
	 * @param userId
	 * @param fileHandle
	 * @return
	 */
	GoogleCloudFileHandle createExternalGoogleCloudFileHandle(Long userId, GoogleCloudFileHandle fileHandle);

	/**
	 * Create a new file handle pointing to an existing s3 file
	 *
	 * @param userId
	 *
	 * @param handleIdToCopyFrom
	 * @param fileName
	 * @param contentType
	 * @return
	 */
	S3FileHandle createS3FileHandleCopy(Long userId, String handleIdToCopyFrom, String fileName, String contentType);

	/**
	 * 
	 * @param userId
	 * @param fileHandleId
	 * @param fileAssociateType
	 * @param fileAssociateId
	 * @return
	 * @throws NotFoundException
	 */
	String getPresignedUrlForFileHandle(Long userId, String fileHandleId,
			FileHandleAssociateType fileAssociateType, String fileAssociateId)
			throws NotFoundException;

	/**
	 * Start or resume a multi-part upload.
	 * @param userId
	 * @param request
	 * @param forceRestart
	 * @return
	 */
	MultipartUploadStatus startMultipart(Long userId, MultipartRequest request, boolean forceRestart);

	/**
	 * Get a batch of pre-signed urls.
	 * @param userId
	 * @param request
	 * @return
	 */
	BatchPresignedUploadUrlResponse getMultipartPresignedUrlBatch(Long userId,
			BatchPresignedUploadUrlRequest request);

	/**
	 * Add a part to a multi-part upload.
	 * @param userId
	 * @param uploadId
	 * @param partNumber
	 * @param partMD5Hex
	 * @return
	 */
	AddPartResponse addPart(Long userId, String uploadId, Integer partNumber,
			String partMD5Hex);

	/**
	 * Complete a multi-part upload.
	 * @param userId
	 * @param uploadId
	 * @return
	 */
	MultipartUploadStatus completeMultipartUpload(Long userId, String uploadId);

	/**
	 * Get a batch of FileHandles for the given batch of requests.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	BatchFileResult getFileHandleAndUrlBatch(Long userId, BatchFileRequest request);

	/**
	 * Copy a batch of FileHandles.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	BatchFileHandleCopyResult copyFileHandles(Long userId, BatchFileHandleCopyRequest request);

	/**
	 * Add the given files to the user's download list.
	 * @param userId
	 * @param request
	 * @return
	 */
	DownloadList addFilesToDownloadList(Long userId, FileHandleAssociationList request);

	/**
	 * Remove the given list of files from the user's download list.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	DownloadList removeFilesFromDownloadList(Long userId, FileHandleAssociationList request);

	/**
	 * Clear the user's download list.
	 * 
	 * @param userId
	 * @return
	 */
	DownloadList clearDownloadList(Long userId);

	/**
	 * Create a DownloadOrder from the user's current download list.
	 * @param userId
	 * @param zipFileName 
	 * @return
	 */
	DownloadOrder createDownloadOrder(Long userId, String zipFileName);

	/**
	 * Get a download order given its ID.
	 * @param userId
	 * @param orderId
	 * @return
	 */
	DownloadOrder getDownloadOrder(Long userId, String orderId);

	/**
	 * Get a user's download history in reverse chronological order.
	 * @param userId
	 * @param request
	 * @return
	 */
	DownloadOrderSummaryResponse getDownloadOrderHistory(Long userId, DownloadOrderSummaryRequest request);

	/**
	 * Get the user's current download list.
	 * 
	 * @param userId
	 * @return
	 */
	DownloadList getDownloadList(Long userId);
}
