package org.sagebionetworks.repo.manager.storagelocation.objectreaders;

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
		S3Object s3object;
		try {
			s3object = s3client.getObject(bucketName, key);
		} catch (AmazonServiceException e) {
			if (AmazonErrorCodes.S3_BUCKET_NOT_FOUND.equals(e.getErrorCode())) {
				throw new IllegalArgumentException("Did not find S3 bucket " + bucketName);
			} else if (AmazonErrorCodes.S3_NOT_FOUND.equals(e.getErrorCode())
					|| AmazonErrorCodes.S3_KEY_NOT_FOUND.equals(e.getErrorCode())) {
				throw new IllegalArgumentException("Did not find S3 object at key " + key + " from bucket " + bucketName);

			}
			throw new IllegalArgumentException("Could not read S3 object at key " + key + " from bucket " + bucketName + ": " + e.getMessage());
		}
		return s3object.getObjectContent();
	}

}
