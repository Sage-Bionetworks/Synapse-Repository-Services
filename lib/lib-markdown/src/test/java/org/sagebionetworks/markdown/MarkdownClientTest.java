package org.sagebionetworks.markdown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

public class MarkdownClientTest {
	@Mock
	SimpleHttpClient mockHttpClient;
	@Mock
	SimpleHttpResponse mockResponse;
	MarkdownClient markdownClient;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		markdownClient = new MarkdownClient();
		ReflectionTestUtils.setField(markdownClient, "simpleHttpClient", mockHttpClient);
	}

	@Test
	public void testRequestMarkdownConversionFailure() throws Exception {
		String request = "{\"markdown\":\"## a heading\"}";
		String response = "{\"error\":\"Service unavailable\"}";
		when(mockResponse.getStatusCode()).thenReturn(500);
		when(mockResponse.getContent()).thenReturn(response);
		when(mockHttpClient.post(any(SimpleHttpRequest.class), eq(request))).thenReturn(mockResponse);
		try {
			markdownClient.requestMarkdownConversion(request);
			fail("expecting exception");
		} catch (MarkdownClientException e) {
			verify(mockHttpClient).post(any(SimpleHttpRequest.class), eq(request));
			assertEquals(500, e.getStatusCode());
			assertEquals("Fail to request markdown conversion for request: "+request, e.getMessage());
		};
	}

	@Test
	public void testRequestMarkdownConversionSuccess() throws Exception {
		String request = "{\"markdown\":\"## a heading\"}";
		String response = "{\"html\":\"<h2 toc=\\\"true\\\">a heading</h2>\\n\"}";
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn(response);
		when(mockHttpClient.post(any(SimpleHttpRequest.class), eq(request))).thenReturn(mockResponse);
		assertEquals(response, markdownClient.requestMarkdownConversion(request));
	}

}
