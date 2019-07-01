package org.sagebionetworks.gcp;

import java.util.Properties;

import com.amazonaws.util.StringUtils;
import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

public abstract class AbstractSynapseGoogleCloudCredentialsProvider implements CredentialsProvider {

	public static final String GOOGLE_CLOUD_CREDENTIALS_WERE_NOT_FOUND = "Google Cloud credentials were not found.";
	public static final String ORG_SAGEBIONETWORKS_GOOGLE_CLOUD_CLIENT_ID = "org.sagebionetworks.google.cloud.client.id";
	public static final String ORG_SAGEBIONETWORKS_GOOGLE_CLOUD_CLIENT_EMAIL = "org.sagebionetworks.google.cloud.client.email";
	public static final String ORG_SAGEBIONETWORKS_GOOGLE_CLOUD_CLIENT_PRIVATE_KEY = "org.sagebionetworks.google.cloud.key";
	public static final String ORG_SAGEBIONETWORKS_GOOGLE_CLOUD_CLIENT_PRIVATE_KEY_ID = "org.sagebionetworks.google.cloud.key.id";

	final Properties properties;

	AbstractSynapseGoogleCloudCredentialsProvider(Properties properties) {
		this.properties = properties;
	}

	/**
	 * Search the provided Properties for the credentials.
	 */
	final public Credentials getCredentials() {
		try {
			Properties properties = getProperties();
			if (properties != null) {
				String clientId = StringUtils.trim(properties.getProperty(ORG_SAGEBIONETWORKS_GOOGLE_CLOUD_CLIENT_ID));
				String clientEmail = StringUtils.trim(properties.getProperty(ORG_SAGEBIONETWORKS_GOOGLE_CLOUD_CLIENT_EMAIL));
				String privateKeyPkcs8 = StringUtils.trim(properties.getProperty(ORG_SAGEBIONETWORKS_GOOGLE_CLOUD_CLIENT_PRIVATE_KEY));
				String privateKeyId = StringUtils.trim(properties.getProperty(ORG_SAGEBIONETWORKS_GOOGLE_CLOUD_CLIENT_PRIVATE_KEY_ID));;
				if (clientId != null && clientEmail != null && privateKeyPkcs8 != null && privateKeyId != null) {
					return ServiceAccountCredentials.fromPkcs8(clientId, clientEmail, privateKeyPkcs8, privateKeyId, null);
				}
			}
			throw new IllegalStateException(GOOGLE_CLOUD_CREDENTIALS_WERE_NOT_FOUND);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Extending classes provide a Properties object that could contain sage credentials.
	 * @return
	 */
	 Properties getProperties() {
	 	return this.properties;
	 }


}
