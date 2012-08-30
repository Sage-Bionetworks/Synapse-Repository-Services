package org.sagebionetworks.sweeper.log4j;

import com.amazonaws.services.s3.AmazonS3;

public interface AmazonS3Provider {
	public AmazonS3 getS3Client(String awsAccessKeyId, String awsAccessSecretKey);
}
