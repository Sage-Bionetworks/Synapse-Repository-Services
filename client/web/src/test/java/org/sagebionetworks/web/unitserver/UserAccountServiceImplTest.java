package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.web.client.security.AuthenticationException;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.server.RestTemplateProviderImpl;
import org.sagebionetworks.web.server.servlet.ServiceUrlProvider;
import org.sagebionetworks.web.server.servlet.UserAccountServiceImpl;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.AclPrincipal;
import org.sagebionetworks.web.shared.users.UserData;
import org.sagebionetworks.web.shared.users.UserRegistration;
import org.sagebionetworks.web.util.LocalAuthServiceStub;
import org.sagebionetworks.web.util.LocalAuthServiceStub.AclPrincipalTest;

import com.sun.grizzly.http.SelectorThread;
import com.sun.istack.logging.Logger;

/**
 * This is a unit test of the DatasetServiceImpl service.
 * It depends on a local stub implementation of the platform API
 * to be deployed locally.
 * 
 * @author dburdick
 *
 */
public class UserAccountServiceImplTest {
	
	public static Logger logger = Logger.getLogger(UserAccountServiceImplTest.class);
	
	/**
	 * This is our handle to the local grizzly container.
	 * It can be used to communicate with the container or 
	 * shut it down.
	 */
	private static SelectorThread selector = null;
	
	private static String serviceHost = "localhost";
	private static int servicePort = 9998;
	private static URL serviceUrl = null;
	
	// This is our service.
	private static UserAccountServiceImpl service = null;
	private static RestTemplateProvider provider = null;
	
	private UserRegistration user1 = new UserRegistration("test@test.com", "test", "user", "test user");
	private String user1password = "password";
	private UserRegistration user2 = new UserRegistration("bar@foo.com", "bar", "foo", "barfoo");
	private String user2password = "otherpass";
	
	private static AclPrincipal user1acl = new AclPrincipal("test@test.com", "test user", new Date(), null, null, true);
	private static AclPrincipal user2acl = new AclPrincipal("bar@foo.com", "barfoo", new Date(), null, null, true);
	
	private static AclPrincipal group1 = new AclPrincipal("people@fake.com", "People", new Date(), null, null, false);
	private static AclPrincipal group2 = new AclPrincipal("morePeople@fake.com", "More People", new Date(), null, null, false);
	
	@BeforeClass
	public static void beforeClass() throws Exception{
		// Start the local stub implementation of the the platform
		// api.  This stub services runs in a local grizzly/jersey 
		// container.
		
		// First setup the url
		serviceUrl = UriBuilder.fromUri("http://"+serviceHost+"/").port(servicePort).build().toURL();
		// Now start the container
		selector = LocalAuthServiceStub.startServer(serviceHost, servicePort);
		
		// Create the RestProvider
		int timeout = 1000*60*2; // 2 minute timeout
		int maxTotalConnections = 1; // Only need one for this test.
		provider = new RestTemplateProviderImpl(timeout, maxTotalConnections);
		// Create the service
		service = new UserAccountServiceImpl();
		// Inject the required values
		service.setRestTemplate(provider);
		ServiceUrlProvider urlProvider = new ServiceUrlProvider();
		urlProvider.setAuthServiceUrl(serviceUrl.toString() + "auth/v1");		
		service.setServiceUrlProvider(urlProvider);
		
		LocalAuthServiceStub.groups.add(group1);
		LocalAuthServiceStub.groups.add(group2);
		LocalAuthServiceStub.users.add(user1acl);
		LocalAuthServiceStub.users.add(user2acl);

	}
	
	/**
	 * Clear all of the data in the stub service.
	 */
	private static void clearStubData(){
	}
	
	/**
	 * Clear all of the data in the stub service.
	 */
	private static void generateRandomData(int number){
		clearStubData();
	}
	
	@AfterClass
	public static void afterClass(){
		// Shut down the grizzly container at the end of this suite.
		if(selector != null){
			selector.stopEndpoint();
		}
	}
	
