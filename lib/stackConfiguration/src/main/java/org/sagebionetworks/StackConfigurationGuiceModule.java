package org.sagebionetworks;

import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.aws.v2.AwsClientFactoryV2;

import software.amazon.awssdk.services.kms.KmsClient;
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
	public KmsClient provideKmsClient() {
		return AwsClientFactoryV2.createKmsClient();
	}

	@Provides
	public SynapseS3Client provideAmazonS3Client() {
		return AwsClientFactory.createAmazonS3Client();
	}

}
