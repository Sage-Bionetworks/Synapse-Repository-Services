package org.sagebionetworks.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

@ExtendWith(MockitoExtension.class)
public class MarkdownClientTest {
	@Mock
	private SimpleHttpClient mockHttpClient;
	@Mock
	private SimpleHttpResponse mockResponse;
	@Mock
	private RequestSigner mockSigner;
	private MarkdownClient markdownClient;

	@BeforeEach
	public void before() {
		markdownClient = new MarkdownClient(
			"https://abc123.execute-api.us-east-1.amazonaws.com/prod/markdown",
			mockSigner,
			mockHttpClient
		);
	}

	@Test
	public void testRequestMarkdownConversionFailure() throws Exception {
		String request = "{\"markdown\":\"## a heading\"}";
		when(mockResponse.getStatusCode()).thenReturn(500);
		when(mockSigner.sign(any(URI.class), any(byte[].class))).thenReturn(new HashMap<>());
		when(mockHttpClient.post(any(SimpleHttpRequest.class), eq(request))).thenReturn(mockResponse);

		MarkdownClientException e = assertThrows(MarkdownClientException.class, ()->{
			// call under test
			markdownClient.requestMarkdownConversion(request);
		});
		assertEquals(500, e.getStatusCode());
		assertEquals("Fail to request markdown conversion for request: "+request, e.getMessage());
	}

	@Test
	public void testRequestMarkdownConversionIOException() throws Exception {
		String request = "{\"markdown\":\"## a heading\"}";
		IOException io = new IOException("some kind of connection problem");
		when(mockSigner.sign(any(URI.class), any(byte[].class))).thenReturn(new HashMap<>());
		when(mockHttpClient.post(any(SimpleHttpRequest.class), eq(request))).thenThrow(io);

		MarkdownClientException e = assertThrows(MarkdownClientException.class, ()->{
			// call under test
			markdownClient.requestMarkdownConversion(request);
		});
		assertEquals(-1, e.getStatusCode());
		assertEquals(io, e.getCause());
	}

	@Test
	public void testRequestMarkdownConversionSuccess() throws Exception {
		String request = "{\"markdown\":\"## a heading\"}";
		String response = "{\"html\":\"<h2 toc=\\\"true\\\">a heading</h2>\\n\"}";
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn(response);
		when(mockSigner.sign(any(URI.class), any(byte[].class))).thenReturn(new HashMap<>());
		when(mockHttpClient.post(any(SimpleHttpRequest.class), eq(request))).thenReturn(mockResponse);

		// call under test
		String result = markdownClient.requestMarkdownConversion(request);

		assertEquals(response, result);
	}

	@Test
	public void testRequestMarkdownConversionCallsSigner() throws Exception {
		String request = "{\"markdown\":\"## a heading\"}";
		String endpoint = "https://abc123.execute-api.us-east-1.amazonaws.com/prod/markdown";
		String response = "{\"html\":\"<h2 toc=\\\"true\\\">a heading</h2>\\n\"}";

		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn(response);

		Map<String, String> signedHeaders = new HashMap<>();
		signedHeaders.put("Authorization", "AWS4-HMAC-SHA256 Credential=...");
		signedHeaders.put("X-Amz-Date", "20240101T000000Z");
		when(mockSigner.sign(any(URI.class), any(byte[].class))).thenReturn(signedHeaders);

		ArgumentCaptor<SimpleHttpRequest> requestCaptor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		when(mockHttpClient.post(requestCaptor.capture(), eq(request))).thenReturn(mockResponse);

		// call under test
		markdownClient.requestMarkdownConversion(request);

		// Verify signer was called with correct URI and payload
		ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
		ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
		verify(mockSigner).sign(uriCaptor.capture(), payloadCaptor.capture());

		assertEquals(endpoint, uriCaptor.getValue().toString());
		assertEquals(request, new String(payloadCaptor.getValue(), StandardCharsets.UTF_8));

		// Verify signed headers were included in the request
		SimpleHttpRequest capturedRequest = requestCaptor.getValue();
		Map<String, String> headers = capturedRequest.getHeaders();
		assertEquals("AWS4-HMAC-SHA256 Credential=...", headers.get("Authorization"));
		assertEquals("20240101T000000Z", headers.get("X-Amz-Date"));
		assertEquals("application/json", headers.get("Content-Type"));
	}
}
