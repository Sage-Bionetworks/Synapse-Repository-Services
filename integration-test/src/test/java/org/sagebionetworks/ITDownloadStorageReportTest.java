package org.sagebionetworks;

import static org.aspectj.bridge.MessageUtil.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.repo.model.report.StorageReportType;

public class ITDownloadStorageReportTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	private static final long RETRY_TIME = 1000L;

	@BeforeClass
	public static void beforeClass() throws Exception {
		StackConfiguration config = StackConfigurationSingleton.singleton();

		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(config.getMigrationAdminUsername());
		adminSynapse.setApiKey(config.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
		SynapseClientHelper.setEndpoints(synapse);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		try { adminSynapse.deleteUser(userToDelete); } catch (SynapseException e) { }
	}

	@Test
	public void generateReportUnauthorized() throws SynapseException, InterruptedException {
		String jobToken = synapse.generateStorageReportAsyncStart(StorageReportType.ALL_PROJECTS);
		boolean jobProcessed = false;
		while (!jobProcessed) {
			Thread.sleep(RETRY_TIME);
			try {
				synapse.generateStorageReportAsyncGet(jobToken);
				fail("Expected exception");
			} catch (SynapseForbiddenException e) {
				// As expected
				break;
			}
		}
	}
}
