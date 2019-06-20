package org.sagebionetworks.aws;

import java.util.Properties;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.util.StringUtils;

/**
 * Abstract AWS credential provider that will look for 'org.sagebionetworks.stack.iam.id'
 * and 'org.sagebionetworks.stack.iam.key' in the provided properties.
 *
 */
public abstract class AbstractSynapseAWSCredentialsProvider implements AWSCredentialsProvider {

	public static final String AWS_CREDENTIALS_WERE_NOT_FOUND = "AWS credentials were not found.";
	public static final String ORG_SAGEBIONETWORKS_STACK_IAM_ID = "org.sagebionetworks.stack.iam.id";
	public static final String ORG_SAGEBIONETWORKS_STACK_IAM_KEY = "org.sagebionetworks.stack.iam.key";

	/**
	 * Search the provided Properties for the credentials.
	 */
	final public AWSCredentials getCredentials() {
		try {
			Properties properties = getProperties();
			if (properties != null) {
				String accessKey = StringUtils.trim(properties.getProperty(ORG_SAGEBIONETWORKS_STACK_IAM_ID));
				String secretKey = StringUtils.trim(properties.getProperty(ORG_SAGEBIONETWORKS_STACK_IAM_KEY));
				if (accessKey != null && secretKey != null) {
					return new BasicAWSCredentials(accessKey, secretKey);
				}
			}
			throw new IllegalStateException(AWS_CREDENTIALS_WERE_NOT_FOUND);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Extending classes provide a Properties object that could contain sage credentials.
	 * @return
	 */
	abstract Properties getProperties();

}
