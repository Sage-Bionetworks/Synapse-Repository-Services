package org.sagebionetworks.aws.v2;

import java.util.Properties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.utils.StringUtils;

/**
 * A credential provider that attempts to find Synapse specific aws id & key in
 * the provided properties. If the properties are null, then null credentials
 */
public class SynapsePropertyCredentialProvider implements AwsCredentialsProvider {

	private final String name;
	private final AwsCredentials credentials;

	public SynapsePropertyCredentialProvider(String name, Properties props) {
		super();
		String key = props != null ? StringUtils.trim(props.getProperty("org.sagebionetworks.stack.iam.key")) : null;
		String id = props != null ? StringUtils.trim(props.getProperty("org.sagebionetworks.stack.iam.id")) : null;
		credentials = key != null && id != null ? AwsBasicCredentials.create(id, key) : null;
		this.name = name;
	}

	@Override
	public AwsCredentials resolveCredentials() {
		if (credentials != null) {
			return credentials;
		}
		throw new IllegalStateException("No properties for name: " + name);
	}
}