package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;

public class CloudSearchClientTest {

	@Mock
	SimpleHttpClient mockHttpClient;
	@Mock
	SimpleHttpResponse mockResponse;
	CloudSearchClient cloudSearchClient;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		cloudSearchClient = new CloudSearchClient("https://svc.endpoint.com", "https://doc.endpoint.com");
		ReflectionTestUtils.setField(cloudSearchClient, "httpClient", mockHttpClient);
	}
	
	
	@Test
	public void testPLFM2968NoError() throws Exception {
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("s");
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		assertEquals(mockResponse.getContent(), cloudSearchClient.performSearch("aQuery"));
		verify(mockHttpClient).get(any(SimpleHttpRequest.class));
	}
	
	@Test
	public void testPLFM2968NoRecover() throws Exception {
		when(mockResponse.getStatusCode()).thenReturn(507);
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
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
		when(mockResponse.getStatusCode()).thenReturn(504);
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		try {
			cloudSearchClient.performSearch("aQuery");
		} catch (CloudSearchClientException e) {
			assertEquals(504, e.getStatusCode());
		} finally {
			verify(mockHttpClient).get(any(SimpleHttpRequest.class));
		}
	}

}
