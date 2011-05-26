package org.sagebionetworks.repo.manager;


import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.junit.runner.RunWith;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class UserManagerImplTest {
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	UserGroupDAO userGroupDAO;

	private List<UserGroup> toDelete = new ArrayList<UserGroup>();
	
	@Before
	public void setUp() throws Exception {
		userManager.setUserDAO(new TestUserDAO());
	}

	@After
	public void tearDown() throws Exception {
		for (UserGroup g : toDelete) userGroupDAO.delete(g.getId());
	}
	
	@Test
	public void testGetAnonymous() throws Exception {
		UserInfo ui = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		assertEquals(AuthorizationConstants.ANONYMOUS_USER_ID, ui.getUser().getId());
		
		
	}
	
	// invoke getUserInfo for Anonymous and check returned userInfo
	//	should include Public group
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
