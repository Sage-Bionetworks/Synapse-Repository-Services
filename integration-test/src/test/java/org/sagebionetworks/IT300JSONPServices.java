package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseClient;
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
@ExtendWith(ITTestExtension.class)
public class IT300JSONPServices {

	private static SimpleHttpClient simpleHttpClient;
	
	private Team teamToDelete = null;
	
	private SynapseClient synapse;
	
	public IT300JSONPServices(SynapseClient synapse) {
		this.synapse = synapse;
	}
	
	@BeforeAll
	public static void beforeClass() throws Exception {
		simpleHttpClient = new SimpleHttpClientImpl();
	}
	
	@AfterEach
	public void cleanUpTeam() throws Exception {
		if (teamToDelete != null) {
			synapse.deleteTeam(teamToDelete.getId());
			teamToDelete = null;
		}
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
		assertTrue(responseBody.startsWith(expectedPrefix), "expected response starting with '"+expectedPrefix+"' but found "+responseBody);
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
		assertTrue(responseBody.startsWith(expectedPrefix), "expected response starting with '"+expectedPrefix+"' but found "+responseBody);
		assertTrue(responseBody.endsWith(expectedSuffix));
		String extractedJson = responseBody.substring(expectedPrefix.length(), responseBody.length()-2);
		// Make sure we can parse the results		
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(extractedJson);
		PaginatedResults<Team> results = PaginatedResults.createFromJSONObjectAdapter(adapter, Team.class);
		assertNotNull(results.getTotalNumberOfResults());
	}
}
