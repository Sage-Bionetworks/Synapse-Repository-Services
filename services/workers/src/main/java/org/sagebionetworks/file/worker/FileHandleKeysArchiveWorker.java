package org.sagebionetworks.file.worker;

import java.time.Instant;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleArchivalManager;
import org.sagebionetworks.repo.model.file.FileHandleKeysArchiveRequest;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker will actually perform the archival of a set of S3 keys in a bucket
 * 
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

		FileHandleKeysArchiveRequest request = archivalManager.parseArchiveKeysRequestFromSqsMessage(message);

		String bucket = request.getBucket();
		Instant modifiedBefore = Instant.ofEpochMilli(request.getModifiedBefore());

		for (String key : request.getKeys()) {
			archivalManager.archiveUnlinkedFileHandlesByKey(bucket, key, modifiedBefore);
		}
	}

}
