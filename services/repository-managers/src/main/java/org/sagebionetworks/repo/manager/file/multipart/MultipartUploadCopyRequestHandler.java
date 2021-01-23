package org.sagebionetworks.repo.manager.file.multipart;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationManager;
import org.sagebionetworks.repo.manager.file.MultipartUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultipartRequestUtils;
import org.sagebionetworks.repo.model.file.AbortMultipartRequest;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.PartUtils;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.jdo.NameValidation;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAO;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAOProvider;
import org.sagebionetworks.upload.multipart.PresignedUrl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MultipartUploadCopyRequestHandler implements MultipartRequestHandler<MultipartUploadCopyRequest> {

	private CloudServiceMultipartUploadDAOProvider cloudServiceDaoProvider;

	private FileHandleAuthorizationManager authManager;
	
	private FileHandleDao fileHandleDao;
	
	@Autowired
	public MultipartUploadCopyRequestHandler(
			CloudServiceMultipartUploadDAOProvider cloudServiceDaoProvider, 
			FileHandleAuthorizationManager authManager,
			FileHandleDao fileHandleDao) {
		this.cloudServiceDaoProvider = cloudServiceDaoProvider;
		this.authManager = authManager;
		this.fileHandleDao = fileHandleDao;
	}

	@Override
	public Class<MultipartUploadCopyRequest> getRequestClass() {
		return MultipartUploadCopyRequest.class;
	}

	@Override
	public void validateRequest(UserInfo user, MultipartUploadCopyRequest request) {
		ValidateArgument.required(request, "The request");
		ValidateArgument.required(request.getStorageLocationId(), "MultipartUploadCopyRequest.storageLocationId");

		FileHandleAssociation association = request.getSourceFileHandleAssociation();
		
		ValidateArgument.required(association, "MultipartUploadCopyRequest.sourceFileHandleAssociation");
		ValidateArgument.required(association.getFileHandleId(), "MultipartUploadCopyRequest.sourceFileHandleAssociation.fileHandleId");
		ValidateArgument.required(association.getAssociateObjectId(), "MultipartUploadCopyRequest.sourceFileHandleAssociation.associateObjectId");
		ValidateArgument.required(association.getAssociateObjectType(), "MultipartUploadCopyRequest.sourceFileHandleAssociation.associateObjectType");
		
		if (!StringUtils.isBlank(request.getFileName())) {
			//validate file name
			NameValidation.validateName(request.getFileName());
		}
	}

	@Override
	public CreateMultipartRequest initiateRequest(UserInfo user, MultipartUploadCopyRequest request, String requestHash,
			StorageLocationSetting storageLocation) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(request, "The request");
		ValidateArgument.required(storageLocation, "The storageLocation");
		
		UploadType uploadType = storageLocation.getUploadType();
		
		FileHandleAssociation association = request.getSourceFileHandleAssociation();
		
		// Verifies that the user can download the source file
		authManager.canDownLoadFile(user, Arrays.asList(association))
			.stream()
			.filter(status -> status.getStatus().isAuthorized())
			.findFirst()
			.orElseThrow(() -> 
				new UnauthorizedException("The user is not authorized to access the source file.")
			);
		
		// Makes sure the user owns the storage location, note that the storage location id is required in the request
		if (!user.isAdmin() && !user.getId().equals(storageLocation.getCreatedBy())) {
			throw new UnauthorizedException("The user does not own the destination storage location.");
		}
		
		FileHandle fileHandle = fileHandleDao.get(association.getFileHandleId());
		
		if (fileHandle.getContentSize() == null) {
			throw new IllegalArgumentException("The source file handle does not define its size.");
		}
		
		if (fileHandle.getContentMd5() == null) {
			throw new IllegalArgumentException("The source file handle does not define its content MD5.");
		}

		if (storageLocation.getStorageLocationId().equals(fileHandle.getStorageLocationId())) {
			throw new IllegalArgumentException("The source file handle is already in the given destination storage location.");
		}
		
		if (!(fileHandle instanceof CloudProviderFileHandleInterface)) {
			throw new IllegalArgumentException("The source file must be stored in a cloud bucket accessible by Synapse.");
		}
		
		final CloudServiceMultipartUploadDAO cloudDao = cloudServiceDaoProvider.getCloudServiceMultipartUploadDao(uploadType);
		
		CloudProviderFileHandleInterface cloudFileHandle = (CloudProviderFileHandleInterface) fileHandle;
		
		String sourceFileEtag = cloudDao.getObjectEtag(cloudFileHandle.getBucketName(), cloudFileHandle.getKey());
		
		// Sets a file name as it is optional, but we save it in the request (the hash won't contain this)
		request.setFileName(StringUtils.isBlank(request.getFileName()) ? fileHandle.getFileName() : request.getFileName());
		
		String requestJson = MultipartRequestUtils.createRequestJSON(request);
		
		// the bucket depends on the upload location used.
		String bucket = MultipartUtils.getBucket(storageLocation);
		// create a new key for this file.
		String key = MultipartUtils.createNewKey(user.getId().toString(), request.getFileName(), storageLocation);
		
		String uploadToken = cloudDao.initiateMultipartUploadCopy(bucket, key, request, fileHandle);
		
		int numberParts = PartUtils.calculateNumberOfParts(fileHandle.getContentSize(), request.getPartSizeBytes());
		
		// Creates the copy request
		return new CreateMultipartRequest(user.getId(),
						requestHash, requestJson, uploadToken, uploadType,
						bucket, key, numberParts, request.getPartSizeBytes(), fileHandle.getId(), sourceFileEtag);
	}

	@Override
	public PresignedUrl getPresignedUrl(CompositeMultipartUploadStatus status, long partNumber, String contentType) {
		ValidateArgument.required(status, "The upload status");
		
		final CloudServiceMultipartUploadDAO cloudDao = cloudServiceDaoProvider.getCloudServiceMultipartUploadDao(status.getUploadType());
		
		return cloudDao.createPartUploadCopyPresignedUrl(status, partNumber, contentType);
	}

	@Override
	public void validateAddedPart(CompositeMultipartUploadStatus status, long partNumber, String partMD5Hex) {
		ValidateArgument.required(status, "The upload status");
		ValidateArgument.required(partMD5Hex, "The part MD5");
		
		final CloudServiceMultipartUploadDAO cloudDao = cloudServiceDaoProvider.getCloudServiceMultipartUploadDao(status.getUploadType());
		
		cloudDao.validatePartCopy(status, partNumber, partMD5Hex);
	}

	@Override
	public FileHandleCreateRequest getFileHandleCreateRequest(CompositeMultipartUploadStatus status, String originalRequest) {
		ValidateArgument.required(status, "The upload status");
		ValidateArgument.required(originalRequest, "The original request");
		
		MultipartUploadCopyRequest request = MultipartRequestUtils.getRequestFromJson(originalRequest, MultipartUploadCopyRequest.class);
		
		FileHandle fileHandle = fileHandleDao.get(status.getSourceFileHandleId().toString());

		// Note that the filename in the request is optional, but we do save the file handle name in the request if not supplied
		return new FileHandleCreateRequest(request.getFileName(), fileHandle.getContentType(), fileHandle.getContentMd5(), request.getStorageLocationId(), request.getGeneratePreview());
	
	}

	@Override
	public void tryAbortMultipartRequest(CompositeMultipartUploadStatus status) {
		ValidateArgument.required(status, "The upload status");
		
		final CloudServiceMultipartUploadDAO cloudDao = cloudServiceDaoProvider.getCloudServiceMultipartUploadDao(status.getUploadType());
		
		final String uploadId = status.getMultipartUploadStatus().getUploadId();
		final String uploadToken = status.getUploadToken();
		final String bucket = status.getBucket();
		final String key = status.getKey();
		
		final AbortMultipartRequest request = new AbortMultipartRequest(uploadId, uploadToken, bucket, key);
		
		cloudDao.tryAbortMultipartRequest(request);
	}

}
