package org.sagebionetworks;

import org.sagebionetworks.aws.AwsClientFactory;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Provides;

/**
 * Provides dependency injection mapping for the StackConfiguration project.
 *
 */
public class StackConfigurationGuiceModule extends com.google.inject.AbstractModule {

	@Override
	protected void configure() {
		bind(LoggerProvider.class).to(LoggerProviderImpl.class);
		bind(PropertyProvider.class).to(PropertyProviderImpl.class);
		bind(ConfigurationProperties.class).to(ConfigurationPropertiesImpl.class);
		bind(StackConfiguration.class).to(StackConfigurationImpl.class);
	}
	
	@Provides
	public AWSKMS provideAWSKMSClient() {
		return AwsClientFactory.createAmazonKeyManagementServiceClient();
	}
	
	@Provides
	public AmazonS3 provideAmazonS3Client() {
		return AwsClientFactory.createAmazonS3Client();
	}

}
