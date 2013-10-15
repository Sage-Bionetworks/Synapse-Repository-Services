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
import java.util.UUID;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.http.HttpStatus;

/**
 * CrowdAuthUtil
 */
public class IT990CrowdAuthentication {
	private static SynapseClientImpl synapse;
	
	private static final String username = StackConfiguration.getIntegrationTestUserThreeName();
	private static final String password = StackConfiguration.getIntegrationTestUserThreePassword();

	@BeforeClass
	public static void beforeClass() throws Exception {
		String authEndpoint = StackConfiguration.getAuthenticationServicePrivateEndpoint();
		String repoEndpoint = StackConfiguration.getRepositoryServiceEndpoint();
		synapse = new SynapseClientImpl();
		synapse.setAuthEndpoint(authEndpoint);
		synapse.setRepositoryEndpoint(repoEndpoint);
	}
	
	@Before
	public static void setup() throws Exception {
		synapse.login(StackConfiguration.getIntegrationTestUserThreeName(),
				StackConfiguration.getIntegrationTestUserThreePassword());
	}

	@Test
	public void testCreateSession() throws Exception {
		synapse.login(username, password);
		assertNotNull(synapse.getCurrentSessionToken());
	}
	
	@Test
	public void testCreateSessionSigningTermsOfUse() throws Exception {
		synapse.login(username, password, true);
		assertNotNull(synapse.getCurrentSessionToken());
	}
	
	@Test(expected = SynapseBadRequestException.class)
	public void testCreateSessionBadCredentials() throws Exception {
		synapse.login(username, "incorrectPassword");
	}
	
	@Test(expected = SynapseForbiddenException.class)
	public void testCreateSessionNoTermsOfUse() throws Exception {
		String username = StackConfiguration.getIntegrationTestRejectTermsOfUseEmail();
		String password = StackConfiguration.getIntegrationTestRejectTermsOfUsePassword();
		synapse.login(username, password);
	}
	
	
	@Test
	public void testRevalidateSvc() throws Exception {
		synapse.login(username, password);
		synapse.revalidateSession();
	}
	
	@Test(expected=SynapseNotFoundException.class)
	public void testRevalidateBadTokenSvc() throws Exception {
		synapse.login(username, password);
		synapse.setSessionToken("invalid-session-token");
		synapse.revalidateSession();
	}
	
