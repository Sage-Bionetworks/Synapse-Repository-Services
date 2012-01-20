package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * TODO these are actually integration tests since they hit CloudSearch for
 * real, consider moving them to the integration test suite
 * 
 * @author deflaux
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SearchControllerAutowiredTest {

	private static final Logger log = Logger
			.getLogger(SearchControllerAutowiredTest.class.getName());

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
		testSearchHelper("q=prostate&return-fields=name", 1);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testBooleanQuerySearch() throws Exception {
		testSearchHelper("q=prostate&return-fields=name&bq=node_type:'dataset'", 1);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testSearchAuthorizationFilter() throws Exception {
		JSONObject result = testSearchHelper("q=prostate&return-fields=name", 1);
		if(null != result) {
			String cloudSearchMatchExpr = result.getString("match-expr");
			assertTrue(-1 < cloudSearchMatchExpr.indexOf("(or acl:"));
			assertTrue(-1 < cloudSearchMatchExpr.indexOf("acl:'test-user@sagebase.org'"));
			assertTrue(-1 < cloudSearchMatchExpr.indexOf("acl:'test-group'"));
			assertTrue(-1 < cloudSearchMatchExpr.indexOf("acl:'AUTHENTICATED_USERS'"));
			assertTrue(-1 < cloudSearchMatchExpr.indexOf("acl:'PUBLIC'"));
		}
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testAnonymousSearchAuthorizationFilter() throws Exception {
		testHelper.setTestUser(AuthorizationConstants.ANONYMOUS_USER_ID);
		JSONObject result = testSearchHelper("q=prostate&return-fields=name", 0);
		if(null != result) {
			String cloudSearchMatchExpr = result.getString("match-expr");
			assertTrue(-1 < cloudSearchMatchExpr.indexOf("(or acl:"));
			assertTrue(-1 < cloudSearchMatchExpr.indexOf("acl:'anonymous@sagebase.org'", 0));
			assertTrue(-1 < cloudSearchMatchExpr.indexOf("acl:'PUBLIC'"));
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testAdminSearchAuthorizationFilter() throws Exception {
		testHelper.setTestUser(TestUserDAO.ADMIN_USER_NAME);
		JSONObject result = testSearchHelper("q=prostate&return-fields=name", 1);
		if(null != result) {
			String cloudSearchMatchExpr = result.getString("match-expr");
			// We don't add an authorization filter for admin users
			assertEquals(-1, cloudSearchMatchExpr.indexOf("acl"));
		}
	}	

	private JSONObject testSearchHelper(String query, int expectedMinimumNumberOfResults) throws Exception {
		JSONObject searchResult = null;
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("q", query);

		try {
			JSONObject result = testHelper.getSearchResults(params);
			assertNotNull(result);
			searchResult = new JSONObject(result.getString("result"));
			log.info(searchResult.toString(4));
			assertTrue(expectedMinimumNumberOfResults <= searchResult.getJSONObject("hits").getInt("found"));
		} catch (ServletTestHelperException e) {
			// Its not a failure if CloudSearch is not accessible from the host
			// running this test
			if ((null == e.getMessage()) || (!e.getMessage().endsWith("Forbidden"))) {
				throw e;
			}
		}
		return searchResult;
	}
	
}
