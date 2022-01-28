package org.sagebionetworks;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants;

/**
 * Test to validate a cookie can be used to authenticate.
 * @author jmhill
 *
 */
public class IT045CookieAuthentication extends BaseITTest {
	
	private static Cookie cookie;
	
	@BeforeAll
	public static void beforeClass() throws Exception {
		cookie = new BasicClientCookie(AuthorizationConstants.SESSION_TOKEN_COOKIE_NAME, synapse.getAccessToken());
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

}
