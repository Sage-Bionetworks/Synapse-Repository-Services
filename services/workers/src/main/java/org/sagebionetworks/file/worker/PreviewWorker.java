package org.sagebionetworks.file.worker;

import java.time.Instant;
import java.time.Period;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.imageio.IIOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.preview.PreviewGenerationNotSupportedException;
import org.sagebionetworks.repo.manager.file.preview.PreviewManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.google.common.collect.Sets;

/**
 * This worker process file create messages. When a file is created without a
 * preview, this worker will create on for it.
 * 
 * @author John
 *
 */
public class PreviewWorker implements ChangeMessageDrivenRunner {

	private static final Set<String> IGNORED_AMAZON_S3_EXCEPTION_ERROR_CODES = Collections.unmodifiableSet(
			Sets.newHashSet(
					"NoSuchKey", //ignore because key was deleted
					"AccessDenied" //ignore because we no longer have access to that bucket
			)
	);

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
					.getChangeType()) && changeMessage.getTimestamp().after(Date.from(Instant.now().minus(Period.ofDays(1)))) 	) {
				// This is a file message so look up the file
				FileHandle metadata = previewManager
						.getFileMetadata(changeMessage.getObjectId());
				if (!(metadata instanceof CloudProviderFileHandleInterface)) {
					log.warn("Currently do not support previews for " + metadata.getClass().getName());
				} else {
					CloudProviderFileHandleInterface cloudFHMetadata = (CloudProviderFileHandleInterface) metadata;
					if (cloudFHMetadata.getIsPreview()) {
						// We do not make previews of previews
						return;
					}
					// Only generate a preview if we do not already have one.
					if (cloudFHMetadata.getPreviewId() == null) {
						// Generate a preview.
						previewManager.generatePreview(cloudFHMetadata);
					}
				}
			}
		} catch (PreviewGenerationNotSupportedException e){
			//preview was not able to be generated for the file
			log.info("Preview generator determined it was impossible to generate a preview for this because "
					+ e.getMessage() + " " + changeMessage);
		} catch (NotFoundException e) {
			// we can ignore messages for files that no longer exist.
		} catch (CannotDetermineBucketLocationException e){
			//nothing to do because the bucket no longer exists
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
			log.warn("Failed to process message: " + changeMessage.toString(), e);
			workerLogger.logWorkerFailure(this.getClass(), changeMessage, e,
					true);
			throw new RecoverableMessageException();
		} catch (AmazonS3Exception e) {
			if (IGNORED_AMAZON_S3_EXCEPTION_ERROR_CODES.contains(e.getErrorCode())) {
				//nothing to do because the file no longer exists
				log.warn("Unable to process message: " + changeMessage.toString() + " because received " + e.getStatusCode() +  " Error Code: " + e.getErrorCode() + " from Amazon S3");
			} else {
				handleThrowable(changeMessage, e);
			}
		} catch (Throwable e) {
			handleThrowable(changeMessage, e);
		}
	}

	private void handleThrowable(ChangeMessage changeMessage, Throwable e) {
		if (e.getCause() instanceof IIOException) {
			log.info("Failed to process message: " + changeMessage.getChangeNumber() + ". Unable to read file (IIOException).");
		} else {
			// If we do not know what went wrong then we do no re-try
			log.error("Failed to process message: " + changeMessage.toString(), e);
			workerLogger.logWorkerFailure(this.getClass(), changeMessage, e,false);
		}
	}

}
