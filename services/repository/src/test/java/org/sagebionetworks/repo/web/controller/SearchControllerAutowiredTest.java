package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
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
	public void testFacetedSearch() throws Exception {
		SearchResults result = testSearchHelper("q=cancer&return-fields=id,name,description,etag&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference", 1);
		if(null != result) {
			assertNotNull(result.getHits().get(0).getName());
			assertNotNull(result.getFacets());
			assertTrue(11 <= result.getFacets().size());
		}
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testNoResultsFacetedSearch() throws Exception {
		SearchResults result = testSearchHelper("q=cancer&bq=node_type:'DoesNotExist'&facet=node_type,species,disease,modified_on,tissue,num_samples,created_by&return-fields=name,description,id", 0);
		if(null != result) {
			assertEquals(new Long(0), result.getFound());
			assertEquals(0, result.getHits().size());
			assertEquals(0, result.getFacets().size());
		}
	}	
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testMultiWordFreeTextSearch() throws Exception {
		testSearchHelper("q=prostate+cancer&return-fields=name&bq=node_type:'dataset'", 1);
		testSearchHelper("q=prostate cancer&return-fields=name&bq=node_type:'dataset'", 1);
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
		SearchResults result = testSearchHelper("q=prostate&return-fields=name", 1);
		if(null != result) {
			String cloudSearchMatchExpr = result.getMatchExpression();
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
		SearchResults result = testSearchHelper("q=prostate&return-fields=name", 0);
		if(null != result) {
			String cloudSearchMatchExpr = result.getMatchExpression();
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
		SearchResults result = testSearchHelper("q=prostate&return-fields=name", 1);
		if(null != result) {
			String cloudSearchMatchExpr = result.getMatchExpression();
			// We don't add an authorization filter for admin users
			assertEquals(-1, cloudSearchMatchExpr.indexOf("acl"));
		}
	}	
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testDescription() throws Exception {
		testHelper.setTestUser(TestUserDAO.ADMIN_USER_NAME);
		SearchResults result = testSearchHelper("q=prostate&return-fields=id,name,description", 1);
		if(null != result) {
			for(Hit hit : result.getHits()) {
				String description = hit.getDescription();
				if(null != description) {
					assertFalse("[]".equals(description));
				}
			}
		}
	}	

	private SearchResults testSearchHelper(String query, int expectedMinimumNumberOfResults) throws Exception {
		SearchResults searchResults = null;
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("q", query);

		try {
			searchResults = testHelper.getSearchResults(params);
			assertNotNull(searchResults);
			assertTrue(expectedMinimumNumberOfResults <= searchResults.getFound());
		} catch (ServletTestHelperException e) {
			// Its not a failure if CloudSearch is not accessible from the host
			// running this test
			if ((null == e.getMessage()) || (!e.getMessage().endsWith("Forbidden"))) {
				throw e;
			}
		}
		return searchResults;
	}
	
}
