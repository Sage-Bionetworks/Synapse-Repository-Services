package org.sagebionetworks.gcp;


import com.google.cloud.storage.StorageOptions;

/**
 * Abstracts away Google Cloud API to expose just the methods needed by Synapse.
 */
public class SynapseGoogleCloudClientFactory {
	/**
	 * Create all region-specific instances of the AmazonS3 client using a credential chain.
	 *
	 * @return
	 */
	public static SynapseGoogleCloudStorageClient createGoogleCloudStorageClient() {
		return new SynapseGoogleCloudStorageClientImpl(
				StorageOptions.newBuilder()
						.setCredentials(SynapseGCPCredentialsProviderChain.getInstance().getCredentials())
						.build()
						.getService()
		);
	}
}
