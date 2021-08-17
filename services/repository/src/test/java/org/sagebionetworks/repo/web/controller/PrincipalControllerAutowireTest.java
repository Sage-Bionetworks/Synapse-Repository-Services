package org.sagebionetworks.repo.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;

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
		assertFalse(response.getAvailable());
	}

	@Test
	public void testGetPrincipalAlias() throws Exception {
		PrincipalAliasRequest request = new PrincipalAliasRequest();
		request.setAlias("anonymous");
		request.setType(AliasType.USER_NAME);
		PrincipalAliasResponse response = servletTestHelper.getPrincipalAlias(dispatchServlet, request);
		assertNotNull(response);
		assertEquals(response.getPrincipalId(), BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
	}
}
