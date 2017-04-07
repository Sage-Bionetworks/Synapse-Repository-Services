package org.sagebionetworks.repo.manager.file;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartMD5;
import org.sagebionetworks.repo.model.file.PartPresignedUrl;
import org.sagebionetworks.repo.model.file.PartUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.AddPartState;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.upload.multipart.S3MultipartUploadDAO;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;


public class MultipartManagerV2Impl implements MultipartManagerV2 {

	@Autowired
	S3MultipartUploadDAO s3multipartUploadDAO;

	@Autowired
	MultipartUploadDAO multipartUploadDAO;

	@Autowired
	FileHandleDao fileHandleDao;

	@Autowired
	ProjectSettingsManager projectSettingsManager;

	@Autowired
	IdGenerator idGenerator;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.file.MultipartManagerV2#
	 * startOrResumeMultipartUpload(org.sagebionetworks.repo.model.UserInfo,
	 * org.sagebionetworks.repo.model.file.MultipartUploadRequest,
	 * java.lang.Boolean)
	 */
	@WriteTransactionReadCommitted
	@Override
	public MultipartUploadStatus startOrResumeMultipartUpload(UserInfo user,
			MultipartUploadRequest request, Boolean forceRestart) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(user.getId(), "UserInfo.getId");
		ValidateArgument.required(request, "MultipartUploadRequest");
		ValidateArgument.required(request.getFileName(),
				"MultipartUploadRequest.fileName");
		ValidateArgument.required(request.getFileSizeBytes(),
				"MultipartUploadRequest.fileSizeBytes");
		ValidateArgument.required(request.getPartSizeBytes(),
				"MultipartUploadRequest.PartSizeBytes");
		ValidateArgument.required(request.getContentMD5Hex(),
				"MultipartUploadRequest.MD5Hex");

		// anonymous cannot upload. See: PLFM-2621.
		if (AuthorizationUtils.isUserAnonymous(user)) {
			throw new UnauthorizedException("Anonymous cannot upload files.");
		}
		// The MD5 is used to identify if this upload request already exists for
		// this user.
		String requestMD5Hex = calculateMD5AsHex(request);
		// Clear all data if this is a forced restart
		if (forceRestart != null && forceRestart.booleanValue()) {
			multipartUploadDAO.deleteUploadStatus(user.getId(), requestMD5Hex);
		}

