package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseClientImpl;


/**
 * CrowdAuthUtil
 */

public class IT980ResourceAccess {
	private static SynapseClientImpl synapse = null;
	private static String authEndpoint = null;
	private static String repoEndpoint = null;
	
	private static final String RESOURCE_NAME = "TestResource";
	private static final String USER_DATA = "{\"foo\":\"bar\"}";

	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		authEndpoint = StackConfiguration.getAuthenticationServicePrivateEndpoint();
		repoEndpoint = StackConfiguration.getRepositoryServiceEndpoint();
		synapse = new SynapseClientImpl();
		synapse.setRepositoryEndpoint(StackConfiguration.getRepositoryServiceEndpoint());
	}
	
	@Test
	public void testCreateResourceAccess() throws Exception {
		synapse.setAuthEndpoint(authEndpoint);
		synapse.setRepositoryEndpoint(repoEndpoint);
		synapse.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());
		String resourceUserName = StackConfiguration.getIntegrationTestUserOneName();
		JSONObject obj = new JSONObject();
		obj.put("userName", resourceUserName);
		obj.put("userData", USER_DATA);
		synapse.createJSONObjectEntity(authEndpoint, "/resourceAccess/"+RESOURCE_NAME, obj);
		synapse.deleteUri(authEndpoint, "/resourceAccess/"+RESOURCE_NAME+
				"?resourceUserName="+URLEncoder.encode(resourceUserName, "UTF-8"));
	}
	
	@Test
	public void testCreateAndGetResourceSession() throws Exception {
		synapse.setAuthEndpoint(authEndpoint);
		synapse.setRepositoryEndpoint(repoEndpoint);
		// log in as admin
		synapse.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());
		String resourceUserName = StackConfiguration.getIntegrationTestUserOneName();
		JSONObject obj = new JSONObject();
		obj.put("userName", resourceUserName);
		obj.put("userData", USER_DATA);
		synapse.createJSONObjectEntity(authEndpoint, "/resourceAccess/"+RESOURCE_NAME, obj);
		
		// now create the session
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
		JSONObject empty = new JSONObject();
		JSONObject session = synapse.createJSONObjectEntity(authEndpoint, "/resourceSession/"+RESOURCE_NAME, empty);
		assertTrue(session.has("resourceAccessToken"));
		String resourceAccessToken = session.getString("resourceAccessToken");
		
		// get the session:  can be done by another user (e.g. some service account) 
		SynapseClientImpl secondUser = new SynapseClientImpl();
		secondUser.setAuthEndpoint(authEndpoint);
		secondUser.setRepositoryEndpoint(repoEndpoint);
		secondUser.login(StackConfiguration.getIntegrationTestUserTwoName(),
				StackConfiguration.getIntegrationTestUserTwoPassword());
		JSONObject userData = secondUser.getSynapseEntity(authEndpoint, "/resourceSession/"+resourceAccessToken);
		assertTrue(userData.has("userName"));
		assertEquals(StackConfiguration.getIntegrationTestUserOneName(), userData.getString("userName"));
		assertTrue(userData.has("userData"));
		assertEquals(USER_DATA, userData.getString("userData"));
		
		// finally, delete the resource-access record
		synapse.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());
		synapse.deleteUri(authEndpoint, "/resourceAccess/"+RESOURCE_NAME+
				"?resourceUserName="+URLEncoder.encode(resourceUserName, "UTF-8"));
	}
	

}
