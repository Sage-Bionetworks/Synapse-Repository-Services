package org.sagebionetworks.gcp;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.storage.StorageOptions;

/**
 * A AWSCredentialsProvider that will attempt to load AWS credentials from the
 * Maven .m2/settings.xml file.
 *
 */
public class DefaultGCPCredentialsProvider implements CredentialsProvider {
	@Override
	public Credentials getCredentials() {
		return StorageOptions.getDefaultInstance().getCredentials();
	}
}
