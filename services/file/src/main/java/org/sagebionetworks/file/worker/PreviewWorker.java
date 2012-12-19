package org.sagebionetworks.file.worker;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;

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
	
	private FileMetadataDao fileMetadataDao;
	private List<Message> messages;
	
	/**
	 * Instances of this class are created on the fly as needed.  Therefore, all of the
	 * dependencies must be provide in the constructor.
	 * @param fileMetadataDao
	 * @param messages
	 */
	public PreviewWorker(FileMetadataDao fileMetadataDao, List<Message> messages) {
		super();
		this.fileMetadataDao = fileMetadataDao;
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
				
				
				// This message was processed
				processedMessage.add(message);
			}catch (Throwable e){
				// Failing to process a message should not terminate the rest of the message processing.
				log.error("Failed to process message: "+message.toString(), e);
			}
		}
		return processedMessage;
	}

}
