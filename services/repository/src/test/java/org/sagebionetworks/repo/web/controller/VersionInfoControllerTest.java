package org.sagebionetworks.repo.web.controller;

import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;

import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.sagebionetworks.repo.model.UserGroupDAO;

/**
 *
 * @author xschildw
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class VersionInfoControllerTest {

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
	public void testGetVersionInfo() throws Exception {
		String s;
		s = testHelper.getVersionInfo();
		assertTrue(s.length() > 0);
	}
	
}
