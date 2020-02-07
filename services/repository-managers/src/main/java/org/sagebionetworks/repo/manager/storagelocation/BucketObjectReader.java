package org.sagebionetworks.repo.manager.storagelocation;

import java.io.InputStream;

import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;

public interface BucketObjectReader {
	
	/**
	 * @return The type of storage location supported by this reader
	 */
	Class<? extends BucketOwnerStorageLocationSetting> getSupportedStorageLocationType();
	
	/**
	 * Verifies that we have access to the bucket with the given name
	 * 
	 * @param bucketName
	 * @throws IllegalArgumentException If synapse cannot access the given bucket
	 */
	void verifyBucketAccess(String bucketName);

	/**
	 * Returns a stream to the given key in the given bucket
	 * 
	 * @param bucketName
	 * @param key
	 * @return
	 * 
	 * @throws IllegalArgumentException If synapse cannot open a stream to the given key
	 */
	InputStream openStream(String bucketName, String key);
	
}
