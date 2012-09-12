package org.sagebionetworks.repo.manager;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.DatastoreException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * A simple utility for uploading and downloading from S3.
 * 
 * @author jmhill
 *
 */
public class AmazonS3UtilityImpl implements AmazonS3Utility{
	
	static private Log log = LogFactory.getLog(AmazonS3UtilityImpl.class);

	private static final String S3_BUCKET = StackConfiguration.getS3Bucket();
	
	/**
	 * Create a new AWS client using the configuration credentials.
	 * @return
	 */
	private AmazonS3Client createNewAWSClient() {
		String iamId = StackConfiguration.getIAMUserId();
		String iamKey = StackConfiguration.getIAMUserKey();
		if (iamId == null) throw new IllegalArgumentException("IAM id cannot be null");
		if (iamKey == null)	throw new IllegalArgumentException("IAM key cannot be null");
		AWSCredentials creds = new BasicAWSCredentials(iamId, iamKey);
		AmazonS3Client client = new AmazonS3Client(creds);
		return client;
	}

	@Override
	public File downloadFromS3(String key) throws DatastoreException {
		AmazonS3Client client = createNewAWSClient();
		log.info("Attempting to download: "+key+" from "+S3_BUCKET);
		GetObjectRequest getObjectRequest = new GetObjectRequest(S3_BUCKET, key);
		File temp;
		try {
			temp = File.createTempFile("AmazonS3Utility", ".tmp");
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		client.getObject(getObjectRequest, temp);
		return temp;
	}

	@Override
	public boolean uploadToS3(File toUpload, String key) {
		log.info("Attempting to upload: "+key+" to "+S3_BUCKET);
		AmazonS3Client client = createNewAWSClient();
		PutObjectResult results = client.putObject(S3_BUCKET, key, toUpload);
		log.info(results);
		return results.getETag() != null;
	}

	@Override
	public boolean doesExist(String key) {
		AmazonS3Client client = createNewAWSClient();
		try{
			ObjectMetadata metadata = client.getObjectMetadata(S3_BUCKET, key);
			if(metadata == null) return false;
			return metadata.getETag() != null;
		}catch (Exception e){
			return false;
		}
	}

	@Override
	public boolean deleteFromS3(String key) {
		AmazonS3Client client = createNewAWSClient();
		try{
			log.info("Deleting: "+key+" from "+S3_BUCKET);
			client.deleteObject(S3_BUCKET, key);
			return true;
		}catch(Exception e){
			return false;
		}
	}
}
