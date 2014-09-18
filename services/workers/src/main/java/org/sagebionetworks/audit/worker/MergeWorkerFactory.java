package org.sagebionetworks.audit.worker;

import org.sagebionetworks.audit.dao.AccessRecordDAO;
import org.sagebionetworks.repo.model.dbo.dao.semaphore.ProgressCallback;
import org.sagebionetworks.repo.model.dbo.dao.semaphore.ProgressingRunner;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Spring injects all dependencies into this factory. When its run method is
 * called a new MergeWorker() object is created with the dependences passed to
 * the constructor and then run.
 * 
 * @author jmhill
 * 
 */
public class MergeWorkerFactory implements ProgressingRunner {
	
	@Autowired
	private AccessRecordDAO accessRecordDAO;

	@Override
	public void run(ProgressCallback callback) {
		// Create a worker with IoC
		MergeWorker worker = new MergeWorker(accessRecordDAO, callback);

		worker.mergeOneBatch();
	}
	
}
