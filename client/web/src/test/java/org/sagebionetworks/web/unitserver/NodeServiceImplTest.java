package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.server.RestTemplateProviderImpl;
import org.sagebionetworks.web.server.servlet.NodeServiceImpl;
import org.sagebionetworks.web.server.servlet.ServiceUrlProvider;
import org.sagebionetworks.web.server.servlet.UserAccountServiceImpl;
import org.sagebionetworks.web.shared.users.AclPrincipal;
import org.sagebionetworks.web.shared.users.UserRegistration;
import org.sagebionetworks.web.util.LocalNodeServiceStub;
import org.sagebionetworks.web.util.LocalStubLauncher;

import com.sun.grizzly.http.SelectorThread;
import com.sun.istack.logging.Logger;

/**
 * This is a unit test of the UserAccountServiceImpl service.
 * It depends on a local stub implementation of the platform API
 * to be deployed locally.
 * 
 * @author dburdick
 *
 */
public class NodeServiceImplTest {
	
	public static Logger logger = Logger.getLogger(NodeServiceImplTest.class);
	
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
	private static NodeServiceImpl service = null;
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
		selector = LocalNodeServiceStub.startServer(serviceHost, servicePort);
		
		// Create the RestProvider
		int timeout = 1000*60*2; // 2 minute timeout
		int maxTotalConnections = 1; // Only need one for this test.
		provider = new RestTemplateProviderImpl(timeout, maxTotalConnections);
		// Create the service
		service = new NodeServiceImpl();
		// Inject the required values
		service.setRestTemplate(provider);
		ServiceUrlProvider urlProvider = new ServiceUrlProvider();
		urlProvider.setRepositoryServiceUrl(serviceUrl.toString() + "repo/v1");		
		service.setServiceUrlProvider(urlProvider);
		
		LocalNodeServiceStub.groups.add(group1);
		LocalNodeServiceStub.groups.add(group2);
		LocalNodeServiceStub.users.add(user1acl);
		LocalNodeServiceStub.users.add(user2acl);

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
	public void testGetAllUsers() {
		List<AclPrincipal> userList;

		// Add some users and test to make sure those users were returned
		userList = service.getAllUsers();
		assertEquals(user1acl, userList.get(userList.indexOf(user1acl)));
		assertEquals(user2acl, userList.get(userList.indexOf(user2acl)));
		assertEquals(2, userList.size());
	}
		
	@Test
	public void testGetAllGroups() {
		List<AclPrincipal> groupList;
		
		groupList = service.getAllGroups();
		assertEquals(group1, groupList.get(groupList.indexOf(group1)));
		assertEquals(group2, groupList.get(groupList.indexOf(group2)));
		assertEquals(2, groupList.size());
	}
		
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
}
