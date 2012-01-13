package org.sagebionetworks.repo.web.controller;


import static org.junit.Assert.assertNotNull;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SearchControllerTest {

	@Autowired
	private ServletTestHelper testHelper;
	
	@Before
	public void setUp() throws Exception {
		testHelper.setUp();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSearch() throws Exception {
		Map<String, String> params = new HashMap<String,String>();
		params.put("q", "q=prostate&return-fields=name");
		JSONObject result = testHelper.getSearchResults(params);
		assertNotNull(result);
		JSONObject searchResult = new JSONObject(result.getString("result"));
		System.out.println("foo " + searchResult.toString(4));
	}
	
}
