package org.sagebionetworks.repo.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.sagebionetworks.repo.model.AuthorizationConstants.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityAuthorizationManagerTest {

	@Autowired
	private UserManager userManager;

	@Autowired
	private EntityAuthorizationManager entityAuthManager;

	@Autowired
	private EntityPermissionsManager entityPermissionManager;

	private UserInfo adminUserInfo;
	private UserInfo userOne;

	@BeforeEach
	public void before() {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);

		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");

		// Need two users for this test
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		userOne = userManager.createOrGetTestUser(adminUserInfo, nu, cred, tou);
	}

	@Test
	public void testHasAccessWithEntityDoesNotExist() {
		String entityId = "syn123";
		UserInfo user = userOne;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;
		// old call under test
		String oldMessage = assertThrows(NotFoundException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(NotFoundException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND, newMessage);
	}

}
