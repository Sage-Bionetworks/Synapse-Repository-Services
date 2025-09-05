package org.sagebionetworks.repo.manager.file;

import java.io.InputStream;

public interface BucketObjectReader {
	
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
