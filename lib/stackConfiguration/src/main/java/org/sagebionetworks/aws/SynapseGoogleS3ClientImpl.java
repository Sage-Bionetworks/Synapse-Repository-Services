package org.sagebionetworks.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.Region;

public class SynapseGoogleS3ClientImpl implements SynapseS3Client {

	private final AmazonS3 amazonS3;

	/**
	 * Create a S3 client that connects to Google Cloud using special IAM
	 * credentials on the Google service user.
	 * 
	 * @param iamKey
	 * @param iamSecret
	 */
	public SynapseGoogleS3ClientImpl(String iamKey, String iamSecret) {
		if(iamKey == null || iamSecret == null) {
			throw new IllegalArgumentException("Google IAM key and secret are required");
		}
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
		builder.withCredentials(new AWSCredentialsProvider() {

			@Override
			public AWSCredentials getCredentials() {
				return new BasicAWSCredentials(iamKey, iamSecret);
			}

			@Override
			public void refresh() {

			}
		});
		builder.withEndpointConfiguration(new EndpointConfiguration("https://storage.googleapis.com", "auto"));
		builder.withPathStyleAccessEnabled(true);
		amazonS3 = builder.build();
	}

	@Override
	public AmazonS3 getS3ClientForBucket(String bucket) {
		return amazonS3;
	}

	@Override
	public Bucket createBucket(String bucketName) throws SdkClientException, AmazonServiceException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Region getRegionForBucket(String sourceBucket) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AmazonS3 getUSStandardAmazonClient() {
		throw new UnsupportedOperationException();
	}

}
