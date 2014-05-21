package org.sagebionetworks.table.worker;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * Autowired builder of the TableCSVDownloadWorkers
 * 
 * @author jmhill
 *
 */
public class TableCSVDownloadWorkerFactory implements MessageWorkerFactory {
	
	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private TableRowManager tableRowManager;
	@Autowired
	private UserManager userManger;
	@Autowired
	private FileHandleManager fileHandleManager;

	@Override
	public Callable<List<Message>> createWorker(List<Message> messages,
			WorkerProgress workerProgress) {
		return new TableCSVDownloadWorker(messages, asynchJobStatusManager, tableRowManager, userManger, fileHandleManager, workerProgress);
	}

}
