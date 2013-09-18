/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sagebionetworks.repo.web.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author xavier
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class HealthCheckControllerTest {

	@Autowired
	ServletTestHelper testHelper;
	@Autowired
	UserGroupDAO userGroupDAO;
	
	@Before
	public void before() throws Exception {
		testHelper.setUp();
	}
	
	@After
	public void after() throws Exception {
		testHelper.tearDown();
	}

	
	@Test
	public void testCheckAmznHealth() throws Exception {
		String s;
		s = testHelper.checkAmznHealth();
		System.out.println(s);
	}
}
