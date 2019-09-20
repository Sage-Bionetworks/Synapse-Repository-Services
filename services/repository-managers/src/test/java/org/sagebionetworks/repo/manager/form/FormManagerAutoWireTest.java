package org.sagebionetworks.repo.manager.form;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FormManagerAutoWireTest {
	
	@Autowired
	FormManager formManager;
	@Autowired
	UserManager userManager;
	
	UserInfo adminUserInfo;
	
	@BeforeEach
	public void beforeEach() {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}

	/**
	 * Test for PLFM-5821.
	 */
	@Test
	public void testCreateGroup() {
		String groupName = "Group Name";
		FormGroup group = formManager.createGroup(adminUserInfo, groupName);
		assertNotNull(group);
		AccessControlList acl = formManager.getGroupAcl(adminUserInfo, group.getGroupId());
		assertNotNull(acl);
	}
}
