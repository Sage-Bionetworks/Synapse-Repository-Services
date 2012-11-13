package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.UserSessionData;
import org.springframework.http.HttpStatus;


/**
 * CrowdAuthUtil
 */

public class IT990CrowdAuthentication {
	private static Synapse synapse = null;
	private static String authEndpoint = null;
	private static String repoEndpoint = null;
	/**
	 * @throws Exception
	 * 
	 */
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

	private static final String SESSION_TOKEN_LABEL = "sessionToken";
	
	@Test
	public void testCreateSession() throws Exception {
		JSONObject loginRequest = new JSONObject();
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		loginRequest.put("email", username);
		loginRequest.put("password", password);

		JSONObject session = synapse.createAuthEntity("/session", loginRequest);
		assertTrue(session.has(SESSION_TOKEN_LABEL));
	}
	
	@Test
	public void testCreateSessionSigningTermsOfUse() throws Exception {
		JSONObject loginRequest = new JSONObject();
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		loginRequest.put("email", username);
		loginRequest.put("password", password);

		JSONObject session = synapse.createAuthEntity("/session?acceptsTermsOfUse=true", loginRequest);
		assertTrue(session.has(SESSION_TOKEN_LABEL));
	}
	
	@Test(expected = SynapseBadRequestException.class)
	public void testCreateSessionBadCredentials() throws Exception {
		JSONObject loginRequest = new JSONObject();
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		loginRequest.put("email", username);
		loginRequest.put("password", "incorrectPassword");
	
		// should throw SynapseBadRequestException
		synapse.createAuthEntity("/session", loginRequest);

	}
	
	@Test(expected = SynapseForbiddenException.class)
	public void testCreateSessionNoTermsOfUse() throws Exception {
		JSONObject loginRequest = new JSONObject();
		String username = StackConfiguration.getIntegrationTestRejectTermsOfUseEmail();
		String password = StackConfiguration.getIntegrationTestRejectTermsOfUsePassword();
		loginRequest.put("email", username);
		loginRequest.put("password", password);
	
		// should throw SynapseBadRequestException
		synapse.createAuthEntity("/session", loginRequest);

	}
	
	
	@Test
	public void testRevalidateSvc() throws Exception {
		// start session
		JSONObject loginRequest = new JSONObject();
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		loginRequest.put("email", username);
		loginRequest.put("password", password);

		JSONObject session = synapse.createAuthEntity("/session", loginRequest);
		assertTrue(session.has(SESSION_TOKEN_LABEL));
		String token = session.getString(SESSION_TOKEN_LABEL);
		// revalidate
		session = new JSONObject();
		session.put(SESSION_TOKEN_LABEL, token);
		synapse.putJSONObject(authEndpoint, "/session", session, new HashMap<String,String>());
	}
	
	@Test(expected=SynapseNotFoundException.class)
	public void testRevalidateBadTokenSvc() throws Exception {
		// start session
		JSONObject loginRequest = new JSONObject();
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		loginRequest.put("email", username);
		loginRequest.put("password", password);

		JSONObject session = synapse.createAuthEntity("/session", loginRequest);
		assertTrue(session.has(SESSION_TOKEN_LABEL));
		String token = session.getString(SESSION_TOKEN_LABEL);
		assertNotNull(token);
		// revalidate
		session = new JSONObject();
		session.put(SESSION_TOKEN_LABEL, "invalid-session-token");
		
		synapse.putJSONObject(authEndpoint, "/session", session, new HashMap<String,String>());
	}
	
	@Test
	public void testCreateSessionThenLogout() throws Exception {
		// start session
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();

		synapse.login(username, password);
		// logout
		synapse.deleteUri(authEndpoint, "/session");
	}
	
	
	@Test(expected = SynapseBadRequestException.class)
	public void testCreateExistingUser() throws Exception {	
		// start session
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();

		synapse.login(username, password);
		JSONObject userRequest = new JSONObject();

		userRequest.put("email", username);
		userRequest.put("firstName", "dev");
		userRequest.put("lastName", "usr");
		userRequest.put("displayName", "dev usr");

		// expect exception
		synapse.createAuthEntity("/user", userRequest);
	}
	