	@Test
	public void testCreateSessionThenLogout() throws Exception {
		// Start session
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();

		synapse.login(username, password);
		
		// Logout
		synapse.deleteUri(authEndpoint, "/session");
	}
	
	
	@Test(expected = SynapseBadRequestException.class)
	public void testCreateExistingUser() throws Exception {	
		// Start session
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();

		synapse.login(username, password);
		JSONObject userRequest = new JSONObject();

		userRequest.put("email", username);
		userRequest.put("firstName", "dev");
		userRequest.put("lastName", "usr");
		userRequest.put("displayName", "dev usr");

		synapse.createAuthEntity("/user", userRequest);
	}
	
	
	@Test
	public void testCreateUserAndAcceptToU() throws Exception {	
		String username = "integration@test." + UUID.randomUUID();
		String password = "password";

		JSONObject userRequest = new JSONObject();
		userRequest.put("email", username);
		userRequest.put("firstName", "foo");
		userRequest.put("lastName", "bar");
		userRequest.put("displayName", "foo bar");
		// Note: passwords are only accepted in this request in non-production stacks
		userRequest.put("password", password);
		synapse.createAuthEntity("/user", userRequest);
		
		// Expect a ToU failure here, which means the user was created
		try {
			synapse.login(username, password);
			Assert.fail();
		} catch (SynapseUnauthorizedException e) { 
			assertTrue(e.getMessage().contains("Terms of Use"));
		}
		
		// Now accept the terms and get a session token
		userRequest.put("acceptsTermsOfUse", "true");
		JSONObject session = synapse.createAuthEntity("/session", userRequest);
		assertTrue(session.has(SESSION_TOKEN_LABEL));
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
	public void testChangePassword() throws Exception {
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		synapse.login(username, password);

		String testNewPassword = "newPassword";
		JSONObject obj = new JSONObject();
		obj.put("newPassword", testNewPassword);
		synapse.createAuthEntity("/userPassword", obj);
		
		// To check the password, we have to try to log-in:
		synapse.deleteUri(authEndpoint, "/session");
		synapse.login(username, testNewPassword);
		
		// Restore original password
		obj = new JSONObject();
		obj.put("newPassword", password);
		synapse.createAuthEntity("/userPassword", obj);
	}
		
	@Test(expected=SynapseNotFoundException.class)
	public void testChangeEmail() throws Exception {
		// Get a session token
		JSONObject loginRequest = new JSONObject();
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		loginRequest.put("email", username);
		loginRequest.put("password", password);

		JSONObject session = synapse.createAuthEntity("/session", loginRequest);
		assertTrue(session.has(SESSION_TOKEN_LABEL));
		String token = session.getString(SESSION_TOKEN_LABEL);
		synapse.setSessionToken(token);

		JSONObject obj = new JSONObject();
		obj.put("registrationToken", AuthorizationConstants.CHANGE_EMAIL_TOKEN_PREFIX + token);
		obj.put("password", password); // Don't change the password
		synapse.createAuthEntity("/changeEmail", obj);
	}
	
	@Test
	public void testRegisterChangePassword() throws Exception {
		// Get a session token
		JSONObject loginRequest = new JSONObject();
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		loginRequest.put("email", username);
		loginRequest.put("password", password);

		JSONObject session = synapse.createAuthEntity("/session", loginRequest);
		assertTrue(session.has(SESSION_TOKEN_LABEL));
		String token = session.getString(SESSION_TOKEN_LABEL);

		String testNewPassword = "newPassword";
		JSONObject registrationInfo = new JSONObject();
		registrationInfo.put("registrationToken", AuthorizationConstants.REGISTRATION_TOKEN_PREFIX + token);
		registrationInfo.put("password", testNewPassword);
		synapse.createAuthEntity("/registeringUserPassword", registrationInfo);
		
		// To check the password, we have to try to log-in:
		synapse.deleteUri(authEndpoint, "/session");
		synapse.login(username, testNewPassword);
		
		// Restore original password
		JSONObject obj = new JSONObject();
		obj.put("newPassword", password);
		synapse.createAuthEntity("/userPassword", obj);
	}
	
	@Test
	public void testSendResetPasswordEmail() throws Exception {
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		synapse.login(username, password);

		// Note: non-production stacks do not send emails, but instead print a log message
		JSONObject obj = new JSONObject();
		obj.put("email", username);
		synapse.createAuthEntity("/userPasswordEmail", obj);
	}
	
	@Test
	public void testSetAPIPasswordEmail() throws Exception {
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		String password = StackConfiguration.getIntegrationTestUserThreePassword();
		synapse.login(username, password);

		// Note: non-production stacks do not send emails, but instead print a log message
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
	
		// Invalidate the key
		synapse.deleteUri(authEndpoint, "/secretKey");
		
		// Get the key again
		response = synapse.getSynapseEntity(authEndpoint, "/secretKey");
		assertTrue(response.has("secretKey"));
		String secondKey = response.getString("secretKey");
		assertNotNull(secondKey);
		
		// Should be different from the first one
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
	
	/**
	 * Since we don't know Google's private OpenID information, this is a bit difficult to integration test
	 * At best, this test makes sure the service is wired up
	 */
	@Test
	public void testOpenIDCallback() throws Exception {
		try {
			synapse.createAuthEntity("/openIdCallback", new JSONObject());
			fail();
		} catch (SynapseServiceException e) {
			// This is the result of a failed argument check
			assertTrue(e.getMessage().contains("Missing required discovery information"));
		}
	}
}
