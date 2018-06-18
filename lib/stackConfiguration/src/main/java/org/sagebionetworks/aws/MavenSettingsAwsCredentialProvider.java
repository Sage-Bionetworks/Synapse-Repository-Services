package org.sagebionetworks.aws;

import java.util.Properties;

import org.sagebionetworks.PropertyProvider;

/**
 * A AWSCredentialsProvider that will attempt to load AWS credentials from the
 * Maven .m2/settings.xml file.
 *
 */
public class MavenSettingsAwsCredentialProvider extends AbstractSynapseCredentialProvider {

	
	private PropertyProvider propertyProvider;
	private Properties properties;
	
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
	public void refresh() {
		// reload the maven settings properties.
		properties = propertyProvider.getMavenSettingsProperties();
	}

	@Override
	Properties getProperties() {
		return properties;
	}

}
