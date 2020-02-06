package org.sagebionetworks.repo.manager.storagelocation.objectreaders;

import java.io.IOException;
import java.io.InputStream;

import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.storagelocation.BucketObjectReader;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.util.AmazonErrorCodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.S3Object;

@Service
public class S3BucketObjectReader implements BucketObjectReader {

	@Autowired
	private SynapseS3Client s3client;

	@Override
	public Class<? extends BucketOwnerStorageLocationSetting> getSupportedStorageLocationType() {
		return ExternalS3StorageLocationSetting.class;
	}
	
	@Override
	public void verifyBucketAccess(String bucketName) {
		s3client.getRegionForBucket(bucketName);
	}

	@Override
	public InputStream openStream(String bucketName, String key) {
		S3Object s3object = null;
		
		try {
			s3object = s3client.getObject(bucketName, key);
			
			return s3object.getObjectContent();
		
		} catch (Throwable e) {

			dispose(s3object);
			
			if (e instanceof AmazonServiceException) {
				handleAmazonServiceException((AmazonServiceException) e, bucketName, key);
			}
			
			throw new IllegalArgumentException("Could not read S3 object at key " + key + " from bucket " + bucketName + ": " + e.getMessage(), e);
		}
	}
	
	private void handleAmazonServiceException(AmazonServiceException e, String bucketName, String key) {
		String errorCode = e.getErrorCode();
		if (AmazonErrorCodes.S3_BUCKET_NOT_FOUND.equals(errorCode)) {
			throw new IllegalArgumentException("Did not find S3 bucket " + bucketName);
		} 
		if (AmazonErrorCodes.S3_NOT_FOUND.equals(errorCode) || AmazonErrorCodes.S3_KEY_NOT_FOUND.equals(errorCode)) {
			throw new IllegalArgumentException("Did not find S3 object at key " + key + " from bucket " + bucketName);
		}
	}
	
	private void dispose(S3Object s3object) {
		if (s3object == null) {
			return;
		}
		
		try {
			s3object.close();
		} catch (IOException ex) {
			// Nothing we can do
		}
	}

}
