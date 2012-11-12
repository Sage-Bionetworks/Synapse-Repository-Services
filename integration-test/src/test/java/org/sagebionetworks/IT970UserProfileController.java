package org.sagebionetworks;

import java.util.logging.Logger;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;

public class IT970UserProfileController {
	private static final Logger log = Logger.getLogger(IT970UserProfileController.class.getName());
	
	private static Synapse synapse = null;
	private static String repoEndpoint = null;
	private static String authEndpoint = null;	

	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		authEndpoint = StackConfiguration.getAuthenticationServicePrivateEndpoint();
		repoEndpoint = StackConfiguration.getRepositoryServiceEndpoint();
		synapse = new Synapse();
	}
	
	@Test
	public void testGetAndUpdateOwnUserProfile() throws Exception {
		synapse.setAuthEndpoint(authEndpoint);
		synapse.setRepositoryEndpoint(repoEndpoint);
		synapse.login(StackConfiguration.getIntegrationTestUserTwoName(),
				StackConfiguration.getIntegrationTestUserTwoPassword());
		JSONObject userProfile = synapse.getSynapseEntity(repoEndpoint, "/userProfile");
		System.out.println(userProfile);
		// now update the fields
		userProfile.put("firstName", "foo");
		userProfile.put("lastName", "bar");
		synapse.putJSONObject("/userProfile", userProfile, null);
	}
	

}
