package org.sagebionetworks.repo.manager.file;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.file.multipart.FileHandleCreateRequest;
import org.sagebionetworks.repo.manager.file.multipart.MultipartRequestHandler;
import org.sagebionetworks.repo.manager.file.multipart.MultipartRequestHandlerProvider;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultipartRequestUtils;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.AddPartState;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.MultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartMD5;
import org.sagebionetworks.repo.model.file.PartPresignedUrl;
import org.sagebionetworks.repo.model.file.PartUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAO;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAOProvider;
import org.sagebionetworks.upload.multipart.PresignedUrl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MultipartManagerV2Impl implements MultipartManagerV2 {
	
	private static final String RESTARTED_HASH_PREFIX = "R_";

	@Autowired
	private MultipartUploadDAO multipartUploadDAO;

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private StorageLocationDAO storageLocationDao;

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private CloudServiceMultipartUploadDAOProvider cloudDaoProvider;
	
	@Autowired
	private MultipartRequestHandlerProvider handlerProvider;
	
	@Override
	@WriteTransaction
	public MultipartUploadStatus startOrResumeMultipartOperation(UserInfo user, MultipartRequest request, boolean forceRestart) {
		ValidateArgument.required(request, "request");
		
		if (request instanceof MultipartUploadRequest) {
			return startOrResumeMultipartUpload(user, (MultipartUploadRequest) request, forceRestart);
		} else if (request instanceof MultipartUploadCopyRequest) {
			return startOrResumeMultipartUploadCopy(user, (MultipartUploadCopyRequest) request, forceRestart);
		}
		
		throw new IllegalArgumentException("Request type unsupported: " + request.getClass().getSimpleName());
	}

	@Override
	@WriteTransaction
	public MultipartUploadStatus startOrResumeMultipartUpload(UserInfo user, MultipartUploadRequest request, boolean forceRestart) {
		MultipartRequestHandler<MultipartUploadRequest> handler = handlerProvider.getHandlerForClass(MultipartUploadRequest.class);
		
		return startOrResumeMultipartRequest(handler, user, request, forceRestart);
	}
	
	@Override
	@WriteTransaction
	public MultipartUploadStatus startOrResumeMultipartUploadCopy(UserInfo user, MultipartUploadCopyRequest request, boolean forceRestart) {
		MultipartRequestHandler<MultipartUploadCopyRequest> handler = handlerProvider.getHandlerForClass(MultipartUploadCopyRequest.class);
		
		return startOrResumeMultipartRequest(handler, user, request, forceRestart);
	}
	
	<T extends MultipartRequest> MultipartUploadStatus startOrResumeMultipartRequest(MultipartRequestHandler<T> handler, UserInfo user, T request, boolean forceRestart) {
		
		// Common validation of the request
		validateMultipartRequest(user, request);
		
		handler.validateRequest(user, request);
		
		// Makes sure we set a default storage location
		if (request.getStorageLocationId() == null) {
			request.setStorageLocationId(StorageLocationDAO.DEFAULT_STORAGE_LOCATION_ID);
		}

		// The MD5 is used to identify if this upload request already exists for this user.
		String requestMD5Hex = MultipartRequestUtils.calculateMD5AsHex(request);
		
		// When forcing a restart we clear the old hash changing the cache key, the previous multipart upload will be garbage collected by a worker
		if (forceRestart) {
			multipartUploadDAO.setUploadStatusHash(user.getId(), requestMD5Hex, RESTARTED_HASH_PREFIX + requestMD5Hex + "_" + UUID.randomUUID().toString());
		}

		// Has an upload already been started for this user and request
		CompositeMultipartUploadStatus status = multipartUploadDAO.getUploadStatus(user.getId(), requestMD5Hex);
		
		if (status == null) {
			StorageLocationSetting storageLocation = storageLocationDao.get(request.getStorageLocationId());
			
			// Since the status for this file does not exist, create it.
			CreateMultipartRequest createRequest;
			
			try {
				createRequest = handler.initiateRequest(user, request, requestMD5Hex, storageLocation);
			} catch (UnsupportedOperationException e) {
				// Not all the source/targets are supported by the cloud providers. Rather than returning a 500 we turn around and return a 400
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			
			status = multipartUploadDAO.createUploadStatus(createRequest);
		}
		
		// Calculate the parts state for this file.
		String partsState = setupPartState(status);
		
		status.getMultipartUploadStatus().setPartsState(partsState);
		
		return status.getMultipartUploadStatus();
	}
	
	private void validateMultipartRequest(UserInfo user, MultipartRequest request) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(user.getId(), "UserInfo.getId");
		ValidateArgument.required(request, "request");
		
		String requestClass = request.getClass().getSimpleName();
		
		ValidateArgument.required(request.getPartSizeBytes(), requestClass + ".partSizeBytes");
		
		PartUtils.validatePartSize(request.getPartSizeBytes());

		// anonymous cannot upload. See: PLFM-2621.
		if (AuthorizationUtils.isUserAnonymous(user)) {
			throw new UnauthorizedException("Anonymous cannot upload files.");
		}
	}

	/**
	 * Setup the partsState string for a given status multi-part status. For the
	 * cases where the file upload is complete, this string is built without a
	 * database call.
	 * 
	 * @param status
	 */
	public String setupPartState(CompositeMultipartUploadStatus status) {
		if (MultipartUploadState.COMPLETED.equals(status.getMultipartUploadStatus().getState())) {
			// When the upload is done we just create a string of all '1'
			// without hitting the DB.
			return getCompletePartStateString(status.getNumberOfParts());
		} else {
			// Get the parts state from the DAO.
			return multipartUploadDAO.getPartsState(status.getMultipartUploadStatus().getUploadId(), status.getNumberOfParts());
		}
	}

	/**
	 * Create string of '1' of the size of the passed number of parts.
	 * 
	 * @param numberOfParts
	 * @return
	 */
	public static String getCompletePartStateString(int numberOfParts) {
		char[] chars = new char[numberOfParts];
		Arrays.fill(chars, '1');
		return new String(chars);
	}

	@Override
	public BatchPresignedUploadUrlResponse getBatchPresignedUploadUrls(UserInfo user, BatchPresignedUploadUrlRequest request) {

		ValidateArgument.required(request, "BatchPresignedUploadUrlRequest");
		ValidateArgument.required(request.getPartNumbers(), "BatchPresignedUploadUrlRequest.partNumbers");
		
		if (request.getPartNumbers().isEmpty()) {
			throw new IllegalArgumentException("BatchPresignedUploadUrlRequest.partNumbers must contain at least one value");
		}
		
		// lookup this upload.
		CompositeMultipartUploadStatus status = multipartUploadDAO.getUploadStatus(request.getUploadId());

		// validate the caller is the user that started this upload.
		validateStartedBy(user, status);
		
		final int numberOfParts = status.getNumberOfParts();
		
		final List<PartPresignedUrl> partUrls = new ArrayList<>(request.getPartNumbers().size());
		
		final MultipartRequestHandler<? extends MultipartRequest> handler = handlerProvider.getHandlerForType(status.getRequestType());
		
		final List<Long> partNumbers = request.getPartNumbers();

		for (int i = 0; i < partNumbers.size(); i++) {

			final Long partNumber = partNumbers.get(i);

			ValidateArgument.required(partNumber, "PartNumber");

			validatePartNumber(partNumber.intValue(), numberOfParts);

			PresignedUrl url = handler.getPresignedUrl(status, partNumber, request.getContentType());

			partUrls.add(map(url, partNumber));
		}

		BatchPresignedUploadUrlResponse response = new BatchPresignedUploadUrlResponse();
		
		response.setPartPresignedUrls(partUrls);
		
		return response;
	}
	
	private static PartPresignedUrl map(PresignedUrl presignedUrl, Long partNumber) {
		PartPresignedUrl part = new PartPresignedUrl();
		
		part.setPartNumber(partNumber);
		part.setUploadPresignedUrl(presignedUrl.getUrl().toString());
		part.setSignedHeaders(presignedUrl.getSignedHeaders());
		
		return part;
	}

	/**
	 * Validate a part number is within range.
	 * 
	 * @param partNumber
	 * @param numberOfParts
	 */
	public static void validatePartNumber(int partNumber, int numberOfParts) {
		if (partNumber < 1) {
			throw new IllegalArgumentException(
					"Part numbers cannot be less than one.");
		}
		if (numberOfParts < 1) {
			throw new IllegalArgumentException(
					"Number of parts cannot be less than one.");
		}
		if (partNumber > numberOfParts) {
			throw new IllegalArgumentException(
					"Part number cannot be larger than number of parts. Number of parts: "
							+ numberOfParts + ", provided part number: "
							+ partNumber);
		}
	}

	/**
	 * Validate the caller started the given upload.
	 * 
	 * @param user
	 */
	public static void validateStartedBy(UserInfo user,
			CompositeMultipartUploadStatus composite) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(composite, "CompositeMultipartUploadStatus");
		ValidateArgument.required(composite.getMultipartUploadStatus(),
				"CompositeMultipartUploadStatus.multipartUploadStatus");
		ValidateArgument
				.required(composite.getMultipartUploadStatus().getStartedBy(),
						"CompositeMultipartUploadStatus.multipartUploadStatus.startedBy");
		Long startedBy = Long.parseLong(composite.getMultipartUploadStatus()
				.getStartedBy());
		if (!startedBy.equals(user.getId())) {
			throw new UnauthorizedException(
					"Only the user that started a multipart upload can get part upload pre-signed URLs for that file upload.");
		}
	}

	@WriteTransaction
	@Override
	public AddPartResponse addMultipartPart(UserInfo user, String uploadId, Integer partNumber, String partMD5Hex) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(uploadId, "uploadId");
		ValidateArgument.required(partNumber, "partNumber");
		ValidateArgument.required(partMD5Hex, "partMD5Hex");
		// lookup this upload.
		CompositeMultipartUploadStatus composite = multipartUploadDAO.getUploadStatus(uploadId);
		// block add if the upload is complete
		if (MultipartUploadState.COMPLETED.equals(composite.getMultipartUploadStatus().getState())){
			throw new IllegalArgumentException("Cannot add parts to completed file upload.");
		}
		
		validatePartNumber(partNumber, composite.getNumberOfParts());
		// validate the user started this upload.
		validateStartedBy(user, composite);
		
		AddPartResponse response = new AddPartResponse();
		
		response.setPartNumber(new Long(partNumber));
		response.setUploadId(uploadId);
		
		final MultipartRequestHandler<? extends MultipartRequest> handler = handlerProvider.getHandlerForType(composite.getRequestType());

		try {
			handler.validateAddedPart(composite, partNumber, partMD5Hex);
			// added the part successfully.
			multipartUploadDAO.addPartToUpload(uploadId, partNumber, partMD5Hex);
			response.setAddPartState(AddPartState.ADD_SUCCESS);
		} catch (Exception e) {
			response.setErrorMessage(e.getMessage());
			response.setAddPartState(AddPartState.ADD_FAILED);
			String errorDetails = ExceptionUtils.getStackTrace(e);
			multipartUploadDAO.setPartToFailed(uploadId, partNumber, errorDetails);
		}
		return response;
	}

	@WriteTransaction
	@Override
	public MultipartUploadStatus completeMultipartUpload(UserInfo user, String uploadId) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(uploadId, "uploadId");
		
		CompositeMultipartUploadStatus composite = multipartUploadDAO.getUploadStatus(uploadId);
		
		// validate the user started this upload.
		validateStartedBy(user, composite);
		
		// Is the upload already complete?
		if (MultipartUploadState.COMPLETED.equals(composite.getMultipartUploadStatus().getState())) {
			// nothing left to do.
			return prepareCompleteStatus(composite);
		}
		
		// Are all parts added?
		List<PartMD5> addedParts = multipartUploadDAO.getAddedPartMD5s(uploadId);
		
		validateParts(composite.getNumberOfParts(), addedParts);
		
		final CloudServiceMultipartUploadDAO cloudDao = cloudDaoProvider.getCloudServiceMultipartUploadDao(composite.getUploadType());
		
		final MultipartRequestHandler<? extends MultipartRequest> handler = handlerProvider.getHandlerForType(composite.getRequestType());
		
		final String requestJson = multipartUploadDAO.getUploadRequest(uploadId);
		
		// complete the upload
		CompleteMultipartRequest completeRequest = new CompleteMultipartRequest();
		
		completeRequest.setUploadId(Long.valueOf(composite.getMultipartUploadStatus().getUploadId()));
		completeRequest.setNumberOfParts(composite.getNumberOfParts().longValue());
		completeRequest.setAddedParts(addedParts);
		completeRequest.setBucket(composite.getBucket());
		completeRequest.setKey(composite.getKey());
		completeRequest.setUploadToken(composite.getUploadToken());
		
		final long fileSize = cloudDao.completeMultipartUpload(completeRequest);
				
		final FileHandleCreateRequest fileHandleCreateRequest = handler.getFileHandleCreateRequest(composite, requestJson);
		
		// create a file handle to represent this file.
		String resultFileHandleId = createFileHandle(fileSize, composite, fileHandleCreateRequest).getId();
		
		// complete the upload.
		composite = multipartUploadDAO.setUploadComplete(uploadId, resultFileHandleId);
		
		return prepareCompleteStatus(composite);
	}
	
	@Override
	public List<String> getUploadsModifiedBefore(int numberOfDays, long batchSize) {
		ValidateArgument.requirement(numberOfDays >= 0, "The number of days must be equal or greater than zero.");
		ValidateArgument.requirement(batchSize > 0, "The batch size must be greater than zero.");
		
		return multipartUploadDAO.getUploadsModifiedBefore(numberOfDays, batchSize);
	}
	
	@Override
	@WriteTransaction
	public void clearMultipartUpload(String uploadId) {
		ValidateArgument.required(uploadId, "The upload id");
		
		final CompositeMultipartUploadStatus status;
		
		try {
			status = multipartUploadDAO.getUploadStatus(uploadId);
		} catch (NotFoundException e) {
			// Nothing to do
			return;
		}
		
		if (!MultipartUploadState.COMPLETED.equals(status.getMultipartUploadStatus().getState())) {
			final MultipartRequestHandler<? extends MultipartRequest> handler = handlerProvider.getHandlerForType(status.getRequestType());
			
			handler.tryAbortMultipartRequest(status);
		}
		
		multipartUploadDAO.deleteUploadStatus(uploadId);
	}
	
	/**
	 * Validate there is one part for the expected number of parts.
	 * 
	 * @param numberOfParts
	 * @param addedParts
	 */
	public static void validateParts(int numberOfParts,
			List<PartMD5> addedParts) {
		if (addedParts.size() < numberOfParts) {
			int missingPartCount = numberOfParts - addedParts.size();
			throw new IllegalArgumentException("Missing "
							+ missingPartCount
							+ " part(s).  All parts must be successfully added before a file upload can be completed.");
		}
	}
	
	/**
	 * Prepare a COMPLETED status for a given composite.
	 * @param composite
	 * @return
	 */
	public static MultipartUploadStatus prepareCompleteStatus(CompositeMultipartUploadStatus composite){
		ValidateArgument.required(composite, "CompositeMultipartUploadStatus");
		ValidateArgument.required(composite.getMultipartUploadStatus(), "MultipartUploadStatus");
		ValidateArgument.required(composite.getMultipartUploadStatus().getResultFileHandleId(), "ResultFileHandleId");
		ValidateArgument.required(composite.getNumberOfParts(), "NumberOfParts");
		MultipartUploadStatus status = composite.getMultipartUploadStatus();
		if(!MultipartUploadState.COMPLETED.equals(status.getState())){
			throw new IllegalArgumentException("Expected a COMPLETED state");
		}
		// Build a state string of all '1's.
		String completePartsState = getCompletePartStateString(composite.getNumberOfParts());
		status.setPartsState(completePartsState);
		return status;
	}
	
	/**
	 * Create an S3FileHandle for a multi-part upload.
	 * 
	 * @param fileSize
	 * @param composite
	 * @param request
	 * @return
	 */
	CloudProviderFileHandleInterface createFileHandle(long fileSize, CompositeMultipartUploadStatus composite, FileHandleCreateRequest request) {
		// Convert all of the data to a file handle.
		CloudProviderFileHandleInterface fileHandle;
		if (composite.getUploadType().equals(UploadType.S3)) {
			fileHandle = new S3FileHandle();
		} else if (composite.getUploadType().equals(UploadType.GOOGLECLOUDSTORAGE)) {
			fileHandle = new GoogleCloudFileHandle();
		} else {
			throw new IllegalArgumentException("Cannot create a FileHandle from a multipart upload with upload type " + composite.getUploadType());
		}
		fileHandle.setFileName(request.getFileName());
		fileHandle.setContentType(request.getContentType());
		fileHandle.setBucketName(composite.getBucket());
		fileHandle.setKey(composite.getKey());
		fileHandle.setCreatedBy(composite.getMultipartUploadStatus().getStartedBy());
		fileHandle.setCreatedOn(new Date(System.currentTimeMillis()));
		fileHandle.setEtag(UUID.randomUUID().toString());
		fileHandle.setContentMd5(request.getContentMD5());
		fileHandle.setStorageLocationId(request.getStorageLocationId());
		fileHandle.setContentSize(fileSize);
		fileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		// By default a preview should be created.
		if(request.getGeneratePreview() != null && !request.getGeneratePreview()){
			fileHandle.setPreviewId(fileHandle.getId());
		}
		// dao creates the files handle.
		return (CloudProviderFileHandleInterface) fileHandleDao.createFile(fileHandle);
	}

	@Override
	public void truncateAll() {
		this.multipartUploadDAO.truncateAll();
	}
	
	@Override
	public List<String> getUploadsOrderByUpdatedOn(long batchSize) {
		return this.multipartUploadDAO.getUploadsOrderByUpdatedOn(batchSize);
	}

}
