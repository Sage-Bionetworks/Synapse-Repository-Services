package org.sagebionetworks.gcp;

import org.sagebionetworks.PropertyProvider;

/**
 * A CredentialsProvider that will attempt to load Google Cloud credentials from the Maven .m2/settings.xml file.
 */
public class MavenSettingsGoogleCloudCredentialsProvider extends AbstractSynapseGoogleCloudCredentialsProvider {
	public MavenSettingsGoogleCloudCredentialsProvider(PropertyProvider propertyProvider) {
		super(propertyProvider.getMavenSettingsProperties());
	}
}