		// Has an upload already been started for this user and request
		CompositeMultipartUploadStatus status = multipartUploadDAO
				.getUploadStatus(user.getId(), requestMD5Hex);
		if (status == null) {
			// Since the status for this file does not exist, create it.
			status = createNewMultipartUpload(user, request, requestMD5Hex);
		}
		// Calculate the parts state for this file.
		String partsState = setupPartState(status);
		status.getMultipartUploadStatus().setPartsState(partsState);
		return status.getMultipartUploadStatus();
	}

	/**
	 * Create a new multi-part upload both in S3 and the database.
	 * 
	 * @param user
	 * @param request
	 * @param requestMD5Hex
	 * @return
	 */
	private CompositeMultipartUploadStatus createNewMultipartUpload(
			UserInfo user, MultipartUploadRequest request, String requestMD5Hex) {
		// This is the first time we have seen this request.
		StorageLocationSetting locationSettings = getStorageLocationSettions(request
				.getStorageLocationId());
		// the bucket depends on the upload location used.
		String bucket = MultipartUtils.getBucket(locationSettings);
		// create a new key for this file.
		String key = MultipartUtils.createNewKey(user.getId().toString(),
				request.getFileName(), locationSettings);
		String uploadToken = s3multipartUploadDAO.initiateMultipartUpload(
				bucket, key, request);
		String requestJson = createRequestJSON(request);
		// How many parts will be needed to upload this file?
		int numberParts = PartUtils.calculateNumberOfParts(request.getFileSizeBytes(),
				request.getPartSizeBytes());
		// Start the upload
		return multipartUploadDAO
				.createUploadStatus(new CreateMultipartRequest(user.getId(),
						requestMD5Hex, requestJson, uploadToken, bucket, key,
						numberParts));
	}

	/**
	 * Setup the partsState string for a given status multi-part status. For the
	 * cases where the file upload is complete, this string is built without a
	 * database call.
	 * 
	 * @param status
	 */
	public String setupPartState(CompositeMultipartUploadStatus status) {
		if (status.getMultipartUploadStatus().getResultFileHandleId() != null) {
			// When the upload is done we just create a string of all '1'
			// without hitting the DB.
			return getCompletePartStateString(status.getNumberOfParts());
		} else {
			// Get the parts state from the DAO.
			return multipartUploadDAO.getPartsState(status
					.getMultipartUploadStatus().getUploadId(), status
					.getNumberOfParts());
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

	/**
	 * Get the JSON string of the request.
	 * 
	 * @param request
	 * @return
	 */
	public static String createRequestJSON(MultipartUploadRequest request) {
		try {
			return EntityFactory.createJSONStringForEntity(request);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private StorageLocationSetting getStorageLocationSettions(Long storageId) {
		if (storageId == null) {
			return null;
		}
		return projectSettingsManager.getStorageLocationSetting(storageId);
	}

	/**
	 * Calculate the MD5 of the given
	 * 
	 * @param request
	 * @return
	 */
	public static String calculateMD5AsHex(JSONEntity request) {
		try {
			String json = EntityFactory.createJSONStringForEntity(request);
			byte[] jsonBytes = json.getBytes("UTF-8");
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			byte[] md5Byptes = messageDigest.digest(jsonBytes);
			return new String(Hex.encodeHex(md5Byptes));
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.file.MultipartManagerV2#
	 * getBatchPresignedUploadUrls(org.sagebionetworks.repo.model.UserInfo,
	 * org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest)
	 */
	@Override
	public BatchPresignedUploadUrlResponse getBatchPresignedUploadUrls(
			UserInfo user, BatchPresignedUploadUrlRequest request) {

		ValidateArgument.required(request, "BatchPresignedUploadUrlRequest");
		ValidateArgument.required(request.getPartNumbers(),
				"BatchPresignedUploadUrlRequest.partNumbers");
		if (request.getPartNumbers().isEmpty()) {
			throw new IllegalArgumentException(
					"BatchPresignedUploadUrlRequest.partNumbers must contain at least one value");
		}
		// lookup this upload.
		CompositeMultipartUploadStatus status = multipartUploadDAO
				.getUploadStatus(request.getUploadId());
		int numberOfParts = status.getNumberOfParts();

		// validate the caller is the user that started this upload.
		validateStartedBy(user, status);
		List<PartPresignedUrl> partUrls = new LinkedList<PartPresignedUrl>();
		for (Long partNumberL : request.getPartNumbers()) {
			ValidateArgument.required(partNumberL, "PartNumber cannot be null");
			int partNumber = partNumberL.intValue();
			validatePartNumber(partNumber, numberOfParts);
			String partKey = createPartKey(status.getKey(), partNumber);
			URL url = s3multipartUploadDAO.createPreSignedPutUrl(
					status.getBucket(), partKey, request.getContentType());
			PartPresignedUrl part = new PartPresignedUrl();
			part.setPartNumber((long) partNumber);
			part.setUploadPresignedUrl(url.toString());
			partUrls.add(part);
		}
		BatchPresignedUploadUrlResponse response = new BatchPresignedUploadUrlResponse();
		response.setPartPresignedUrls(partUrls);
		return response;
	}

	/**
	 * Create a part key using the base and part number.
	 * 
	 * @param baseKey
	 * @param partNumber
	 * @return
	 */
	public static String createPartKey(String baseKey, int partNumber) {
		return String.format("%1$s/%2$d", baseKey, partNumber);
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
	 * @param startedBy
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

	@WriteTransactionReadCommitted
	@Override
	public AddPartResponse addMultipartPart(UserInfo user, String uploadId,
			Integer partNumber, String partMD5Hex) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(uploadId, "uploadId");
		ValidateArgument.required(partNumber, "partNumber");
		ValidateArgument.required(partMD5Hex, "partMD5Hex");
		// lookup this upload.
		CompositeMultipartUploadStatus composite = multipartUploadDAO
				.getUploadStatus(uploadId);
		// block add if the upload is complete
		if(MultipartUploadState.COMPLETED.equals(composite.getMultipartUploadStatus().getState())){
			throw new IllegalArgumentException("Cannot add parts to completed file upload.");
		}
		validatePartNumber(partNumber, composite.getNumberOfParts());
		// validate the user started this upload.
		validateStartedBy(user, composite);
		String partKey = createPartKey(composite.getKey(), partNumber);

		AddPartResponse response = new AddPartResponse();
		response.setPartNumber(new Long(partNumber));
		response.setUploadId(uploadId);
		try {
			s3multipartUploadDAO.addPart(new AddPartRequest(composite
					.getUploadToken(), composite.getBucket(), composite
					.getKey(), partKey, partMD5Hex, partNumber));
			// added the part successfully.
			multipartUploadDAO
					.addPartToUpload(uploadId, partNumber, partMD5Hex);
			response.setAddPartState(AddPartState.ADD_SUCCESS);
			// after a part is added we can delete the part file
			s3multipartUploadDAO.deleteObject(composite.getBucket(), partKey);
		} catch (Exception e) {
			response.setErrorMessage(e.getMessage());
			response.setAddPartState(AddPartState.ADD_FAILED);
			String errorDetails = ExceptionUtils.getStackTrace(e);
			multipartUploadDAO.setPartToFailed(uploadId, partNumber,
					errorDetails);
		}
		return response;
	}

	@WriteTransactionReadCommitted
	@Override
	public MultipartUploadStatus completeMultipartUpload(UserInfo user,
			String uploadId) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(uploadId, "uploadId");
		CompositeMultipartUploadStatus composite = multipartUploadDAO
				.getUploadStatus(uploadId);
		// validate the user started this upload.
		validateStartedBy(user, composite);
		// Is the upload already complete?
		if (MultipartUploadState.COMPLETED.equals(composite
				.getMultipartUploadStatus().getState())) {
			// nothing left to do.
			return prepareCompleteStatus(composite);
		}
		// Are all parts added?
		List<PartMD5> addedParts = multipartUploadDAO
				.getAddedPartMD5s(uploadId);
		validateParts(composite.getNumberOfParts(), addedParts);
		// complete the upload
		CompleteMultipartRequest request = new CompleteMultipartRequest();
		request.setAddedParts(addedParts);
		request.setBucket(composite.getBucket());
		request.setKey(composite.getKey());
		request.setUploadToken(composite.getUploadToken());
		// This will create the object in S3
		long fileContentSize = s3multipartUploadDAO
				.completeMultipartUpload(request);

		// Get the original request
		MultipartUploadRequest originalRequest = getRequestForUpload(uploadId);
		// create a file handle to represent this file.
		S3FileHandle resultFileHandle = createFileHandle(fileContentSize, composite, originalRequest);
		// complete the upload.
		composite = multipartUploadDAO.setUploadComplete(uploadId, resultFileHandle.getId());
		return prepareCompleteStatus(composite);
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
			int missingPartCount = numberOfParts
					- addedParts.size();
			throw new IllegalArgumentException(
					"Missing "
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.manager.file.MultipartManagerV2#getRequestForUpload
	 * (java.lang.String)
	 */
	public MultipartUploadRequest getRequestForUpload(String uploadId) {
		String requestJson = multipartUploadDAO.getUploadRequest(uploadId);
		try {
			return EntityFactory.createEntityFromJSONString(requestJson,
					MultipartUploadRequest.class);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Create an S3FileHandle for a multi-part upload.
	 * 
	 * @param fileSize
	 * @param composite
	 * @param request
	 * @return
	 */
	@WriteTransactionReadCommitted
	@Override
	public S3FileHandle createFileHandle(long fileSize, CompositeMultipartUploadStatus composite, MultipartUploadRequest request){
		// Convert all of the data to a file handle.
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setFileName(request.getFileName());
		fileHandle.setContentType(request.getContentType());
		fileHandle.setBucketName(composite.getBucket());
		fileHandle.setKey(composite.getKey());
		fileHandle.setCreatedBy(composite.getMultipartUploadStatus().getStartedBy());
		fileHandle.setCreatedOn(new Date(System.currentTimeMillis()));
		fileHandle.setEtag(UUID.randomUUID().toString());
		fileHandle.setContentMd5(request.getContentMD5Hex());
		fileHandle.setStorageLocationId(request.getStorageLocationId());
		fileHandle.setContentSize(fileSize);
		// By default a preview should be created.	
		fileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		if(request.getGeneratePreview() != null && !request.getGeneratePreview()){
			fileHandle.setPreviewId(fileHandle.getId());
		}
		// dao creates the files handle.
		return (S3FileHandle) fileHandleDao.createFile(fileHandle);
	}

	@Override
	public void truncateAll() {
		this.multipartUploadDAO.truncateAll();
	}

}
