package org.sagebionetworks.gcp;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.amazonaws.util.StringUtils;
import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

public abstract class AbstractSynapseGcpCredentialsProvider implements CredentialsProvider {

	public static final String GCP_CREDENTIALS_WERE_NOT_FOUND = "Google Cloud credentials were not found.";
	public static final String ORG_SAGEBIONETWORKS_STACK_GCP_SVC_ACCOUNT_KEY = "org.sagebionetworks.stack.gcp.key";

	/**
	 * Search the provided Properties for the credentials.
	 */
	final public Credentials getCredentials() {
		try {
			Properties properties = getProperties();
			if (properties != null) {
				String accessKey = StringUtils.trim(properties.getProperty(ORG_SAGEBIONETWORKS_STACK_GCP_SVC_ACCOUNT_KEY));
				if (accessKey != null) {
					return ServiceAccountCredentials.fromStream(new ByteArrayInputStream(accessKey.getBytes(StandardCharsets.UTF_8)));
				}
			}
			throw new IllegalStateException(GCP_CREDENTIALS_WERE_NOT_FOUND);
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
