package org.sagebionetworks.doi;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.sagebionetworks.repo.model.doi.Doi;
import org.springframework.test.util.ReflectionTestUtils;

public class DxAsyncClientTest {

	@Test
	public void testRedirect() throws Exception {

		final EzidDoi ezidDoi = new EzidDoi();
		final Doi dto = new Doi();
		ezidDoi.setDto(dto);
		final String doi = "doi:10.7303/syn111";
		ezidDoi.setDoi(doi);
		final EzidMetadata metadata = new EzidMetadata();
		ezidDoi.setMetadata(metadata);

		// Mock 303 -- 303 See Other is the expected redirect response
		HttpResponse mockResponse = mock(HttpResponse.class);
		when(mockResponse.getEntity()).thenReturn(new StringEntity("response body"));
		StatusLine mockStatusLine = mock(StatusLine.class);
		when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_SEE_OTHER);
		when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
		Header mockHeader = mock(Header.class);
		when(mockHeader.getValue()).thenReturn("syn111");
		when(mockResponse.getFirstHeader("Location")).thenReturn(mockHeader);
		RetryableHttpClient mockClient = mock(RetryableHttpClient.class);
		when(mockClient.executeWithRetry(any(HttpGet.class))).thenReturn(mockResponse);

		DxAsyncClient dxClient = new DxAsyncClient();
		ReflectionTestUtils.setField(dxClient, "httpClient", mockClient);
		ReflectionTestUtils.setField(dxClient, "delay", 1000L);
		ReflectionTestUtils.setField(dxClient, "decay", 300L);

		DxAsyncCallback callback = mock(DxAsyncCallback.class);
		dxClient.resolve(ezidDoi, callback);
		verify(callback).onSuccess(ezidDoi);
	}
	
	@Test
	public void testNotFound() throws Exception {

		final EzidDoi ezidDoi = new EzidDoi();
		final Doi dto = new Doi();
		ezidDoi.setDto(dto);
		final String doi = "doi:10.7303/syn111";
		ezidDoi.setDoi(doi);
		final EzidMetadata metadata = new EzidMetadata();
		ezidDoi.setMetadata(metadata);

		// Mock 404
		HttpResponse mockResponse = mock(HttpResponse.class);
		when(mockResponse.getEntity()).thenReturn(new StringEntity("response body"));
		StatusLine mockStatusLine = mock(StatusLine.class);
		when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
		when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
		RetryableHttpClient mockClient = mock(RetryableHttpClient.class);
		when(mockClient.executeWithRetry(any(HttpGet.class))).thenReturn(mockResponse);

		DxAsyncClient dxClient = new DxAsyncClient();
		ReflectionTestUtils.setField(dxClient, "httpClient", mockClient);
		ReflectionTestUtils.setField(dxClient, "delay", 1000L);
		ReflectionTestUtils.setField(dxClient, "decay", 300L);

		DxAsyncCallback callback = mock(DxAsyncCallback.class);
		dxClient.resolve(ezidDoi, callback);
		verify(callback).onError(same(ezidDoi), any(RuntimeException.class));
		verify(mockClient, times(4)).executeWithRetry(any(HttpGet.class));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullEzidDoi() {
		DxAsyncClient dxClient = new DxAsyncClient();
		dxClient.resolve(null, new DxAsyncCallback() {
			@Override
			public void onSuccess(EzidDoi ezidDoi) {}
			@Override
			public void onError(EzidDoi ezidDoi, Exception e) {}
		});
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullCallback() {
		final EzidDoi ezidDoi = new EzidDoi();
		final Doi dto = new Doi();
		ezidDoi.setDto(dto);
		final String doi = "doi:10.7303/syn1720822.1";
		ezidDoi.setDoi(doi);
		final EzidMetadata metadata = new EzidMetadata();
		ezidDoi.setMetadata(metadata);
		DxAsyncClient dxClient = new DxAsyncClient();
		dxClient.resolve(ezidDoi, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInvalidDoiString() {
		final EzidDoi ezidDoi = new EzidDoi();
		final Doi dto = new Doi();
		ezidDoi.setDto(dto);
		final String doi = "10.7303/syn1720822.1";
		ezidDoi.setDoi(doi);
		final EzidMetadata metadata = new EzidMetadata();
		ezidDoi.setMetadata(metadata);
		DxAsyncClient dxClient = new DxAsyncClient();
		dxClient.resolve(ezidDoi, new DxAsyncCallback() {
			@Override
			public void onSuccess(EzidDoi ezidDoi) {}
			@Override
			public void onError(EzidDoi ezidDoi, Exception e) {}
		});
	}
}
