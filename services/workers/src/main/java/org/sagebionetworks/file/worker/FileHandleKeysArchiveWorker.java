package org.sagebionetworks.file.worker;

import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleArchivalManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandleKeyArchiveResult;
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
	
	private static final Logger LOG = LogManager.getLogger(FileHandleKeysArchiveWorker.class);

	private UserManager userManager;
	private FileHandleArchivalManager archivalManager;

	@Autowired
	public FileHandleKeysArchiveWorker(UserManager userManager, FileHandleArchivalManager archivalManager) {
		this.userManager = userManager;
		this.archivalManager = archivalManager;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {

		UserInfo adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		FileHandleKeysArchiveRequest request = archivalManager.parseArchiveKeysRequestFromSqsMessage(message);

		String bucket = request.getBucket();
		Instant modifiedBefore = Instant.ofEpochMilli(request.getModifiedBefore());

		for (String key : request.getKeys()) {
			try {
				FileHandleKeyArchiveResult result = archivalManager.archiveUnlinkedFileHandlesByKey(adminUser, bucket, key, modifiedBefore);
				LOG.debug("Key {} in bucket {} processed for archival ({} archived, wasTagged: {})", key, bucket, result.getArchivedCount(), result.isWasTagged());
			} catch (Exception ex) {
				boolean retryBatch = ex instanceof RecoverableMessageException;
				LOG.error("Attempt to archive key {} in bucket {} failed (will retry: {}): {}", key, bucket, retryBatch, ex.getMessage(), ex);
				if (retryBatch) {
					throw ex;
				}
				// Try to keep going with the rest of the keys
			}
		}
	}

}
