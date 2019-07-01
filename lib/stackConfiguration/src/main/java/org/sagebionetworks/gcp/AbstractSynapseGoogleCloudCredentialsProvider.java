package org.sagebionetworks.gcp;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

import com.amazonaws.util.StringUtils;
import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

public abstract class AbstractSynapseGoogleCloudCredentialsProvider implements CredentialsProvider {

	public static final String GOOGLE_CLOUD_CREDENTIALS_WERE_NOT_FOUND = "Google Cloud credentials were not found.";
	public static final String ORG_SAGEBIONETWORKS_GOOGLE_CLOUD_CLIENT_KEY = "org.sagebionetworks.gcp.key";

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
				String accessKey = new String(Base64.getDecoder().decode(
						StringUtils.trim(properties.getProperty(ORG_SAGEBIONETWORKS_GOOGLE_CLOUD_CLIENT_KEY))
								.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
				return ServiceAccountCredentials.fromStream(new ByteArrayInputStream(accessKey.getBytes(StandardCharsets.UTF_8)));
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
