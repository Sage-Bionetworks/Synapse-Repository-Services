package org.sagebionetworks.file.worker;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.file.preview.PreviewManager;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker process file create messages.
 * When a file is created without a preview, this worker will create on for it.
 * 
 * @author John
 *
 */
public class PreviewWorker implements Callable<List<Message>> {
	
	static private Log log = LogFactory.getLog(PreviewWorker.class);
	
	private PreviewManager previewManager;
	private List<Message> messages;
	
	/**
	 * Instances of this class are created on the fly as needed.  Therefore, all of the
	 * dependencies must be provide in the constructor.
	 * @param fileMetadataDao
	 * @param messages
	 */
	public PreviewWorker(PreviewManager previewManager, List<Message> messages) {
		super();
		this.previewManager = previewManager;
		this.messages = messages;
	}

	@Override
	public List<Message> call() throws Exception {
		// Any message we return will treated as processed and deleted from the queue.
		List<Message> processedMessage = new LinkedList<Message>();
		// Process the messages
		for(Message message: messages){
			try{
				ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
				// Ignore all non-file messages.
				if (ObjectType.FILE == changeMessage.getObjectType()
						&& (ChangeType.CREATE == changeMessage.getChangeType() || ChangeType.UPDATE == changeMessage.getChangeType())) {
					// This is a file message so look up the file
					FileHandle metadata = previewManager.getFileMetadata(changeMessage.getObjectId());
					if (metadata instanceof PreviewFileHandle) {
						// We do not make previews of previews
					} else if (metadata instanceof S3FileHandle) {
						S3FileHandle s3fileMeta = (S3FileHandle) metadata;
						// Only generate a preview if we do not already have one.
						if(s3fileMeta.getPreviewId() == null){
							// Generate a preview.
							previewManager.generatePreview(s3fileMeta);
						}
					} else if (metadata instanceof ExternalFileHandle) {
						// we need to add support for this
						log.warn("Currently do not support previews for ExternalFileHandles");
					} else {
						// We will never be able to process such a message.
						throw new IllegalArgumentException("Unknown file type: "+ metadata.getClass().getName());
					}
				}
				// This message was processed
				processedMessage.add(message);
			}catch (NotFoundException e){
				// we can ignore messages for files that no longer exist.
				processedMessage.add(message);
			}catch (IllegalArgumentException e){
				// We cannot recover from this exception so log the error
				// and treat the message as processed.
				processedMessage.add(message);
				log.error("Failed to process message: "+message.toString(), e);
			}catch (UnsupportedOperationException e){
				// We cannot recover from this exception so log the error
				// and treat the message as processed.
				processedMessage.add(message);
				log.error("Failed to process message: "+message.toString(), e);
			}catch (TemporarilyUnavailableException e){
				// When this occurs we want the message to go back on the queue, so we can try again later.
				log.info("Failed to process message: "+message.toString(), e);
			}catch (Throwable e){
				// Failing to process a message should not terminate the rest of the message processing.
				// For unknown errors we leave the messages on the queue.
				log.error("Failed to process message: "+message.toString(), e);
			}
		}
		return processedMessage;
	}

}
