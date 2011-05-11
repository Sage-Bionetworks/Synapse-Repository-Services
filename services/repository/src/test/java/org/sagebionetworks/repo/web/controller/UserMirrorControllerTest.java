package org.sagebionetworks.repo.web.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit tests for the User-Mirror service
 * 
 * To run:
 * mvn test -Dtest=UserMirrorControllerTest -DAUTHENTICATION_SERVICE_URL=https://ssl.latest.deflaux-test.appspot.com
 * 
 * @author bhoff
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserMirrorControllerTest {
	
	
	@Test
	public void dummy() {
		// pass!
	}
//
//	@Autowired
//	private Helpers helper;
//	
//	private CrowdAuthUtil crowdAuthUtil = new CrowdAuthUtil();
//
//	public static Iterable<String> iterableFromEnumeration(final Enumeration<String> e) {
//		return new Iterable<String>() {
//			public Iterator<String> iterator() {
//				return new Iterator<String>() {
//	
//					public boolean hasNext() {
//						return e.hasMoreElements();
//					}
//	
//					public String next() {
//						return e.nextElement();
//					}
//	
//					public void remove() {
//						throw new UnsupportedOperationException("Not implemented.");
//					}
//				};
//			}
//		};
//	}
//	/**
//	 * @throws java.lang.Exception
//	 */
//	@Before
//	public void setUp() throws Exception {
//		HttpServlet servlet = helper.setUp();
//		// initialize users and groups (i.e. add admin user)
//		CrowdAuthUtil.acceptAllCertificates();
//	}
//
//	/**
//	 * @throws java.lang.Exception
//	 */
//	@After
//	public void tearDown() throws Exception {
//		helper.tearDown();
//	}
//
//
//	/**
//	 * 
//	 * test only runs when "AUTHENTICATION_SERVICE_URL" env var is set
//		check that account doesn't exist in persistence layer
//		'log in' as self
//		create account in Crowd
//		run mirror service via the mock servlet
//		check that account exists in PL
//		remove account from Crowd
//		remove account from PL
//
//	 * @throws Exception
//	 */
//	@Test
//	public void testUserMirror() throws Exception {
//		assertNotNull(crowdAuthUtil);
//		
//		String authenticationServiceURL = System.getProperty("AUTHENTICATION_SERVICE_URL");
//		if (authenticationServiceURL==null) return; // we are not in 'integration test' mode
//		String userId = "user-org.sagebionetworks.repo.web.controller.UserMirrorControllerTest"; // crowdAuthUtil.getIntegrationTestUser();
////		'log in' as admin
//		UserDAO userDAO  = daoFactory.getUserDAO(AuthUtilConstants.ADMIN_USER_ID);
////		check that account doesn't exist in persistence layer
//		try {
//			/*Collection<User> users = */
//			userDAO.getInRangeHavingPrimaryField(0, 1, "userId", userId);
//			// exception occurs because the userId passed to the DAO doesn't exist
//			fail("Exception expected");
//		} catch (NotFoundException nfe) {
//			//as expected
//		}		
//		{
//			org.sagebionetworks.authutil.User crowdUser = new org.sagebionetworks.authutil.User();
//			crowdUser.setUserId(userId);
//			crowdUser.setPassword(userId);
//			crowdUser.setEmail("dummy@sagebase.org"); // won't be used...
//			crowdUser.setFirstName("First");
//			crowdUser.setLastName("Last");
//			crowdUser.setDisplayName("First Last");
//			try {
//				crowdAuthUtil.createUser(crowdUser);
//			} catch (AuthenticationException ae) {
//				if (ae.getRespStatus()==HttpStatus.BAD_REQUEST.value()) {
//					// then the user already exists, so continue...
//				} else {
//					throw ae;
//				}
//			}
//		}
////		run mirror service via the mock servlet
////		Map<String,String> headers = new HashMap<String,String>();
//		helper.testCreateNoResponse("/userMirror?"+AuthUtilConstants.USER_ID_PARAM+"="+AuthUtilConstants.ADMIN_USER_ID ,"{}");
////		check that account exists in PL
//		{
//			Collection<User> users = userDAO.getInRangeHavingPrimaryField(0, 1, "userId", userId);
//			assertEquals(1, users.size());
//		}
//		{
//			org.sagebionetworks.authutil.User crowdUser = new org.sagebionetworks.authutil.User();
//			crowdUser.setUserId(userId);
//			crowdAuthUtil.deleteUser(crowdUser);
//		}
////		run mirror service via the mock servlet
//		helper.testCreateNoResponse("/userMirror" ,"");
////		check that account doesn't exist in persistence layer
//		try {
//			Collection<User> users = userDAO.getInRangeHavingPrimaryField(0, 1, "userId", userId);
//			// exception occurs because the userId passed to the DAO doesn't exist
//			fail("Exception expected");
//		} catch (NotFoundException nfe) {
//			//as expected
//		}
//	}



}
