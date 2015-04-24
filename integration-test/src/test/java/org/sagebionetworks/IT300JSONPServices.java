package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptResponsePage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;

/**
 * Tests for JSONP supported methods.
 * @author John
 *
 */
public class IT300JSONPServices {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private Team teamToDelete = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}
	
	@After
	public void cleanUpTeam() throws Exception {
		if (teamToDelete != null) {
			synapse.deleteTeam(teamToDelete.getId());
			teamToDelete = null;
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		adminSynapse.deleteUser(userToDelete);
	}
	
	@Test
	public void testGetChildrenTransitiveJSONP() throws ClientProtocolException, IOException, JSONObjectAdapterException{
		// Make a simple call to the repository service 
		StringBuilder urlBuilder = new StringBuilder(StackConfiguration.getRepositoryServiceEndpoint());
		String callbackName = "parseMe";
		urlBuilder.append("/concept/11291/childrenTransitive?callback="); 
		urlBuilder.append(callbackName);
		HttpResponse response = DefaultHttpClientSingleton.getInstance().execute(new HttpGet(urlBuilder.toString()));
		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertNotNull(response.getEntity());
		String responseBody = EntityUtils.toString(response.getEntity());
		String expectedPrefix = callbackName+"(";
		String expectedSuffix = ");";
		assertTrue(responseBody.startsWith(expectedPrefix));
		assertTrue(responseBody.endsWith(expectedSuffix));
		String extractedJson = responseBody.substring(expectedPrefix.length(), responseBody.length()-2);
		// Make sure we can parse the results
		ConceptResponsePage crp = EntityFactory.createEntityFromJSONString(extractedJson, ConceptResponsePage.class);
		assertNotNull(crp);
		assertEquals("http://synapse.sagebase.org/ontology#11291", crp.getParentConceptUri());
	}
	
	@Test
	public void testGetConceptJSONP() throws ClientProtocolException, IOException, JSONObjectAdapterException{
		// Make a simple call to the repository service 
		StringBuilder urlBuilder = new StringBuilder(StackConfiguration.getRepositoryServiceEndpoint());
		String callbackName = "parseMe";
		urlBuilder.append("/concept/11291?callback="); 
		urlBuilder.append(callbackName);
		HttpResponse response = DefaultHttpClientSingleton.getInstance().execute(new HttpGet(urlBuilder.toString()));
		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertNotNull(response.getEntity());
		String responseBody = EntityUtils.toString(response.getEntity());
		String expectedPrefix = callbackName+"(";
		String expectedSuffix = ");";
		assertTrue(responseBody.startsWith(expectedPrefix));
		assertTrue(responseBody.endsWith(expectedSuffix));
		String extractedJson = responseBody.substring(expectedPrefix.length(), responseBody.length()-2);
		// Make sure we can parse the results
		Concept crp = EntityFactory.createEntityFromJSONString(extractedJson, Concept.class);
		assertNotNull(crp);
		assertEquals("http://synapse.sagebase.org/ontology#11291", crp.getUri());
	}
	
	@Ignore // need to figure our how to do this right
	@Test
	public void testIllegalHeaderJSONP() throws ClientProtocolException, IOException, JSONObjectAdapterException{
		// Make a simple call to the repository service 
		StringBuilder urlBuilder = new StringBuilder(StackConfiguration.getRepositoryServiceEndpoint());
		String callbackName = "parseMe";
		urlBuilder.append("/concept/11291?callback="); 
		urlBuilder.append(callbackName);
		HttpRequestBase request = new HttpGet(urlBuilder.toString());
		// adding a session token to the request is not allowed.
		request.setHeader("sessionToken", "not allowed");
		HttpResponse response = DefaultHttpClientSingleton.getInstance().execute(request);
		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertNotNull(response.getEntity());
		String responseBody = EntityUtils.toString(response.getEntity());
		String expectedPrefix = callbackName+"(";
		String expectedSuffix = ");";
		assertTrue(responseBody.startsWith(expectedPrefix));
		assertTrue(responseBody.endsWith(expectedSuffix));
		String extractedJson = responseBody.substring(expectedPrefix.length(), responseBody.length()-2);
		// Make sure we can parse the results
		Concept crp = EntityFactory.createEntityFromJSONString(extractedJson, Concept.class);
		assertNotNull(crp);
		assertEquals("http://synapse.sagebase.org/ontology#11291", crp.getUri());
	}

	@Test
	public void testTeamJSONP() throws Exception {
		// Make a simple call to the repository service 
		StringBuilder urlBuilder = new StringBuilder(StackConfiguration.getRepositoryServiceEndpoint());
		String callbackName = "parseMe";
		urlBuilder.append("/teams"+"?callback="); 
		urlBuilder.append(callbackName);
		HttpResponse response = DefaultHttpClientSingleton.getInstance().execute(new HttpGet(urlBuilder.toString()));
		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertNotNull(response.getEntity());
		String responseBody = EntityUtils.toString(response.getEntity());
		String expectedPrefix = callbackName+"(";
		String expectedSuffix = ");";
		assertTrue("expected response starting with '"+expectedPrefix+"' but found "+responseBody, responseBody.startsWith(expectedPrefix));
		assertTrue(responseBody.endsWith(expectedSuffix));
		String extractedJson = responseBody.substring(expectedPrefix.length(), responseBody.length()-2);
		// Make sure we can parse the results		
		JSONObject jsonObj = new JSONObject(extractedJson);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Team> results = new PaginatedResults<Team>(Team.class);
		results.initializeFromJSONObject(adapter);
		assertNotNull(results.getTotalNumberOfResults());
	}
	
	private String makeATeam() throws Exception {
		String name = "IT300-Test-Team-Name";
		String description = "Test-Team-Description";
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapse.createTeam(team);
		teamToDelete = createdTeam;
		return createdTeam.getId();
	}

	@Test
	public void testTeamMembershipJSONP() throws Exception {
		// Make a simple call to the repository service 
		StringBuilder urlBuilder = new StringBuilder(StackConfiguration.getRepositoryServiceEndpoint());
		String callbackName = "parseMe";
		String teamId = makeATeam();
		urlBuilder.append("/teamMembers/"+teamId+"?callback="); 
		urlBuilder.append(callbackName);
		HttpResponse response = DefaultHttpClientSingleton.getInstance().execute(new HttpGet(urlBuilder.toString()));
		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertNotNull(response.getEntity());
		String responseBody = EntityUtils.toString(response.getEntity());
		String expectedPrefix = callbackName+"(";
		String expectedSuffix = ");";
		assertTrue("expected response starting with '"+expectedPrefix+"' but found "+responseBody, responseBody.startsWith(expectedPrefix));
		assertTrue(responseBody.endsWith(expectedSuffix));
		String extractedJson = responseBody.substring(expectedPrefix.length(), responseBody.length()-2);
		// Make sure we can parse the results		
		JSONObject jsonObj = new JSONObject(extractedJson);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<TeamMember> results = new PaginatedResults<TeamMember>(TeamMember.class);
		results.initializeFromJSONObject(adapter);
		assertNotNull(results.getTotalNumberOfResults());
	}
}
