package org.sagebionetworks.aws.v2;

import java.util.Properties;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * A credential provider that attempts to find Synapse specific aws id & key in
 * the provided properties. If the properties are null, then null credentials
 */
public class SynapsePropertyCredentialProvider implements AwsCredentialsProvider {

	private final Properties props;
	private final String name;

	public SynapsePropertyCredentialProvider(String name, Properties props) {
		super();
		this.props = props;
		this.name = name;
	}

	@Override
	public AwsCredentials resolveCredentials() {
		if (props == null) {
			throw new RuntimeException("No properties for name: "+name);
		}
		return new AwsCredentials() {

			@Override
			public String secretAccessKey() {
				return props.getProperty("org.sagebionetworks.stack.iam.key");
			}

			@Override
			public String accessKeyId() {
				return props.getProperty("org.sagebionetworks.stack.iam.id");
			}
		};
	}
}