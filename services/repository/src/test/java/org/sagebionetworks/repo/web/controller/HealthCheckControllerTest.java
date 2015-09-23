/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sagebionetworks.repo.web.controller;

import org.junit.Test;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author xavier
 */
public class HealthCheckControllerTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	UserGroupDAO userGroupDAO;
	
	
	@Test
	public void testCheckAmznHealth() throws Exception {
		String s;
		s = servletTestHelper.checkAmznHealth();
		System.out.println(s);
	}
}
