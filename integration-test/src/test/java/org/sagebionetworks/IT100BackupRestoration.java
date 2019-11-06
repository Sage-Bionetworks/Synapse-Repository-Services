package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.status.StackStatus;

/**
 * This test will push data from a backup into synapse.
 * 
 * @author jmhill
 * 
 */
public class IT100BackupRestoration {

	private static SynapseAdminClient adminSynapse;

	public static final long TEST_TIME_OUT = 1000 * 60 * 4; // Currently 4 mins
	
	private List<Entity> toDelete = null;

	@BeforeClass
	public static void beforeClass() throws Exception {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
	}
	
	@Before
	public void before()throws Exception {
		toDelete = new ArrayList<Entity>();
	}
	
	@After
	public void after() throws Exception {
		if(adminSynapse != null && toDelete != null){
			for(Entity e: toDelete){
				adminSynapse.deleteAndPurgeEntity(e);
			}
		}
	}
	
	
	@Test
	public void testGetAndUpdateStatus() throws Exception {
		StackStatus status = adminSynapse.getCurrentStackStatus();
		assertNotNull(status);
		// Set the status
		status.setPendingMaintenanceMessage("Testing that we can set the pending message");
		StackStatus updated = adminSynapse.updateCurrentStackStatus(status);
		assertEquals(status, updated);
		// Clear out the message
		status.setPendingMaintenanceMessage(null);
		updated = adminSynapse.updateCurrentStackStatus(status);
		assertEquals(status, updated);
	}
	
	/**
	 * Extract the filename from the full url.
	 * @param fullUrl
	 * @return
	 */
	public String getFileNameFromUrl(String fullUrl){;
		int index = fullUrl.lastIndexOf("/");
		return fullUrl.substring(index+1, fullUrl.length());
	}

}
