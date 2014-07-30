package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserSessionData;



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
		requestHeaders.put("User-Agent", "python-request/foo");
		UserSessionData userSessionData = synapseOne.getUserSessionData();
		String sessionToken = userSessionData.getSession().getSessionToken();
		requestHeaders.put("sessionToken", sessionToken);
		HttpResponse response = conn.performRequest(endpoint+uri, "GET", null, requestHeaders);
		HttpEntity responseEntity = response.getEntity();
		int statusCode = response.getStatusLine().getStatusCode();
		assertEquals(200, statusCode);
		// check that the response header does not have a character encoding
		Header contentTypeHeader = responseEntity.getContentType();
		String contentTypeString = contentTypeHeader.getValue();
		ContentType contentType = ContentType.parse(contentTypeString);
		assertNull("Content-Type: "+contentTypeString, contentType.getCharset());
	}

}
