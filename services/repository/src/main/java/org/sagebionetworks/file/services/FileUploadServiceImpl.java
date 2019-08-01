package org.sagebionetworks.file.services;

import java.net.URL;
import java.util.List;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.manager.file.MultipartManagerV2;
import org.sagebionetworks.repo.manager.file.download.BulkDownloadManager;
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
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of the file upload service.
 * @author John
 *
 */
public class FileUploadServiceImpl implements FileUploadService {
	
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	FileHandleManager fileHandleManager;
	
	@Autowired
	MultipartManagerV2 multipartManagerV2;
	
	@Autowired
	BulkDownloadManager bulkDownloadManager;


	@Override
	public FileHandle getFileHandle(String handleId, Long userId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new UnauthorizedException("The user must be authenticated");
		if(handleId == null) throw new IllegalArgumentException("FileHandleId cannot be null");
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.getRawFileHandle(userInfo, handleId);
	}

	@Override
	public void deleteFileHandle(String handleId, Long userId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new UnauthorizedException("The user must be authenticated");
		if(handleId == null) throw new IllegalArgumentException("FileHandleId cannot be null");
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		fileHandleManager.deleteFileHandle(userInfo, handleId);
	}
	
	@Override
	public void clearPreview(String handleId, Long userId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new UnauthorizedException("The user must be authenticated");
		if(handleId == null) throw new IllegalArgumentException("FileHandleId cannot be null");
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		fileHandleManager.clearPreview(userInfo, handleId);
	}

	@Override
	public ExternalFileHandleInterface createExternalFileHandle(Long userId,
			ExternalFileHandleInterface fileHandle) throws DatastoreException, NotFoundException {
		if(userId == null) throw new UnauthorizedException("The user must be authenticated");
		if(fileHandle == null) throw new IllegalArgumentException("FileHandleId cannot be null");
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.createExternalFileHandle(userInfo, fileHandle);
	}

	@Override
	public ChunkedFileToken createChunkedFileUploadToken(Long userId,	CreateChunkedFileTokenRequest ccftr) throws DatastoreException, NotFoundException {
		if(userId == null) throw new UnauthorizedException("The user must be authenticated");
		if(ccftr == null) throw new IllegalArgumentException("CreateChunkedFileTokenRequest cannot be null");
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.createChunkedFileUploadToken(userInfo, ccftr);
	}

	@Override
	public URL createChunkedFileUploadPartURL(Long userId, ChunkRequest cpr) throws DatastoreException, NotFoundException {
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.createChunkedFileUploadPartURL(userInfo, cpr);
	}

	@Override
	public ChunkResult addChunkToFile(Long userId, ChunkRequest cpr) throws DatastoreException, NotFoundException {
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.addChunkToFile(userInfo, cpr);
	}

	@Override
	public S3FileHandle completeChunkFileUpload(Long userId, CompleteChunkedFileRequest ccfr) throws DatastoreException, NotFoundException {
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.completeChunkFileUpload(userInfo, ccfr);
	}

	@Override
	public UploadDaemonStatus startUploadDeamon(Long userId, CompleteAllChunksRequest cacf) throws DatastoreException,
			NotFoundException {
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.startUploadDeamon(userInfo, cacf);
	}

	@Override
	public UploadDaemonStatus getUploadDaemonStatus(Long userId, String daemonId) throws DatastoreException, NotFoundException {
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.getUploadDaemonStatus(userInfo, daemonId);
	}

