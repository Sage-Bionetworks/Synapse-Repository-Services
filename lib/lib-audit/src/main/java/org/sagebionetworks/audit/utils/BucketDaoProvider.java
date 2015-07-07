package org.sagebionetworks.audit.utils;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.utils.s3.BucketDao;
import org.sagebionetworks.aws.utils.s3.BucketDaoImpl;

import com.amazonaws.services.s3.AmazonS3Client;

/**
 * This is a helper class that manages bucketDao and bucketName resources for ObjectRecordDAO.
 * 
 * @author kimyen
 *
 */
public class BucketDaoProvider {

	private AmazonS3Client s3Client;
	private String stack;
	private Map<String, String> bucketNameMap;

	public BucketDaoProvider(AmazonS3Client s3Client, String stack) {
		super();
		this.s3Client = s3Client;
		this.stack = stack;
		this.bucketNameMap = new HashMap<String, String>();
	}
	
	/**
	 * 
	 * @param type
	 * @return the bucketDao of the requested type
	 */
	public BucketDao getBucketDao(String type) {
		return new BucketDaoImpl(s3Client, getBucketName(type));
	}
	
	/**
	 * 
	 * @param type
	 * @effect if bucketNameMap does not contain key type, before this method returns,
	 * it tries to create a new bucket and adds a new pair <type, new bucketName> to 
	 * bucketNameMap 
	 * @return the bucketName of the requested type
	 */
	public String getBucketName(String type) {
		if (bucketNameMap.containsKey(type)) {
			return bucketNameMap.get(type);
		}
		
		// first time seeing this type	
		String bucketName = StackConfiguration.getObjectRecordBucketName(stack, type);
		
		// Create the bucket if it does not exist
		s3Client.createBucket(bucketName);
		bucketNameMap.put(type, bucketName);
		return bucketName;
	}
}
