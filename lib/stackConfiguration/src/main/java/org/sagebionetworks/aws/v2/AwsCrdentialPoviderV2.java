package org.sagebionetworks.aws.v2;

import org.sagebionetworks.PropertyProviderImpl;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

public class AwsCrdentialPoviderV2 {

	public static final AwsCredentialsProvider PROVIDER_CHAIN = createCredentialProvider();
	
	public static AwsCredentialsProvider createCredentialProvider() {
		PropertyProviderImpl provider = new PropertyProviderImpl();
		return AwsCredentialsProviderChain.builder()
				.addCredentialsProvider(new MavenSettingsAWSCredentialsProvider(provider))
				.addCredentialsProvider(new SynapseSystemPropertiesAWSCredentialsProvider(provider))
				.addCredentialsProvider(DefaultCredentialsProvider.builder().build()).build();
	}


}
