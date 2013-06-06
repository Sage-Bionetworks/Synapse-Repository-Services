package org.sagebionetworks.doi;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class EzidAsyncClientTest {

	@Test
	public void testCreateSuccess() throws Exception {

		DoiAsyncClient asyncClient = new EzidAsyncClient();
		EzidClient ezidClient = mock(EzidClient.class);
		ReflectionTestUtils.setField(asyncClient, "ezidClient", ezidClient);

		EzidDoi doi = mock(EzidDoi.class);
		EzidAsyncCallback callback = mock(EzidAsyncCallback.class);
		asyncClient.create(doi, callback);

		// Wait 100 ms before verifying the call
		// the execution is on a separate thread
		Thread.sleep(100L);
		verify(ezidClient, times(1)).create(doi);
		verify(callback, times(1)).onSuccess(doi);
	}

	@Test
	public void testCreateError() throws Exception {

		EzidClient ezidClient = mock(EzidClient.class);
		EzidDoi doiWithError = mock(EzidDoi.class);
		Exception e = new RuntimeException("Mocked exception");
		doThrow(e).when(ezidClient).create(doiWithError);
		DoiAsyncClient asyncClient = new EzidAsyncClient();
		ReflectionTestUtils.setField(asyncClient, "ezidClient", ezidClient);

		EzidAsyncCallback callback = mock(EzidAsyncCallback.class);
		asyncClient.create(doiWithError, callback);

		// Wait 100 ms before verifying the call
		// the execution is on a separate thread
		Thread.sleep(100L);
		verify(ezidClient, times(1)).create(doiWithError);
		verify(callback, times(1)).onError(doiWithError, e);
	}

	@Test
	public void testUpdateSuccess() throws Exception {

		DoiAsyncClient asyncClient = new EzidAsyncClient();
		EzidClient ezidClient = mock(EzidClient.class);
		ReflectionTestUtils.setField(asyncClient, "ezidClient", ezidClient);

		EzidDoi doi = mock(EzidDoi.class);
		EzidAsyncCallback callback = mock(EzidAsyncCallback.class);
		asyncClient.update(doi, callback);

		// Wait 100 ms before verifying the call
		// the execution is on a separate thread
		Thread.sleep(100L);
		verify(ezidClient, times(1)).update(doi);
		verify(callback, times(1)).onSuccess(doi);
	}

	@Test
	public void testUpdateError() throws Exception {

		EzidClient ezidClient = mock(EzidClient.class);
		EzidDoi doiWithError = mock(EzidDoi.class);
		Exception e = new RuntimeException("Mocked exception");
		doThrow(e).when(ezidClient).update(doiWithError);
		DoiAsyncClient asyncClient = new EzidAsyncClient();
		ReflectionTestUtils.setField(asyncClient, "ezidClient", ezidClient);

		EzidAsyncCallback callback = mock(EzidAsyncCallback.class);
		asyncClient.update(doiWithError, callback);

		// Wait 100 ms before verifying the call
		// the execution is on a separate thread
		Thread.sleep(100L);
		verify(ezidClient, times(1)).update(doiWithError);
		verify(callback, times(1)).onError(doiWithError, e);
	}
}
