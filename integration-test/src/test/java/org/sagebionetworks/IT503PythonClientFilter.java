package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SharedClientConnection;
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
import org.sagebionetworks.utils.MD5ChecksumHelper;



public class IT503PythonClientFilter {
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static Long user1ToDelete;

	private static String oneId;

	private Project project;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create 2 users
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);
		user1ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);
		
		oneId = synapseOne.getMyProfile().getOwnerId();
		
	}
	
	@SuppressWarnings("serial")
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
		SharedClientConnection conn = synapseOne.getSharedClientConnection();
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
		HttpResponse response = conn.performRequest(endpoint+uri, "GET", null, requestHeaders);
		String body = checkNoCharEncoding(response, 200);
		assertTrue(body.length()>0);

		// test user agent string with dev tag
		requestHeaders.put("User-Agent", "synapseclient/1.0.dev1 python-requests/2.4.0 cpython/2.7.6");
		response = conn.performRequest(endpoint+uri, "GET", null, requestHeaders);
		body = checkNoCharEncoding(response, 200);
		assertTrue(body.length()>0);

		// test unparsable synapse client version number
		requestHeaders.put("User-Agent", "synapseclient/1.unparsable.junk python-requests/2.4.0 cpython/2.7.6");
		response = conn.performRequest(endpoint+uri, "GET", null, requestHeaders);
		assertEquals(200, response.getStatusLine().getStatusCode());
	}
	
	private String checkNoCharEncoding(HttpResponse response, int expectedStatus) throws Exception {
		HttpEntity responseEntity = response.getEntity();
		int statusCode = response.getStatusLine().getStatusCode();
		// check that the response header does not have a character encoding
		Header contentTypeHeader = responseEntity.getContentType();
		String contentTypeString = contentTypeHeader.getValue();
		ContentType contentType = ContentType.parse(contentTypeString);
		String responseBody = IOUtils.toString(responseEntity.getContent(), "ISO-8859-1");
		System.out.println(responseBody);
		assertEquals(expectedStatus, statusCode);
		assertNull("Content-Type: "+contentTypeString, contentType.getCharset());
		return responseBody;
	}
	
	@Test
	public void testFileServices() throws Exception {
		// get the underlying SharedClientConnection so we can 'roll our own' request
		SharedClientConnection conn = synapseOne.getSharedClientConnection();
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
		HttpResponse response = conn.performRequest(endpoint+uri, "POST", requestBody, requestHeaders);
		String body = checkNoCharEncoding(response, 201);
		assertTrue(body.length()>0);
		ChunkedFileToken token = EntityFactory.createEntityFromJSONString(body, ChunkedFileToken.class);
		
		ChunkRequest chunkRequest = new ChunkRequest();
		chunkRequest.setChunkNumber(1L);
		chunkRequest.setChunkedFileToken(token);

		requestBody = EntityFactory.createJSONStringForEntity(chunkRequest);
		uri = "/createChunkedFileUploadChunkURL";
		response = conn.performRequest(endpoint+uri, "POST", requestBody, requestHeaders);
		body = checkNoCharEncoding(response, 201);
		assertTrue(body.length()>0);
	}

}
