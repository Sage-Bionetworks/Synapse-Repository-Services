package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

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
	private DispatcherServlet servlet;
	
	// TODO put these in some sore of configuration file
	private static final String protocol = "https";
	private static final String host = "ec2-50-16-158-220.compute-1.amazonaws.com";
	private static final int port = 8443;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		servlet = helper.setUp();
		CrowdAuthUtil.setUpSSLForTesting();
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
		CrowdAuthUtil cau = new CrowdAuthUtil(protocol, host, port);
		String userId = null;
		try {
			userId = cau.revalidate(sessionToken);
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
		CrowdAuthUtil cau = new CrowdAuthUtil(protocol, host, port);
		try {
			cau.revalidate("invalidToken");
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
	public void testCreateUser() throws Exception {
		try {
			helper.testCreateJsonEntity("/user",
				"{\"userId\":\"newuser\","+
				"\"password\":\"newuser-pw\","+
				"\"email\":\"newuser@sagebase.org\","+
				"\"firstName\":\"New\","+
				"\"lastName\":\"User\","+
				"\"displayName\":\"New User\""+
					"}");
		} finally {
			CrowdAuthUtil cau = new CrowdAuthUtil(protocol, host, port);
			User user = new User();
			user.setUserId("newuser");
			cau.deleteUser(user);
		}

	}
	

	@Test
	public void testCreateAndUpdateUser() throws Exception {
		try {
			helper.testCreateJsonEntity("/user",
					"{\"userId\":\"newuser\","+
					"\"password\":\"newuser-pw\","+
					"\"email\":\"newuser@sagebase.org\","+
					"\"firstName\":\"New\","+
					"\"lastName\":\"User\","+
					"\"displayName\":\"New User\""+
						"}");
				
			helper.testUpdateJsonEntity("/user",
					"{\"userId\":\"newuser\","+
					"\"password\":\"newuser-NEWpw\","+
					"\"email\":\"NEWEMAIL@sagebase.org\","+
					"\"firstName\":\"NewNEW\","+
					"\"lastName\":\"UserNEW\","+
					"\"displayName\":\"New NEW User\""+
						"}");
		} finally {
			CrowdAuthUtil cau = new CrowdAuthUtil(protocol, host, port);
			User user = new User();
			user.setUserId("newuser");
			cau.deleteUser(user);
		}
	}
	
	@Ignore
	@Test
	public void testSendResetPasswordEmail() throws Exception {
		 helper.testCreateJsonEntity("/userPasswordEmail","{\"userId\":\"demouser\"}");
	}
}
