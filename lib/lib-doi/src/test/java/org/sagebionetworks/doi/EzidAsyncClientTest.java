package org.sagebionetworks.doi;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;

import org.junit.Test;

public class EzidAsyncClientTest {

	@Test
	public void testSuccess() throws Exception {

		EzidAsyncCallback callback = mock(EzidAsyncCallback.class);
		DoiClient asyncClient = new EzidAsyncClient(callback);

		Field field = EzidAsyncClient.class.getDeclaredField("ezidClient");
		assertNotNull(field);
		field.setAccessible(true);
		EzidClient ezidClient = mock(EzidClient.class);
		field.set(asyncClient, ezidClient);

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

		Field field = EzidAsyncClient.class.getDeclaredField("ezidClient");
		assertNotNull(field);
		field.setAccessible(true);
		field.set(asyncClient, ezidClient);

		asyncClient.create(metadataWithError);
		verify(ezidClient, times(1)).create(metadataWithError);
		verify(callback, times(1)).onError(metadataWithError, e);
	}
}
