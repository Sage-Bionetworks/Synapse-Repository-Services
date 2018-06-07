package org.sagebionetworks.aws;

import java.util.Properties;

import org.sagebionetworks.PropertyProvider;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.util.StringUtils;

/**
 * A AWSCredentialsProvider that will attempt to load AWS credentials from the
 * Maven .m2/settings.xml file.
 *
 */
public class MavenSettingsAwsCredentialProvider implements AWSCredentialsProvider {


	public static final String AWS_CREDENTIALS_WERE_NOT_FOUND = "AWS credentials were not found in Maven .m2/settings.xml file";
	public static final String ORG_SAGEBIONETWORKS_STACK_IAM_ID = "org.sagebionetworks.stack.iam.id";
	public static final String ORG_SAGEBIONETWORKS_STACK_IAM_KEY = "org.sagebionetworks.stack.iam.key";
	
	private PropertyProvider propertyProvider;
	private Properties settingsProperties;
	
	/**
	 * The only constructor for dependency injection.
	 * @param propertyProvider
	 */
	public MavenSettingsAwsCredentialProvider(PropertyProvider propertyProvider) {
		super();
		this.propertyProvider = propertyProvider;
		this.refresh();
	}

	@Override
	public AWSCredentials getCredentials() {
		try {
			if (settingsProperties != null) {
				String accessKey = StringUtils.trim(settingsProperties.getProperty(ORG_SAGEBIONETWORKS_STACK_IAM_ID));
				String secretKey = StringUtils
						.trim(settingsProperties.getProperty(ORG_SAGEBIONETWORKS_STACK_IAM_KEY));
				if (accessKey != null && secretKey != null) {
					return new BasicAWSCredentials(accessKey, secretKey);
				}
			}
			throw new IllegalStateException(AWS_CREDENTIALS_WERE_NOT_FOUND);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void refresh() {
		settingsProperties = propertyProvider.getMavenSettingsProperties();
	}

}
