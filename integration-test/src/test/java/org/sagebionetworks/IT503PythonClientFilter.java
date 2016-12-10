package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.simpleHttpClient.Header;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.sagebionetworks.utils.MD5ChecksumHelper;



public class IT503PythonClientFilter {
	private static SimpleHttpClient simpleClient;
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static Long user1ToDelete;

	private Project project;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create 2 users
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);
		user1ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);
		
		simpleClient = new SimpleHttpClientImpl();
	}
	
	@Before
	public void before() throws Exception {
	}

	@After
	public void after() throws Exception {
		if (project != null) {
			try {
				adminSynapse.deleteAndPurgeEntityById(project.getId());
			} catch (SynapseException e) { }
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {	
		try {
			adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseException e) { }

	}
	
	private static Random rand = new Random();
	
	@Test
	public void testPythonClientFilter() throws Exception {
		// user synapseOne to create a Project
		project = new Project();
		project.setName("TestPythonClientFilter_"+rand.nextInt(1000));
		project = synapseOne.createEntity(project);
		
		// get the underlying SharedClientConnection so we can 'roll our own' request
		String endpoint = StackConfiguration.getRepositoryServiceEndpoint();
		String uri = "/entity/"+project.getId();
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Accept", "application/json");
		// before issuing the request, set the User-Agent to indicate the affected Python client
		// Per Chris, if there's 'python-request' in the string and no 'synapseclient' then it's python client <0.5 and affected
		requestHeaders.put("User-Agent", "python-requests/foo");
		UserSessionData userSessionData = synapseOne.getUserSessionData();
		String sessionToken = userSessionData.getSession().getSessionToken();
		requestHeaders.put("sessionToken", sessionToken);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(endpoint+uri);
		request.setHeaders(requestHeaders);
		SimpleHttpResponse response = simpleClient.get(request);
		String body = checkNoCharEncoding(response, 200);
		assertTrue(body.length()>0);

		// test user agent string with dev tag
		requestHeaders.put("User-Agent", "synapseclient/1.0.dev1 python-requests/2.4.0 cpython/2.7.6");
		request.setHeaders(requestHeaders);
		response = simpleClient.get(request);
		body = checkNoCharEncoding(response, 200);
		assertTrue(body.length()>0);

		// test unparsable synapse client version number
		requestHeaders.put("User-Agent", "synapseclient/1.unparsable.junk python-requests/2.4.0 cpython/2.7.6");
		request.setHeaders(requestHeaders);
		response = simpleClient.get(request);
		assertEquals(200, response.getStatusCode());
	}
	
	private String checkNoCharEncoding(SimpleHttpResponse response, int expectedStatus) throws Exception {
		int statusCode = response.getStatusCode();
		// check that the response header does not have a character encoding
		Header contentTypeHeader = response.getFirstHeader("Content-Type");
		String contentTypeString = contentTypeHeader.getValue();
		ContentType contentType = ContentType.parse(contentTypeString);
		String responseBody = response.getContent();
		System.out.println(responseBody);
		assertEquals(expectedStatus, statusCode);
		assertNull("Content-Type: "+contentTypeString, contentType.getCharset());
		return responseBody;
	}
	
	@Test
	public void testFileServices() throws Exception {
		String endpoint = StackConfiguration.getFileServiceEndpoint();
		String uri = "/createChunkedFileUploadToken";
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Accept", "application/json");
		// before issuing the request, set the User-Agent to indicate the affected Python client
		// Per Chris, if there's 'python-request' in the string and no 'synapseclient' then it's python client <0.5 and affected
		requestHeaders.put("User-Agent", "python-requests/foo");
		UserSessionData userSessionData = synapseOne.getUserSessionData();
		String sessionToken = userSessionData.getSession().getSessionToken();
		requestHeaders.put("sessionToken", sessionToken);
		requestHeaders.put("Content-Type", "application/json");
		
		ContentType fileContentType = ContentType.parse("text/plain;charset=UTF-8");
		String content = "my dog has fleas"; // content of a file to upload
		String contentMD5 = null;
		try {
			contentMD5 = MD5ChecksumHelper.getMD5ChecksumForByteArray(content.getBytes());
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}
    	 
		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		ccftr.setFileName("content");
		ccftr.setContentType(fileContentType.toString());
		ccftr.setContentMD5(contentMD5);

		String requestBody = EntityFactory.createJSONStringForEntity(ccftr);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(endpoint+uri);
		request.setHeaders(requestHeaders);
		SimpleHttpResponse response = simpleClient.post(request, requestBody);
		String body = checkNoCharEncoding(response, 201);
		assertTrue(body.length()>0);
		ChunkedFileToken token = EntityFactory.createEntityFromJSONString(body, ChunkedFileToken.class);
		
		ChunkRequest chunkRequest = new ChunkRequest();
		chunkRequest.setChunkNumber(1L);
		chunkRequest.setChunkedFileToken(token);

		requestBody = EntityFactory.createJSONStringForEntity(chunkRequest);
		uri = "/createChunkedFileUploadChunkURL";
		request.setUri(endpoint+uri);
		response = simpleClient.post(request, requestBody);
		body = checkNoCharEncoding(response, 201);
		assertTrue(body.length()>0);
	}

}
