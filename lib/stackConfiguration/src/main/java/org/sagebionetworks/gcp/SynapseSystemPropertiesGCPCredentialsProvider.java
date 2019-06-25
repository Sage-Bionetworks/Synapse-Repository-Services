package org.sagebionetworks.gcp;

import java.util.Properties;

import org.sagebionetworks.PropertyProvider;

/**
 * An AWS credential provider that will look for:
 * 'org.sagebionetworks.stack.iam.id' and 'org.sagebionetworks.stack.iam.key' on
 * the Java System.properties.
 *
 */
public class SynapseSystemPropertiesGCPCredentialsProvider extends AbstractSynapseGcpCredentialsProvider {

	Properties properties;

	public SynapseSystemPropertiesGCPCredentialsProvider(PropertyProvider propertyProvider) {
		this.properties = propertyProvider.getSystemProperties();
	}

	@Override
	Properties getProperties() {
		return properties;
	}

}
