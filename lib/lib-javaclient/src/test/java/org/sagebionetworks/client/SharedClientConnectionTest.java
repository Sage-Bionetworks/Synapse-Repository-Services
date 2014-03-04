package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;

public class SharedClientConnectionTest {
	
	private SharedClientConnection sharedClientConnection;
	private HttpClientProvider mockClientProvider;
	HttpResponse mockResponse;
	private String endpoint;
	
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
		endpoint="http://reposvcs.org";
		mockResponse = Mockito.mock(HttpResponse.class);
		when(mockClientProvider.performRequest(any(String.class),any(String.class),any(String.class),(Map<String,String>)anyObject())).thenReturn(mockResponse);
	}

	@Test
	public void testHappyPath() throws Exception {
		String uri = "/some/uri";
		String jsonString = "jsonString";
		String userAgent = "user/agent";
		String expectedResponse = "{\"foo\":\"bar\"}";
		configureMockHttpResponse(201, expectedResponse);
		JSONObject result = sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
		assertEquals(expectedResponse, result.toString());
	}

	@Test(expected=SynapseBadRequestException.class)
	public void testBadRequest() throws Exception {
		String uri = "/some/uri";
		String jsonString = "jsonString";
		String userAgent = "user/agent";
		configureMockHttpResponse(400, "{\"foo\":\"bar\"}");
		sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
	}

	@Test(expected=SynapseUnauthorizedException.class)
	public void testUnauthorized() throws Exception {
		String uri = "/some/uri";
		String jsonString = "jsonString";
		String userAgent = "user/agent";
		configureMockHttpResponse(403, "{\"foo\":\"bar\"}");
		sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
	}

	@Test(expected=SynapseNotFoundException.class)
	public void testNotFoundRequest() throws Exception {
		String uri = "/some/uri";
		String jsonString = "jsonString";
		String userAgent = "user/agent";
		configureMockHttpResponse(404, "{\"foo\":\"bar\"}");
		sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
	}

	@Test(expected=SynapseForbiddenException.class)
	public void testForbiddenRequest() throws Exception {
		String uri = "/some/uri";
		String jsonString = "jsonString";
		String userAgent = "user/agent";
		configureMockHttpResponse(403, "{\"foo\":\"bar\"}");
		sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
	}

	@Test(expected=SynapseServerException.class)
	public void testSynapseServerRequest() throws Exception {
		String uri = "/some/uri";
		String jsonString = "jsonString";
		String userAgent = "user/agent";
		configureMockHttpResponse(500, "{\"foo\":\"bar\"}");
		sharedClientConnection.postJson(endpoint, uri,jsonString, userAgent, null);
	}

}
