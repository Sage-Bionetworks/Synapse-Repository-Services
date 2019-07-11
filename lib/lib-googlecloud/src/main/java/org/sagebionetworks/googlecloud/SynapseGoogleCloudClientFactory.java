package org.sagebionetworks.googlecloud;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.StorageOptions;

/**
 * Abstracts away Google Cloud API to expose just the methods needed by Synapse.
 */
public class SynapseGoogleCloudClientFactory {

	@Autowired
	StackConfiguration stackConfiguration;

	/**
	 * Create an instance of a {@link SynapseGoogleCloudStorageClient} using credentials.
	 */
	public SynapseGoogleCloudStorageClient createGoogleCloudStorageClient() throws IOException {
		return new SynapseGoogleCloudStorageClientImpl(
				StorageOptions.newBuilder()
						.setCredentials(ServiceAccountCredentials.fromStream(
								new ByteArrayInputStream(stackConfiguration.getDecodedGoogleCloudServiceAccountCredentials().getBytes(StandardCharsets.UTF_8))
						))
						.build()
						.getService()
		);
	}
}
