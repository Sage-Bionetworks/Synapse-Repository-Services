package org.sagebionetworks.upload.multipart;

import org.mockito.Mockito;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring FactoryBean for creating a Mockito mock of SynapseGoogleCloudStorageClient.
 * This is needed because Mockito 5's generic mock() method doesn't provide enough
 * type information for Spring's bean type resolution.
 */
public class GoogleCloudStorageClientMockFactory implements FactoryBean<SynapseGoogleCloudStorageClient> {

	@Override
	public SynapseGoogleCloudStorageClient getObject() throws Exception {
		return Mockito.mock(SynapseGoogleCloudStorageClient.class);
	}

	@Override
	public Class<?> getObjectType() {
		return SynapseGoogleCloudStorageClient.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
