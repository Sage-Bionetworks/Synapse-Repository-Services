package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.StorageLocations;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class StorageLocationUtils {

	/**
	 * Prepares a batch of DBO for insertions.
	 */
	public static List<DBOStorageLocation> createBatch(StorageLocations storageLocations) {

		List<DBOStorageLocation> dboList = new ArrayList<DBOStorageLocation>();

		Long nodeId = storageLocations.getNodeId();
		Long userId = storageLocations.getUserId();

		List<AttachmentData> attachmentList = storageLocations.getAttachments();
		for (AttachmentData attachment : attachmentList) {
			DBOStorageLocation dbo = new DBOStorageLocation();
			dbo.setNodeId(nodeId);
			dbo.setUserId(userId);
			dbo.setIsAttachment(true);
			dbo.setLocation("/" + nodeId + "/" + attachment.getTokenId());
			dbo.setStorageProvider(LocationTypeNames.awss3.name());
			dbo.setContentType(attachment.getContentType());
			dbo.setContentMd5(attachment.getMd5());
			setContentSize(dbo);
			dboList.add(dbo);
		}

		List<LocationData> locationList = storageLocations.getLocations();
		String contentType = null;
		String md5 = null;
		if (locationList.size() > 0) {
			Map<String, List<String>> strAnnotations = storageLocations.getStrAnnotations();
			if (strAnnotations != null) {
				List<String> contentTypes = strAnnotations.get("contentType");
				if (contentTypes != null && contentTypes.size() > 0) {
					contentType = contentTypes.get(0);
				}
				List<String> md5s = strAnnotations.get("md5");
				if (md5s != null && md5s.size() > 0) {
					md5 = md5s.get(0);
				}
			}
		}
		for (LocationData location : locationList) {
			DBOStorageLocation dbo = new DBOStorageLocation();
			dbo.setNodeId(nodeId);
			dbo.setUserId(userId);
			dbo.setIsAttachment(false);
			dbo.setLocation(location.getPath());
			dbo.setStorageProvider(location.getType().name());
			if (contentType != null) {
				dbo.setContentType(contentType);
			}
			if (md5 != null) {
				dbo.setContentMd5(md5);
			}
			setContentSize(dbo);
			dboList.add(dbo);
		}

		return dboList;
	}

	////// private //////

	/**
	 * Calls external services to determine the size of the storage used to store the object.
	 * Also sets other attributes if they are null on the current DB object and are available
	 * from the external storage services.
	 *
	 * @throws AmazonClientException  When the AWS S3 client encounters an internal error
	 */
	private static boolean setContentSize(DBOStorageLocation dbo) {
		if (LocationTypeNames.awss3.name().equalsIgnoreCase(dbo.getStorageProvider())) {
			try {
				String path = dbo.getLocation();
				ObjectMetadata metaData = S3_CLIENT.getObjectMetadata(BUCKET, path);
				long length = metaData.getContentLength(); // number of bytes
				dbo.setContentSize(length);
				if (dbo.getContentMd5() == null) {
					dbo.setContentMd5(metaData.getContentMD5());
				}
				if (dbo.getContentType() == null) {
					dbo.setContentType(metaData.getContentType());
				}
				return true;
			} catch (AmazonServiceException ase) {
				StringBuilder errMsg = new StringBuilder()
						.append("bucket = ").append(BUCKET).append(", ")
						.append("path = ").append(dbo.getLocation()).append(", ")
						.append("Request ID = ").append(ase.getRequestId()).append(", ")
						.append("Error Message = ").append(ase.getMessage()).append(", ")
						.append("HTTP Status Code = ").append(ase.getStatusCode()).append(", ")
						.append("AWS Error Code = ").append(ase.getErrorCode()).append(", ")
						.append("Error Type = ").append(ase.getErrorType());
				logger.info(errMsg.toString());
			}
		}
		return false;
	}

	private static final String BUCKET = StackConfiguration.getS3Bucket();

	private static final AmazonS3 S3_CLIENT;
	static {
		String accessKey = StackConfiguration.getIAMUserId();
		String secretKey = StackConfiguration.getIAMUserKey();
		AWSCredentials awsCredential = new BasicAWSCredentials(accessKey, secretKey);
		S3_CLIENT = new AmazonS3Client(awsCredential);
	}

	private static final Logger logger = Logger.getLogger(StorageLocationUtils.class);
}
