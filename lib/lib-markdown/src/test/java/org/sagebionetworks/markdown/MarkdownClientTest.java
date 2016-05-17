package org.sagebionetworks.markdown;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.test.util.ReflectionTestUtils;

public class MarkdownClientTest {
	@Mock
	HttpClient mockHttpClient;
	@Mock
	HttpResponse mockResponse;
	@Mock
	StatusLine mockStatusLine;
	@Mock
	HttpEntity mockEntity;
	MarkdownClient markdownClient;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		markdownClient = new MarkdownClient();
		ReflectionTestUtils.setField(markdownClient, "httpClient", mockHttpClient);
		when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
	}

	@Test
	public void testRequestMarkdownConversionFailure() throws Exception {
		String request = "{\"markdown\":\"## a heading\"}";
		String response = "{\"error\":\"Service unavailable\"}";
		when(mockStatusLine.getStatusCode()).thenReturn(500);
		when(mockResponse.getEntity()).thenReturn(mockEntity);
		when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(response.getBytes()));
		when(mockHttpClient.execute(any(HttpUriRequest.class))).thenReturn(mockResponse);
		try {
			markdownClient.requestMarkdownConversion(request);
		} catch (HttpClientHelperException e) {
			assertEquals(500, e.getHttpStatus());
			assertEquals(response, e.getResponse());
			assertEquals("Fail to request markdown conversion for request: "+request, e.getMessage());
		};
	}

	@Test
	public void testRequestMarkdownConversionSuccess() throws Exception {
		String request = "{\"markdown\":\"## a heading\"}";
		String response = "{\"html\":\"<h2 toc=\\\"true\\\">a heading</h2>\\n\"}";
		when(mockStatusLine.getStatusCode()).thenReturn(200);
		when(mockResponse.getEntity()).thenReturn(mockEntity);
		when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(response.getBytes()));
		when(mockHttpClient.execute(any(HttpUriRequest.class))).thenReturn(mockResponse);
		assertEquals(response, markdownClient.requestMarkdownConversion(request));
	}

}
