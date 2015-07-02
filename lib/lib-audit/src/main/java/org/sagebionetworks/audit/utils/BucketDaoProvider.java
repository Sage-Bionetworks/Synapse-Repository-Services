package org.sagebionetworks.audit.utils;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.utils.s3.BucketDao;
import org.sagebionetworks.aws.utils.s3.BucketDaoImpl;

import com.amazonaws.services.s3.AmazonS3Client;

/**
 * This is a helper class that manages bucketDao resources for ObjectRecordDAO.
 * 
 * @author kimyen
 *
 */
public class BucketDaoProvider {

	private Map<String, BucketDao> bucketDaoMap;
	private AmazonS3Client s3Client;
	private String stack;

	public BucketDaoProvider(AmazonS3Client s3Client, String stack) {
		super();
		this.s3Client = s3Client;
		this.stack = stack;
		this.bucketDaoMap = new HashMap<String, BucketDao>();
	}
	
	/**
	 * 
	 * @param type
	 * @effect if bucketDaoMap does not contain key type, before this method returns,
	 * it adds a new pair <type, new bucketDao> to bucketDaoMap
	 * @return the bucketDao of the requested type
	 */
	public BucketDao getBucketDao(String type) {
		if (bucketDaoMap.containsKey(type)) {
			return bucketDaoMap.get(type);
		}

		// first time seeing this type
		
		String bucketName = getBucketName(type);
		
		// Create the bucket if it does not exist
		s3Client.createBucket(bucketName);
		
		// create the bucketDao for this type and save it
		BucketDao bucketDao = new BucketDaoImpl(s3Client, bucketName);
		bucketDaoMap.put(type, bucketDao);
		return bucketDao;
	}
	
	/**
	 * 
	 * @param type
	 * @return the bucketName of the requested type
	 */
	public String getBucketName(String type) {
		return StackConfiguration.getObjectRecordBucketName(stack, type);
	}
}
