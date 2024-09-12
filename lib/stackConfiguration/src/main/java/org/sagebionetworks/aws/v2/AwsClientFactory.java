package org.sagebionetworks.aws.v2;

import java.time.Duration;

import org.sagebionetworks.PropertyProviderImpl;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;

public class AwsClientFactory {

	public static final AwsCredentialsProvider PROVIDER_CHAIN = createCredentialProvider();
	
	public static AwsCredentialsProvider createCredentialProvider() {
		PropertyProviderImpl provider = new PropertyProviderImpl();
		return AwsCredentialsProviderChain.builder()
				.addCredentialsProvider(new MavenSettingsAWSCredentialsProvider(provider))
				.addCredentialsProvider(new SynapseSystemPropertiesAWSCredentialsProvider(provider))
				.addCredentialsProvider(DefaultCredentialsProvider.builder().build()).build();
	}

	public BedrockAgentRuntimeAsyncClient createBedrockRuntime() {
		return BedrockAgentRuntimeAsyncClient.builder().credentialsProvider(PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.httpClient(NettyNioAsyncHttpClient.builder().connectionTimeout(Duration.ofSeconds(60)).build())
				.build();
	}
}
