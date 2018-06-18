package org.sagebionetworks;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.repo.model.AuthorizationConstants;

/**
 * Test to validate a cookie can be used to authenticate.
 * @author jmhill
 *
 */
public class IT045CookieAuthentication {
	
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private static Cookie cookie;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
		
		cookie = new BasicClientCookie(AuthorizationConstants.SESSION_TOKEN_COOKIE_NAME, synapse.getCurrentSessionToken());
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		adminSynapse.deleteUser(userToDelete);
	}
	
	@Test
	public void testFileCookieAutheticate(){
	    // Create a local instance of cookie store
	    CookieStore cookieStore = new BasicCookieStore();
	     
	    // Create local HTTP context
	    HttpContext localContext = new BasicHttpContext();
	    // Bind custom cookie store to the local context
	    localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	    cookieStore.addCookie(cookie);
	    new HttpGet("http://www.google.com/"); 
		
	}

	@Test
	public void testRepoCookieAuthenticate(){
		//TODO Can we make a repository call with a cookie
	}
}