	@Ignore
	@Test
	public void testCreateNewUser() throws Exception {
		// delete the user
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		synapse.login(username, password);
		// this says 'delete me'.  It only works for the specified integration test user
		synapse.deleteUri(authEndpoint, "/user");
		
		// verify that can't log in
		// expect exception
		try {
			synapse.login(username, password);
			fail("exception expected");
		} catch (SynapseBadRequestException e) {
			// as expected
		}
		
		// now recreate it
		JSONObject userRequest = new JSONObject();

		userRequest.put("email", username);
		userRequest.put("password", password);
		userRequest.put("firstName", "dev");
		userRequest.put("lastName", "usr3");
		userRequest.put("displayName", "dev usr3");

		synapse.createAuthEntity("/user", userRequest);
		
		// now log in to verify it's there
		synapse.login(username, password);
	}
	
	
	@Test
	public void testGetUser() throws Exception {
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		synapse.login(username, StackConfiguration.getIntegrationTestUserThreePassword());
		JSONObject user = synapse.getSynapseEntity(authEndpoint, "/user");
		assertEquals(StackConfiguration.getIntegrationTestUserThreeEmail(), user.getString("email"));
		assertEquals("dev", user.getString("firstName"));
		assertEquals("usr3", user.getString("lastName"));
		assertEquals("dev usr3", user.getString("displayName"));
	}
	
	@Test
	public void testCreateUserAndChangePassword() throws Exception {
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		synapse.login(username, password);

		String testNewPassword = "newPassword";
		JSONObject obj = new JSONObject();
		obj.put("newPassword", testNewPassword);
		synapse.createAuthEntity("/userPassword", obj);
		
		// to check the password, we have to try to log-in:
		// logout...
		synapse.deleteUri(authEndpoint, "/session");
		// ... login
		synapse.login(username, testNewPassword);
		
		// restore original password
		obj = new JSONObject();
		obj.put("newPassword", password);
		synapse.createAuthEntity("/userPassword", obj);
		
	}
	
	// can't expect to do this regularly, as it generates email messages
	@Ignore
	@Test
	public void testSendResetPasswordEmail() throws Exception {
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		synapse.login(username, password);

		JSONObject obj = new JSONObject();
		obj.put("email", username);
		synapse.createAuthEntity("/userPasswordEmail", obj);
	}
	
	
	// can't expect to do this regularly, as it generates email messages
	@Ignore
	@Test
	public void testSetAPIPasswordEmail() throws Exception {
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		synapse.login(username, password);

		JSONObject obj = new JSONObject();
		obj.put("email", username);
		synapse.createAuthEntity("/apiPasswordEmail", obj);
	}
	
	
	@Test(expected = SynapseBadRequestException.class)
	public void testSendEmailInvalidUser() throws Exception {
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		synapse.login(username, password);

		JSONObject obj = new JSONObject();
		obj.put("email", "invalid-user-name@sagebase.org");
		synapse.createAuthEntity("/userPasswordEmail", obj);
	}
	
	@Test
	public void testGetSecretKey() throws Exception {
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		synapse.login(username, password);

		JSONObject response = synapse.getSynapseEntity(authEndpoint, "/secretKey");
		assertTrue(response.has("secretKey"));
	}
	
	@Test
	public void testInvalidateSecretKey() throws Exception {
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		synapse.login(username, password);

		JSONObject response = synapse.getSynapseEntity(authEndpoint, "/secretKey");
		assertTrue(response.has("secretKey"));
		String secretKey = response.getString("secretKey");
		assertNotNull(secretKey);
	
		// now invalidate the key
		synapse.deleteUri(authEndpoint, "/secretKey");
		
		// now get the key again...
		response = synapse.getSynapseEntity(authEndpoint, "/secretKey");
		assertTrue(response.has("secretKey"));
		String secondKey = response.getString("secretKey");
		assertNotNull(secondKey);
		
		// ... should be different from the first one
		assertFalse(secretKey.equals(secondKey));
	}
	
