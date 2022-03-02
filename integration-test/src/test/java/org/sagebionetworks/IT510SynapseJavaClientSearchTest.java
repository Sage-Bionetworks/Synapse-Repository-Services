package org.sagebionetworks;


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseClient;
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
@ExtendWith(ITTestExtension.class)
public class IT510SynapseJavaClientSearchTest {
	
	private static final long MAX_WAIT_TIME_MS = 2*60*1000; // 2 min
	
	/**
	 * All objects are added to this project.
	 */
	private static Project project;
	
	private SynapseClient synapse;
	
	public IT510SynapseJavaClientSearchTest(SynapseClient synapse) {
		this.synapse = synapse;
	}
	
	@BeforeAll
	public static void beforeClass(StackConfiguration config, SynapseClient synapse) throws Exception {
		// Only run this test if search is enabled.
		assumeTrue(config.getSearchEnabled());
		
		// Setup a project for this test.
		project = new Project();
		project.setDescription("This is a base project to hold entites for test: "+IT510SynapseJavaClientSearchTest.class.getName());
		project = synapse.createEntity(project);

	}
	
	@AfterAll
	public static void afterClass(StackConfiguration config, SynapseClient synapse) throws Exception {
		// There's nothing to do if search is disabled
		if (!config.getSearchEnabled()) {
			return;
		}
		
		if (synapse != null && project != null) {
			synapse.deleteEntity(project);
		}
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
	private void waitForId(String id) throws UnsupportedEncodingException, SynapseException, JSONObjectAdapterException, InterruptedException{
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
			assertTrue(elapse < MAX_WAIT_TIME_MS, "Timed out waiting for entity to be published to the search index, id: "+id);
		}
	}


}
