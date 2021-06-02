package org.sagebionetworks.repo.manager.file.multipart;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.manager.file.MultipartUtils;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultipartRequestUtils;
import org.sagebionetworks.repo.model.file.AbortMultipartRequest;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.PartUtils;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.jdo.NameValidation;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAO;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAOProvider;
import org.sagebionetworks.upload.multipart.MultipartUploadUtils;
import org.sagebionetworks.upload.multipart.PresignedUrl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MultipartUploadRequestHandler implements MultipartRequestHandler<MultipartUploadRequest> {

	private CloudServiceMultipartUploadDAOProvider cloudServiceDaoProvider;

	@Autowired
	public MultipartUploadRequestHandler(CloudServiceMultipartUploadDAOProvider cloudServiceDaoProvider) {
		this.cloudServiceDaoProvider = cloudServiceDaoProvider;
	}

	@Override
	public Class<MultipartUploadRequest> getRequestClass() {
		return MultipartUploadRequest.class;
	}

	@Override
	public void validateRequest(UserInfo user, MultipartUploadRequest request) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(request, "The request");
		
		ValidateArgument.requiredNotEmpty(request.getFileName(), "MultipartUploadRequest.fileName");
		//validate file name
		NameValidation.validateName(request.getFileName());		
		ValidateArgument.required(request.getFileSizeBytes(), "MultipartUploadRequest.fileSizeBytes");
		ValidateArgument.requiredNotEmpty(request.getContentMD5Hex(), "MultipartUploadRequest.contentMD5Hex");
		
		PartUtils.validateFileSize(request.getFileSizeBytes());

	}

	@Override
	public CreateMultipartRequest initiateRequest(UserInfo user, MultipartUploadRequest request, String requestHash, StorageLocationSetting storageLocation) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(request, "The request");
		ValidateArgument.required(storageLocation, "The storage location");
		
		UploadType uploadType = storageLocation.getUploadType();
		
		// the bucket depends on the upload location used.
		String bucket = MultipartUtils.getBucket(storageLocation);
		// create a new key for this file.
		String key = MultipartUtils.createNewKey(user.getId().toString(), request.getFileName(), storageLocation);
		
		CloudServiceMultipartUploadDAO cloudDao = cloudServiceDaoProvider.getCloudServiceMultipartUploadDao(uploadType);
		
		String uploadToken = cloudDao.initiateMultipartUpload(bucket, key, request);
		
		String requestJson = MultipartRequestUtils.createRequestJSON(request);
		// How many parts will be needed to upload this file?
		int numberParts = PartUtils.calculateNumberOfParts(request.getFileSizeBytes(), request.getPartSizeBytes());
		// Create the upload request
		return new CreateMultipartRequest(user.getId(),
						requestHash, requestJson, uploadToken, uploadType,
						bucket, key, numberParts, request.getPartSizeBytes());
	}

	@Override
	public PresignedUrl getPresignedUrl(CompositeMultipartUploadStatus status, long partNumber, String contentType) {
		ValidateArgument.required(status, "The upload status");
		
		String partKey = MultipartUploadUtils.createPartKey(status.getKey(), partNumber);
		
		CloudServiceMultipartUploadDAO cloudDao = cloudServiceDaoProvider.getCloudServiceMultipartUploadDao(status.getUploadType());
		
		return cloudDao.createPartUploadPreSignedUrl(status.getBucket(), partKey, contentType);
	}

	@Override
	public void validateAddedPart(CompositeMultipartUploadStatus status, long partNumber, String partMD5Hex) {
		ValidateArgument.required(status, "The upload status");
		ValidateArgument.required(partMD5Hex, "The part MD5");
		
		final String partKey = MultipartUploadUtils.createPartKey(status.getKey(), partNumber);
		
		final CloudServiceMultipartUploadDAO cloudDao = cloudServiceDaoProvider.getCloudServiceMultipartUploadDao(status.getUploadType());
		
		cloudDao.validateAndAddPart(
				new AddPartRequest(status.getMultipartUploadStatus().getUploadId(), status
						.getUploadToken(), status.getBucket(), status
						.getKey(), partKey, partMD5Hex, partNumber, status.getNumberOfParts())
		);

	}

	@Override
	public FileHandleCreateRequest getFileHandleCreateRequest(CompositeMultipartUploadStatus status, String originalRequest) {
		ValidateArgument.required(status, "The upload status");
		ValidateArgument.required(originalRequest, "The original request");
		
		MultipartUploadRequest request = MultipartRequestUtils.getRequestFromJson(originalRequest, MultipartUploadRequest.class);
		
		return new FileHandleCreateRequest(request.getFileName(), request.getContentType(), request.getContentMD5Hex(), request.getStorageLocationId(), request.getGeneratePreview());
		
	}
	
	@Override
	public void tryAbortMultipartRequest(CompositeMultipartUploadStatus status) {
		ValidateArgument.required(status, "The upload status");
		
		List<String> partKeys = new ArrayList<>(status.getNumberOfParts());
		
		for (long partNumber = 1; partNumber <= status.getNumberOfParts(); partNumber++) {
			partKeys.add(MultipartUploadUtils.createPartKey(status.getKey(), partNumber));
		}
		
		final CloudServiceMultipartUploadDAO cloudDao = cloudServiceDaoProvider.getCloudServiceMultipartUploadDao(status.getUploadType());
		
		final String uploadId = status.getMultipartUploadStatus().getUploadId();
		final String uploadToken = status.getUploadToken();
		final String bucket = status.getBucket();
		final String key = status.getKey();
		
		final AbortMultipartRequest request = new AbortMultipartRequest(uploadId, uploadToken, bucket, key).withPartKeys(partKeys);
		
		cloudDao.tryAbortMultipartRequest(request);

	}
}
