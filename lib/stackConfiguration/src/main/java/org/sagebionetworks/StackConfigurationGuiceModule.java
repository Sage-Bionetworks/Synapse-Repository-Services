package org.sagebionetworks;

import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.aws.SynapseS3Client;

import com.amazonaws.services.kms.AWSKMS;
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
		bind(StackEncrypter.class).to(StackEncrypterImpl.class);
	}
	
	@Provides
	public AWSKMS provideAWSKMSClient() {
		return AwsClientFactory.createAmazonKeyManagementServiceClient();
	}
	
	@Provides
	public SynapseS3Client provideAmazonS3Client() {
		return AwsClientFactory.createAmazonS3Client();
	}

}