	@Override
	public String getPresignedUrlForFileHandle(Long userId, String fileHandleId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandleId);
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}

	@Override
	public String getPresignedUrlForFileHandle(Long userId, String fileHandleId, FileHandleAssociateType fileAssociateType, String fileAssociateId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandleId)
				.withAssociation(fileAssociateType, fileAssociateId);
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}

	@Override
	@Deprecated
	public List<UploadDestination> getUploadDestinations(Long userId, String parentId) throws DatastoreException, UnauthorizedException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.getUploadDestinations(userInfo, parentId);
	}

	@Override
	public List<UploadDestinationLocation> getUploadDestinationLocations(Long userId, String parentId) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.getUploadDestinationLocations(userInfo, parentId);
	}

	@Override
	public UploadDestination getUploadDestination(Long userId, String parentId, Long storageLocationId) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.getUploadDestination(userInfo, parentId, storageLocationId);
	}

	@Override
	public UploadDestination getDefaultUploadDestination(Long userId, String parentId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.getDefaultUploadDestination(userInfo, parentId);
	}

	@Override
	public S3FileHandle createExternalS3FileHandle(Long userId,
			S3FileHandle fileHandle) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.createExternalS3FileHandle(userInfo, fileHandle);
	}

	@Override
	public S3FileHandle createS3FileHandleCopy(Long userId, String handleIdToCopyFrom, String fileName, String contentType) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.createS3FileHandleCopy(userInfo, handleIdToCopyFrom, fileName, contentType);
	}

	@Override
	public MultipartUploadStatus startMultipartUpload(Long userId,
			MultipartUploadRequest request, boolean forceRestart) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return multipartManagerV2.startOrResumeMultipartUpload(userInfo, request, forceRestart);
	}

	@Override
	public BatchPresignedUploadUrlResponse getMultipartPresignedUrlBatch(
			Long userId, BatchPresignedUploadUrlRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return multipartManagerV2.getBatchPresignedUploadUrls(userInfo, request);
	}

	@Override
	public AddPartResponse addPart(Long userId, String uploadId,
			Integer partNumber, String partMD5Hex) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return multipartManagerV2.addMultipartPart(userInfo, uploadId, partNumber, partMD5Hex);
	}

	@Override
	public MultipartUploadStatus completeMultipartUpload(Long userId,
			String uploadId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return multipartManagerV2.completeMultipartUpload(userInfo, uploadId);
	}

	@Override
	public BatchFileResult getFileHandleAndUrlBatch(Long userId,
			BatchFileRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.getFileHandleAndUrlBatch(userInfo, request);
	}

	@Override
	public BatchFileHandleCopyResult copyFileHandles(Long userId, BatchFileHandleCopyRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileHandleManager.copyFileHandles(userInfo, request);
	}

	@Override
	public DownloadList addFilesToDownloadList(Long userId, FileHandleAssociationList request) {
		UserInfo user = userManager.getUserInfo(userId);
		return bulkDownloadManager.addFileHandleAssociations(user, request.getList());
	}

	@Override
	public DownloadList removeFilesFromDownloadList(Long userId, FileHandleAssociationList request) {
		UserInfo user = userManager.getUserInfo(userId);
		return bulkDownloadManager.removeFileHandleAssociations(user, request.getList());
	}

	@Override
	public DownloadList clearDownloadList(Long userId) {
		UserInfo user = userManager.getUserInfo(userId);
		return bulkDownloadManager.clearDownloadList(user);
	}

	@Override
	public DownloadOrder createDownloadOrder(Long userId, String zipFileName) {
		UserInfo user = userManager.getUserInfo(userId);
		return bulkDownloadManager.createDownloadOrder(user, zipFileName);
	}

	@Override
	public DownloadOrder getDownloadOrder(Long userId, String orderId) {
		UserInfo user = userManager.getUserInfo(userId);
		return bulkDownloadManager.getDownloadOrder(user, orderId);
	}

	@Override
	public DownloadOrderSummaryResponse getDownloadOrderHistory(Long userId, DownloadOrderSummaryRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return bulkDownloadManager.getDownloadHistory(user, request);
	}

	@Override
	public DownloadList getDownloadList(Long userId) {
		UserInfo user = userManager.getUserInfo(userId);
		return bulkDownloadManager.getDownloadList(user);
	}
}
