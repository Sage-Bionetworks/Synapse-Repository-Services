package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.unsuccessfulattemptlockout.UnsuccessfulAttemptLockoutException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnsuccessfulAttemptLockoutDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AuthenticationManagerImplAutowiredTest {
	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserManager userManager;

	@Autowired
	UnsuccessfulAttemptLockoutDAO unsuccessfulAttemptLockoutDAO;

	Long createdUserId = null;

	@Before
	public void setup(){
		if(createdUserId == null) {
			NewUser newUser = new NewUser();
			newUser.setUserName("someUsername");
			newUser.setEmail("myTestEmail@testEmailthingy.com");

			createdUserId = userManager.createUser(newUser);

			String password = UUID.randomUUID().toString();
			authenticationManager.setPassword(createdUserId, password);

			assertNotNull(authenticationManager.login(createdUserId, password, null));
		}
	}

	@After
	public void tearDown(){
		if(createdUserId != null) {
			userManager.deletePrincipal(new UserInfo(true, 42L), createdUserId);
		}
		unsuccessfulAttemptLockoutDAO.truncateTable();
	}

	@Test
	public void testLoginWrongPassword_Lockout(){
		String wrongPassword = "hunter2";
		try {
			for (int i = 0; i < 10; i++) {
				try {
					authenticationManager.login(createdUserId, wrongPassword, null);
					fail("expected exception to be throw for wrong password");
				} catch (UnauthenticatedException e) {
					//expected
				}
			}
		} catch (UnsuccessfulAttemptLockoutException e){
			//expected. return early to avoid fail condition
			return;
		}

		fail("expected UnsuccessfulAttemptLockoutException to be thrown after many failed login attempts");
	}
}
