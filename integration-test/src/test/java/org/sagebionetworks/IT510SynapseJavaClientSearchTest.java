package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Run this integration test as a sanity check to ensure our Synapse Java Client
 * is working
 * 
 * @author deflaux
 */
public class IT510SynapseJavaClientSearchTest {
	
	
	public static final long MAX_WAIT_TIME_MS = 10*60*1000; // ten min.

	private static Synapse synapse = null;
	
	/**
	 * All objects are added to this project.
	 */
	private static Project project;
	private static List<Data> dataList;
	static private long entitesWithDistictValue = 5;
	static private String distictValue1;
	static private String distictValue2;

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
		
		// Setup all of the objects for this test.
		project = new Project();
		project.setDescription("This is a base project to hold entites for test: "+IT510SynapseJavaClientSearchTest.class.getName());
		project = synapse.createEntity(project);
		List<String> idsToWaitFor = new LinkedList<String>();
		idsToWaitFor.add(project.getId());
		// We use the project's etag as a distict string
		distictValue1 = project.getEtag();
		distictValue2 = project.getId();
		dataList = new LinkedList<Data>();
		// Add some data with unique Strings
		for(int i=0; i<entitesWithDistictValue; i++){
			Data data = new Data();
			data.setParentId(project.getId());
			// Use the etag as a description.
			data.setDescription(distictValue1+" "+distictValue2);
			data.setDisease("disease"+i);
			data.setNumSamples(new Long(i));
			data.setPlatform("platform"+i);
			data.setTissueType("tissue"+i);
			data.setSpecies("species"+i);
			data = synapse.createEntity(data);
			idsToWaitFor.add(data.getId());
			dataList.add(data);
		}
		// Wait for all IDs to show up in the search index.
		waitForIds(idsToWaitFor);
	}
	
	@AfterClass
	public static void afterClass() throws SynapseException{
		if(synapse != null && project != null){
			synapse.deleteAndPurgeEntity(project);
		}
	}
	
	/**
	 * Wait for ids to be published to the search index.
	 * @param idList
	 * @throws UnsupportedEncodingException
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 * @throws InterruptedException
	 */
	private static void waitForIds(List<String> idList) throws UnsupportedEncodingException, SynapseException, JSONObjectAdapterException, InterruptedException{
		// Wait for all entities on the list
		for(String id: idList){
			waitForId(id);
		}
	}

	/**
	 * Helper to wait for a single entity ID to be published to a search index.
	 * @param id
	 * @throws UnsupportedEncodingException
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 * @throws InterruptedException
	 */
	private static void waitForId(String id) throws UnsupportedEncodingException, SynapseException, JSONObjectAdapterException, InterruptedException{
		SearchQuery searchQuery = new SearchQuery();
		searchQuery.setBooleanQuery(new LinkedList<KeyValue>());
		KeyValue kv = new KeyValue();
		kv.setKey("id");
		kv.setValue(id);
		searchQuery.getBooleanQuery().add(kv);
		long start = System.currentTimeMillis();
		while(true){
			SearchResults results = synapse.search(searchQuery);
			if(results.getFound() == 1) return;
			System.out.println("Waiting for entity to be published to the search index, id: "+id+"...");
			Thread.sleep(2000);
			long elapse = System.currentTimeMillis()-start;
			assertTrue("Timed out waiting for entity to be published to the search index, id: "+id,elapse < MAX_WAIT_TIME_MS);
		}
	}

	/**
	 * @throws Exception
	 */
	@Ignore // This test does not seem stable.
	@Test
	public void testSearch() throws Exception {
		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add(distictValue1);
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("id");
		returnFields.add("name");
		searchQuery.setReturnFields(returnFields);
		SearchResults results = synapse.search(searchQuery);
		assertNotNull(results);
		assertNotNull(results.getFound());
		assertEquals(entitesWithDistictValue, results.getFound().longValue());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testAllReturnFields() throws Exception {
		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		// Lookup the first data object by its id.
		queryTerms.add(dataList.get(0).getId());
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
	@Ignore // This test does not appear to be stable.
	@Test
	public void testFacetedSearch() throws Exception {
		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add(distictValue1);
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
		assertEquals(entitesWithDistictValue, results.getFound().longValue());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testNoResultsFacetedSearch() throws Exception {
		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add(distictValue1);
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
		queryTerms.add(distictValue1+" "+distictValue2);
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("name");
		searchQuery.setReturnFields(returnFields);
		SearchResults results = synapse.search(searchQuery);

		assertTrue(1 <= results.getFound());

		// try url-escaped space too
		queryTerms = new ArrayList<String>();
		queryTerms.add(distictValue1+"+"+distictValue2);
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
		queryTerms.add(distictValue1);
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("name");
		searchQuery.setReturnFields(returnFields);
		KeyValue booleanQueryClause = new KeyValue();
		booleanQueryClause.setKey("node_type");
		booleanQueryClause.setValue("data");
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
		booleanQueryClause.setValue(project.getId());
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
		queryTerms.add(distictValue1);
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
		queryTerms.add(distictValue2);
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
		queryTerms.add(distictValue2);
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
	
	@Test
	public void testBadSearch() throws ServletException, IOException, JSONException, JSONObjectAdapterException, InterruptedException {
		// First run query
		SearchQuery query = new SearchQuery();
		query.setBooleanQuery(new LinkedList<KeyValue>());
		KeyValue kv = new KeyValue();
		kv.setKey("ugh");
		kv.setValue(project.getId());
		query.getBooleanQuery().add(kv);
		// this should throw an error
		try{
			synapse.search(query);
			fail("This was a bad query");
		}catch (SynapseException e) {
			// did we get the expected message.
			assertTrue(e.getMessage().indexOf("'ugh' is not defined in the metadata for this collection") > 0);
			assertFalse("The error message contains the URL of the search index", e.getMessage().indexOf("http://search") > 0);
		}
	}
}
