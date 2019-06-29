package org.sagebionetworks;

import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Project;
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

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private static final long MAX_WAIT_TIME_MS = 15*60*1000; // 15 min
	
	/**
	 * All objects are added to this project.
	 */
	private static Project project;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		StackConfiguration config = StackConfigurationSingleton.singleton();
		// Only run this test if search is enabled.
		Assume.assumeTrue(config.getSearchEnabled());
		
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(config.getMigrationAdminUsername());
		adminSynapse.setApiKey(config.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
		
		// Setup a project for this test.
		project = new Project();
		project.setDescription("This is a base project to hold entites for test: "+IT510SynapseJavaClientSearchTest.class.getName());
		project = synapse.createEntity(project);

	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		StackConfiguration config = StackConfigurationSingleton.singleton();
		// There's nothing to do if search is disabled
		if (!config.getSearchEnabled()) {
			return;
		}
		
		if (synapse != null && project != null) {
			synapse.deleteAndPurgeEntity(project);
		}
		
		adminSynapse.deleteUser(userToDelete);
	}
	
	@Test
	public void testSearch() throws Exception{
		// wait for the project to appear in the search
		waitForId(project.getId());
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
		kv.setKey("_id");
		kv.setValue(id);
		searchQuery.getBooleanQuery().add(kv);
		long start = System.currentTimeMillis();
		while(true){
			SearchResults results = synapse.search(searchQuery);
			if (results.getFound() == 1) {
				System.out.println("Found entity " + id + " in search index");
				return;
			}
			System.out.println("Waiting for entity to be published to the search index, id: "+id+"...");
			Thread.sleep(2000);
			long elapse = System.currentTimeMillis()-start;
			assertTrue("Timed out waiting for entity to be published to the search index, id: "+id,elapse < MAX_WAIT_TIME_MS);
		}
	}


}
