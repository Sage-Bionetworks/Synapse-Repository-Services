package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.auth.NewUser;

public class IT990AuthenticationController {
	private static SynapseClient synapse;
	
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
	public void setup() throws Exception {
		synapse.login(username, password);
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
	
	@Test(expected = SynapseUnauthorizedException.class)
	public void testCreateSessionBadCredentials() throws Exception {
		synapse.login(username, "incorrectPassword");
	}
	
	@Test(expected = SynapseTermsOfUseException.class)
	public void testCreateSessionNoTermsOfUse() throws Exception {
		String username = StackConfiguration.getIntegrationTestRejectTermsOfUseName();
		String password = StackConfiguration.getIntegrationTestRejectTermsOfUsePassword();
		synapse.login(username, password);
	}
	
	
	@Test
	public void testRevalidateSvc() throws Exception {
		synapse.revalidateSession();
		assertNotNull(synapse.getCurrentSessionToken());
	}
	
	@Test(expected=SynapseUnauthorizedException.class)
	public void testRevalidateBadTokenSvc() throws Exception {
		synapse.setSessionToken("invalid-session-token");
		synapse.revalidateSession();
	}
	
	@Test
	public void testCreateSessionThenLogout() throws Exception {
		synapse.logout();
		assertNull(synapse.getCurrentSessionToken());
	}
	
	@Test(expected = SynapseUnauthorizedException.class)
	public void testCreateExistingUser() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(username);
		user.setFirstName("dev");
		user.setLastName("usr");
		user.setDisplayName("dev usr");
		
		synapse.createUser(user);
	}
	
	
	@Test
	public void testCreateUserAndAcceptToU() throws Exception {	
		String username = "integration@test." + UUID.randomUUID();
		String password = "password";
		
		NewUser user = new NewUser();
		user.setEmail(username);
		user.setFirstName("foo");
		user.setLastName("bar");
		user.setDisplayName("foo bar");
		// Note: passwords are only accepted in this request in non-production stacks
		user.setPassword(password);
		
		synapse.createUser(user);
		
		// Expect a ToU failure here, which means the user was created
		try {
			synapse.login(username, password);
			fail();
		} catch (SynapseTermsOfUseException e) { }
		
		// Now accept the terms and get a session token
		synapse.login(username, password, true);
		assertNotNull(synapse.getCurrentSessionToken());
	}
	
	@Test
	public void testGetUser() throws Exception {
		NewUser user = synapse.getAuthUserInfo();
		assertEquals(username, user.getEmail());
		assertEquals("First-" + username, user.getFirstName());
		assertEquals("Last-" + username, user.getLastName());
		assertEquals(username, user.getDisplayName());
	}
	
	@Test
	public void testChangePassword() throws Exception {
		String testNewPassword = "newPassword";
		synapse.changePassword(testNewPassword);
		synapse.logout();
		synapse.login(username, testNewPassword);
		
		// Restore original password
		synapse.changePassword(password);
	}
	
	/**
	 * Functionality is currently disabled pending PLFM-2231
	 * https://sagebionetworks.jira.com/browse/PLFM-2231
	 */
	@Ignore
	@Test(expected=SynapseNotFoundException.class)
	public void testChangeEmail() throws Exception {
		// Changes the current email (IT user 3) to the email of the session token (IT user 3)
		synapse.changeEmail(synapse.getCurrentSessionToken(), password);
		
		//TODO actually change the email
		//TODO change the email back
	}
	
	@Test
	public void testRegisterChangePassword() throws Exception {
		String testNewPassword = "newPassword";
		synapse.changePassword(synapse.getCurrentSessionToken(), testNewPassword);
		
		// To check the password, we have to try to log-in:
		synapse.login(username, testNewPassword);
		
		// Restore original password
		synapse.changePassword(password);
	}
	
	@Test
	public void testResentPasswordEmail() throws Exception {
		// Note: non-production stacks do not send emails, but instead print a log message
		synapse.resentPasswordEmail(username);
	}
	
	@Test
	public void testSendResetPasswordEmail() throws Exception {
		// Note: non-production stacks do not send emails, but instead print a log message
		synapse.sendPasswordResetEmail(username);
	}
	
	@Test
	public void testSetAPIPasswordEmail() throws Exception {
		// Note: non-production stacks do not send emails, but instead print a log message
		synapse.sendPasswordResetEmail();
	}
	
	
	@Test(expected = SynapseNotFoundException.class)
	public void testSendEmailInvalidUser() throws Exception {
		// There's no way a user like this exists :D
		synapse.sendPasswordResetEmail("invalid-user-name@sagebase.org" + UUID.randomUUID());
	}
	
	@Test
	public void testGetSecretKey() throws Exception {
		String apikey = synapse.retrieveApiKey();
		assertNotNull(apikey);
	}
	
	@Test
	public void testInvalidateSecretKey() throws Exception {
		String apikey = synapse.retrieveApiKey();
		synapse.invalidateApiKey();
		String secondKey = synapse.retrieveApiKey();
		
		// Should be different from the first one
		assertFalse(apikey.equals(secondKey));
	}

	private static long UBER_TIMEOUT = 60 * 1000L;
	private class MutableLong {
		long L = 0L;

		public void set(long L) {
			this.L = L;
	}

		public long get() {
			return L;
				}
			}
	
	private void authenticate() throws Exception {
		String username = StackConfiguration.getIntegrationTestUserThreeName();
		synapse.login(username, StackConfiguration.getIntegrationTestUserThreePassword());
	}
	
	/**
	 * Without Crowd in place, this test may or may not be needed anymore
	 */
	@Ignore
	@Test
	public void testMultipleLoginsMultiThreaded() throws Exception {
		for (int n : new int[] { 100 }) {
		Map<Integer, MutableLong> times = new HashMap<Integer, MutableLong>();
			for (int i = 0; i < n; i++) {
			final MutableLong L = new MutableLong();
			times.put(i, L);
		 	Thread thread = new Thread() {
				public void run() {
					try {
						long start = System.currentTimeMillis();
						authenticate();
							L.set(System.currentTimeMillis() - start);
					} catch (Exception e) {
							// fail(e.toString());
						e.printStackTrace(); // 'fail' will be thrown below
					}
				}
			};
			thread.start();
		}
		int count = 0;
		long elapsed = 0L;
		Set<Long> sortedTimes = new TreeSet<Long>();
			long uberTimeOut = System.currentTimeMillis() + UBER_TIMEOUT;
			while (!times.isEmpty() && System.currentTimeMillis() < uberTimeOut) {
				for (int i : times.keySet()) {
				long L = times.get(i).get();
					if (L != 0) {
					elapsed += L;
					sortedTimes.add(L);
					count++;
					times.remove(i);
					break;
				}
			}
		}
			System.out.println(count
					+ " authentication request response time (sec): min "
					+ ((float) sortedTimes.iterator().next() / 1000L) + " avg "
					+ ((float) elapsed / count / 1000L) + " max "
					+ ((float) getLast(sortedTimes) / 1000L));
	}
	}
	
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
			synapse.passThroughOpenIDParameters("org.sagebionetworks.openid.provider=GOOGLE", null);
			fail();
		} catch (SynapseUnauthorizedException e) {
			assertTrue(e.getMessage().contains("Required parameter missing"));
		}
	}
}
