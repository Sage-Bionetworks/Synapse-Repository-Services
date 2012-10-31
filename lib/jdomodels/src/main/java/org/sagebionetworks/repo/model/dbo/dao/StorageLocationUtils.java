package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Translates between database objects and data transfer objects for StorageLocation.
 *
 * @author ewu
 */
public class StorageLocationUtils {

	/**
	 * Prepares a batch of DBO for insertions.
	 */
	public static List<DBOStorageLocation> createBatch(
			StorageLocations storageLocations, AmazonS3 s3Client) {

		if (storageLocations == null) {
			throw new NullPointerException();
		}
		if (s3Client == null) {
			throw new NullPointerException();
		}

		List<DBOStorageLocation> dboList = new ArrayList<DBOStorageLocation>();

		Long nodeId = storageLocations.getNodeId();
		Long userId = storageLocations.getUserId();

		// Attachments
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
			dboList.add(dbo);
		}

		// Locations
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
			dboList.add(dbo);
		}

		// Update content from Amazon S3
		updateContent(nodeId, dboList, s3Client);

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
	private static void updateContent(Long nodeId, List<DBOStorageLocation> dboList, AmazonS3 s3Client) {

		// No need to call S3 if there is no storage
		if (dboList.size() == 0) {
			return;
		}

		int numRetries = 0;
		boolean retry = true;
		while (retry) {

			retry = false;
			numRetries++;

			// Gather the list of all the storage locations under this node
			Map<String, S3ObjectSummary> s3ObjectMap = getObjectMapFromS3(nodeId, s3Client);
			String awss3 = LocationTypeNames.awss3.name();

			// Update the DBOs with information retrieved from S3
			for (DBOStorageLocation dbo : dboList) {
				if (awss3.equals(dbo.getStorageProvider())) {
					String key = dbo.getLocation();
					assert key != null;
					if (key.startsWith("/")) {
						key = key.substring(1);
					}
					if (s3ObjectMap.containsKey(key)) {
						S3ObjectSummary s3ObjectSummary = s3ObjectMap.get(key);
						dbo.setContentSize(s3ObjectSummary.getSize());
					} else {
						logger.info(key + " not found");
						logger.info("Retry " + numRetries);
						retry = true;
						if (numRetries > 2) {
							logger.info("Max number of retries reached. Retry aborted.");
							retry = false;
						}
					}
				}
			}
		}
	}

	private static Map<String, S3ObjectSummary> getObjectMapFromS3(Long nodeId, AmazonS3 s3Client) {

		// Gather the list of all the storage locations under this node
		Map<String, S3ObjectSummary> s3ObjectMap = new HashMap<String, S3ObjectSummary>();
		String prefix = nodeId.toString() + "/"; // Don't forget the delimiter '/'
		try {
			ObjectListing objectList = s3Client.listObjects(BUCKET, prefix);
			for (S3ObjectSummary objectSummary : objectList.getObjectSummaries()) {
				s3ObjectMap.put(objectSummary.getKey(), objectSummary);
			}
			while (objectList.isTruncated()) {
				objectList = s3Client.listNextBatchOfObjects(objectList);
				for (S3ObjectSummary objectSummary : objectList.getObjectSummaries()) {
					s3ObjectMap.put(objectSummary.getKey(), objectSummary);
				}
			}
		} catch (AmazonServiceException ase) {
			StringBuilder errMsg = new StringBuilder()
				.append("bucket = ").append(BUCKET).append(", ")
				.append("prefix = ").append(prefix).append(", ")
				.append("Request ID = ").append(ase.getRequestId()).append(", ")
				.append("Error Message = ").append(ase.getMessage()).append(", ")
				.append("HTTP Status Code = ").append(ase.getStatusCode()).append(", ")
				.append("AWS Error Code = ").append(ase.getErrorCode()).append(", ")
				.append("Error Type = ").append(ase.getErrorType());
			logger.warn(errMsg.toString());
		}

		return s3ObjectMap;
	}

	private static final Logger logger = Logger.getLogger(StorageLocationUtils.class);
	private static final String BUCKET = StackConfiguration.getS3Bucket();
}
