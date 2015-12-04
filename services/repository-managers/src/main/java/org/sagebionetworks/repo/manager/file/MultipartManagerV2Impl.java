package org.sagebionetworks.repo.manager.file;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.upload.multipart.S3MultipartUploadDAO;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class MultipartManagerV2Impl implements MultipartManagerV2 {
	
	public static final int MAX_NUMBER_OF_PARTS = 10*1000;
	public static final int MIN_PART_SIZE_BYTES = 5*1024*1024; // 5MB
	
	@Autowired
	S3MultipartUploadDAO s3multipartUploadDAO;
	
	@Autowired
	MultipartUploadDAO multiparUploadDAO;
	
	@Autowired
	ProjectSettingsManager projectSettingsManager;

	@WriteTransactionReadCommitted
	@Override
	public MultipartUploadStatus startOrResumeMultipartUpload(UserInfo user,
			MultipartUploadRequest request, Boolean forceRestart) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(user.getId(), "UserInfo.getId");
		ValidateArgument.required(request, "MultipartUploadRequest");
		ValidateArgument.required(request.getFileName(), "MultipartUploadRequest.fileName");
		ValidateArgument.required(request.getFileSizeBytes(), "MultipartUploadRequest.fileSizeBytes");
		ValidateArgument.required(request.getPartSizeBytes(), "MultipartUploadRequest.PartSizeBytes");
		ValidateArgument.required(request.getContentMD5Hex(), "MultipartUploadRequest.MD5Hex");
		if(request.getFileSizeBytes() < 1){
			throw new IllegalArgumentException("File size must be at least one byte");
		}
		if(request.getPartSizeBytes() < MIN_PART_SIZE_BYTES){
			throw new IllegalArgumentException("Part size of "+request.getPartSizeBytes()+" bytes is too small.  The minimum part size is :"+MIN_PART_SIZE_BYTES+" bytes");
		}
		int numberOfParts = calculateNumberOfParts(request.getFileSizeBytes(), request.getPartSizeBytes());
		if(numberOfParts > MAX_NUMBER_OF_PARTS){
			throw new IllegalArgumentException("Upload would required: "+numberOfParts+" parts, which exceeds the maximum number of allowed parts of: "+MAX_NUMBER_OF_PARTS);
		}
		
		// anonymous cannot upload.
		if(AuthorizationUtils.isUserAnonymous(user)){
			throw new UnauthorizedException("Anonymous cannot upload files.");
		}
		
		// The MD5 is used to identify if this upload request already exists for this user.
		String requestMD5Hex = calculateMD5AsHex(request);
		// Clear all data if this is a forced restart
		if(forceRestart != null && forceRestart.booleanValue()){
			multiparUploadDAO.deleteUploadStatus(user.getId(), requestMD5Hex);
		}
		
		// Has an upload already been started for this user and request
		CompositeMultipartUploadStatus status = multiparUploadDAO.getUploadStatus(user.getId(), requestMD5Hex);
		if(status == null){
			// This is the first time we have seen this request.
			StorageLocationSetting locationSettings = getStorageLocationSettions(request.getStorageLocationId());
			String bucket = MultipartUtils.getBucket(locationSettings);
			String key = MultipartUtils.createNewKey(user.getId().toString(), request.getFileName(), locationSettings);
			String uploadToken = s3multipartUploadDAO.initiateMultipartUpload(bucket, key, request);
			String requestJson = createRequestJSON(request);
			// Start the upload
			status = multiparUploadDAO.createUploadStatus(new CreateMultipartRequest(user.getId(), requestMD5Hex, requestJson, uploadToken, bucket, key));
			// create all of the parts for this file
			String uploadId = status.getMultipartUploadStatus().getUploadId();
			
		}
		
		return status.getMultipartUploadStatus();
	}
	
	/**
	 * Calculate the number of parts required to upload file given a part size and file size.
	 * @param fileSize
	 * @param partSize
	 * @return
	 */
	public static int calculateNumberOfParts(long fileSize, long partSize){
		if(partSize > fileSize){
			return 1;
		}
		int remainder = (int) (fileSize%partSize);
		return ((int) (fileSize/partSize)) + ( remainder > 0 ? 1:0);
	}
	
	/**
	 * Get the JSON string of the request.
	 * @param request
	 * @return
	 */
	private String createRequestJSON(MultipartUploadRequest request){
		try {
			return EntityFactory.createJSONStringForEntity(request);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	private StorageLocationSetting getStorageLocationSettions(Long storageId){
		if(storageId == null){
			return null;
		}
		return projectSettingsManager.getStorageLocationSetting(storageId);
	}
	
	/**
	 * Calculate the MD5 of the given 
	 * @param request
	 * @return
	 */
	public static String calculateMD5AsHex(JSONEntity request){
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

}
