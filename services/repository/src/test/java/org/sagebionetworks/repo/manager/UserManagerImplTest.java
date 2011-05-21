package org.sagebionetworks.repo.manager;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class UserManagerImplTest {
	
	@Autowired
	UserManager userManager;


	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		// delete all users and groups created as a side effect of calling 'getUserInfo'
	}
	
	// invoke getUserInfo for Anonymous and check returned userInfo
	//	should include Public group
	// call for non-existent user.  should get exception
	// call for a user in the User system but not the Permissions system.  
	//		verify the user gets created
	//		check the returned userInfo
	//		should include Public group
	// call for a user in a Group not in the Permissions system
	//		verify the group is created in the Permissions system
	//		check the returned userInfo
	// call for a user in the Admin group; with no Admin group in the Permissions system
	//		verify that an exception is thrown
	
	@Test
	public void testGetAnonymousUserInfo() throws Exception {
		userManager.getUserInfo(AuthUtilConstants.ANONYMOUS_USER_ID);
	}

	@Test
	public void testIdempotency() throws Exception {
		userManager.getUserInfo(AuthUtilConstants.ANONYMOUS_USER_ID);
		userManager.getUserInfo(AuthUtilConstants.ANONYMOUS_USER_ID);
	}

}
