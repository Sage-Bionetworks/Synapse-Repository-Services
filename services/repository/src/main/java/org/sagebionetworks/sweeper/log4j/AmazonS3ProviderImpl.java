package org.sagebionetworks.sweeper.log4j;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

public class AmazonS3ProviderImpl implements AmazonS3Provider {

	@Override
	public AmazonS3 getS3Client(String awsAccessKeyId, String awsAccessSecretKey) {
		return new AmazonS3Client(new BasicAWSCredentials(awsAccessKeyId, awsAccessSecretKey));
	}

}
