package org.sagebionetworks.repo.manager.file.readers;

import java.io.InputStream;

import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.file.BucketObjectReader;
import org.sagebionetworks.util.AmazonErrorCodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Service
public class S3BucketObjectReader implements BucketObjectReader {

	// A response with no error body carries no error code, so the status is the only signal that the
	// key is missing.
	private static final int HTTP_NOT_FOUND = 404;

	@Autowired
	private SynapseS3Client s3client;

	@Override
	public void verifyBucketAccess(String bucketName) {
		s3client.getRegionForBucketV2(bucketName);
	}

	@Override
	public InputStream openStream(String bucketName, String key) {
		try {
			return s3client.getObjectV2(GetObjectRequest.builder().bucket(bucketName).key(key).build());
		} catch (Throwable e) {

			if (e instanceof AwsServiceException) {
				handleAwsServiceException((AwsServiceException) e, bucketName, key);
			}

			throw new IllegalArgumentException("Could not read S3 object at key " + key + " from bucket " + bucketName + ": " + e.getMessage(), e);
		}
	}

	private void handleAwsServiceException(AwsServiceException e, String bucketName, String key) {
		String errorCode = e.awsErrorDetails() == null ? null : e.awsErrorDetails().errorCode();
		if (AmazonErrorCodes.S3_BUCKET_NOT_FOUND.equals(errorCode)) {
			throw new IllegalArgumentException("Did not find S3 bucket " + bucketName);
		}
		if (AmazonErrorCodes.S3_KEY_NOT_FOUND.equals(errorCode) || e.statusCode() == HTTP_NOT_FOUND) {
			throw new IllegalArgumentException("Did not find S3 object at key " + key + " from bucket " + bucketName);
		}
	}

}
