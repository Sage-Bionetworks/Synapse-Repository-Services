package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;

public class SharedClientConnectionTest {
	
	private SharedClientConnection sharedClientConnection;
	private HttpClientProvider mockClientProvider;
	private HttpResponse mockResponse;
	private ArgumentCaptor<Map> requestHeaderCaptor;

	private String 	endpoint="http://reposvcs.org";
	private static final String uri = "/some/uri";
	private static final String jsonString = "jsonString";
	private static final String userAgent = "user/agent";
	
	private static Header mockHeader(final String name, final String value) {
		Header header = Mockito.mock(Header.class);
		when(header.getName()).thenReturn(name);
		when(header.getValue()).thenReturn(value);
		HeaderElement he = Mockito.mock(HeaderElement.class);
		when(he.getName()).thenReturn(name);
		when(he.getValue()).thenReturn(value);
		when(header.getElements()).thenReturn(new HeaderElement[]{he});
		return header;
	}

	private void configureMockHttpResponse(final int statusCode, final String responseBody) {
		StatusLine statusLine = Mockito.mock(StatusLine.class);
		when(statusLine.getStatusCode()).thenReturn(statusCode);
		when(mockResponse.getStatusLine()).thenReturn(statusLine);
		
		HttpEntity entity = new HttpEntity() {
			@Override
			public boolean isRepeatable() {return false;}
			@Override
			public boolean isChunked() {return false;}
			@Override
			public long getContentLength() {return responseBody.getBytes().length;}
			@Override
			public Header getContentType() {return mockHeader("ContentType", "text/plain");}
			@Override
			public Header getContentEncoding() {return mockHeader("ContentEncoding", "application/json");}
			@Override
			public InputStream getContent() throws IOException, IllegalStateException 
					{return new ByteArrayInputStream(responseBody.getBytes());}
			@Override
			public void writeTo(OutputStream outstream) throws IOException {}
			@Override
			public boolean isStreaming() {return false;}
			@Override
			public void consumeContent() throws IOException {}
		};
		when(mockResponse.getEntity()).thenReturn(entity);
	}
	
	@Before
	public void before() throws Exception {
		mockClientProvider = Mockito.mock(HttpClientProvider.class);
		sharedClientConnection = new SharedClientConnection(mockClientProvider);
		mockResponse = Mockito.mock(HttpResponse.class);
		requestHeaderCaptor = ArgumentCaptor.forClass(Map.class);
		when(mockClientProvider.performRequest(any(String.class),any(String.class),any(String.class),requestHeaderCaptor.capture())).thenReturn(mockResponse);
		when(mockClientProvider.execute(any(HttpUriRequest.class))).thenReturn(mockResponse);
		sharedClientConnection.setRetryRequestIfServiceUnavailable(false);
	}

	@Test
	public void testHappyPath() throws Exception {
		String expectedResponse = "{\"foo\":\"bar\"}";
		configureMockHttpResponse(201, expectedResponse);
		String sessionToken = "a-session-token";
		sharedClientConnection.setSessionToken(sessionToken);
		JSONObject result = sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
		assertEquals(expectedResponse, result.toString());
		assertEquals(sessionToken, requestHeaderCaptor.getValue().get("sessionToken"));
	}

	@Test
	public void testHappyPathWithAPIKey() throws Exception {
		String expectedResponse = "{\"foo\":\"bar\"}";
		configureMockHttpResponse(201, expectedResponse);
		sharedClientConnection.setApiKey("some key");
		sharedClientConnection.setSessionToken(null);
		JSONObject result = sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
		assertEquals(expectedResponse, result.toString());
		assertNotNull(requestHeaderCaptor.getValue().get("signature"));
		assertNotNull(requestHeaderCaptor.getValue().get("signatureTimestamp"));
	}

	@Test
	public void testHappyPathWithRetry() throws Exception {
		sharedClientConnection.setRetryRequestIfServiceUnavailable(true);
		String expectedResponse = "{\"foo\":\"bar\"}";
		configureMockHttpResponse(201, expectedResponse);
		JSONObject result = sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
		assertEquals(expectedResponse, result.toString());
	}

	@Test
	public void testBadRequest() throws Exception {
		sharedClientConnection.setRetryRequestIfServiceUnavailable(true);
		configureMockHttpResponse(HttpStatus.SC_BAD_REQUEST, "{\"reason\":\"user message\"}");
		try {
			sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
			fail("expected exception");
		} catch (SynapseBadRequestException e) {
			//verify does not retry with BAD_REQUEST
			verify(mockClientProvider, times(1)).performRequest(anyString(), anyString(), anyString(), anyMap());
			assertEquals("user message", e.getMessage());
		}
	}
	

