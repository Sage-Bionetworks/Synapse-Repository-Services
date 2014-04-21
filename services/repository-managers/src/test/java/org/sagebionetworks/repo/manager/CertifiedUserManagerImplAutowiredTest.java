package org.sagebionetworks.repo.manager;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class CertifiedUserManagerImplAutowiredTest {
	
	@Autowired
	private GroupMembersDAO groupMembersDao;
	


	// this simple round-trip makes sure that the certified user group was properly bootstrapped
	@Test
	public void testCertifiedUserGroupExistence() throws Exception {
		Long arbitraryPrincipalId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		String certifiedUserGroupId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString();
		List<String> idList = Collections.singletonList(arbitraryPrincipalId.toString());
		groupMembersDao.addMembers(certifiedUserGroupId, idList);
		groupMembersDao.removeMembers(certifiedUserGroupId, idList);
	}

}
