package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.authutil.User;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 
 */

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:authentication-context.xml", "classpath:authentication-servlet.xml" })
public class AuthenticationControllerTest {

	private static final Logger log = Logger
			.getLogger(AuthenticationControllerTest.class.getName());
	private Helpers helper = new Helpers();
	//private DispatcherServlet servlet;
		
	private CrowdAuthUtil crowdAuthUtil = new CrowdAuthUtil();


	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		//DispatcherServlet servlet = 
			helper.setUp();
		CrowdAuthUtil.acceptAllCertificates();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	
	@Test
	public void testCreateSession() throws Exception {
		JSONObject session = helper.testCreateJsonEntity("/session",
				"{\"userId\":\"demouser\",\"password\":\"demouser-pw\"}");
		assertTrue(session.has("sessionToken"));
		assertEquals("Demo User", session.getString("displayName"));
	}


	@Test
	public void testCreateSessionBadCredentials() throws Exception {
		JSONObject session = helper.testCreateJsonEntityShouldFail("/session",
				"{\"userId\":\"demouser\",\"password\":\"incorrectPassword\"}", HttpStatus.BAD_REQUEST);
		assertEquals("Unable to authenticate", session.getString("reason"));
		// AuthenticationURL: https://ssl.latest.deflaux-test.appspot.com/auth/v1/session
	}


	@Test
	public void testRevalidateUtil() throws Exception {
		// start session
		JSONObject session = helper.testCreateJsonEntity("/session",
				"{\"userId\":\"demouser\",\"password\":\"demouser-pw\"}");
		String sessionToken = session.getString("sessionToken");
		assertEquals("Demo User", session.getString("displayName"));
		
		// revalidate via utility function
		String userId = null;
		try {
			userId = crowdAuthUtil.revalidate(sessionToken);
		} catch (Exception e) {
			log.log(Level.WARNING, "exception during 'revalidate'", e);
		}
		log.info("UserId: "+userId);
		assertEquals("demouser", userId);
	}

	@Test
	public void testRevalidateSvc() throws Exception {
		// start session
		JSONObject session = helper.testCreateJsonEntity("/session",
				"{\"userId\":\"demouser\",\"password\":\"demouser-pw\"}");
		String sessionToken = session.getString("sessionToken");
		assertEquals("Demo User", session.getString("displayName"));
		
		// revalidate via web service
		helper.testUpdateJsonEntity("/session",	"{\"sessionToken\":\""+sessionToken+"\"}");
		
	}


	@Test
	public void testRevalidateBadTokenUtil() throws Exception {
		try {
			crowdAuthUtil.revalidate("invalidToken");
			fail("exception expected");
		} catch (Exception e) {
			// as expected
			//log.log(Level.INFO, "this exception is expected", e);
		}
	}


	@Test
	public void testRevalidateBadTokenSvc() throws Exception {
		
		// revalidate via web service
		helper.testUpdateJsonEntityShouldFail("/session", "{\"sessionToken\":\"invalid-token\"}", HttpStatus.NOT_FOUND);
	}


	@Test
	public void testCreateSessionThenLogout() throws Exception {
		JSONObject session = helper.testCreateJsonEntity("/session",
				"{\"userId\":\"demouser\",\"password\":\"demouser-pw\"}");
		String sessionToken = session.getString("sessionToken");
		assertEquals("Demo User", session.getString("displayName"));
		
		helper.testDeleteJsonEntity("/session", "{\"sessionToken\":\""+sessionToken+"\"}");
		
	}


	@Test
	public void testCreateExistingUser() throws Exception {
			helper.testCreateJsonEntityShouldFail("/user",
				"{\"userId\":\"demouser\","+
				"\"email\":\"notused@sagebase.org\","+
				"\"firstName\":\"Demo\","+
				"\"lastName\":\"User\","+
				"\"displayName\":\"Demo User\""+
					"}", HttpStatus.BAD_REQUEST);


	}
	

	@Test
	public void testCreateNewUser() throws Exception {
		// special userId for testing -- no confirmation email is sent!
		Properties props = new Properties();
        InputStream is = AuthenticationControllerTest.class.getClassLoader().getResourceAsStream("authenticationcontroller.properties");
        try {
        	props.load(is);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
        String userId = props.getProperty("integrationTestUser");

		assertNotNull(userId);
		try {
			helper.testCreateJsonEntity("/user",
					"{\"userId\":\""+userId+"\","+
					"\"email\":\"notused@sagebase.org\","+
					 // integration testing with this special user is the only time a password may be specified
					"\"password\":\""+userId+"\","+
				"\"firstName\":\"New\","+
				"\"lastName\":\"User\","+
				"\"displayName\":\"New User\""+
					"}");
		} finally {
			User user = new User();
			user.setUserId(userId);
			try {
				crowdAuthUtil.deleteUser(user);
			} catch (AuthenticationException ae) {
				if (ae.getRespStatus()==HttpStatus.NOT_FOUND.value()) {
					// that's OK, it just means that the user never was created in the first place
				} else {
					throw ae;
				}
			}
		}

	}
	

	@Test
	public void testCreateAndUpdateUser() throws Exception {
		
		// special userId for testing -- no confirmation email is sent!
		// special userId for testing -- no confirmation email is sent!
		Properties props = new Properties();
        InputStream is = AuthenticationControllerTest.class.getClassLoader().getResourceAsStream("authenticationcontroller.properties");
        try {
        	props.load(is);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
        String userId = props.getProperty("integrationTestUser");
		assertNotNull(userId);
		try {
			
			helper.testCreateJsonEntity("/user",
					"{\"userId\":\""+userId+"\","+
					 // integration testing with this special user is the only time a password may be specified
					"\"password\":\""+userId+"\","+
					"\"email\":\"notused@sagebase.org\","+
					"\"firstName\":\"New\","+
					"\"lastName\":\"User\","+
					"\"displayName\":\"New User\""+
						"}");
				
			helper.testUpdateJsonEntity("/user",
					"{\"userId\":\""+userId+"\","+
					"\"email\":\"notused@sagebase.org\","+
					"\"firstName\":\"NewNEW\","+
					"\"lastName\":\"UserNEW\","+
					"\"displayName\":\"New NEW User\""+
						"}");
		} finally {
			User user = new User();
			user.setUserId(userId);
			crowdAuthUtil.deleteUser(user);
		}
	}
	
	@Ignore
	@Test
	public void testSendResetPasswordEmail() throws Exception {
		 helper.testCreateJsonEntity("/userPasswordEmail","{\"userId\":\"demouser\"}", HttpStatus.NO_CONTENT);
	}
}


