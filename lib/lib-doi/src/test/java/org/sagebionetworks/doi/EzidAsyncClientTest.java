package org.sagebionetworks.doi;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class EzidAsyncClientTest {

	@Test
	public void testSuccess() throws Exception {

		EzidAsyncCallback callback = mock(EzidAsyncCallback.class);
		DoiClient asyncClient = new EzidAsyncClient(callback);

		EzidClient ezidClient = mock(EzidClient.class);
		ReflectionTestUtils.setField(asyncClient, "ezidClient", ezidClient);

		EzidMetadata metadata = mock(EzidMetadata.class);
		asyncClient.create(metadata);
		verify(ezidClient, times(1)).create(metadata);
		verify(callback, times(1)).onSuccess(metadata);
	}

	@Test
	public void testError() throws Exception {

		EzidAsyncCallback callback = mock(EzidAsyncCallback.class);
		DoiClient asyncClient = new EzidAsyncClient(callback);

		EzidClient ezidClient = mock(EzidClient.class);
		EzidMetadata metadataWithError = mock(EzidMetadata.class);
		Exception e = new RuntimeException();
		doThrow(e).when(ezidClient).create(metadataWithError);
		ReflectionTestUtils.setField(asyncClient, "ezidClient", ezidClient);

		asyncClient.create(metadataWithError);
		verify(ezidClient, times(1)).create(metadataWithError);
		verify(callback, times(1)).onError(metadataWithError, e);
	}
}
