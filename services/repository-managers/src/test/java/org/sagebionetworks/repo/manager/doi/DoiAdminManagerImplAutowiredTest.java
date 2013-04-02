package org.sagebionetworks.repo.manager.doi;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DoiAdminManagerImplAutowiredTest {

	@Autowired private DoiAdminManager doiAdminManager;
	@Autowired private UserProvider userProvider;
	private UserInfo testUserInfo;
	private UserInfo testAdminUserInfo;

	@Before
	public void before() {
		testUserInfo = userProvider.getTestUserInfo();
		assertNotNull(testUserInfo);
		testAdminUserInfo = userProvider.getTestAdminUserInfo();
		assertNotNull(testAdminUserInfo);
	}

	@Test
	public void testAdmin() throws Exception {
		doiAdminManager.clear(testAdminUserInfo.getIndividualGroup().getName());
	}

	@Test(expected=UnauthorizedException.class)
	public void testNotAdmin() throws Exception {
		doiAdminManager.clear(testUserInfo.getIndividualGroup().getName());
	}
}
