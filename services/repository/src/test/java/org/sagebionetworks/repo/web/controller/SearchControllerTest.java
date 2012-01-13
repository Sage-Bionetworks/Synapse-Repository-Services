package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
		Map<String, String> params = new HashMap<String, String>();
		params.put("q", "q=prostate&return-fields=name");

		try {
			JSONObject result = testHelper.getSearchResults(params);
			assertNotNull(result);
			JSONObject searchResult = new JSONObject(result.getString("result"));
		} catch (ServletTestHelperException e) {
			// Its not a failure if CloudSearch is not accessible from the host
			// running this test
			if (!e.getMessage().endsWith("Forbidden")) {
				fail("fail");
			}
		}
	}

}
