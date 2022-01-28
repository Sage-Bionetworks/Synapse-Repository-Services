package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.repo.model.report.StorageReportType;

public class ITDownloadStorageReportTest extends BaseITTest {

	private static final long RETRY_TIME = 1000L;
	
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
