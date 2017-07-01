package org.sagebionetworks.file.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.preview.PreviewManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker process file create messages. When a file is created without a
 * preview, this worker will create on for it.
 * 
 * @author John
 *
 */
public class PreviewWorker implements ChangeMessageDrivenRunner {

	static private Logger log = LogManager.getLogger(PreviewWorker.class);

	@Autowired
	PreviewManager previewManager;

	@Autowired
	WorkerLogger workerLogger;

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage changeMessage)
			throws RecoverableMessageException, Exception {

		try {
			// Ignore all non-file messages.
			if (ObjectType.FILE == changeMessage.getObjectType()
					&& (ChangeType.CREATE == changeMessage.getChangeType() || ChangeType.UPDATE == changeMessage
							.getChangeType())) {
				// This is a file message so look up the file
				FileHandle metadata = previewManager
						.getFileMetadata(changeMessage.getObjectId());
				if (metadata instanceof PreviewFileHandle) {
					// We do not make previews of previews
				} else if (metadata instanceof S3FileHandle) {
					S3FileHandle s3fileMeta = (S3FileHandle) metadata;
					// Only generate a preview if we do not already have one.
					if (s3fileMeta.getPreviewId() == null) {
						// Generate a preview.
						previewManager.generatePreview(s3fileMeta);
					}
				} else if (metadata instanceof ExternalFileHandle) {
					// we need to add support for this
					log.warn("Currently do not support previews for ExternalFileHandles");
				} else if (metadata instanceof ProxyFileHandle) {
					// we need to add support for this
					log.warn("Currently do not support previews for ProxyFileHandles");
				} else {
					// We will never be able to process such a message.
					throw new IllegalArgumentException("Unknown file type: "
							+ metadata.getClass().getName());
				}
			}
		} catch (NotFoundException e) {
			// we can ignore messages for files that no longer exist.
		} catch (IllegalArgumentException e) {
			// We cannot recover from this exception so log the error
			// and treat the message as processed.
			log.error("Failed to process message: " + changeMessage.toString(), e);
			workerLogger.logWorkerFailure(this.getClass(), changeMessage, e,
					false);
		} catch (UnsupportedOperationException e) {
			// We cannot recover from this exception so log the error
			// and treat the message as processed.
			log.error("Failed to process message: " + changeMessage.toString(), e);
			workerLogger.logWorkerFailure(this.getClass(), changeMessage, e,
					false);
		} catch (TemporarilyUnavailableException e) {
			// When this occurs we want the message to go back on the queue, so
			// we can try again later.
			log.info("Failed to process message: " + changeMessage.toString(), e);
			workerLogger.logWorkerFailure(this.getClass(), changeMessage, e,
					true);
			throw new RecoverableMessageException();
		} catch (Throwable e) {
			// If we do not know what went wrong then we do no re-try
			log.error("Failed to process message: " + changeMessage.toString(), e);
			workerLogger.logWorkerFailure(this.getClass(), changeMessage, e,
					false);
		}
	}

}
