package org.sagebionetworks.aws;

import java.util.Properties;

import org.sagebionetworks.PropertyProvider;

/**
 * An AWS credential provider that will look for:
 * 'org.sagebionetworks.stack.iam.id' and 'org.sagebionetworks.stack.iam.key' on
 * the Java System.properties.
 *
 */
public class SynapseSystemPropertiesAWSCredentialsProvider extends AbstractSynapseAWSCredentialsProvider {

	Properties properties;

	public SynapseSystemPropertiesAWSCredentialsProvider(PropertyProvider propertyProvider) {
		this.properties = propertyProvider.getSystemProperties();
	}

	@Override
	public void refresh() {
		// nothing to do here.
	}

	@Override
	Properties getProperties() {
		return properties;
	}

}
