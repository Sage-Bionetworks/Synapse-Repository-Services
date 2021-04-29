package org.sagebionetworks.file.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleUnlinkedManager;
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.file.FileHandleUnlinkedRequest;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * The worker process an SQS message that contains the Athena query results pointer for files to be set as unlinked, the message itself will also contain an
 * optional pageToken so that we can limit the processing time of a single run to a single page of results (e.g. for recovery).
 */
public class FileHandleUnlinkedWorker implements MessageDrivenRunner {
	
	private static final Logger LOG = LogManager.getLogger(FileHandleUnlinkedWorker.class);
	
	private FileHandleUnlinkedManager manager;
	
	@Autowired
	public FileHandleUnlinkedWorker(FileHandleUnlinkedManager manager) {
		this.manager = manager;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		
		FileHandleUnlinkedRequest request = manager.fromSqsMessage(message);
		
		try {
			manager.processFileHandleUnlinkRequest(request);
		} catch (RecoverableException e) {
			LOG.warn(e.getMessage(), e);
			throw new RecoverableMessageException(e.getMessage(), e);
		}
		
	}

}
