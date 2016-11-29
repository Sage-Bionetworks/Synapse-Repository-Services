package org.sagebionetworks.doi;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
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
		SimpleHttpResponse mockResponse = mock(SimpleHttpResponse.class);
		when(mockResponse.getContent()).thenReturn("{"
					+ "\"responseCode\": 1,"
					+ "\"handle\": \"10.1000/1\","
					+ "\"values\": ["
						+ "{"
							+ "\"index\": 1,"
							+ "\"type\": \"URL\","
							+ "\"data\": { \"format\": \"string\", \"value\": \"http://www.doi.org/index.html\" },"
							+ "\"ttl\": 86400,"
							+ "\"timestamp\": \"2004-09-10T19:49:59Z\""
						+ "}"
					+ "]"
				+ "}");
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
		SimpleHttpClient mockClient = mock(SimpleHttpClient.class);
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

		DxAsyncClient dxClient = new DxAsyncClient();
		ReflectionTestUtils.setField(dxClient, "simpleHttpClient", mockClient);
		ReflectionTestUtils.setField(dxClient, "delay", 100L);
		ReflectionTestUtils.setField(dxClient, "decay", 30L);

		DxAsyncCallback callback = mock(DxAsyncCallback.class);
		dxClient.resolve(ezidDoi, callback);
		Thread.sleep(1000L);
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
		SimpleHttpResponse mockResponse = mock(SimpleHttpResponse.class);
		when(mockResponse.getContent()).thenReturn("response body");
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
		SimpleHttpClient mockClient = mock(SimpleHttpClient.class);
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

		DxAsyncClient dxClient = new DxAsyncClient();
		ReflectionTestUtils.setField(dxClient, "simpleHttpClient", mockClient);
		ReflectionTestUtils.setField(dxClient, "delay", 100L);
		ReflectionTestUtils.setField(dxClient, "decay", 30L);

		DxAsyncCallback callback = mock(DxAsyncCallback.class);
		dxClient.resolve(ezidDoi, callback);
		Thread.sleep(1000L);
		verify(callback).onError(same(ezidDoi), any(RuntimeException.class));
		verify(mockClient, times(4)).get(any(SimpleHttpRequest.class));
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

	@Test
	public void testGetLocationWithNull() {
		assertNull(DxAsyncClient.getLocation(null));
	}

	@Test
	public void testGetLocationWithEmptyJsonObject() {
		assertNull(DxAsyncClient.getLocation("{}"));
	}

	@Test
	public void testGetLocationWithoutValues() {
		assertNull(DxAsyncClient.getLocation("{"
					+ "\"responseCode\": 1,"
					+ "\"handle\": \"10.1000/1\""
				+ "}"));
	}

	@Test
	public void testGetLocationWithEmptyValues() {
		assertNull(DxAsyncClient.getLocation("{"
					+ "\"responseCode\": 1,"
					+ "\"handle\": \"10.1000/1\","
					+ "\"values\": ["
					+ "]"
				+ "}"));
	}

	@Test
	public void testGetLocationWithoutData() {
		assertNull(DxAsyncClient.getLocation("{"
					+ "\"responseCode\": 1,"
					+ "\"handle\": \"10.1000/1\","
					+ "\"values\": ["
						+ "{"
							+ "\"index\": 1,"
							+ "\"type\": \"URL\","
							+ "\"ttl\": 86400,"
							+ "\"timestamp\": \"2004-09-10T19:49:59Z\""
						+ "}"
					+ "]"
				+ "}"));
	}

	@Test
	public void testGetLocationWithoutURL() {
		assertNull(DxAsyncClient.getLocation("{"
					+ "\"responseCode\": 1,"
					+ "\"handle\": \"10.1000/1\","
					+ "\"values\": ["
						+ "{"
							+ "\"index\": 1,"
							+ "\"type\": \"URL\","
							+ "\"data\": { \"format\": \"string\"},"
							+ "\"ttl\": 86400,"
							+ "\"timestamp\": \"2004-09-10T19:49:59Z\""
						+ "}"
					+ "]"
				+ "}"));
	}

	@Test
	public void testGetLocationSuccess() {
		assertEquals("http://www.doi.org/index.html",
				DxAsyncClient.getLocation("{"
					+ "\"responseCode\": 1,"
					+ "\"handle\": \"10.1000/1\","
					+ "\"values\": ["
						+ "{"
							+ "\"index\": 1,"
							+ "\"type\": \"URL\","
							+ "\"data\": { \"format\": \"string\", \"value\": \"http://www.doi.org/index.html\" },"
							+ "\"ttl\": 86400,"
							+ "\"timestamp\": \"2004-09-10T19:49:59Z\""
						+ "}"
					+ "]"
				+ "}"));
	}
}
