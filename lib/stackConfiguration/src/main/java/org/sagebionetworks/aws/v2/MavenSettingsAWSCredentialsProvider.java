package org.sagebionetworks.aws.v2;

import java.util.Properties;

import org.sagebionetworks.PropertyProvider;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * A AWSCredentialsProvider that will attempt to load AWS credentials from the
 * Maven .m2/settings.xml file.
 *
 */
public class MavenSettingsAWSCredentialsProvider implements AwsCredentialsProvider {

	
	private Properties properties;
	
	/**
	 * The only constructor for dependency injection.
	 * @param propertyProvider
	 */
	public MavenSettingsAWSCredentialsProvider(PropertyProvider propertyProvider) {
		super();
		this.properties = propertyProvider.getMavenSettingsProperties();
	}



	@Override
	public AwsCredentials resolveCredentials() {
		return new AwsCredentials() {
			
			@Override
			public String secretAccessKey() {
				return properties.getProperty("org.sagebionetworks.stack.iam.id");
			}
			
			@Override
			public String accessKeyId() {
				return properties.getProperty("org.sagebionetworks.stack.iam.key");
			}
		};
	}

}
