package org.sagebionetworks.repo.manager;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.CertifiedUsersDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class CertifiedUserManagerImplAutowiredTest {
	
	@Autowired
	private CertifiedUsersDAO certifiedUsersDAO;
	


	// this simple round-trip makes sure that the certified user group was properly bootstrapped
	@Test
	public void testCertifiedUserGroupExistence() {
		Long arbitraryPrincipalId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		certifiedUsersDAO.addCertifiedUser(arbitraryPrincipalId, true);
		certifiedUsersDAO.removeCertifiedUser(arbitraryPrincipalId);
	}

}
