package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Date;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.storage.StorageUsage;

/**
 * Translates between database objects and data transfer objects for StorageLocation.
 */
public class StorageLocationUtils {

	private static final String BUCKET = StackConfiguration.getS3Bucket();
	
	/**
	 * Create a FileHandle from a StorageUsage object.
	 * @param su
	 * @return
	 */
	public static S3FileHandle createFileHandle(StorageUsage su){
		S3FileHandle handle = new S3FileHandle();
		handle.setBucketName(BUCKET);
		handle.setContentMd5(su.getContentMd5());
		handle.setContentSize(su.getContentSize());
		handle.setContentType(su.getContentType());
		handle.setCreatedBy(su.getUserId());
		handle.setCreatedOn(new Date(System.currentTimeMillis()));
		String location = su.getLocation();
		String key = location.substring(1, location.length());
		String fileName = location.substring(location.lastIndexOf("/")+1, location.length());
		handle.setKey(key);
		handle.setFileName(fileName);
		handle.setEtag(UUID.randomUUID().toString());
		return handle;
	}
}
