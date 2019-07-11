package org.sagebionetworks.googlecloud;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.StorageOptions;

/**
 * Abstracts away Google Cloud API to expose just the methods needed by Synapse.
 */
public class SynapseGoogleCloudClientFactory {

	private static StackConfiguration stackConfiguration = StackConfigurationSingleton.singleton();

	/**
	 * Create an instance of a {@link SynapseGoogleCloudStorageClient} using credentials.
	 */
	public static SynapseGoogleCloudStorageClient createGoogleCloudStorageClient() {
		try {
			return new SynapseGoogleCloudStorageClientImpl(
					StorageOptions.newBuilder()
							.setCredentials(ServiceAccountCredentials.fromStream(
									new ByteArrayInputStream(stackConfiguration.getDecodedGoogleCloudServiceAccountCredentials().getBytes(StandardCharsets.UTF_8))
							))
							.build()
							.getService()
			);
		} catch (IOException e) {
			throw new RuntimeException("Unable to load credentials for the Google Cloud storage client", e);
		}
	}
}
