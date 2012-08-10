package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;

/**
 * Run this integration test as a sanity check to ensure our Synapse Java Client
 * is working
 * 
 * @author deflaux
 */
@Ignore
public class IT510SynapseJavaClientSearchTest {

	private static Synapse synapse = null;

	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testSearch() throws Exception {
		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add("cancer");
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("id");
		returnFields.add("name");
		searchQuery.setReturnFields(returnFields);
		SearchResults results = synapse.search(searchQuery);

		assertTrue(10 < results.getFound());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testAllReturnFields() throws Exception {
		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add("syn4494");
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("id");
		returnFields.add("name");
		returnFields.add("path");
		returnFields.add("description");
		returnFields.add("etag");
		returnFields.add("modified_on");
		returnFields.add("created_on");
		returnFields.add("num_samples");
		returnFields.add("created_by_r");
		returnFields.add("modified_by_r");
		returnFields.add("node_type_r");
		returnFields.add("disease_r");
		returnFields.add("tissue_r");
		searchQuery.setReturnFields(returnFields);

		SearchResults results = synapse.search(searchQuery);

		assertTrue(new Long(0) < results.getFound());

		Hit hit = results.getHits().get(0);
		assertNotNull(hit.getId());
		assertNotNull(hit.getName());
		assertNotNull(hit.getPath());
		assertNotNull(hit.getDescription());
		assertNotNull(hit.getEtag());
		assertNotNull(hit.getModified_on());
		assertNotNull(hit.getCreated_on());
		assertNotNull(hit.getNum_samples());
		assertNotNull(hit.getCreated_by());
		assertNotNull(hit.getModified_by());
		assertNotNull(hit.getNode_type());
		assertNotNull(hit.getDisease());
		assertNotNull(hit.getTissue());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testFacetedSearch() throws Exception {
		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add("cancer");
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("id");
		returnFields.add("name");
		returnFields.add("description");
		searchQuery.setReturnFields(returnFields);
		List<String> facets = new ArrayList<String>();
		facets.add("node_type");
		facets.add("disease");
		facets.add("species");
		facets.add("tissue");
		facets.add("platform");
		facets.add("num_samples");
		facets.add("created_by");
		facets.add("modified_by");
		facets.add("created_on");
		facets.add("modified_on");
		facets.add("acl");
		facets.add("reference");
		searchQuery.setFacet(facets);

		SearchResults results = synapse.search(searchQuery);

		assertNotNull(results.getHits().get(0).getName());
		assertNotNull(results.getFacets());
		assertTrue(11 <= results.getFacets().size());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testNoResultsFacetedSearch() throws Exception {
		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add("cancer");
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("id");
		returnFields.add("name");
		returnFields.add("description");
		searchQuery.setReturnFields(returnFields);
		List<String> facets = new ArrayList<String>();
		facets.add("node_type");
		facets.add("disease");
		facets.add("species");
		facets.add("tissue");
		facets.add("platform");
		facets.add("num_samples");
		facets.add("created_by");
		facets.add("modified_by");
		facets.add("created_on");
		facets.add("modified_on");
		facets.add("acl");
		facets.add("reference");
		searchQuery.setFacet(facets);
		KeyValue doesNotExist = new KeyValue();
		doesNotExist.setKey("node_type");
		doesNotExist.setValue("DoesNotExist");
		List<KeyValue> booleanQuery = new ArrayList<KeyValue>();
		booleanQuery.add(doesNotExist);
		searchQuery.setBooleanQuery(booleanQuery);
		SearchResults results = synapse.search(searchQuery);

		assertEquals(new Long(0), results.getFound());
		assertEquals(0, results.getHits().size());
		assertEquals(0, results.getFacets().size());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testMultiWordFreeTextSearch() throws Exception {
		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add("prostate cancer");
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("name");
		searchQuery.setReturnFields(returnFields);
		KeyValue booleanQueryClause = new KeyValue();
		booleanQueryClause.setKey("node_type");
		booleanQueryClause.setValue("study");
		List<KeyValue> booleanQuery = new ArrayList<KeyValue>();
		booleanQuery.add(booleanQueryClause);
		searchQuery.setBooleanQuery(booleanQuery);
		SearchResults results = synapse.search(searchQuery);

		assertTrue(1 <= results.getFound());

		// try url-escaped space too
		queryTerms = new ArrayList<String>();
		queryTerms.add("prostate+cancer");
		searchQuery.setQueryTerm(queryTerms);
		results = synapse.search(searchQuery);

		assertTrue(1 <= results.getFound());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testBooleanQuerySearch() throws Exception {
		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add("cancer");
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("name");
		searchQuery.setReturnFields(returnFields);
		KeyValue booleanQueryClause = new KeyValue();
		booleanQueryClause.setKey("node_type");
		booleanQueryClause.setValue("study");
		List<KeyValue> booleanQuery = new ArrayList<KeyValue>();
		booleanQuery.add(booleanQueryClause);
		searchQuery.setBooleanQuery(booleanQuery);
		SearchResults results = synapse.search(searchQuery);

		assertTrue(1 <= results.getFound());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testParentIdBooleanQuerySearch() throws Exception {
		SearchQuery searchQuery = new SearchQuery();
		KeyValue booleanQueryClause = new KeyValue();
		booleanQueryClause.setKey("parent_id");
		booleanQueryClause.setValue("syn4492");
		List<KeyValue> booleanQuery = new ArrayList<KeyValue>();
		booleanQuery.add(booleanQueryClause);
		searchQuery.setBooleanQuery(booleanQuery);
		SearchResults results = synapse.search(searchQuery);

		assertTrue(5 <= results.getFound());
	}
	
	/**
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testUpdateACLBooleanQuerySearch() throws Exception {
		SearchQuery searchQuery = new SearchQuery();
		KeyValue booleanQueryClause = new KeyValue();
		booleanQueryClause.setKey("update_acl");
		booleanQueryClause.setValue("Sage Curators");
		List<KeyValue> booleanQuery = new ArrayList<KeyValue>();
		booleanQuery.add(booleanQueryClause);
		searchQuery.setBooleanQuery(booleanQuery);
		SearchResults results = synapse.search(searchQuery);

		assertTrue(1 <= results.getFound());
	}
	
	private static String getGroupPrincipalIdFromGroupName(String groupName) throws SynapseException {
		PaginatedResults<UserGroup> paginated = synapse.getGroups(0,100);
		int total = (int)paginated.getTotalNumberOfResults();
		List<UserGroup> groups = paginated.getResults();
		if (groups.size()<total) throw new RuntimeException("System has "+total+" total users but we've only retrieved "+groups.size());
		for (UserGroup group : groups) {
			if (group.getName().equalsIgnoreCase(groupName)) return group.getId();
		}
		throw new RuntimeException("Cannot find "+groupName+" among groups.");
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testSearchAuthorizationFilter() throws Exception {
		UserProfile myProfile = synapse.getMyProfile();
		assertNotNull(myProfile);
		String myPrincipalId = myProfile.getOwnerId();
		assertNotNull(myPrincipalId);
		
		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add("cancer");
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("name");
		searchQuery.setReturnFields(returnFields);
		SearchResults results = synapse.search(searchQuery);
		
		
		String cloudSearchMatchExpr = results.getMatchExpression();
		assertTrue(-1 < cloudSearchMatchExpr.indexOf("(or acl:"));
		assertTrue(-1 < cloudSearchMatchExpr
				.indexOf("acl:'" + myPrincipalId + "'"));
		assertTrue(-1 < cloudSearchMatchExpr
				.indexOf("acl:'"+getGroupPrincipalIdFromGroupName("AUTHENTICATED_USERS")+"'"));
		assertTrue(-1 < cloudSearchMatchExpr.indexOf("acl:'"+getGroupPrincipalIdFromGroupName("PUBLIC")+"'"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testAnonymousSearchAuthorizationFilter() throws Exception {
		
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
		
		String publicPrincipalId = getGroupPrincipalIdFromGroupName("PUBLIC");

		// now 'log out'
		synapse.setSessionToken(null);

		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add("prostate");
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("name");
		searchQuery.setReturnFields(returnFields);
		SearchResults results = synapse.search(searchQuery);

		String cloudSearchMatchExpr = results.getMatchExpression();
		assertTrue(-1 < cloudSearchMatchExpr.indexOf("(or acl:"));
		
		// TODO reenable the following line, which depends on how the 'display name' for 'anonymous' is configured
		//assertTrue(-1 < cloudSearchMatchExpr.indexOf("acl:'anonymous@sagebase.org'", 0));
		
		assertTrue(-1 < cloudSearchMatchExpr.indexOf("acl:'"+publicPrincipalId+"'"));

		// We are reusing this client, so restore the prior logged in user
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testAdminSearchAuthorizationFilter() throws Exception {
		// Login as an administrator
		synapse.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());

		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add("prostate");
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("name");
		searchQuery.setReturnFields(returnFields);
		SearchResults results = synapse.search(searchQuery);

		String cloudSearchMatchExpr = results.getMatchExpression();

		// We don't add an authorization filter for admin users
		assertEquals(-1, cloudSearchMatchExpr.indexOf("acl"));

		// We are reusing this client, so restore the prior logged in user
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDescription() throws Exception {

		// When descriptions are not null, make sure they are not just an empty
		// json array (this was an old bug)

		SearchQuery searchQuery = new SearchQuery();
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("description");
		searchQuery.setReturnFields(returnFields);
		KeyValue booleanQueryClause = new KeyValue();
		booleanQueryClause.setKey("node_type");
		booleanQueryClause.setValue("*");
		List<KeyValue> booleanQuery = new ArrayList<KeyValue>();
		booleanQuery.add(booleanQueryClause);
		searchQuery.setBooleanQuery(booleanQuery);
		SearchResults results = synapse.search(searchQuery);

		assertTrue(1 <= results.getFound());

		for (Hit hit : results.getHits()) {
			String description = hit.getDescription();
			if (null != description) {
				assertFalse("[]".equals(description));
			}
		}
	}
}
