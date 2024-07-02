package org.sagebionetworks.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.client.BaseClientImpl.MAX_RETRY_SERVICE_UNAVAILABLE_COUNT;
import static org.sagebionetworks.client.Method.GET;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseServiceUnavailable;
import org.sagebionetworks.client.exceptions.SynapseTooManyRequestsException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.simpleHttpClient.Header;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

@ExtendWith(MockitoExtension.class)
public class BaseClientImplTest {

	private static final String USER_AGENT = "JavaClient";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String CONTENT_LENGTH = "Content-Length";
	private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json; charset=utf-8";
	private static final String SESSION_ID_VALUE = "mySessionIdValue";

	@Mock
	private SimpleHttpClient mockClient;
	@Mock
	private SimpleHttpResponse mockResponse;
	@Mock
	private SimpleHttpResponse mockResponse2;
	@Mock
	private File mockFile;
	@Mock
	private Header mockContentTypeHeader;
	@Mock
	private Header mockContentLengthHeader;

	private BaseClientImpl baseClient;

	@BeforeEach
	public void before() {
		MockitoAnnotations.initMocks(this);

		baseClient = new BaseClientImpl(USER_AGENT);
		baseClient.setSimpleHttpClient(mockClient);

	}

	@Test
	public void testAppendAgentWithNull() {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.appendUserAgent(null);
		});
	}

	@Test
	public void testAppendAgentAlreadyExist() {
		assertEquals(USER_AGENT, baseClient.getUserAgent());
		baseClient.appendUserAgent(USER_AGENT);
		assertEquals(USER_AGENT, baseClient.getUserAgent());
	}

	@Test
	public void testAppendAgent() {
		assertEquals(USER_AGENT, baseClient.getUserAgent());
		baseClient.appendUserAgent("v1");
		assertEquals(USER_AGENT + " v1", baseClient.getUserAgent());
	}

	@Test
	public void testLoginWithNullRequest() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.login(null);
		});
	}

	@Test
	public void testLoginWithNullUsername() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setPassword("password");

		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.login(request);
		});
	}

	@Test
	public void testLoginWithNullPassword() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setUsername("username");

		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.login(request);
		});
	}

	@Test
	public void testLogin() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setUsername("username");
		request.setPassword("password");
		when(mockClient.post(any(SimpleHttpRequest.class), anyString())).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn(
				"{" + "\"sessionToken\":\"token\"," + "\"authenticationReceipt\":\"receipt\"," + "\"acceptsTermsOfUse\":\"true\"," + "}");
		LoginResponse loginResponse = new LoginResponse();
		loginResponse.setAcceptsTermsOfUse(true);
		loginResponse.setAuthenticationReceipt("receipt");
		loginResponse.setSessionToken("token");
		assertEquals(loginResponse, baseClient.login(request));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).post(captor.capture(), eq(EntityFactory.createJSONObjectForEntity(request).toString()));
		assertEquals("https://repo-prod.prod.sagebase.org/auth/v1/login", captor.getValue().getUri());
	}

	@Test
	public void testLoginForAccessToken() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setUsername("username");
		request.setPassword("password");
		when(mockClient.post(any(SimpleHttpRequest.class), anyString())).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn(
				"{" + "\"accessToken\":\"token\"," + "\"authenticationReceipt\":\"receipt\"," + "\"acceptsTermsOfUse\":\"true\"," + "}");
		LoginResponse loginResponse = new LoginResponse();
		loginResponse.setAcceptsTermsOfUse(true);
		loginResponse.setAuthenticationReceipt("receipt");
		loginResponse.setAccessToken("token");
		assertEquals(loginResponse, baseClient.loginForAccessToken(request));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).post(captor.capture(), eq(EntityFactory.createJSONObjectForEntity(request).toString()));
		assertEquals("https://repo-prod.prod.sagebase.org/auth/v1/login2", captor.getValue().getUri());
		assertEquals("token", baseClient.getAccessToken());
	}

	@Test
	public void testLogout() throws Exception {
		baseClient.setSessionToken("some token");

		baseClient.deleteSessionTokenHeader();

		assertNull(baseClient.getCurrentSessionToken());
	}

	@Test
	public void testLogoutForAccessToken() throws Exception {
		when(mockClient.delete(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		
		baseClient.logoutForAccessToken();

		assertNull(baseClient.getAuthorizationHeader());
	}

	@Test
	public void testGetAccessTokenAfterLogout() throws Exception {
		when(mockClient.delete(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		
		baseClient.logoutForAccessToken();

		assertThrows(IllegalStateException.class, () -> {
			assertNull(baseClient.getAccessToken());
		});
	}

	@Test
	public void testInvalidateAPIKey() throws Exception {
		when(mockClient.delete(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		baseClient.invalidateApiKey();
		;
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).delete(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/auth/v1/secretKey", captor.getValue().getUri());
	}

	@Test
	public void testPutFileToURLWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.putFileToURL(null, mockFile, "contentType");
		});

	}

	@Test
	public void testPutFileToURLWithNullFile() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.putFileToURL(new URL("https://repo-prod.prod.sagebase.org"), null, "contentType");
		});
	}

	@Test
	public void testPutFileToURLWithNullContentType() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.putFileToURL(new URL("https://repo-prod.prod.sagebase.org"), mockFile, null);
		});
	}

	@Test
	public void testPutFileToURL() throws Exception {
		when(mockClient.putFile(any(SimpleHttpRequest.class), any(File.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("content");
		assertEquals("content", baseClient.putFileToURL(new URL("https://repo-prod.prod.sagebase.org"), mockFile, "contentType"));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).putFile(captor.capture(), eq(mockFile));
		assertEquals("https://repo-prod.prod.sagebase.org", captor.getValue().getUri());
	}

	@Test
	public void testDownloadZippedFileToStringWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.downloadFileToString(null, "uri", true);
		});
	}

	@Test
	public void testDownloadZippedFileToStringWithNullURI() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.downloadFileToString("endpoint", null, true);
		});
	}

	@Test
	public void testDownloadFileToString() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockContentTypeHeader.getValue()).thenReturn("text/plain; charset=UTF-8");

		ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
		when(mockClient.getFile(any(SimpleHttpRequest.class), fileCaptor.capture())).thenAnswer(new Answer<SimpleHttpResponse>() {
			@Override
			public SimpleHttpResponse answer(InvocationOnMock invocation) throws Throwable {
				try (FileOutputStream fos = new FileOutputStream(fileCaptor.getValue())) {
					fos.write("some content".getBytes());
				}
				return mockResponse2;
			}
		});

		when(mockResponse2.getStatusCode()).thenReturn(200);
		when(mockResponse2.getFirstHeader(CONTENT_TYPE)).thenReturn(mockContentTypeHeader);

		baseClient.downloadFileToString("https://repo-prod.prod.sagebase.org", "/fileToDownload?redirect=false", false);

		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		ArgumentCaptor<SimpleHttpRequest> redirCaptor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(redirCaptor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/fileToDownload?redirect=false", redirCaptor.getValue().getUri());
		verify(mockClient).getFile(captor.capture(), fileCaptor.capture());
		File file = fileCaptor.getValue();
		assertEquals(fileCaptor.getValue().getName(), file.getName());
		// has been deleted
		assertFalse(file.exists());
	}

	@Test
	public void testDownloadFromSynapseWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.downloadFromSynapse(null, "md5", mockFile);
		});
	}

	@Test
	public void testDownloadFromSynapseWithNullFile() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.downloadFromSynapse("url", "md5", null);
		});
	}

	@Test
	public void testDownloadFromSynapseMd5DoesNotMatch() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockFile.getAbsolutePath()).thenReturn("fakePath");
		when(mockClient.getFile(any(SimpleHttpRequest.class), any(File.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);

		assertThrows(SynapseClientException.class, () -> {
			baseClient.downloadFromSynapse("https://repo-prod.prod.sagebase.org/fileToDownload?redirect=false", "md5", mockFile);
		}).getMessage();

		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/fileToDownload?redirect=false", captor.getValue().getUri());
	}

	@Test
	public void testDownloadFromSynapseWithNullMd5() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);

		when(mockClient.getFile(any(SimpleHttpRequest.class), any(File.class))).thenReturn(mockResponse2);
		when(mockResponse2.getStatusCode()).thenReturn(200);
		when(mockContentTypeHeader.getValue()).thenReturn(CONTENT_TYPE_APPLICATION_JSON);
		when(mockResponse2.getFirstHeader(CONTENT_TYPE)).thenReturn(mockContentTypeHeader);

		assertEquals(Charset.forName("utf-8"),
				baseClient.downloadFromSynapse("https://repo-prod.prod.sagebase.org/fileToDownload?redirect=false", null, mockFile));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/fileToDownload?redirect=false", captor.getValue().getUri());
	}

	/*
	 * PLFM-4349
	 */
	@Test
	public void testDownloadFromSynapseWithJsonError() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(403);
		when(mockResponse.getContent()).thenReturn("{\"reason\":\"User lacks READ_PRIVATE_SUBMISSION access to Evaluation 8719759\"}");

		assertThrows(SynapseForbiddenException.class, () -> {
			baseClient.downloadFromSynapse("https://repo-prod.prod.sagebase.org/fileToDownload", null, mockFile);
		});
	}

	@Test
	public void testDownloadFromSynapseWithNonJsonError() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(403);
		when(mockResponse.getContent()).thenReturn("User lacks READ_PRIVATE_SUBMISSION access to Evaluation 8719759");

		assertThrows(SynapseForbiddenException.class, () -> {
			baseClient.downloadFromSynapse("https://repo-prod.prod.sagebase.org/fileToDownload", null, mockFile);
		});
	}

	@Test
	public void testGetJsonWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getJson(null, "/entity");
		});
	}

	@Test
	public void testGetJsonWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getJson("https://repo-prod.prod.sagebase.org", null);
		});
	}

	@Test
	public void testGetJson() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

		mockJsonResponse("{\"id\":\"0\"}");

		assertNotNull(baseClient.getJson("https://repo-prod.prod.sagebase.org", "/entity"));
	}

	@Test
	public void testPostJsonWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.postJson(null, "/entity", null, null);
		});
	}

	@Test
	public void testPostJsonWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.postJson("https://repo-prod.prod.sagebase.org", null, null, null);
		});
	}

	@Test
	public void testPostJson() throws Exception {
		when(mockClient.post(any(SimpleHttpRequest.class), isNull())).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{}");
		assertNotNull(baseClient.postJson("https://repo-prod.prod.sagebase.org", "/entity", null, null));
	}

	@Test
	public void testPutJsonWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.putJson(null, "/entity", null);
		});
	}

	@Test
	public void testPutJsonWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.putJson("https://repo-prod.prod.sagebase.org", null, null);
		});
	}

	@Test
	public void testPutJson() throws Exception {
		when(mockClient.put(any(SimpleHttpRequest.class), isNull())).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{}");
		assertNotNull(baseClient.putJson("https://repo-prod.prod.sagebase.org", "/entity", null));
	}

	@Test
	public void testGetStringDirectWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getStringDirect(null, "/entity");
		});
	}

	@Test
	public void testGetStringDirectWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getStringDirect("https://repo-prod.prod.sagebase.org", null);
		});
	}

	@Test
	public void testGetStringDirect() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("content");

		assertEquals("content", baseClient.getStringDirect("https://repo-prod.prod.sagebase.org", "/entity"));
	}

	@Test
	public void testPostStringDirectWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.postStringDirect(null, "/entity", null);
		});
	}

	@Test
	public void testPostStringDirectWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.postStringDirect("https://repo-prod.prod.sagebase.org", null, null);
		});
	}

	@Test
	public void testPostStringDirect() throws Exception {
		when(mockClient.post(any(SimpleHttpRequest.class), isNull())).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("content");
		assertEquals("content", baseClient.postStringDirect("https://repo-prod.prod.sagebase.org", "/entity", null));
	}

	@Test
	public void testDeleteUriWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.deleteUri(null, "/entity");
		});
	}

	@Test
	public void testDeleteUriWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.deleteUri("https://repo-prod.prod.sagebase.org", null);
		});
	}

	@Test
	public void testDeleteUri() throws Exception {
		when(mockClient.delete(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		baseClient.deleteUri("https://repo-prod.prod.sagebase.org", "/entity");
	}

	@Test
	public void testPutJSONEntityWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.putJSONEntity(null, "url", null, EntityId.class);
		});
	}

	@Test
	public void testPutJSONEntityWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.putJSONEntity("endpoint", null, null, EntityId.class);
		});
	}

	@Test
	public void testPutJSONEntityWithNullReturnClass() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.putJSONEntity("endpoint", "url", null, null);
		});
	}

	@Test
	public void testPutJSONEntity() throws Exception {
		when(mockClient.put(any(SimpleHttpRequest.class), isNull())).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{\"id\":\"0\"}");
		EntityId id = new EntityId();
		id.setId("0");
		assertEquals(id, baseClient.putJSONEntity("https://repo-prod.prod.sagebase.org", "/entityId", null, EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).put(captor.capture(), isNull());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId", captor.getValue().getUri());
	}

	@Test
	public void testVoidPutWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.voidPut(null, "url", null);
		});
	}

	@Test
	public void testVoidPutWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.voidPut("endpoint", null, null);
		});
	}

	@Test
	public void testVoidPut() throws Exception {
		when(mockClient.put(any(SimpleHttpRequest.class), isNull())).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		baseClient.voidPut("https://repo-prod.prod.sagebase.org", "/entityId", null);
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).put(captor.capture(), isNull());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId", captor.getValue().getUri());
	}

	@Test
	public void testPostJSONEntityWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.postJSONEntity(null, "url", null, EntityId.class);
		});
	}

	@Test
	public void testPostJSONEntityWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.postJSONEntity("endpoint", null, null, EntityId.class);
		});
	}

	@Test
	public void testPostJSONEntityWithNullReturnClass() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.postJSONEntity("endpoint", "url", null, null);
		});
	}

	@Test
	public void testPostJSONEntity() throws Exception {
		when(mockClient.post(any(SimpleHttpRequest.class), isNull())).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{\"id\":\"0\"}");
		EntityId id = new EntityId();
		id.setId("0");
		assertEquals(id, baseClient.postJSONEntity("https://repo-prod.prod.sagebase.org", "/entityId", null, EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).post(captor.capture(), isNull());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId", captor.getValue().getUri());
	}

	@Test
	public void testVoidPostWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.voidPost(null, "url", null, null);
		});
	}

	@Test
	public void testVoidPostWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.voidPost("endpoint", null, null, null);
		});
	}

	@Test
	public void testVoidPost() throws Exception {
		when(mockClient.post(any(SimpleHttpRequest.class), isNull())).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		baseClient.voidPost("https://repo-prod.prod.sagebase.org", "/entityId", null, null);
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).post(captor.capture(), isNull());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId", captor.getValue().getUri());
	}

	@Test
	public void testGetJSONEntityWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getJSONEntity(null, "url", EntityId.class);
		});
	}

	@Test
	public void testGetJSONEntityWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getJSONEntity("endpoint", null, EntityId.class);
		});
	}

	@Test
	public void testGetJSONEntityWithNullReturnClass() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getJSONEntity("endpoint", "url", null);
		});
	}

	@Test
	public void testGetJSONEntityWithNullJSONObject() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

		mockJsonResponse("");

		assertNull(baseClient.getJSONEntity("https://repo-prod.prod.sagebase.org", "/entityId", EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId", captor.getValue().getUri());
	}

	@Test
	public void testGetJSONEntity() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

		mockJsonResponse("{\"id\":\"0\"}");

		// Successful
		EntityId id = new EntityId();
		id.setId("0");
		assertEquals(id, baseClient.getJSONEntity("https://repo-prod.prod.sagebase.org", "/entityId", EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId", captor.getValue().getUri());
	}

	@Test
	public void testGetPaginatedResultsWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getPaginatedResults(null, "url", EntityId.class);
		});
	}

	@Test
	public void testGetPaginatedResultsWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getPaginatedResults("endpoint", null, EntityId.class);
		});
	}

	@Test
	public void testGetPaginatedResultsWithNullReturnClass() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getPaginatedResults("endpoint", "url", null);
		});
	}

	@Test
	public void testGetPaginatedResults() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);

		mockJsonResponse("{" + "\"totalNumberOfResults\":\"0\"," + "\"results\":[]," + "}");

		PaginatedResults<EntityId> results = new PaginatedResults<EntityId>();
		results.setTotalNumberOfResults(0);
		results.setResults(new LinkedList<EntityId>());
		assertEquals(results, baseClient.getPaginatedResults("https://repo-prod.prod.sagebase.org", "/entityId", EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId", captor.getValue().getUri());
	}

	@Test
	public void testGetPaginatedResultsByPostWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getPaginatedResults(null, "url", new EntityId(), EntityId.class);
		});
	}

	@Test
	public void testGetPaginatedResultsByPostWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getPaginatedResults("endpoint", null, new EntityId(), EntityId.class);
		});
	}

	@Test
	public void testGetPaginatedResultsByPostWithNullRequestBody() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getPaginatedResults("endpoint", null, null, EntityId.class);
		});
	}

	@Test
	public void testGetPaginatedResultsByPostWithNullReturnClass() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getPaginatedResults("endpoint", "url", new EntityId(), null);
		});
	}

	@Test
	public void testGetPaginatedResultsByPost() throws Exception {
		when(mockClient.post(any(SimpleHttpRequest.class), anyString())).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{" + "\"totalNumberOfResults\":\"0\"," + "\"results\":[]," + "}");
		PaginatedResults<EntityId> results = new PaginatedResults<EntityId>();
		results.setTotalNumberOfResults(0);
		results.setResults(new LinkedList<EntityId>());
		assertEquals(results,
				baseClient.getPaginatedResults("https://repo-prod.prod.sagebase.org", "/entityId", new EntityId(), EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).post(captor.capture(), anyString());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId", captor.getValue().getUri());
	}

	@Test
	public void testGetListOfJSONEntityWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getListOfJSONEntity(null, "url", EntityId.class);
		});
	}

	@Test
	public void testGetListOfJSONEntityWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getListOfJSONEntity("endpoint", null, EntityId.class);
		});
	}

	@Test
	public void testGetListOfJSONEntityWithNullReturnClass() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getListOfJSONEntity("endpoint", "url", null);
		});
	}

	@Test
	public void testGetListOfJSONEntity() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		mockJsonResponse("{" + "\"concreteType\":\"org.sagebionetworks.repo.model.EntityId\"," + "\"list\":[]," + "}");
		List<EntityId> results = new LinkedList<EntityId>();
		assertEquals(results, baseClient.getListOfJSONEntity("https://repo-prod.prod.sagebase.org", "/entityId", EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId", captor.getValue().getUri());
	}

	@Test
	public void testGetListOfJSONEntityByPostWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getListOfJSONEntity(null, "url", new EntityId(), EntityId.class);
		});
	}

	@Test
	public void testGetListOfJSONEntityByPostWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getListOfJSONEntity("endpoint", null, new EntityId(), EntityId.class);
		});
	}

	@Test
	public void testGetListOfJSONEntityByPostWithNullRequestBody() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getListOfJSONEntity("endpoint", null, null, EntityId.class);
		});
	}

	@Test
	public void testGetListOfJSONEntityByPostWithNullReturnClass() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getListOfJSONEntity("endpoint", "url", new EntityId(), null);
		});
	}

	@Test
	public void testGetListOfJSONEntityByPost() throws Exception {
		when(mockClient.post(any(SimpleHttpRequest.class), anyString())).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent())
				.thenReturn("{" + "\"concreteType\":\"org.sagebionetworks.repo.model.EntityId\"," + "\"list\":[]," + "}");
		List<EntityId> results = new LinkedList<EntityId>();
		assertEquals(results,
				baseClient.getListOfJSONEntity("https://repo-prod.prod.sagebase.org", "/entityId", new EntityId(), EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).post(captor.capture(), anyString());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId", captor.getValue().getUri());
	}

	@Test
	public void testGetBooleanResultWithNullEndpoint() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getBooleanResult(null, "url");
		});
	}

	@Test
	public void testGetBooleanResultWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.getBooleanResult("endpoint", null);
		});
	}

	@Test
	public void testGetBooleanResult() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

		mockJsonResponse("{\"result\":\"true\"}");

		assertEquals(true, baseClient.getBooleanResult("https://repo-prod.prod.sagebase.org", "/entityId"));

		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId", captor.getValue().getUri());
	}

	@Test
	public void testAddDigitalSignatureWithNullURL() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.addDigitalSignature(null, new HashMap<String, String>());
		});
	}

	@Test
	public void testAddDigitalSignatureWithNullHeaders() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			baseClient.addDigitalSignature("https://repo-prod.prod.sagebase.org/entityId", null);
		});
	}

	@Test
	public void testAddDigitalSignature() throws Exception {
		baseClient.setUsername("username");
		baseClient.setApiKey("apiKey");
		HashMap<String, String> headers = new HashMap<String, String>();
		baseClient.addDigitalSignature("https://repo-prod.prod.sagebase.org/entityId", headers);
		assertEquals("username", headers.get(AuthorizationConstants.USER_ID_HEADER));
		assertNotNull(headers.get(AuthorizationConstants.SIGNATURE_TIMESTAMP));
		assertNotNull(headers.get(AuthorizationConstants.SIGNATURE));
	}

	@Test
	public void testPerformRequestWithRetryWithNullURL() throws Exception {
		assertThrows(SynapseClientException.class, () -> {
			baseClient.performRequestWithRetry(null, GET, null, null);
		});
	}

	@Test
	public void testPerformRequestWithRetryWithNullMethod() throws Exception {
		assertThrows(SynapseClientException.class, () -> {
			baseClient.performRequestWithRetry("https://repo-prod.prod.sagebase.org/entityId", null, null, null);
		});
	}

	@Test
	public void testPerformRequestWithRetrySuccess() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		assertEquals(mockResponse, baseClient.performRequestWithRetry("https://repo-prod.prod.sagebase.org/entityId", GET, null, null));
		verify(mockClient).get(any(SimpleHttpRequest.class));
	}

	@Test
	public void testPerformRequestWithoutRetryFailure() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(500);
		assertEquals(mockResponse, baseClient.performRequestWithRetry("https://repo-prod.prod.sagebase.org/entityId", GET, null, null));
		verify(mockClient).get(any(SimpleHttpRequest.class));
	}

	@Test
	public void testPerformRequestWithRetryFailureOnServiceUnavailable() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_SERVICE_UNAVAILABLE);

		assertThrows(SynapseServiceUnavailable.class, () -> {
			baseClient.performRequestWithRetry("https://repo-prod.prod.sagebase.org/entityId", GET, null, null);
		});

		verify(mockClient, times(MAX_RETRY_SERVICE_UNAVAILABLE_COUNT)).get(any(SimpleHttpRequest.class));
	}

	@Test
	public void testPerformRequestWithRetryFailureOnTooManyRequests() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(SynapseTooManyRequestsException.TOO_MANY_REQUESTS_STATUS_CODE);

		assertThrows(SynapseTooManyRequestsException.class, () -> {
			baseClient.performRequestWithRetry("https://repo-prod.prod.sagebase.org/entityId", GET, null, null);
		});

		verify(mockClient, times(MAX_RETRY_SERVICE_UNAVAILABLE_COUNT)).get(any(SimpleHttpRequest.class));
	}

	@Test
	public void testSetSessionId() {
		// method under test
		baseClient.setSessionId(SESSION_ID_VALUE);

		verify(mockClient).addCookie("repo-prod.prod.sagebase.org", "sessionID", SESSION_ID_VALUE);
	}

	@Test
	public void testGetSessionId() {
		when(mockClient.getFirstCookieValue("repo-prod.prod.sagebase.org", "sessionID")).thenReturn(SESSION_ID_VALUE);

		// method under test
		String result = baseClient.getSessionId();

		assertEquals(SESSION_ID_VALUE, result);
		verify(mockClient).getFirstCookieValue("repo-prod.prod.sagebase.org", "sessionID");
	}

	@Test
	public void testSetRepositoryEndpoint_malformedRepoEndpoint() {
		assertThrows(IllegalArgumentException.class, () -> {
			// set a bad endpoint
			baseClient.setRepositoryEndpoint("asdf.asdf.asdf...");
		});
	}

	@Test
	public void testSetRepositoryEndpoint() {
		String repoEndpoint = "https://my.test.endpoint.com/some/path";

		baseClient.setRepositoryEndpoint(repoEndpoint);

		assertEquals(repoEndpoint, baseClient.getRepoEndpoint());
		assertEquals("my.test.endpoint.com", baseClient.repoEndpointBaseDomain);
	}

	private void mockJsonResponse(String content) {
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn(content);
		when(mockContentLengthHeader.getValue()).thenReturn("1024");
		when(mockResponse.getFirstHeader(CONTENT_LENGTH)).thenReturn(mockContentLengthHeader);
		when(mockContentTypeHeader.getValue()).thenReturn(CONTENT_TYPE_APPLICATION_JSON);
		when(mockResponse.getFirstHeader(CONTENT_TYPE)).thenReturn(mockContentTypeHeader);
	}

}