	@After
	public void tearDown(){
		// After each test clean out all data
		clearStubData();
	}
	
	
	@Test
	public void testValidate(){
		// Create an instance that is not setup correctly
		UserAccountServiceImpl dummy = new UserAccountServiceImpl();
		try{
			dummy.validateService();
			fail("The dummy was not initialized so it should have failed validation");
		}catch(IllegalStateException e){
			//expected;
		}
		// Set the template
		dummy.setRestTemplate(provider);
		try{
			dummy.validateService();
			fail("The dummy was not initialized so it should have failed validation");
		}catch(IllegalStateException e){
			//expected;
		}
		// After setting the url it should pass validation.
		ServiceUrlProvider urlProvider = new ServiceUrlProvider();
		urlProvider.setRepositoryServiceUrl(serviceUrl.toString() + "repo/v1/");		
		dummy.setServiceUrlProvider(urlProvider);
	}
	
	@Test
	public void testCreateUser() {
		try {
			service.createUser(user1);
		} catch (RestServiceException e) {
			fail(e.getMessage());
		}
		
		// assure user was actually created		
		try {
			UserData userData = service.initiateSession(user1.getEmail(), user1password);
			assertEquals(userData.getEmail(), user1.getEmail());
		} catch (AuthenticationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testAuthenticateUser() {
		// try fake user
		try {
			service.initiateSession("junk", "junk");
			fail("unknown user was authenticated!");
		} catch (AuthenticationException e1) {
			// expected
		}
		
		// auth real user
		try {
			UserData userdata = service.initiateSession(user1.getEmail(), user1password);
			assertEquals(user1.getEmail(), userdata.getEmail());
		} catch (AuthenticationException e) {
			fail("user not created properly");
		}		
	}
		
	@Test
	public void testSendPasswordResetEmail(){
		try {
			service.sendPasswordResetEmail(user1.getEmail());
		} catch (RestServiceException e) {
			fail(e.getMessage());
		}
	}
		
	@Test
	public void testTerminateSession() {
		UserData userdata = null;
		try {
			userdata = service.initiateSession(user1.getEmail(), user1password);
		} catch (AuthenticationException e) {
			fail(e.getMessage());
		}
		
		if(userdata == null) fail("test setup error: user doesn't exist");
		
		// terminate unknown session
		try {
			service.terminateSession("junk");
		} catch (Exception e) {
			fail("termination of an unknown session should not throw an exception");			
		}
		
		// terminate real session
		try {
			service.terminateSession(userdata.getToken());			
		} catch (RestServiceException e) {
			fail(e.getMessage());
		}
	}
	
	@Ignore
	@Test
	public void testGetAllUsers() {
		List<AclPrincipal> userList;

		// Add some users and test to make sure those users were returned
		try {
			service.createUser(user2);
		} catch (RestServiceException e) {
			fail(e.getMessage());
		}
		userList = service.getAllUsers();
		assertEquals(user1acl, userList.get(userList.indexOf(user1acl)));
		assertEquals(user2acl, userList.get(userList.indexOf(user2acl)));
		assertEquals(2, userList.size());
	}
	
	@Ignore
	@Test
	public void testGetAllGroups() {
		List<AclPrincipal> groupList;
		
		groupList = service.getAllGroups();
		assertEquals(group1, groupList.get(groupList.indexOf(group1)));
		assertEquals(group2, groupList.get(groupList.indexOf(group2)));
		assertEquals(2, groupList.size());
	}
	
	@Ignore
	@Test
	public void testGetAllUsersAndGroups() {
		List<AclPrincipal> userAndGroupList;
		
		userAndGroupList = service.getAllUsersAndGroups();
		assertEquals(user1acl, userAndGroupList.get(userAndGroupList.indexOf(user1acl)));
		assertEquals(user2acl, userAndGroupList.get(userAndGroupList.indexOf(user2acl)));
		assertEquals(group1, userAndGroupList.get(userAndGroupList.indexOf(group1)));
		assertEquals(group2, userAndGroupList.get(userAndGroupList.indexOf(group2)));
		assertEquals(4, userAndGroupList.size());
	}
	
	@Test
	public void testGetAuthServiceUrl() {
		String authServiceUrl = service.getAuthServiceUrl();
		
		try {
			URI testUri = new URI(authServiceUrl);
		} catch (URISyntaxException e) {
			fail("The Auth Service URL returned was not valid.");
		}
	}
	
	@Test
	public void testGetSynapseWebUrl() {
		String synapseWebUrl = service.getSynapseWebUrl();
		try {
			URI testUri = new URI(synapseWebUrl);
		} catch (URISyntaxException e) {
			fail("The Synapse URL returned was not valid.");
		}
	}
}
