package org.sagebionetworks.doi;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.junit.Test;

public class RetryableHttpClientTest {

	@Test
	public void test() throws Exception {

		final URI uri = URI.create("https://test.com/test/url");

		// Mock 500
		StatusLine status500 = mock(StatusLine.class);
		when(status500.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
		HttpResponse response500 = mock(HttpResponse.class);
		when(response500.getStatusLine()).thenReturn(status500);
		when(response500.getEntity()).thenReturn(new StringEntity("500"));
		HttpPost httpPost500 = new HttpPost(uri);
		httpPost500.setEntity(new StringEntity("text boday", HTTP.PLAIN_TEXT_TYPE, "UTF-8"));

		// Mock 503
		StatusLine status503 = mock(StatusLine.class);
		when(status503.getStatusCode()).thenReturn(HttpStatus.SC_SERVICE_UNAVAILABLE);
		HttpResponse response503 = mock(HttpResponse.class);
		when(response503.getStatusLine()).thenReturn(status503);
		when(response503.getEntity()).thenReturn(new StringEntity("503"));
		HttpPut httpPut503 = new HttpPut(uri);
		httpPut503.setEntity(new StringEntity("text boday", HTTP.PLAIN_TEXT_TYPE, "UTF-8"));

		// Mock 404
		StatusLine status404 = mock(StatusLine.class);
		when(status404.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
		HttpResponse response404 = mock(HttpResponse.class);
		when(response404.getStatusLine()).thenReturn(status404);
		when(response404.getEntity()).thenReturn(new StringEntity("404"));
		HttpGet httpGet404 = new HttpGet(uri);

		// Mock the client
		HttpClient mockClient = mock(HttpClient.class);
		when(mockClient.execute(httpPost500)).thenReturn(response500);
		when(mockClient.execute(httpPut503)).thenReturn(response503);
		when(mockClient.execute(httpGet404)).thenReturn(response404);

		RetryableHttpClient client = new RetryableHttpClient(mockClient);
		long start = System.currentTimeMillis();
		client.executeWithRetry(httpPost500);
		long stop = System.currentTimeMillis();
		assertTrue((stop - start) > (100 + 200 + 400));
		verify(mockClient, times(4)).execute(httpPost500);

		client = new RetryableHttpClient(mockClient, 200, 2);
		start = System.currentTimeMillis();
		client.executeWithRetry(httpPut503);
		stop = System.currentTimeMillis();
		assertTrue((stop - start) > (200 + 400));
		verify(mockClient, times(3)).execute(httpPut503);

		client = new RetryableHttpClient(mockClient);
		client.executeWithRetry(httpGet404);
		verify(mockClient, times(1)).execute(httpGet404);
	}
}
