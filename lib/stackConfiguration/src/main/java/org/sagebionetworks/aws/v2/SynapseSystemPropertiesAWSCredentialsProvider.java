package org.sagebionetworks.aws.v2;

import java.util.Properties;

import org.sagebionetworks.PropertyProvider;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * An AWS credential provider that will look for:
 * 'org.sagebionetworks.stack.iam.id' and 'org.sagebionetworks.stack.iam.key' on
 * the Java System.properties.
 *
 */
public class SynapseSystemPropertiesAWSCredentialsProvider implements AwsCredentialsProvider {

	Properties properties;

	public SynapseSystemPropertiesAWSCredentialsProvider(PropertyProvider propertyProvider) {
		this.properties = propertyProvider.getSystemProperties();
	}

	@Override
	public AwsCredentials resolveCredentials() {

		return new AwsCredentials() {

			@Override
			public String secretAccessKey() {
				return properties.getProperty("org.sagebionetworks.stack.iam.key");
			}

			@Override
			public String accessKeyId() {
				return properties.getProperty("org.sagebionetworks.stack.iam.id");
			}
		};

	}

}
