package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.HashMap;

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
import org.sagebionetworks.web.shared.users.UserData;
import org.sagebionetworks.web.shared.users.UserRegistration;
import org.sagebionetworks.web.util.LocalAuthStubLauncher;
import org.springframework.web.client.RestTemplate;

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
	
	private UserRegistration user1 = new UserRegistration("test@test.com", "test@test.com", "test", "user", "test user");
	private String user1password = "password";
	
	
	@BeforeClass
	public static void beforeClass() throws Exception{
		// Start the local stub implementation of the the platform
		// api.  This stub services runs in a local grizzly/jersey 
		// container.
		
		// First setup the url
		serviceUrl = UriBuilder.fromUri("http://"+serviceHost+"/").port(servicePort).build().toURL();
		// Now start the container
		selector = LocalAuthStubLauncher.startServer(serviceHost, servicePort);
		
		// Create the RestProvider
		int timeout = 1000*60*2; // 2 minute timeout
		int maxTotalConnections = 1; // Only need one for this test.
		provider = new RestTemplateProviderImpl(timeout, maxTotalConnections);
		// Create the service
		service = new UserAccountServiceImpl();
		// Inject the required values
		service.setRestTemplate(provider);
		ServiceUrlProvider urlProvider = new ServiceUrlProvider();
		urlProvider.setAuthEndpoint(serviceUrl.toString());
		urlProvider.setAuthPrefix("auth/v1/");
		service.setServiceUrlProvider(urlProvider);
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
		urlProvider.setRestEndpoint(serviceUrl.toString());
		urlProvider.setServletPrefix("repo/v1");
		dummy.setServiceUrlProvider(urlProvider);
	}
	
	
	@Test
	public void testCreateUser() {
		try {
			service.createUser(user1);
		} catch (RestServiceException e) {
			fail(e.getMessage());
		}
	}

	@Ignore
	@Test
	public void testAuthenticateUser() {
		try {
			UserData userdata = service.authenticateUser(user1.getUserId(), user1password);
			assertEquals(user1.getUserId(), userdata.getUserId());
		} catch (AuthenticationException e) {
			fail("user not created properly");
		}		
	}
	
	@Ignore
	@Test
	public void testSendPasswordResetEmail(){
		try {
			service.sendPasswordResetEmail(user1.getUserId());
		} catch (RestServiceException e) {
			fail(e.getMessage());
		}
	}
	
	@Ignore
	@Test
	public void testTerminateSession() {
		UserData userdata = null;
		try {
			userdata = service.authenticateUser(user1.getUserId(), user1password);
		} catch (AuthenticationException e) {
			fail(e.getMessage());
		}
		
		if(userdata == null) fail("test setup error: user doesn't exist");
		
		try {
			service.terminateSession(userdata.getToken());			
		} catch (RestServiceException e) {
			fail(e.getMessage());
		}
	}

}
