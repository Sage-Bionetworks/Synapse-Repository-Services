package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
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
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
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

public class BaseClientImplTest {
	private static final String USER_AGENT = "JavaClient";
	@Mock
	SimpleHttpClient mockClient;
	@Mock
	SimpleHttpResponse mockResponse;
	@Mock
	File mockFile;
	@Mock
	Header mockHeader;

	BaseClientImpl baseClient;
	private final String sessionIdVal = "mySessionIdValue";


	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		baseClient = new BaseClientImpl(USER_AGENT);
		baseClient.setSimpleHttpClient(mockClient);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testAppendAgentWithNull() {
		baseClient.appendUserAgent(null);
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
		assertEquals(USER_AGENT+" v1", baseClient.getUserAgent());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testLoginWithNullRequest() throws Exception{
		baseClient.login(null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testLoginWithNullUsername() throws Exception{
		LoginRequest request = new LoginRequest();
		request.setPassword("password");
		baseClient.login(request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testLoginWithNullPassword() throws Exception{
		LoginRequest request = new LoginRequest();
		request.setUsername("username");
		baseClient.login(request);
	}

	@Test
	public void testLogin() throws Exception{
		LoginRequest request = new LoginRequest();
		request.setUsername("username");
		request.setPassword("password");
		when(mockClient.post(any(SimpleHttpRequest.class),anyString()))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{"
				+ "\"sessionToken\":\"token\","
				+ "\"authenticationReceipt\":\"receipt\","
				+ "\"acceptsTermsOfUse\":\"true\","
				+ "}");
		LoginResponse loginResponse = new LoginResponse();
		loginResponse.setAcceptsTermsOfUse(true);
		loginResponse.setAuthenticationReceipt("receipt");
		loginResponse.setSessionToken("token");
		assertEquals(loginResponse , baseClient.login(request));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).post(captor.capture(),
				eq(EntityFactory.createJSONObjectForEntity(request).toString()));
		assertEquals("https://repo-prod.prod.sagebase.org/auth/v1/login",
				captor.getValue().getUri());
	}

	@Test
	public void testLogout() throws Exception {
		when(mockClient.delete(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		baseClient.logout();
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).delete(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/auth/v1/session",
				captor.getValue().getUri());
	}

	@Test (expected = SynapseClientException.class)
	public void testRevalidateSessionNotLogin() throws Exception {
		baseClient.revalidateSession();
	}

	@Test
	public void testRevalidateSession() throws Exception {
		when(mockClient.put(any(SimpleHttpRequest.class), anyString()))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		baseClient.setSessionToken("token");
		baseClient.revalidateSession();
		ArgumentCaptor<SimpleHttpRequest> requestCaptor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(mockClient).put(requestCaptor.capture(), bodyCaptor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/auth/v1/session",
				requestCaptor.getValue().getUri());
		assertEquals("{\"sessionToken\":\"token\"}", bodyCaptor.getValue());
	}

	@Test
	public void testInvalidateAPIKey() throws Exception {
		when(mockClient.delete(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		baseClient.invalidateApiKey();;
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).delete(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/auth/v1/secretKey",
				captor.getValue().getUri());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPutFileToURLWithNullURL() throws Exception {
		baseClient.putFileToURL(null, mockFile, "contentType");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPutFileToURLWithNullFile() throws Exception {
		baseClient.putFileToURL(new URL("https://repo-prod.prod.sagebase.org"),
				null, "contentType");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPutFileToURLWithNullContentType() throws Exception {
		baseClient.putFileToURL(new URL("https://repo-prod.prod.sagebase.org"),
				mockFile, null);
	}

	@Test
	public void testPutFileToURL() throws Exception {
		when(mockClient.putFile(any(SimpleHttpRequest.class), any(File.class)))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("content");
		assertEquals("content",
				baseClient.putFileToURL(new URL("https://repo-prod.prod.sagebase.org"),
						mockFile, "contentType"));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).putFile(captor.capture(), eq(mockFile));
		assertEquals("https://repo-prod.prod.sagebase.org",
				captor.getValue().getUri());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDownloadZippedFileToStringWithNullEndpoint() throws Exception {
		baseClient.downloadZippedFileToString(null, "uri");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDownloadZippedFileToStringWithNullURI() throws Exception {
		baseClient.downloadZippedFileToString("endpoint", null);
	}

	@Test
	public void testDownloadZippedFileToString() throws Exception {
		when(mockClient.getFile(any(SimpleHttpRequest.class), any(File.class)))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockHeader.getValue()).thenReturn("text/plain; charset=UTF-8");
		when(mockResponse.getFirstHeader("Content-Type")).thenReturn(mockHeader);
		try {
			baseClient.downloadZippedFileToString("https://repo-prod.prod.sagebase.org", "/fileToDownload");
			fail("should throw a FileNotFoundException");
		} catch (FileNotFoundException e) {
			// since we do not mock the client to actually download the file
		}
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
		verify(mockClient).getFile(captor.capture(), fileCaptor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/fileToDownload",
				captor.getValue().getUri());
		File file = fileCaptor.getValue();
		assertEquals("zipped", file.getName());
		// has been deleted
		assertFalse(file.exists());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDownloadFromSynapseWithNullURL() throws Exception {
		baseClient.downloadFromSynapse(null, "md5", mockFile);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDownloadFromSynapseWithNullFile() throws Exception {
		baseClient.downloadFromSynapse("url", "md5", null);
	}

	@Test
	public void testDownloadFromSynapseMd5DoesNotMatch() throws Exception {
		when(mockFile.getAbsolutePath()).thenReturn("fakePath");
		when(mockClient.getFile(any(SimpleHttpRequest.class), any(File.class)))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		try {
			baseClient.downloadFromSynapse("https://repo-prod.prod.sagebase.org/fileToDownload", "md5", mockFile);
			// expected FileNotFoundException when checking md5
		} catch (Exception e) {
			// the mockFile doesn't exist
		}
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).getFile(captor.capture(), eq(mockFile));
		assertEquals("https://repo-prod.prod.sagebase.org/fileToDownload",
				captor.getValue().getUri());
	}

	@Test
	public void testDownloadFromSynapseWithNullMd5() throws Exception {
		when(mockClient.getFile(any(SimpleHttpRequest.class), any(File.class)))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		assertEquals(mockFile, baseClient.downloadFromSynapse(
				"https://repo-prod.prod.sagebase.org/fileToDownload", null, mockFile));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).getFile(captor.capture(), eq(mockFile));
		assertEquals("https://repo-prod.prod.sagebase.org/fileToDownload",
				captor.getValue().getUri());
	}

	/*
	 * PLFM-4349
	 */
	@Test (expected = SynapseForbiddenException.class)
	public void testDownloadFromSynapseWithJsonError() throws Exception {
		when(mockClient.getFile(any(SimpleHttpRequest.class), any(File.class)))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(403);
		when(mockResponse.getContent()).thenReturn("{\"reason\":\"User lacks READ_PRIVATE_SUBMISSION access to Evaluation 8719759\"}");
		baseClient.downloadFromSynapse("https://repo-prod.prod.sagebase.org/fileToDownload", null, mockFile);
	}

	@Test (expected = SynapseForbiddenException.class)
	public void testDownloadFromSynapseWithNonJsonError() throws Exception {
		when(mockClient.getFile(any(SimpleHttpRequest.class), any(File.class)))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(403);
		when(mockResponse.getContent()).thenReturn("User lacks READ_PRIVATE_SUBMISSION access to Evaluation 8719759");
		baseClient.downloadFromSynapse("https://repo-prod.prod.sagebase.org/fileToDownload", null, mockFile);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetJsonWithNullEndpoint() throws Exception{
		baseClient.getJson(null, "/entity");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetJsonWithNullURL() throws Exception{
		baseClient.getJson("https://repo-prod.prod.sagebase.org", null);
	}

	@Test
	public void testGetJson() throws Exception{
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{}");
		assertNotNull(baseClient.getJson("https://repo-prod.prod.sagebase.org", "/entity"));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPostJsonWithNullEndpoint() throws Exception{
		baseClient.postJson(null, "/entity", null, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPostJsonWithNullURL() throws Exception{
		baseClient.postJson("https://repo-prod.prod.sagebase.org", null, null, null);
	}

	@Test
	public void testPostJson() throws Exception{
		when(mockClient.post(any(SimpleHttpRequest.class), isNull()))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{}");
		assertNotNull(baseClient.postJson("https://repo-prod.prod.sagebase.org", "/entity", null, null));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPutJsonWithNullEndpoint() throws Exception{
		baseClient.putJson(null, "/entity", null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPutJsonWithNullURL() throws Exception{
		baseClient.putJson("https://repo-prod.prod.sagebase.org", null, null);
	}

	@Test
	public void testPutJson() throws Exception{
		when(mockClient.put(any(SimpleHttpRequest.class), isNull()))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{}");
		assertNotNull(baseClient.putJson("https://repo-prod.prod.sagebase.org", "/entity", null));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetStringDirectWithNullEndpoint() throws Exception{
		baseClient.getStringDirect(null, "/entity");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetStringDirectWithNullURL() throws Exception{
		baseClient.getStringDirect("https://repo-prod.prod.sagebase.org", null);
	}

	@Test
	public void testGetStringDirect() throws Exception{
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("content");
		assertEquals("content",
				baseClient.getStringDirect("https://repo-prod.prod.sagebase.org", "/entity"));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPostStringDirectWithNullEndpoint() throws Exception{
		baseClient.postStringDirect(null, "/entity", null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPostStringDirectWithNullURL() throws Exception{
		baseClient.postStringDirect("https://repo-prod.prod.sagebase.org", null, null);
	}

	@Test
	public void testPostStringDirect() throws Exception{
		when(mockClient.post(any(SimpleHttpRequest.class), isNull()))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("content");
		assertEquals("content",
				baseClient.postStringDirect("https://repo-prod.prod.sagebase.org", "/entity", null));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteUriWithNullEndpoint() throws Exception{
		baseClient.deleteUri(null, "/entity");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteUriWithNullURL() throws Exception{
		baseClient.deleteUri("https://repo-prod.prod.sagebase.org", null);
	}

	@Test
	public void testDeleteUri() throws Exception{
		when(mockClient.delete(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		baseClient.deleteUri("https://repo-prod.prod.sagebase.org", "/entity");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPutJSONEntityWithNullEndpoint() throws Exception {
		baseClient.putJSONEntity(null, "url", null, EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPutJSONEntityWithNullURL() throws Exception {
		baseClient.putJSONEntity("endpoint", null, null, EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPutJSONEntityWithNullReturnClass() throws Exception {
		baseClient.putJSONEntity("endpoint", "url", null, null);
	}

	@Test
	public void testPutJSONEntity() throws Exception {
		when(mockClient.put(any(SimpleHttpRequest.class), isNull()))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{\"id\":\"0\"}");
		EntityId id = new EntityId();
		id.setId("0");
		assertEquals(id,
				baseClient.putJSONEntity("https://repo-prod.prod.sagebase.org",
						"/entityId", null, EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).put(captor.capture(), isNull());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId",
				captor.getValue().getUri());
	}


	@Test (expected = IllegalArgumentException.class)
	public void testVoidPutWithNullEndpoint() throws Exception {
		baseClient.voidPut(null, "url", null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testVoidPutWithNullURL() throws Exception {
		baseClient.voidPut("endpoint", null, null);
	}

	@Test
	public void testVoidPut() throws Exception {
		when(mockClient.put(any(SimpleHttpRequest.class), isNull()))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{\"id\":\"0\"}");
		baseClient.voidPut("https://repo-prod.prod.sagebase.org", "/entityId", null);
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).put(captor.capture(), isNull());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId",
				captor.getValue().getUri());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPostJSONEntityWithNullEndpoint() throws Exception {
		baseClient.postJSONEntity(null, "url", null, EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPostJSONEntityWithNullURL() throws Exception {
		baseClient.postJSONEntity("endpoint", null, null, EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testPostJSONEntityWithNullReturnClass() throws Exception {
		baseClient.postJSONEntity("endpoint", "url", null, null);
	}

	@Test
	public void testPostJSONEntity() throws Exception {
		when(mockClient.post(any(SimpleHttpRequest.class), isNull()))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{\"id\":\"0\"}");
		EntityId id = new EntityId();
		id.setId("0");
		assertEquals(id,
				baseClient.postJSONEntity("https://repo-prod.prod.sagebase.org",
						"/entityId", null, EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).post(captor.capture(), isNull());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId",
				captor.getValue().getUri());
	}


	@Test (expected = IllegalArgumentException.class)
	public void testVoidPostWithNullEndpoint() throws Exception {
		baseClient.voidPost(null, "url", null, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testVoidPostWithNullURL() throws Exception {
		baseClient.voidPost("endpoint", null, null, null);
	}

	@Test
	public void testVoidPost() throws Exception {
		when(mockClient.post(any(SimpleHttpRequest.class), isNull()))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{\"id\":\"0\"}");
		baseClient.voidPost("https://repo-prod.prod.sagebase.org", "/entityId", null, null);
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).post(captor.capture(), isNull());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId",
				captor.getValue().getUri());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetJSONEntityWithNullEndpoint() throws Exception {
		baseClient.getJSONEntity(null, "url", EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetJSONEntityWithNullURL() throws Exception {
		baseClient.getJSONEntity("endpoint", null, EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetJSONEntityWithNullReturnClass() throws Exception {
		baseClient.getJSONEntity("endpoint", "url", null);
	}

	@Test
	public void testGetJSONEntityWithNullJSONObject() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("");
		assertNull(baseClient.getJSONEntity("https://repo-prod.prod.sagebase.org",
						"/entityId", EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId",
				captor.getValue().getUri());
	}

	@Test
	public void testGetJSONEntity() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{\"id\":\"0\"}");
		EntityId id = new EntityId();
		id.setId("0");
		assertEquals(id,
				baseClient.getJSONEntity("https://repo-prod.prod.sagebase.org",
						"/entityId", EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId",
				captor.getValue().getUri());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetPaginatedResultsWithNullEndpoint() throws Exception {
		baseClient.getPaginatedResults(null, "url", EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetPaginatedResultsWithNullURL() throws Exception {
		baseClient.getPaginatedResults("endpoint", null, EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetPaginatedResultsWithNullReturnClass() throws Exception {
		baseClient.getPaginatedResults("endpoint", "url", null);
	}

	@Test
	public void testGetPaginatedResults() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{"
				+ "\"totalNumberOfResults\":\"0\","
				+ "\"results\":[],"
				+ "}");
		PaginatedResults<EntityId> results = new PaginatedResults<EntityId>();
		results.setTotalNumberOfResults(0);
		results.setResults(new LinkedList<EntityId>());
		assertEquals(results,
				baseClient.getPaginatedResults("https://repo-prod.prod.sagebase.org",
						"/entityId", EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId",
				captor.getValue().getUri());
	}


	@Test (expected = IllegalArgumentException.class)
	public void testGetPaginatedResultsByPostWithNullEndpoint() throws Exception {
		baseClient.getPaginatedResults(null, "url", new EntityId(), EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetPaginatedResultsByPostWithNullURL() throws Exception {
		baseClient.getPaginatedResults("endpoint", null, new EntityId(), EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetPaginatedResultsByPostWithNullRequestBody() throws Exception {
		baseClient.getPaginatedResults("endpoint", null, null, EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetPaginatedResultsByPostWithNullReturnClass() throws Exception {
		baseClient.getPaginatedResults("endpoint", "url", new EntityId(), null);
	}

	@Test
	public void testGetPaginatedResultsByPost() throws Exception {
		when(mockClient.post(any(SimpleHttpRequest.class), anyString()))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{"
				+ "\"totalNumberOfResults\":\"0\","
				+ "\"results\":[],"
				+ "}");
		PaginatedResults<EntityId> results = new PaginatedResults<EntityId>();
		results.setTotalNumberOfResults(0);
		results.setResults(new LinkedList<EntityId>());
		assertEquals(results,
				baseClient.getPaginatedResults("https://repo-prod.prod.sagebase.org",
						"/entityId", new EntityId(), EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).post(captor.capture(), anyString());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId",
				captor.getValue().getUri());
	}


	@Test (expected = IllegalArgumentException.class)
	public void testGetListOfJSONEntityWithNullEndpoint() throws Exception {
		baseClient.getListOfJSONEntity(null, "url", EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetListOfJSONEntityWithNullURL() throws Exception {
		baseClient.getListOfJSONEntity("endpoint", null, EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetListOfJSONEntityWithNullReturnClass() throws Exception {
		baseClient.getListOfJSONEntity("endpoint", "url", null);
	}

	@Test
	public void testGetListOfJSONEntity() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{"
				+ "\"concreteType\":\"org.sagebionetworks.repo.model.EntityId\","
				+ "\"list\":[],"
				+ "}");
		List<EntityId> results = new LinkedList<EntityId>();
		assertEquals(results,
				baseClient.getListOfJSONEntity("https://repo-prod.prod.sagebase.org",
						"/entityId", EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId",
				captor.getValue().getUri());
	}


	@Test (expected = IllegalArgumentException.class)
	public void testGetListOfJSONEntityByPostWithNullEndpoint() throws Exception {
		baseClient.getListOfJSONEntity(null, "url", new EntityId(), EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetListOfJSONEntityByPostWithNullURL() throws Exception {
		baseClient.getListOfJSONEntity("endpoint", null, new EntityId(), EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetListOfJSONEntityByPostWithNullRequestBody() throws Exception {
		baseClient.getListOfJSONEntity("endpoint", null, null, EntityId.class);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetListOfJSONEntityByPostWithNullReturnClass() throws Exception {
		baseClient.getListOfJSONEntity("endpoint", "url", new EntityId(), null);
	}

	@Test
	public void testGetListOfJSONEntityByPost() throws Exception {
		when(mockClient.post(any(SimpleHttpRequest.class), anyString()))
				.thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{"
				+ "\"concreteType\":\"org.sagebionetworks.repo.model.EntityId\","
				+ "\"list\":[],"
				+ "}");
		List<EntityId> results = new LinkedList<EntityId>();
		assertEquals(results,
				baseClient.getListOfJSONEntity("https://repo-prod.prod.sagebase.org",
						"/entityId", new EntityId(), EntityId.class));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).post(captor.capture(), anyString());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId",
				captor.getValue().getUri());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetBooleanResultWithNullEndpoint() throws Exception {
		baseClient.getBooleanResult(null, "url");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetBooleanResultWithNullURL() throws Exception {
		baseClient.getBooleanResult("endpoint", null);
	}

	@Test
	public void testGetBooleanResult() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		when(mockResponse.getContent()).thenReturn("{\"result\":\"true\"}");
		assertEquals(true,
				baseClient.getBooleanResult("https://repo-prod.prod.sagebase.org", "/entityId"));
		ArgumentCaptor<SimpleHttpRequest> captor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(captor.capture());
		assertEquals("https://repo-prod.prod.sagebase.org/entityId",
				captor.getValue().getUri());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testAddDigitalSignatureWithNullURL() throws Exception {
		baseClient.addDigitalSignature(null, new HashMap<String, String>());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testAddDigitalSignatureWithNullHeaders() throws Exception {
		baseClient.addDigitalSignature("https://repo-prod.prod.sagebase.org/entityId", null);
	}

	@Test
	public void testAddDigitalSignature() throws Exception {
		baseClient.setUsername("username");
		baseClient.setApiKey("apiKey");
		HashMap<String, String> headers = new HashMap<String, String>();
		baseClient.addDigitalSignature(
				"https://repo-prod.prod.sagebase.org/entityId", headers);
		assertEquals("username", headers.get(AuthorizationConstants.USER_ID_HEADER));
		assertNotNull(headers.get(AuthorizationConstants.SIGNATURE_TIMESTAMP));
		assertNotNull(headers.get(AuthorizationConstants.SIGNATURE));
	}

	@Test (expected = SynapseClientException.class)
	public void testPerformRequestWithRetryWithNullURL() throws Exception {
		baseClient.performRequestWithRetry(null, GET, null, null);
	}

	@Test (expected = SynapseClientException.class)
	public void testPerformRequestWithRetryWithNullMethod() throws Exception {
		baseClient.performRequestWithRetry(
				"https://repo-prod.prod.sagebase.org/entityId",
				null, null, null);
	}

	@Test
	public void testPerformRequestWithRetrySuccess() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(200);
		assertEquals(mockResponse, baseClient.performRequestWithRetry(
				"https://repo-prod.prod.sagebase.org/entityId",
				GET, null, null));
		verify(mockClient).get(any(SimpleHttpRequest.class));
	}

	@Test
	public void testPerformRequestWithoutRetryFailure() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(500);
		assertEquals(mockResponse, baseClient.performRequestWithRetry(
				"https://repo-prod.prod.sagebase.org/entityId",
				GET, null, null));
		verify(mockClient).get(any(SimpleHttpRequest.class));
	}

	@Test
	public void testPerformRequestWithRetryFailure() throws Exception {
		when(mockClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(503);
		try {
			baseClient.performRequestWithRetry(
					"https://repo-prod.prod.sagebase.org/entityId",
					GET, null, null);
			fail("expect SynapseServerException");
		} catch (SynapseServerException e) {
			// as expected
		}
		verify(mockClient, times(MAX_RETRY_SERVICE_UNAVAILABLE_COUNT))
				.get(any(SimpleHttpRequest.class));
	}

	@Test
	public void testSetSessionId(){
		//method under test
		baseClient.setSessionId(sessionIdVal);

		verify(mockClient).addCookie("repo-prod.prod.sagebase.org", "sessionID", sessionIdVal);
	}

	@Test
	public void testGetSessionId(){
		when(mockClient.getFirstCookieValue("repo-prod.prod.sagebase.org", "sessionID")).thenReturn(sessionIdVal);

		//method under test
		String result = baseClient.getSessionId();

		assertEquals(sessionIdVal, result);
		verify(mockClient).getFirstCookieValue("repo-prod.prod.sagebase.org", "sessionID");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSetRepositoryEndpoint_malformedRepoEndpoint(){
		//set a bad endpoint
		baseClient.setRepositoryEndpoint("asdf.asdf.asdf...");
	}

	@Test
	public void testSetRepositoryEndpoint(){
		String repoEndpoint = "https://my.test.endpoint.com/some/path";

		baseClient.setRepositoryEndpoint(repoEndpoint);

		assertEquals(repoEndpoint, baseClient.getRepoEndpoint());
		assertEquals("my.test.endpoint.com", baseClient.repoEndpointBaseDomain);
	}
}
