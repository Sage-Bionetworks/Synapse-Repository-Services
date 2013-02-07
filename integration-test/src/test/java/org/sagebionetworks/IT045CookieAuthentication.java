package org.sagebionetworks;

import java.io.File;


import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.HttpClientProviderImpl;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;

/**
 * Test to validate a cookie can be used to authenticate.
 * @author jmhill
 *
 */
public class IT045CookieAuthentication {
	
	private static Synapse synapse = null;
	private static UserSessionData session;
	private static Cookie cookie;
	File imageFile;
	
	private static Synapse createSynapseClient(String user, String pw) throws SynapseException {
		Synapse synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.setFileEndpoint(StackConfiguration.getFileServiceEndpoint());
		session =synapse.login(user, pw);
		cookie = new BasicClientCookie(AuthorizationConstants.SESSION_TOKEN_COOKIE_NAME, session.getSessionToken());
		return synapse;
	}
	
	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		synapse = createSynapseClient(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
	}
	
	@Test
	public void testFileCookieAutheticate(){
		// Use the cookie to upload a file
		HttpClient client = DefaultHttpClientSingleton.getInstance();
	    // Create a local instance of cookie store
	    CookieStore cookieStore = new BasicCookieStore();
	     
	    // Create local HTTP context
	    HttpContext localContext = new BasicHttpContext();
	    // Bind custom cookie store to the local context
	    localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	    cookieStore.addCookie(cookie);
	    HttpGet httpget = new HttpGet("http://www.google.com/"); 
		
	}

	@Test
	public void testRepoCookieAuthenticate(){
		// Can we make a repository call with a cookie
		
		
	}
}
