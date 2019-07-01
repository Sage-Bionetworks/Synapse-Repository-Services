package org.sagebionetworks.gcp;

import org.sagebionetworks.PropertyProvider;

/**
 * A Google Cloud CredentialsProvider that will look for 'org.sagebionetworks.gcp.key' in the Java System.properties.
 */
public class SynapseSystemPropertiesGoogleCloudCredentialsProvider extends AbstractSynapseGoogleCloudCredentialsProvider {
	public SynapseSystemPropertiesGoogleCloudCredentialsProvider(PropertyProvider propertyProvider) {
		super(propertyProvider.getSystemProperties());
	}
}