	class MutableBoolean {
	boolean b = false;
	public void set(boolean b) {this.b=b;}
	public boolean get() {return b;}
	}
	
	public static byte[] executeRequest(HttpURLConnection conn, HttpStatus expectedRc, String failureReason) throws Exception {
		int rc = conn.getResponseCode();
		if (expectedRc.value()==rc) {
			byte[] respBody = (CrowdAuthUtil.readInputStream((InputStream)conn.getContent())).getBytes();
			return respBody;
		} else {
			byte[] respBody = (CrowdAuthUtil.readInputStream((InputStream)conn.getErrorStream())).getBytes();
			throw new AuthenticationException(rc, failureReason, new Exception(new String(respBody)));
		}
	}
	
	// this is meant to recreate the problem described in PLFM-292
	// http://sagebionetworks.jira.com/browse/PLFM-292
	@Test 
	public void testMultipleLogins() throws Exception {

	CrowdAuthUtil.acceptAllCertificates();
	int n = 100;
	Set<Long> sortedTimes = new TreeSet<Long>();
	long elapsed = 0;
	for (int i=0; i<n; i++) {
		final MutableBoolean b = new MutableBoolean();
	 	Thread thread = new Thread() {
			public void run() {
				try {
					authenticate();
					b.set(true);
				} catch (Exception e) {
					e.printStackTrace(); // 'fail' will be thrown below
				}
			}
		};
		thread.start();
		long start = System.currentTimeMillis();
		try {
			thread.join(20000L); // time out
		} catch (InterruptedException ie) {
			// as expected
		}
		long t = System.currentTimeMillis()-start;
		elapsed += t;
		sortedTimes.add(t);
		assertTrue("Failed or timed out after "+i+" iterations.", b.get()); // should have been set to 'true' if successful
	}
	System.out.println(n+" authentication request response time (sec): min "+
			((float)sortedTimes.iterator().next()/1000L)+" avg "+((float)elapsed/n/1000L)+
			" max "+((float)getLast(sortedTimes)/1000L));
	
	}
	
	class MutableLong {
	long L = 0L;
	public void set(long L) {this.L=L;}
	public long get() {return L;}
	}
	
	private void authenticate() throws Exception {
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		synapse.login(username, StackConfiguration.getIntegrationTestUserThreePassword());
	}
	
	@Test 
	public void testMultipleLoginsMultiThreaded() throws Exception {

	CrowdAuthUtil.acceptAllCertificates();

	for (int n : new int[]{100}) {
		Map<Integer, MutableLong> times = new HashMap<Integer, MutableLong>();
		for (int i=0; i<n; i++) {
			final MutableLong L = new MutableLong();
			times.put(i, L);
		 	Thread thread = new Thread() {
				public void run() {
					try {
						long start = System.currentTimeMillis();
						authenticate();
						L.set(System.currentTimeMillis()-start);
					} catch (Exception e) {
						//fail(e.toString());
						e.printStackTrace(); // 'fail' will be thrown below
					}
				}
			};
			thread.start();
		}
		int count = 0;
		long elapsed = 0L;
		Set<Long> sortedTimes = new TreeSet<Long>();
		long uberTimeOut = System.currentTimeMillis()+UBER_TIMEOUT;
		while (!times.isEmpty() && System.currentTimeMillis()<uberTimeOut) {
			for (int i: times.keySet()) {
				long L = times.get(i).get();
				if (L!=0) {
					elapsed += L;
					sortedTimes.add(L);
					count++;
					times.remove(i);
					break;
				}
			}
		}
		System.out.println(count+" authentication request response time (sec): min "+
				((float)sortedTimes.iterator().next()/1000L)+" avg "+((float)elapsed/count/1000L)+
				" max "+((float)getLast(sortedTimes)/1000L));
	}
	}
	
	private static long UBER_TIMEOUT = 5*60*1000L;
	
	private static <T> T getLast(Set<T> set) {
		T ans = null;
		for (T v : set) ans=v;
		return ans;
	}
}
