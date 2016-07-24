package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;

public class DockerControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	private Long adminUserId;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	}

	@Test
	public void test() throws Exception {
		String service = "docker.synapse.org";
		assertNotNull(servletTestHelper.authorizeDockerAccess(dispatchServlet, adminUserId, service, null));
	}

}
