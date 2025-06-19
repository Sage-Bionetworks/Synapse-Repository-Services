package org.sagebionetworks.repo.manager.doi;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DoiAdminManagerImplAutowiredTest {

	@Autowired 
	private DoiAdminManager doiAdminManager;
	
	@Autowired
	private UserManager userManager;
	
	private Long adminUserId;
	private Long testUserId;
	private UserInfo adminUserInfo;

	@BeforeEach
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		testUserId = userManager.createUser(user);
	}
	
	@AfterEach
	public void after() throws Exception {
		userManager.deletePrincipal(adminUserInfo, testUserId);
	}

	@Test
	public void testAdmin() throws Exception {
		doiAdminManager.clear(adminUserId);
	}

	@Test
	public void testNotAdmin() throws Exception {
		assertThrows(UnauthorizedException.class, () -> {			
			doiAdminManager.clear(testUserId);
		});
	}
}
