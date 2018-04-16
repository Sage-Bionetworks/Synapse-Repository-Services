package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.client.ClientUtils.ERROR_REASON_TAG;
import static org.sagebionetworks.client.Method.DELETE;
import static org.sagebionetworks.client.Method.GET;
import static org.sagebionetworks.client.Method.POST;
import static org.sagebionetworks.client.Method.PUT;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.client.exceptions.SynapseDeprecatedServiceException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseLockedException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.client.exceptions.SynapseTooManyRequestsException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.simpleHttpClient.Header;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

public class ClientUtilsTest {
	@Mock
	SimpleHttpResponse mockResponse;
	@Mock
	Header mockHeader;
	@Mock
	SimpleHttpClient mockClient;

	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void test200StatusCode() {
		assertTrue(ClientUtils.is200sStatusCode(200));
	}

	@Test
	public void test100StatusCode() {
		assertFalse(ClientUtils.is200sStatusCode(100));
	}
	
	@Test
	public void test300StatusCode() {
		assertFalse(ClientUtils.is200sStatusCode(300));
	}

	@Test (expected = SynapseUnauthorizedException.class)
	public void testThrowExceptionWithJSONResponse() throws Exception{
		JSONObject response = new JSONObject();
		response.put(ERROR_REASON_TAG, "test");
		ClientUtils.throwException(401, response);
	}

	@Test (expected = SynapseUnauthorizedException.class)
	public void testThrowUnauthorizedException() throws Exception{
		ClientUtils.throwException(401, "test");
	}

	@Test (expected = SynapseForbiddenException.class)
	public void testThrowForbiddenException() throws Exception{
		ClientUtils.throwException(403, "test");
	}

	@Test (expected = SynapseNotFoundException.class)
	public void testThrowNotFoundException() throws Exception{
		ClientUtils.throwException(404, "test");
	}

	@Test (expected = SynapseBadRequestException.class)
	public void testThrowBadRequestException() throws Exception{
		ClientUtils.throwException(400, "test");
	}

	@Test (expected = SynapseLockedException.class)
	public void testThrowLockedException() throws Exception{
		ClientUtils.throwException(423, "test");
	}

	@Test (expected = SynapseConflictingUpdateException.class)
	public void testThrowConflictingUpdateException() throws Exception{
		ClientUtils.throwException(412, "test");
	}

	@Test (expected = SynapseDeprecatedServiceException.class)
	public void testThrowDeprecatedServiceException() throws Exception{
		ClientUtils.throwException(410, "test");
	}

	@Test (expected = SynapseTooManyRequestsException.class)
	public void testThrowTooManyRequestException() throws Exception{
		ClientUtils.throwException(429, "test");
	}

	@Test
	public void testThrowServerException() throws Exception{
		try {
			ClientUtils.throwException(500, "test");
			fail("expect exception");
		} catch (SynapseServerException e) {
			assertEquals("Status Code: 500 message: test", e.getMessage());
		}
	}

	@Test
	public void testCheckStatusCodeAndThrowExceptionFor200() throws Exception{
		when(mockResponse.getStatusCode()).thenReturn(200);
		ClientUtils.checkStatusCodeAndThrowException(mockResponse);
	}


	@Test
	public void testCheckStatusCodeAndThrowExceptionFor400() throws Exception{
		when(mockResponse.getStatusCode()).thenReturn(400);
		when(mockResponse.getContent()).thenReturn("some reason");
		try {
			ClientUtils.checkStatusCodeAndThrowException(mockResponse);
		} catch (SynapseBadRequestException e) {
			assertEquals("some reason", e.getMessage());
		}
	}

	@Test
	public void testCheckStatusCodeAndThrowExceptionForTomcat404() throws Exception{
		when(mockResponse.getStatusCode()).thenReturn(404);
		when(mockResponse.getContent()).thenReturn("some reason");
		try {
			ClientUtils.checkStatusCodeAndThrowException(mockResponse);
		} catch (SynapseNotFoundException e) {
			assertEquals("some reason", e.getMessage());
		}
	}

	@Test
	public void testConvertResponseBodyToJSONAndThrowExceptionFor200() throws Exception{
		when(mockResponse.getContent()).thenReturn("{\"reason\":\"some reason\"}");
		when(mockResponse.getStatusCode()).thenReturn(200);
		JSONObject actual = ClientUtils.convertResponseBodyToJSONAndThrowException(mockResponse);
		assertNotNull(actual);
		assertEquals("some reason", actual.get("reason"));
	}


