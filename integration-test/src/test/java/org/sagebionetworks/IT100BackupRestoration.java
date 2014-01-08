package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

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
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
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

	/**
	 * Wait for a daemon to complete.
	 * @param daemonId
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 * @throws JSONObjectAdapterException
	 * @throws InterruptedException
	 * @throws SynapseException 
	 */
	public BackupRestoreStatus waitForDaemon(String daemonId) throws SynapseException, JSONObjectAdapterException, InterruptedException{
		// Wait for the daemon to finish.
		long start = System.currentTimeMillis();
		while (true) {
			long now = System.currentTimeMillis();
			assertTrue("Timed out waiting for the daemon to complete", now	- start < TEST_TIME_OUT);
			BackupRestoreStatus status = adminSynapse.getDaemonStatus(daemonId);
			assertNotNull(status);
			assertNotNull(status.getStatus());
			// We are done if it failed.
			assertFalse(status.getErrorMessage(), DaemonStatus.FAILED == status.getStatus());
			// Are we done?
			if (DaemonStatus.COMPLETED == status.getStatus()) {
				System.out.println("Restore Complete. Message: "+ status.getProgresssMessage());
				return status;
			} else {
				long current = status.getProgresssCurrent();
				long total = status.getProgresssTotal();
				if (total <= 0) {
					total = 1000000;
				}
				String message = status.getProgresssMessage();
				double percent = ((double) current / (double) total) * 100.0;
				System.out.println("Restore progresss: " + percent+ " % Message: " + message);
			}
			// Wait.
			Thread.sleep(1000);
		}
	}


}
