package org.sagebionetworks.worker.utils;

import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.workers.util.Gate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A simple runner gate that will only run when the stack is in read-write mode.
 *
 */
public class StackStatusGate implements Gate {

	@Autowired
	StackStatusDao stackStatusDao;
	@Autowired
	WorkerLogger workerLogger;
	

	@Override
	public boolean canRun() {
		// Can run as long as the stack is in read-write mode.
		return stackStatusDao.isStackReadWrite();
	}

	@Override
	public void runFailed(Exception error) {
		// log the error
		workerLogger.logWorkerFailure(error.getClass().getName(), error, false);
	}

}
