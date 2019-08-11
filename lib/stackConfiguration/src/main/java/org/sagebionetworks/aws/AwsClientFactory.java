package org.sagebionetworks.aws;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClientBuilder;
import com.amazonaws.services.cloudsearchv2.AmazonCloudSearch;
import com.amazonaws.services.cloudsearchv2.AmazonCloudSearchClientBuilder;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSAsyncClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

/**
 * A factory for creating AWS clients using credential chains.
 *
 */
public class AwsClientFactory {
	
	/*
	 * AmazonS3ClientBuilder takes as a parameter a value from com.amazonaws.regions.Regions, 
	 * which has String values like US_EAST_1, US_WEST_1, CA_CENTRAL_1.
	 * 
	 * AmazonS3.getBucketLocation() returns a String representation of an instance of 
	 * com.amazonaws.services.s3.model.Region, which has String values like null (for us-east-1), 
	 * us-west-1, ca-central-1.
	 * 
	 * To make things more complicated, there is a utility to map from Regions to Region but it's 
	 * com.amazonaws.regions.Region, not com.amazonaws.services.s3.model.Region, and it has values 
	 * like us-east-1, us-west-1, ca-central-1.
	 * 
	 * So we have to map in two steps:
	 */
	public static Region getS3RegionForAWSRegions(Regions awsRegion) {
		if (awsRegion==Regions.US_EAST_1) return Region.US_Standard; // string value of Region.US_Standard is null!
		com.amazonaws.regions.Region regionsRegion = com.amazonaws.regions.Region.getRegion(awsRegion);
		return Region.fromValue(regionsRegion.getName()); // this wouldn't work for us-east-1
	}

	/**
	 * Create all region-specific instances of the AmazonS3 client using a credential chain.
	 * 
	 * @return
	 */
	public static SynapseS3Client createAmazonS3Client() {
		Map<Region, AmazonS3> regionSpecificS3Clients = new HashMap<Region, AmazonS3>();
		for (Regions region: Regions.values() ) {
			AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
			builder.withCredentials(SynapseAWSCredentialsProviderChain.getInstance());
			builder.withRegion(region);
			builder.withPathStyleAccessEnabled(true);
			builder.withForceGlobalBucketAccessEnabled(true);
			AmazonS3 amazonS3 = builder.build();
			regionSpecificS3Clients.put(getS3RegionForAWSRegions(region), amazonS3);
		}
		return new SynapseS3ClientImpl(regionSpecificS3Clients);
	}

	/**
	 * Create an instance of the TransferManager using a credential chain.
	 * 
	 * @return
	 */
	public static TransferManager createTransferManager() {
		return new TransferManager(SynapseAWSCredentialsProviderChain.getInstance());
	}

	/**
	 * Create an instance of the AmazonCloudWatch using a credential chain.
	 * 
	 * @return
	 */
	public static AmazonCloudWatch createCloudWatchClient() {
		AmazonCloudWatchClientBuilder builder = AmazonCloudWatchClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(SynapseAWSCredentialsProviderChain.getInstance());
		return builder.build();
	}

	/**
	 * Create an instance of the AmazonCloudSearch using a credential chain.
	 * 
	 * @return
	 */
	public static AmazonCloudSearch createAmazonCloudSearchClient() {
		AmazonCloudSearchClientBuilder builder = AmazonCloudSearchClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(SynapseAWSCredentialsProviderChain.getInstance());
		return builder.build();
	}

	/**
	 * Create an instance of the AmazonCloudSearchDomain using a credential chain.
	 * 
	 * @return
	 */
	public static AmazonCloudSearchDomain createAmazonCloudSearchDomain(String endpoint) {
		AmazonCloudSearchDomainClientBuilder builder = AmazonCloudSearchDomainClientBuilder.standard();
		builder.withEndpointConfiguration(new EndpointConfiguration(endpoint, Regions.US_EAST_1.getName()));
		builder.withCredentials(SynapseAWSCredentialsProviderChain.getInstance());
		return builder.build();
	}

	/**
	 * Create an instance of the AmazonSQS using a credential chain.
	 * 
	 * @return
	 */
	public static AmazonSQS createAmazonSQSClient() {
		AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(SynapseAWSCredentialsProviderChain.getInstance());
		return builder.build();
	}

	/**
	 * Create an instance of AmazonSNS using a credential chain.
	 * 
	 * @return
	 */
	public static AmazonSNS createAmazonSNSClient() {
		AmazonSNSClientBuilder builder = AmazonSNSClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(SynapseAWSCredentialsProviderChain.getInstance());
		return builder.build();
	}

	/**
	 * Create an instance of AmazonSimpleEmailService using a credential chain.
	 * 
	 * @return
	 */
	public static AmazonSimpleEmailService createAmazonSimpleEmailServiceClient() {
		AmazonSimpleEmailServiceClientBuilder builder = AmazonSimpleEmailServiceClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(SynapseAWSCredentialsProviderChain.getInstance());
		return builder.build();
	}

	/**
	 * Create an instance of AWSKMS client using a credential chain.
	 * 
	 * @return
	 */
	public static AWSKMS createAmazonKeyManagementServiceClient() {
		AWSKMSAsyncClientBuilder builder = AWSKMSAsyncClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(SynapseAWSCredentialsProviderChain.getInstance());
		return builder.build();
	}

	public static AmazonKinesisFirehose createAmazonKinesisFirehoseClient(){
		return AmazonKinesisFirehoseClientBuilder.standard()
				.withRegion(Regions.US_EAST_1)
				.withCredentials(SynapseAWSCredentialsProviderChain.getInstance())
				.build();
	}

}
