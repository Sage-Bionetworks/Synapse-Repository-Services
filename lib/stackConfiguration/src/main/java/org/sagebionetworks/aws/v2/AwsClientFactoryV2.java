package org.sagebionetworks.aws.v2;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigatewayv2.ApiGatewayV2Client;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.firehose.FirehoseClient;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * Factory for creating AWS SDK v2 clients using the Synapse credential chain.
 */
public class AwsClientFactoryV2 {

	public static StsClient createStsClient() {
		return StsClient.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

	public static SnsClient createSnsClient() {
		return SnsClient.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

	public static SqsClient createSqsClient() {
		return SqsClient.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

	public static KmsClient createKmsClient() {
		return KmsClient.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

	public static AppConfigDataClient createAppConfigDataClient() {
		return AppConfigDataClient.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

	public static SsmClient createSsmClient() {
		return SsmClient.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

	public static SfnClient createSfnClient() {
		return SfnClient.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

	public static FirehoseClient createFirehoseClient() {
		return FirehoseClient.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

	public static ApiGatewayV2Client createApiGatewayV2Client() {
		return ApiGatewayV2Client.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

	public static AthenaClient createAthenaClient() {
		return AthenaClient.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

	public static GlueClient createGlueClient() {
		return GlueClient.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

	public static SesClient createSesClient() {
		return SesClient.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

	public static CloudWatchClient createCloudWatchClient() {
		return CloudWatchClient.builder()
				.credentialsProvider(AwsCredentialsProviderV2.PROVIDER_CHAIN)
				.region(Region.US_EAST_1)
				.build();
	}

}