	@Test
	public void testConvertResponseBodyToJSONAndThrowExceptionFor400() throws Exception{
		when(mockResponse.getContent()).thenReturn("{\"reason\":\"some reason\"}");
		when(mockResponse.getStatusCode()).thenReturn(400);
		try {
			ClientUtils.convertResponseBodyToJSONAndThrowException(mockResponse);
		} catch (SynapseBadRequestException e) {
			assertEquals("some reason", e.getMessage());
		}
	}

	@Test
	public void testConvertResponseBodyToJSONAndThrowExceptionForNonJSONResponse() throws Exception{
		when(mockResponse.getContent()).thenReturn("some reason");
		when(mockResponse.getStatusCode()).thenReturn(404);
		try {
			ClientUtils.convertResponseBodyToJSONAndThrowException(mockResponse);
		} catch (SynapseNotFoundException e) {
			assertEquals("some reason", e.getMessage());
		}
	}

	@Test (expected = SynapseClientException.class)
	public void testConvertResponseBodyToJSONAndThrowExceptionForNonJSONResponse200() throws Exception{
		when(mockResponse.getContent()).thenReturn("some reason");
		when(mockResponse.getStatusCode()).thenReturn(200);
		ClientUtils.convertResponseBodyToJSONAndThrowException(mockResponse);
	}

	@Test
	public void testConvertStringToJSONObjectWithNull() throws Exception {
		assertNull(ClientUtils.convertStringToJSONObject(null));
	}

	@Test (expected = JSONException.class)
	public void testConvertStringToJSONObjectWithWrongJSONFormat() throws Exception {
		ClientUtils.convertStringToJSONObject("abc");
	}

	@Test
	public void testConvertStringToJSONObject() throws Exception {
		JSONObject actual = ClientUtils.convertStringToJSONObject("{\"reason\":\"some reason\"}");
		assertNotNull(actual);
		assertEquals("some reason", actual.get("reason"));
	}

	@Test
	public void testGetCharacterSetFromResponse() {
		when(mockResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE))
				.thenReturn(mockHeader);
		when(mockHeader.getValue()).thenReturn("text/plain; charset=UTF-8");
		assertEquals(Charset.forName("UTF-8"),
				ClientUtils.getCharacterSetFromResponse(mockResponse));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateRequestUrlWithNullEndpoint() throws Exception{
		ClientUtils.createRequestUrl(null, "uri", null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateRequestUrlWithNullURI() throws Exception{
		ClientUtils.createRequestUrl("endpoint", null, null);
	}

	@Test
	public void testCreateRequestUrlWithNullParams() throws Exception{
		assertEquals("http://synapse.org/uri",
				ClientUtils.createRequestUrl("http://synapse.org", "/uri", null));
	}

	@Test
	public void testCreateRequestUrlWithParams() throws Exception{
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put("limit", "1");
		params.put("offset", "0");
		assertEquals("http://synapse.org/uri?limit=1&offset=0",
				ClientUtils.createRequestUrl("http://synapse.org", "/uri", params));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPerformRequestWithNullClient() throws Exception{
		ClientUtils.performRequest(null, "requestUrl", GET, null, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPerformRequestWithNullURL() throws Exception{
		ClientUtils.performRequest(mockClient, null, GET, null, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPerformRequestWithNullMethod() throws Exception{
		ClientUtils.performRequest(mockClient, "requestUrl", null, null, null);
	}

	@Test
	public void testPerformGETRequest() throws Exception{
		ClientUtils.performRequest(mockClient, "requestUrl", GET, null, null);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		verify(mockClient).get(request);
	}

	@Test
	public void testPerformGETRequestWithHeaders() throws Exception{
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "text/plain");
		ClientUtils.performRequest(mockClient, "requestUrl", GET, null, headers );
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		request.setHeaders(headers);
		verify(mockClient).get(request);
	}

	@Test
	public void testPerformPOSTRequest() throws Exception{
		ClientUtils.performRequest(mockClient, "requestUrl", POST, null, null);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		verify(mockClient).post(request, null);
	}

	@Test
	public void testPerformPOSTRequestWithRequestBody() throws Exception{
		ClientUtils.performRequest(mockClient, "requestUrl", POST, "body", null);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		verify(mockClient).post(request, "body");
	}

	@Test
	public void testPerformPUTRequest() throws Exception{
		ClientUtils.performRequest(mockClient, "requestUrl", PUT, null, null);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		verify(mockClient).put(request, null);
	}

	@Test
	public void testPerformPUTRequestWithRequestBody() throws Exception{
		ClientUtils.performRequest(mockClient, "requestUrl", PUT, "body", null);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		verify(mockClient).put(request, "body");
	}

	@Test
	public void testPerformDELETERequest() throws Exception{
		ClientUtils.performRequest(mockClient, "requestUrl", DELETE, null, null);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		verify(mockClient).delete(request);
	}
}
