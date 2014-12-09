package org.sagebionetworks.search;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.utils.HttpClientHelperException;

import com.amazonaws.http.HttpRequest;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeast;

public class CloudSearchClientTest {
	
	CloudSearchHttpClientProvider mockHttpClientProvider;
	HttpClient mockHttpClient;
	CloudSearchClient cloudSearchClient;

	@Before
	public void before() {
		mockHttpClientProvider = Mockito.mock(CloudSearchHttpClientProvider.class);
		mockHttpClient = Mockito.mock(HttpClient.class);
		when(mockHttpClientProvider.getHttpClient()).thenReturn(mockHttpClient);
		cloudSearchClient = new CloudSearchClient(mockHttpClientProvider, "https://svc.endpoint.com", "https://doc.endpoint.com")
	;}
	
	@Ignore
	@Test
	public void testPLFM2968NoError() throws Exception {
		//when(mockHttpClient.execute(any(HttpRequestBase.class))).thenThrow(new RuntimeException());
		StatusLine status = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "SomeReason");
		HttpResponse resp = new BasicHttpResponse(status);
		when(mockHttpClient.execute(any(HttpRequestBase.class))).thenReturn(resp);
		cloudSearchClient.performSearch("aQuery");
		verify(mockHttpClient).execute(any(HttpRequestBase.class));
	}
	
	@Test
	public void testPLFM2968NoRecover() throws Exception {
		//when(mockHttpClient.execute(any(HttpRequestBase.class))).thenThrow(new RuntimeException());
		StatusLine status = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 507, "SomeReason");
		HttpResponse resp = new BasicHttpResponse(status);
		when(mockHttpClient.execute(any(HttpRequestBase.class))).thenReturn(resp);
		try {
			cloudSearchClient.performSearch("aQuery");
		} catch (HttpClientHelperException e) {
			assertEquals(507, e.getHttpStatus());
		} finally {
			// Should have retried 6 times (100, 200, 400, 800, 1600, 3200)
			verify(mockHttpClient, times(6)).execute(any(HttpRequestBase.class));
		}
	}

}
