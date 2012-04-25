package org.sagebionetworks.repo.web.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SearchControllerAutowiredTest {

	@Autowired
	private ServletTestHelper testHelper;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		testHelper.setUp();
	}

	/**
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testSearch() throws Exception {
		// All search tests are really integration tests, so they've all been moved to integration-test/IT510SynapseJavaClientSearchTest.java
		
		// If we ever decided to mock the implementation of CloudSearch, then tests against that mock could go here
		
		// see testHelper.getSearchResults()		
	}

}
