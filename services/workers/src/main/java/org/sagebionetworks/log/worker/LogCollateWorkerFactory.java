package org.sagebionetworks.log.worker;

import org.sagebionetworks.logging.s3.LogDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This factory is auto-wired with all of the dependencies.
 * 
 * @author jmhill
 *
 */
public class LogCollateWorkerFactory implements Runnable {
	
	@Autowired
	LogDAO logDAO;

	@Override
	public void run() {
		// Create a new worker and run it
		LogCollateWorker worker = new LogCollateWorker(logDAO);
		worker.mergeOneBatch();
	}

}
