package org.sagebionetworks.table.worker;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.FileViewManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedRunner;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This file view worker will completely re-build a file view
 * on any FileView change.
 *
 */
public class FileViewWorker implements ChangeMessageDrivenRunner {
	
	public static final String FILE_VIEW_WORKER = "fileViewWorker";

	@Autowired
	FileViewManager tableViewManager;
	
	@Autowired
	TableIndexConnectionFactory connectionFactory;
	
	
	@Autowired
	SemaphoreGatedRunner runner;

	@Override
	public void run(ProgressCallback<ChangeMessage> progressCallback,
			ChangeMessage message) throws RecoverableMessageException,
			Exception {
		// This worker is only works on FileView messages
		if(ObjectType.FILE_VIEW.equals(message.getObjectType())){
			// Ensure only one worker works on a given FiewView at a time
			String key = FILE_VIEW_WORKER+message.getObjectId();
		}
	}

}
