package org.sagebionetworks.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSSessionCredentials;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;

/**
 * This class wraps the v2 ProfileCredentialsProvider with a v1 AWSCredentialsProvider
 * for use with the v1 AWS client.
 */
public class ProfileCredentialsProviderV2V1Adapter implements AWSCredentialsProvider {
	
	private ProfileCredentialsProvider profileCredentialsProvider;

	public ProfileCredentialsProviderV2V1Adapter() {
		profileCredentialsProvider = ProfileCredentialsProvider.builder().build();
	}

	@Override
	public void refresh() {
		// nothing to do
	}

	@Override
	public AWSCredentials getCredentials() {
		AwsSessionCredentials v2credentials = (AwsSessionCredentials)
				profileCredentialsProvider.resolveCredentials();
		return new AWSSessionCredentials() {
			public String getAWSAccessKeyId() {return v2credentials.accessKeyId();}
			public String getAWSSecretKey() {return v2credentials.secretAccessKey();}
			public String getSessionToken() {return v2credentials.sessionToken();}
		};
	}
}
