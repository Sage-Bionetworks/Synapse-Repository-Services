package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.report.StorageReportType;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;

@ExtendWith(ITTestExtension.class)
public class ITDownloadStorageReportTest {

	private static final long RETRY_TIME = 1000L;
	
	private SynapseClient synapse;
	
	public ITDownloadStorageReportTest(SynapseClient synapse) {
		this.synapse = synapse;
	}
	
	@Test
	public void generateReportUnauthorized() throws Exception {
		String jobToken = synapse.generateStorageReportAsyncStart(StorageReportType.ALL_PROJECTS);
		
		// We need to wait until we the job is finished processing to see the thrown exception				
		TimeUtils.waitFor(10 * RETRY_TIME, RETRY_TIME, () -> {
			boolean jobDone = AsynchJobState.PROCESSING != synapse.getAsynchronousJobStatus(jobToken).getJobState(); 
			return Pair.create(jobDone, null);
		});
		
		// Now that the job is done we get try and get the result
		assertThrows(SynapseForbiddenException.class, () -> {
			synapse.generateStorageReportAsyncGet(jobToken);
		});
	}
}
