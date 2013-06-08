package org.sagebionetworks.repo.manager.file;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.sagebionetworks.repo.model.dao.UploadDaemonStatusDao;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;

/**
 * This worker will add each chunk to the larger multi-part file and complete the multi-part process.
 * @author jmhill
 *
 */
public class CompleteUploadWorker implements Callable<Boolean>{
	
	UploadDaemonStatusDao uploadDaemonStatusDao;
	ExecutorService uploadFileDaemonThreadPoolSecondary;
	UploadDaemonStatus uploadStatus;
	CompleteAllChunksRequest cacf;

	/**
	 * Create a new worker that should be used only once.  A new worker should be created for each job.
	 * @param uploadDaemonStatusDao
	 * @param fileHandleDao
	 * @param uploadStatus
	 * @param cacf
	 */


	@Override
	public Boolean call() throws Exception {
		// 
		return null;
	}

}
