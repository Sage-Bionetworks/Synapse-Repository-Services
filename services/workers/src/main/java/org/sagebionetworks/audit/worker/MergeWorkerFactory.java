package org.sagebionetworks.audit.worker;

import org.sagebionetworks.audit.dao.AccessRecordDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Spring injects all dependencies into this factory. When its run method is
 * called a new MergeWorker() object is created with the dependences passed to
 * the constructor and then run.
 * 
 * @author jmhill
 * 
 */
public class MergeWorkerFactory implements Runnable {
	
	@Autowired
	private AccessRecordDAO accessRecordDAO;

	@Override
	public void run() {
		// Create a worker with IoC
		MergeWorker worker = new MergeWorker(accessRecordDAO);
		// Merge a single batch when the timer is fired.
		worker.mergeOneBatch();
	}
	
}
