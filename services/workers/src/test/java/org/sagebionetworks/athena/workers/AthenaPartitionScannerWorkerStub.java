package org.sagebionetworks.athena.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;

/**
 * This is used in place to the real worker during integration tests so that we do not waste resources
 * 
 * @author Marco
 *
 */
public class AthenaPartitionScannerWorkerStub implements ProgressingRunner {

	private static final Logger LOG = LogManager.getLogger(AthenaPartitionScannerWorkerStub.class);

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		LOG.warn("This is a stub, does nothing");
	}

}
