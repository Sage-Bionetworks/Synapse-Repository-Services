package org.sagebionetworks.aws;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClientBuilder;
import com.amazonaws.services.cloudsearchv2.AmazonCloudSearch;
import com.amazonaws.services.cloudsearchv2.AmazonCloudSearchClientBuilder;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSAsyncClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

/**
 * A factory for creating AWS clients using credential chains.
 *
 */
public class AwsClientFactory {

	public static final String AWS_SECRET_KEY_OLD = "AWS_SECRET_KEY";
	public static final String AWS_ACCESS_KEY_ID_OLD = "AWS_ACCESS_KEY_ID";
	public static final String AWS_ACCESS_KEY_ID = "aws.accessKeyId";
	public static final String AWS_SECRET_KEY = "aws.secretKey";
	public static final String OLD_ORG_SAGEBIONETWORKS_STACK_IAM_ID = "org.sagebionetworks.stack.iam.id";
	public static final String OLD_ORG_SAGEBIONETWORKS_STACK_IAM_KEY = "org.sagebionetworks.stack.iam.key";

	static {
		/*
		 * If 'aws.accessKeyId' is missing from the system properties, attempt to set it
		 * with 'org.sagebionetworks.stack.iam.key'
		 */
		if (System.getProperty(AWS_ACCESS_KEY_ID) == null) {
			setSystemProperty(AWS_ACCESS_KEY_ID, System.getProperty(OLD_ORG_SAGEBIONETWORKS_STACK_IAM_ID));
		}
		/*
		 * If 'aws.secretKey' is missing from the system properties, attempt to set it
		 * with 'org.sagebionetworks.stack.iam.key'
		 */
		if (System.getProperty(AWS_SECRET_KEY) == null) {
			setSystemProperty(AWS_SECRET_KEY, System.getProperty(OLD_ORG_SAGEBIONETWORKS_STACK_IAM_KEY));
		}
		/*
		 * If 'aws.accessKeyId' is missing from the system properties, attempt to set it
		 * with 'AWS_ACCESS_KEY_ID'
		 */
		if (System.getProperty(AWS_ACCESS_KEY_ID) == null) {
			setSystemProperty(AWS_ACCESS_KEY_ID, System.getProperty(AWS_ACCESS_KEY_ID_OLD));
		}
		/*
		 * If 'aws.secretKey' is missing from the system properties, attempt to set it
		 * with 'AWS_SECRET_KEY'
		 */
		if (System.getProperty(AWS_SECRET_KEY) == null) {
			setSystemProperty(AWS_SECRET_KEY, System.getProperty(AWS_SECRET_KEY_OLD));
		}
	}
	
	/**
	 * Set the given property to System.setProperty() if the key and value are not null and the value is not empty.
	 * @param key
	 * @param value
	 */
	public static void setSystemProperty(String key, String value) {
		if(key != null && value != null) {
			value = value.trim();
			if(value.length() > 0) {
				System.setProperty(key, value);
			}
		}
	}

	/**
	 * Create an instance of the AmazonS3 client using a credential chain.
	 * @return
	 */
	public static AmazonS3 createAmazonS3Client() {
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		builder.withRegion(Regions.US_EAST_1);
		builder.withPathStyleAccessEnabled(true);
		return builder.build();
	}

	/**
	 * Create an instance of the TransferManager using a credential chain.
	 * @return
	 */
	public static TransferManager createTransferManager() {
		return new TransferManager(new DefaultAWSCredentialsProviderChain());
	}
	
	/**
	 * Create an instance of the AmazonCloudWatch using a credential chain.
	 * @return
	 */
	public static AmazonCloudWatch createCloudWatchClient() {
		AmazonCloudWatchClientBuilder builder = AmazonCloudWatchClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		return builder.build();
	}
	
	/**
	 * Create an instance of the AmazonCloudSearch using a credential chain.
	 * @return
	 */
	public static AmazonCloudSearch createAmazonCloudSearchClient() {
		AmazonCloudSearchClientBuilder builder = AmazonCloudSearchClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		return builder.build();
	}
	
	/**
	 * Create an instance of the AmazonCloudSearchDomain using a credential chain.
	 * @return
	 */
	public static AmazonCloudSearchDomain createAmazonCloudSearchDomain(String endpoint) {
		AmazonCloudSearchDomainClientBuilder builder = AmazonCloudSearchDomainClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		builder.setEndpointConfiguration(new EndpointConfiguration(endpoint, Regions.US_EAST_1.getName()));
		return builder.build();
	}
	
	/**
	 * Create an instance of the AmazonSQS using a credential chain.
	 * @return
	 */
	public static AmazonSQS createAmazonSQSClient() {
		AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		return builder.build();
	}
	
	/**
	 * Create an instance of AmazonSNS using a credential chain.
	 * @return
	 */
	public static AmazonSNS createAmazonSNSClient() {
		AmazonSNSClientBuilder builder = AmazonSNSClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		return builder.build();
	}
	
	/**
	 * Create an instance of AmazonSimpleEmailService using a credential chain.
	 * @return
	 */
	public static AmazonSimpleEmailService createAmazonSimpleEmailServiceClient() {
		AmazonSimpleEmailServiceClientBuilder builder = AmazonSimpleEmailServiceClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
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
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		return builder.build();
	}
	
	/**
	 * Create an instance of AWSSecretsManager client using a credential chain.
	 * 
	 * @return
	 */
	public static AWSSecretsManager createAmazonSecretManagerClient() {
		AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		return builder.build();
	}
	
	/**
	 * Create an instance of AWSSimpleSystemsManagement client using a credential chain.
	 * @return
	 */
	public static AWSSimpleSystemsManagement createAmazonSimpleSystemManagementClient() {
		AWSSimpleSystemsManagementClientBuilder builder = AWSSimpleSystemsManagementClientBuilder.standard();
		builder.withRegion(Regions.US_EAST_1);
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		return builder.build();
	}
}
