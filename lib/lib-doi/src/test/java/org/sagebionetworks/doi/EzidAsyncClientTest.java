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

		EzidDoi doi = mock(EzidDoi.class);
		asyncClient.create(doi);
		// Wait 1s before verifying the call
		// the execution is on a separate thread
		Thread.sleep(1000L);
		verify(ezidClient, times(1)).create(doi);
		verify(callback, times(1)).onSuccess(doi);
	}

	@Test
	public void testError() throws Exception {

		EzidAsyncCallback callback = mock(EzidAsyncCallback.class);
		DoiClient asyncClient = new EzidAsyncClient(callback);

		EzidClient ezidClient = mock(EzidClient.class);
		EzidDoi doiWithError = mock(EzidDoi.class);
		Exception e = new RuntimeException("Mocked exception");
		doThrow(e).when(ezidClient).create(doiWithError);
		ReflectionTestUtils.setField(asyncClient, "ezidClient", ezidClient);

		asyncClient.create(doiWithError);
		// Wait 1s before verifying the call
		// the execution is on a separate thread
		Thread.sleep(1000L);
		verify(ezidClient, times(1)).create(doiWithError);
		verify(callback, times(1)).onError(doiWithError, e);
	}
}
