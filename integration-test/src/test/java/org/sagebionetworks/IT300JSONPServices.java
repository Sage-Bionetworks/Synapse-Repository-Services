package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

/**
 * Tests for JSONP supported methods.
 * @author John
 *
 */
public class IT300JSONPServices {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	private static SimpleHttpClient simpleHttpClient;
	
	private Team teamToDelete = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
		simpleHttpClient = new SimpleHttpClientImpl();
	}
	@Before
	public void before() throws SynapseException{
		adminSynapse.clearAllLocks();
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
	public void testTeamJSONP() throws Exception {
		// Make a simple call to the repository service 
		StringBuilder urlBuilder = new StringBuilder(StackConfigurationSingleton.singleton().getRepositoryServiceEndpoint());
		String callbackName = "parseMe";
		urlBuilder.append("/teams"+"?callback="); 
		urlBuilder.append(callbackName);
		
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(urlBuilder.toString());
		SimpleHttpResponse response = simpleHttpClient.get(request);
		assertNotNull(response);
		assertEquals(200, response.getStatusCode());
		String responseBody = response.getContent();
		String expectedPrefix = callbackName+"(";
		String expectedSuffix = ");";
		assertTrue("expected response starting with '"+expectedPrefix+"' but found "+responseBody, responseBody.startsWith(expectedPrefix));
		assertTrue(responseBody.endsWith(expectedSuffix));
		String extractedJson = responseBody.substring(expectedPrefix.length(), responseBody.length()-2);
		// Make sure we can parse the results	
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(extractedJson);
		PaginatedResults<Team> results = PaginatedResults.createFromJSONObjectAdapter(adapter, Team.class);
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
		StringBuilder urlBuilder = new StringBuilder(StackConfigurationSingleton.singleton().getRepositoryServiceEndpoint());
		String callbackName = "parseMe";
		String teamId = makeATeam();
		urlBuilder.append("/teamMembers/"+teamId+"?callback="); 
		urlBuilder.append(callbackName);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(urlBuilder.toString());
		SimpleHttpResponse response = simpleHttpClient.get(request);
		assertNotNull(response);
		assertEquals(200, response.getStatusCode());
		String responseBody = response.getContent();
		String expectedPrefix = callbackName+"(";
		String expectedSuffix = ");";
		assertTrue("expected response starting with '"+expectedPrefix+"' but found "+responseBody, responseBody.startsWith(expectedPrefix));
		assertTrue(responseBody.endsWith(expectedSuffix));
		String extractedJson = responseBody.substring(expectedPrefix.length(), responseBody.length()-2);
		// Make sure we can parse the results		
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(extractedJson);
		PaginatedResults<Team> results = PaginatedResults.createFromJSONObjectAdapter(adapter, Team.class);
		assertNotNull(results.getTotalNumberOfResults());
	}
}
