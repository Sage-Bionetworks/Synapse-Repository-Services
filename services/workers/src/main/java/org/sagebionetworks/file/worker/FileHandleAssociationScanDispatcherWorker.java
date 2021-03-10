package org.sagebionetworks.file.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationScannerJobManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The worker periodically dispatches SQS messages to scan ranges for all the association types
 */
public class FileHandleAssociationScanDispatcherWorker implements ProgressingRunner {
	
	static final int START_INTERVAL_DAYS = 5;
	
	private FileHandleAssociationScannerJobManager manager;
	
	@Autowired
	public FileHandleAssociationScanDispatcherWorker(FileHandleAssociationScannerJobManager manager) {
		this.manager = manager;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		if (manager.isScanJobIdle(START_INTERVAL_DAYS)) {
			manager.startScanJob();
		}
	}

}
