package org.sagebionetworks.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.client.ClientUtils.CONCRETE_TYPE;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.client.exceptions.SynapseDeprecatedServiceException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseLockedException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.client.exceptions.SynapseTooManyRequestsException;
import org.sagebionetworks.client.exceptions.SynapseTwoFactorAuthRequiredException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.ErrorResponseCode;
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

	@BeforeEach
	public void before() {
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

	@Test
	public void testconvertResponseBodyToJSONAndThrowExceptionWithJSONResponse() throws Exception {
		JSONObject response = new JSONObject();
		response.put(ERROR_REASON_TAG, "test");
		response.put(CONCRETE_TYPE, "org.sagebionetworks.repo.model.ErrorResponse");

		when(mockResponse.getStatusCode()).thenReturn(401);
		when(mockResponse.getContent()).thenReturn(response.toString());

		try {
			ClientUtils.convertResponseBodyToJSONAndThrowException(mockResponse);
			fail();
		} catch (SynapseUnauthorizedException e) {
			assertEquals("test", e.getMessage());
			assertEquals(null, e.getErrorResponseCode());
		}
	}

	@Test
	public void testconvertResponseBodyToJSONAndThrowExceptionWithJSONResponseAndErrorCode() throws Exception {
		JSONObject response = new JSONObject();
		response.put(ERROR_REASON_TAG, "test");
		response.put(CONCRETE_TYPE, "org.sagebionetworks.repo.model.ErrorResponse");
		response.put("errorCode", "PASSWORD_RESET_VIA_EMAIL_REQUIRED");

		when(mockResponse.getStatusCode()).thenReturn(401);
		when(mockResponse.getContent()).thenReturn(response.toString());

		try {
			ClientUtils.convertResponseBodyToJSONAndThrowException(mockResponse);
			fail();
		} catch (SynapseUnauthorizedException e) {
			assertEquals("test", e.getMessage());
			assertEquals(ErrorResponseCode.PASSWORD_RESET_VIA_EMAIL_REQUIRED, e.getErrorResponseCode());
		}
	}

	@Test
	public void testThrowUnauthorizedException() throws Exception {
		assertThrows(SynapseUnauthorizedException.class, () -> {
			ClientUtils.throwException(401, "test");
		});
	}

	@Test
	public void testThrowForbiddenException() throws Exception {
		assertThrows(SynapseForbiddenException.class, () -> {
			ClientUtils.throwException(403, "test");
		});
	}

	@Test
	public void testThrowNotFoundException() throws Exception {
		assertThrows(SynapseNotFoundException.class, () -> {
			ClientUtils.throwException(404, "test");
		});
	}

	@Test
	public void testThrowBadRequestException() throws Exception {
		assertThrows(SynapseBadRequestException.class, () -> {
			ClientUtils.throwException(400, "test");
		});
	}

	@Test
	public void testThrowLockedException() throws Exception {
		assertThrows(SynapseLockedException.class, () -> {
			ClientUtils.throwException(423, "test");
		});
	}

	@Test
	public void testThrowConflictingUpdateException() throws Exception {
		assertThrows(SynapseConflictingUpdateException.class, () -> {
			ClientUtils.throwException(412, "test");
		});
	}

	@Test
	public void testThrowDeprecatedServiceException() throws Exception {
		assertThrows(SynapseDeprecatedServiceException.class, () -> {
			ClientUtils.throwException(410, "test");
		});
	}

	@Test
	public void testThrowTooManyRequestException() throws Exception {
		assertThrows(SynapseTooManyRequestsException.class, () -> {
			ClientUtils.throwException(429, "test");
		});
	}

	@Test
	public void testThrowServerException() throws Exception {
		try {
			ClientUtils.throwException(500, "test");
			fail("expect exception");
		} catch (SynapseServerException e) {
			assertEquals("Status Code: 500 message: test", e.getMessage());
		}
	}

	@Test
	public void testCheckStatusCodeAndThrowExceptionFor200() throws Exception {
		when(mockResponse.getStatusCode()).thenReturn(200);
		ClientUtils.checkStatusCodeAndThrowException(mockResponse);
	}

	@Test
	public void testCheckStatusCodeAndThrowExceptionFor400() throws Exception {
		when(mockResponse.getStatusCode()).thenReturn(400);
		when(mockResponse.getContent()).thenReturn("some reason");
		try {
			ClientUtils.checkStatusCodeAndThrowException(mockResponse);
		} catch (SynapseBadRequestException e) {
			assertEquals("some reason", e.getMessage());
		}
	}

	@Test
	public void testCheckStatusCodeAndThrowExceptionForTomcat404() throws Exception {
		when(mockResponse.getStatusCode()).thenReturn(404);
		when(mockResponse.getContent()).thenReturn("some reason");
		try {
			ClientUtils.checkStatusCodeAndThrowException(mockResponse);
		} catch (SynapseNotFoundException e) {
			assertEquals("some reason", e.getMessage());
		}
	}

	@Test
	public void testCheckStatusCodeAndThrowException() throws Exception {
		final String content = "{\"concreteType\":\"org.sagebionetworks.repo.model.ErrorResponse\",\"reason\":\"some reason\",\"errorCode\":\"PASSWORD_RESET_VIA_EMAIL_REQUIRED\"}";
		when(mockResponse.getStatusCode()).thenReturn(400);
		when(mockResponse.getContent()).thenReturn(content);
		try {
			ClientUtils.checkStatusCodeAndThrowException(mockResponse);
		} catch (SynapseBadRequestException e) {
			assertEquals("some reason", e.getMessage());
			assertEquals(ErrorResponseCode.PASSWORD_RESET_VIA_EMAIL_REQUIRED, e.getErrorResponseCode());
		}
	}

	@Test
	public void testCheckStatusCodeAndThrowExceptionWithTwoFactorAuthErrorResponse() throws Exception {
		final String content = "{\"concreteType\":\"org.sagebionetworks.repo.model.auth.TwoFactorAuthErrorResponse\",\"reason\":\"some reason\",\"errorCode\":\"TWO_FA_REQUIRED\",\"userId\":\"123\",\"twoFaToken\":\"token\"}";

		when(mockResponse.getStatusCode()).thenReturn(401);
		when(mockResponse.getContent()).thenReturn(content);

		try {
			ClientUtils.checkStatusCodeAndThrowException(mockResponse);
		} catch (SynapseTwoFactorAuthRequiredException e) {
			assertEquals("some reason", e.getMessage());
			assertEquals(ErrorResponseCode.TWO_FA_REQUIRED, e.getErrorResponseCode());
			assertEquals(123L, e.getUserId());
			assertEquals("token", e.getTwoFaToken());
		}
	}

	@Test
	public void testCheckStatusCodeAndThrowExceptionWithDrsErrorResponse() {
		final String content = "{\"concreteType\":\"org.sagebionetworks.repo.model.drs.DrsErrorResponse\",\"msg\":\"some msg\",\"status_code\":404}";
		when(mockResponse.getStatusCode()).thenReturn(401);
		when(mockResponse.getContent()).thenReturn(content);
		try {
			ClientUtils.checkStatusCodeAndThrowException(mockResponse);
		} catch (SynapseException e) {
			assertEquals("some msg", e.getMessage());
		}
	}

	@Test
	public void testConvertResponseBodyToJSONAndThrowExceptionFor200() throws Exception {
		when(mockResponse.getContent()).thenReturn("{\"reason\":\"some reason\"}");
		when(mockResponse.getStatusCode()).thenReturn(200);
		JSONObject actual = ClientUtils.convertResponseBodyToJSONAndThrowException(mockResponse);
		assertNotNull(actual);
		assertEquals("some reason", actual.get("reason"));
	}

	@Test
	public void testConvertResponseBodyToJSONAndThrowExceptionFor400() throws Exception {
		final String content = "{\"concreteType\":\"org.sagebionetworks.repo.model.ErrorResponse\",\"reason\":\"some reason\"}";
		when(mockResponse.getContent()).thenReturn(content);
		when(mockResponse.getStatusCode()).thenReturn(400);
		try {
			ClientUtils.convertResponseBodyToJSONAndThrowException(mockResponse);
		} catch (SynapseBadRequestException e) {
			assertEquals("some reason", e.getMessage());
		}
	}

	@Test
	public void testConvertResponseBodyToJSONAndThrowExceptionForNonJSONResponse() throws Exception {
		when(mockResponse.getContent()).thenReturn("some reason");
		when(mockResponse.getStatusCode()).thenReturn(404);
		try {
			ClientUtils.convertResponseBodyToJSONAndThrowException(mockResponse);
		} catch (SynapseNotFoundException e) {
			assertEquals("some reason", e.getMessage());
		}
	}

	@Test
	public void testConvertResponseBodyToJSONAndThrowExceptionForNonJSONResponse200() throws Exception {
		when(mockResponse.getContent()).thenReturn("some reason");
		when(mockResponse.getStatusCode()).thenReturn(200);
		assertThrows(SynapseClientException.class, () -> {
			ClientUtils.convertResponseBodyToJSONAndThrowException(mockResponse);
		});
	}

	@Test
	public void testConvertStringToJSONObjectWithNull() throws Exception {
		assertNull(ClientUtils.convertStringToJSONObject(null));
	}

	@Test
	public void testConvertStringToJSONObjectWithWrongJSONFormat() throws Exception {
		assertThrows(JSONException.class, () -> {
			ClientUtils.convertStringToJSONObject("abc");
		});
	}

	@Test
	public void testConvertStringToJSONObject() throws Exception {
		JSONObject actual = ClientUtils.convertStringToJSONObject("{\"reason\":\"some reason\"}");
		assertNotNull(actual);
		assertEquals("some reason", actual.get("reason"));
	}

	@Test
	public void testGetCharacterSetFromResponse() {
		when(mockResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(mockHeader);
		when(mockHeader.getValue()).thenReturn("text/plain; charset=UTF-8");
		assertEquals(Charset.forName("UTF-8"), ClientUtils.getCharacterSetFromResponse(mockResponse));
	}

	@Test
	public void testCreateRequestUrlWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			ClientUtils.createRequestUrl(null, "uri", null);
		});
	}

	@Test
	public void testCreateRequestUrlWithNullURI() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			ClientUtils.createRequestUrl("endpoint", null, null);
		});
	}

	@Test
	public void testCreateRequestUrlWithNullParams() throws Exception {
		assertEquals("http://synapse.org/uri", ClientUtils.createRequestUrl("http://synapse.org", "/uri", null));
	}

	@Test
	public void testCreateRequestUrlWithParams() throws Exception {
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put("limit", "1");
		params.put("offset", "0");
		assertEquals("http://synapse.org/uri?limit=1&offset=0", ClientUtils.createRequestUrl("http://synapse.org", "/uri", params));
	}

	@Test
	public void testPerformRequestWithNullClient() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			ClientUtils.performRequest(null, "requestUrl", GET, null, null);
		});
	}

	@Test
	public void testPerformRequestWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			ClientUtils.performRequest(mockClient, null, GET, null, null);
		});
	}

	@Test
	public void testPerformRequestWithNullMethod() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			ClientUtils.performRequest(mockClient, "requestUrl", null, null, null);
		});
	}

	@Test
	public void testPerformGETRequest() throws Exception {
		ClientUtils.performRequest(mockClient, "requestUrl", GET, null, null);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		verify(mockClient).get(request);
	}

	@Test
	public void testPerformGETRequestWithHeaders() throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "text/plain");
		ClientUtils.performRequest(mockClient, "requestUrl", GET, null, headers);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		request.setHeaders(headers);
		verify(mockClient).get(request);
	}

	@Test
	public void testPerformPOSTRequest() throws Exception {
		ClientUtils.performRequest(mockClient, "requestUrl", POST, null, null);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		verify(mockClient).post(request, null);
	}

	@Test
	public void testPerformPOSTRequestWithRequestBody() throws Exception {
		ClientUtils.performRequest(mockClient, "requestUrl", POST, "body", null);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		verify(mockClient).post(request, "body");
	}

	@Test
	public void testPerformPUTRequest() throws Exception {
		ClientUtils.performRequest(mockClient, "requestUrl", PUT, null, null);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		verify(mockClient).put(request, null);
	}

	@Test
	public void testPerformPUTRequestWithRequestBody() throws Exception {
		ClientUtils.performRequest(mockClient, "requestUrl", PUT, "body", null);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		verify(mockClient).put(request, "body");
	}

	@Test
	public void testPerformDELETERequest() throws Exception {
		ClientUtils.performRequest(mockClient, "requestUrl", DELETE, null, null);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("requestUrl");
		verify(mockClient).delete(request);
	}
}
