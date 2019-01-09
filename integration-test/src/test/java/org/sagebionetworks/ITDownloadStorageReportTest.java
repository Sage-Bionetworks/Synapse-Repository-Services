package org.sagebionetworks;

import static org.aspectj.bridge.MessageUtil.fail;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.report.DownloadStorageReportResponse;
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
		adminSynapse.deleteUser(userToDelete);
	}

	@Test
	public void generateReportAndGet() throws SynapseException, InterruptedException {
		String jobToken = synapse.generateStorageReportAsyncStart(StorageReportType.ALL_PROJECTS);
		DownloadStorageReportResponse response =  null;
		boolean csvCreated = false;
		while (!csvCreated) {
			Thread.sleep(RETRY_TIME);
			try {
				response = synapse.generateStorageReportAsyncGet(jobToken);
				if (response.getResultsFileHandleId() != null) {
					csvCreated = true;
				}
			} catch (SynapseResultNotReadyException e) {
				assertNotEquals(e.getJobStatus().getJobState(), AsynchJobState.FAILED);
			}
		}

		String csvReportFileHandleId = response.getResultsFileHandleId();
		File tempFile;
		try {
			tempFile = File.createTempFile("ITStorageReport", "csv");
			synapse.downloadFromFileHandleTemporaryUrl(csvReportFileHandleId, tempFile);
			tempFile.deleteOnExit();
			String csvContents = tempFile.toString();
			assertTrue(csvContents.startsWith("projectId,projectName,sizeInBytes\n"));
		} catch (IOException e) {
			fail("IO Exception encountered when getting filehandle URL");
		}
	}
}