	@Test
	public void testServiceUnavailableRetry() throws Exception {
		sharedClientConnection.setRetryRequestIfServiceUnavailable(true);
		configureMockHttpResponse(HttpStatus.SC_SERVICE_UNAVAILABLE, "{\"reason\":\"throttled\"}");
		try {
			sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
			fail("expected exception");
		} catch (SynapseServerException e) {
			//verify retried with SERVICE_UNAVAILABLE
			verify(mockClientProvider, times(SharedClientConnection.MAX_RETRY_SERVICE_UNAVAILABLE_COUNT)).performRequest(anyString(), anyString(), anyString(), anyMap());
			assertTrue(e.getMessage().contains("throttled"));
		}
	}
	
	@Test
	public void testSocketTimeoutExceptionRetry() throws Exception {
		sharedClientConnection.setRetryRequestIfServiceUnavailable(true);
		String errorMessage = "a timeout occurred on a socket read (or accept)";
		when(mockClientProvider.performRequest(any(String.class),any(String.class),any(String.class),any(Map.class))).thenThrow(new SocketTimeoutException(errorMessage));
		try {
			sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
			fail("expected exception");
		} catch (SynapseServerException e) {
			//verify retried with SocketTimeoutException
			verify(mockClientProvider, times(SharedClientConnection.MAX_RETRY_SERVICE_UNAVAILABLE_COUNT)).performRequest(anyString(), anyString(), anyString(), anyMap());
			assertTrue(e.getMessage().contains(errorMessage));
		}
	}
	
	@Test
	public void testUnauthorized() throws Exception {
		configureMockHttpResponse(HttpStatus.SC_UNAUTHORIZED, "{\"reason\":\"user message\"}");
		try {
			sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
			fail("expected exception");
		} catch (SynapseUnauthorizedException e) {
			assertEquals("user message", e.getMessage());
		}
	}

	@Test
	public void testNotFoundRequest() throws Exception {
		configureMockHttpResponse(HttpStatus.SC_NOT_FOUND, "{\"reason\":\"user message\"}");
		try {
			sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
			fail("expected exception");
		} catch (SynapseNotFoundException e) {
			assertEquals("user message", e.getMessage());
		}
	}

	@Test
	public void testForbiddenRequest() throws Exception {
		configureMockHttpResponse(HttpStatus.SC_FORBIDDEN, "{\"reason\":\"user message\"}");
		try {
			sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
			fail("expected exception");
		} catch (SynapseForbiddenException e) {
			assertEquals("user message", e.getMessage());
		}
	}

	@Test
	public void testSynapseServerRequest() throws Exception {
		configureMockHttpResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "{\"reason\":\"user message\"}");
		try {
			sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
			fail("expected exception");
		} catch (SynapseServerException e) {
			assertEquals("user message", e.getMessage());
		}
	}

	@Test
	public void testDownloadZippedFileStringErrorHandling() throws Exception {
		configureMockHttpResponse(HttpStatus.SC_UNAUTHORIZED, "{\"reason\":\"user message\"}");
		try {
			sharedClientConnection.downloadZippedFileString(uri, uri, userAgent);
			fail("expected exception");
		} catch (SynapseUnauthorizedException e) {
			assertEquals("user message", e.getMessage());
		}
	}

	@Test
	public void testPostStringDirectErrorHandling() throws Exception {
		configureMockHttpResponse(HttpStatus.SC_UNAUTHORIZED, "{\"reason\":\"user message\"}");
		try {
			sharedClientConnection.postStringDirect("http://host", uri, jsonString, userAgent);
			fail("expected exception");
		} catch (SynapseUnauthorizedException e) {
			assertEquals("user message", e.getMessage());
		}
	}


	@Test
	public void testGetDirectErrorHandling() throws Exception {
		configureMockHttpResponse(HttpStatus.SC_UNAUTHORIZED, "{\"reason\":\"user message\"}");
		try {
			sharedClientConnection.getDirect("http://host", uri, userAgent);
			fail("expected exception");
		} catch (SynapseUnauthorizedException e) {
			assertEquals("user message", e.getMessage());
		}
	}
	
	@Test
	public void testGetDirectWithAPIKey() throws Exception {
		String expectedResponse = "some string";
		configureMockHttpResponse(200, expectedResponse);
		sharedClientConnection.setApiKey("some key");
		sharedClientConnection.setSessionToken(null);
		String result = sharedClientConnection.getDirect(endpoint, uri, userAgent);
		assertEquals(expectedResponse, result);
		assertNotNull(requestHeaderCaptor.getValue().get("signature"));
		assertNotNull(requestHeaderCaptor.getValue().get("signatureTimestamp"));
	}
	
	@Test
	public void testPostStringDirectWithAPIKey() throws Exception {
		String expectedResponse = "some string";
		configureMockHttpResponse(201, expectedResponse);
		sharedClientConnection.setApiKey("some key");
		sharedClientConnection.setSessionToken(null);
		String result = sharedClientConnection.postStringDirect(endpoint, uri, "data", userAgent);
		assertEquals(expectedResponse, result);
		assertNotNull(requestHeaderCaptor.getValue().get("signature"));
		assertNotNull(requestHeaderCaptor.getValue().get("signatureTimestamp"));
	}
	

}
