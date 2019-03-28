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

	/**
	 * Create all region-specific instances of the AmazonS3 client using a credential chain.
	 * 
	 * @return
	 */
	public static SynapseS3Client createAmazonS3Client() {
		Map<Region, AmazonS3> regionSpecificClients = new HashMap<Region, AmazonS3>();
		for (Region region: Region.values() ) {
			AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
			builder.withCredentials(SynapseCredentialProviderChain.getInstance());
			builder.withRegion(region.name());
			builder.withPathStyleAccessEnabled(true);
			AmazonS3 amazonS3 = builder.build();
			regionSpecificClients.put(region, amazonS3);
		}
		return new SynapseS3ClientImpl(regionSpecificClients);
	}

	/**
	 * Create an instance of the TransferManager using a credential chain.
	 * 
	 * @return
	 */
	public static TransferManager createTransferManager() {
		return new TransferManager(SynapseCredentialProviderChain.getInstance());
	}

	/**
	 * Create an instance of the AmazonCloudWatch using a credential chain.
	 * 
	 * @return
	 */
	public static AmazonCloudWatch createCloudWatchClient() {
		AmazonCloudWatchClientBuilder builder = AmazonCloudWatchClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(SynapseCredentialProviderChain.getInstance());
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
		builder.withCredentials(SynapseCredentialProviderChain.getInstance());
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
		builder.withCredentials(SynapseCredentialProviderChain.getInstance());
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
		builder.withCredentials(SynapseCredentialProviderChain.getInstance());
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
		builder.withCredentials(SynapseCredentialProviderChain.getInstance());
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
		builder.withCredentials(SynapseCredentialProviderChain.getInstance());
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
		builder.withCredentials(SynapseCredentialProviderChain.getInstance());
		return builder.build();
	}

}
