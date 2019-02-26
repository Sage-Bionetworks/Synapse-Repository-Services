/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sagebionetworks.repo.web.controller;

import org.junit.Test;

/**
 *
 * @author xavier
 */
public class HealthCheckControllerTest extends AbstractAutowiredControllerTestBase {

	@Test
	public void testCheckAmznHealth() throws Exception {
		String s;
		s = servletTestHelper.checkAmznHealth();
		System.out.println(s);
	}
}
