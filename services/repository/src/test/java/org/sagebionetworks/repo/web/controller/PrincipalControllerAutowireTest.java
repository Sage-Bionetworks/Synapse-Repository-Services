package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.servlet.ServletException;

import org.junit.Test;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.model.principal.AliasType;

public class PrincipalControllerAutowireTest extends AbstractAutowiredControllerTestBase {

	
	@Test
	public void testCheckAvailable() throws Exception {
		AliasCheckRequest request = new AliasCheckRequest();
		// This is valid but already in use
		request.setAlias("anonymous");
		request.setType(AliasType.USER_NAME);
		AliasCheckResponse response = servletTestHelper.checkAlias(dispatchServlet, request);
		assertNotNull(response);
		assertTrue(response.getValid());
		assertFalse("The 'anonymous' users should already have this alias so it cannot be available!",response.getAvailable());
	}
}
