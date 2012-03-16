package org.sagebionetworks;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.HttpClientProvider;
import org.sagebionetworks.client.HttpClientProviderImpl;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;

public class IT960TermsOfUse {
	private static Synapse synapse = null;
	private static String authEndpoint = null;
	private static String repoEndpoint = null;

	@BeforeClass
	public static void beforeClass() throws Exception {

		authEndpoint = StackConfiguration.getAuthenticationServicePrivateEndpoint();
		repoEndpoint = StackConfiguration.getRepositoryServiceEndpoint();
		synapse = new Synapse();
		synapse.setAuthEndpoint(authEndpoint);
		synapse.setRepositoryEndpoint(repoEndpoint);
		synapse.login(StackConfiguration.getIntegrationTestUserThreeName(),
				StackConfiguration.getIntegrationTestUserThreePassword());
	}
	
	// make sure that after the test suite is done running the user has signed the Terms of Use
	@AfterClass
	public static void afterClass() throws Exception {
		JSONObject agreement = new JSONObject();
		agreement.put("agrees", true);
		synapse.createAuthEntity("/termsOfUseAgreement", agreement);
		
	}

	public void testGetTermsOfUse() throws Exception {
		HttpClientProvider clientProvider = new HttpClientProviderImpl();
		String requestUrl = authEndpoint+"/termsOfUse";
		String requestMethod = "GET";
		HttpResponse response = clientProvider.performRequest(requestUrl, requestMethod, null, null);
		String responseBody = (null != response.getEntity()) ? EntityUtils
				.toString(response.getEntity()) : null;
		assertTrue(responseBody.length()>100);
	}
	
	@Test
	public void testSetAndGetAgreement() throws Exception {
		JSONObject agreement = new JSONObject();
		agreement.put("agrees", false);
		
		synapse.createAuthEntity("/termsOfUseAgreement", agreement);
		
		JSONObject agreement2 = synapse.getSynapseEntity(authEndpoint, "/termsOfUseAgreement");

		assertEquals(agreement.getBoolean("agrees"), agreement2.getBoolean("agrees"));
		
		agreement.put("agrees", true);
		
		synapse.createAuthEntity("/termsOfUseAgreement", agreement);
		
		agreement2 = synapse.getSynapseEntity(authEndpoint, "/termsOfUseAgreement");

		assertEquals(agreement.getBoolean("agrees"), agreement2.getBoolean("agrees"));
		
	}
	
	@Test
	public void testFilterWithTermsOfUse() throws Exception {
		// make sure the terms of use have been signed
		JSONObject agreement = new JSONObject();
		agreement.put("agrees", true);
		synapse.createAuthEntity("/termsOfUseAgreement", agreement);
		// now we can make authenticated requests
		synapse.getEntity("/dataset");
	}

	@Test(expected=SynapseForbiddenException.class)
	public void testFilterNoTermsOfUse() throws Exception {
		Synapse notou = new Synapse();
		notou.setAuthEndpoint(authEndpoint);
		notou.setRepositoryEndpoint(repoEndpoint);
		notou.login(StackConfiguration.getIntegrationTestRejectTermsOfUseName(),
				StackConfiguration.getIntegrationTestRejectTermsOfUsePassword());
		
		// make sure the terms of use have NOT been signed
		JSONObject agreement = new JSONObject();
		agreement.put("agrees", false);
		notou.createAuthEntity("/termsOfUseAgreement", agreement);
		// should throw '403' (Forbidden)
		notou.getEntity("/dataset");  
	}
	

}
