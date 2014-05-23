package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.servlet.ServletException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

public class PrincipalControllerAutowireTest extends AbstractAutowiredControllerTestBase {

	
	@Test
	public void testChackAvailable() throws ServletException, Exception{
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
