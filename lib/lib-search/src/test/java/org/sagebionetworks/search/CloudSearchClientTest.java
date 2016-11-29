package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;

public class CloudSearchClientTest {

	SimpleHttpClient mockHttpClient;
	CloudSearchClient cloudSearchClient;

	@Before
	public void before() {
		mockHttpClient = Mockito.mock(SimpleHttpClient.class);
		cloudSearchClient = new CloudSearchClient("https://svc.endpoint.com", "https://doc.endpoint.com");
		ReflectionTestUtils.setField(cloudSearchClient, "httpClient", mockHttpClient);
	}
	
	
	@Test
	public void testPLFM2968NoError() throws Exception {
		SimpleHttpResponse resp = new SimpleHttpResponse();
		resp.setStatusCode(200);
		resp.setContent("s");
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(resp);
		assertEquals(resp.getContent(), cloudSearchClient.performSearch("aQuery"));
		verify(mockHttpClient).get(any(SimpleHttpRequest.class));
	}
	
	@Test
	public void testPLFM2968NoRecover() throws Exception {
		SimpleHttpResponse resp = new SimpleHttpResponse();
		resp.setStatusCode(507);
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(resp);
		try {
			cloudSearchClient.performSearch("aQuery");
		} catch (CloudSearchClientException e) {
			assertEquals(507, e.getStatusCode());
		} finally {
			verify(mockHttpClient, times(6)).get(any(SimpleHttpRequest.class));
		}
	}

	@Test
	public void testPLFM3777() throws Exception {
		SimpleHttpResponse resp = new SimpleHttpResponse();
		resp.setStatusCode(504);
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(resp);
		try {
			cloudSearchClient.performSearch("aQuery");
		} catch (CloudSearchClientException e) {
			assertEquals(504, e.getStatusCode());
		} finally {
			verify(mockHttpClient).get(any(SimpleHttpRequest.class));
		}
	}

}
