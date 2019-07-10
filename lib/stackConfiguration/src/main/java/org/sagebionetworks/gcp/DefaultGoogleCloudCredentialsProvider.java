package org.sagebionetworks.gcp;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.StorageOptions;

/**
 * A Google Cloud Platform credentials provider that will attempt to load Google Cloud credentials from the
 * locations described by {@link  GoogleCredentials#getApplicationDefault()}
 *
 */
public class DefaultGoogleCloudCredentialsProvider implements CredentialsProvider {
	@Override
	public Credentials getCredentials() {
		return StorageOptions.getDefaultInstance().getCredentials();
	}
}
