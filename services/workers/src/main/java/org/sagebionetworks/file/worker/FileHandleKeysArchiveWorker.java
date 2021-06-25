package org.sagebionetworks.file.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleArchivalManager;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker will actually perform the archival of a set of file handles
 * @author Marco Marasca
 *
 */
public class FileHandleKeysArchiveWorker implements MessageDrivenRunner {
	
	private FileHandleArchivalManager archivalManager;

	@Autowired
	public FileHandleKeysArchiveWorker(FileHandleArchivalManager archivalManager) {
		this.archivalManager = archivalManager;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		archivalManager.processFileHandleKeyArchiveRequest(message);
	}

}
