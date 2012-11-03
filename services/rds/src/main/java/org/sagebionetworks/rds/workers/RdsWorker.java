package org.sagebionetworks.rds.workers;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.sqs.model.Message;

/**
 * The worker that processes messages for RDS asynchronous jobs.
 * 
 * @author jmhill
 *
 */
public class RdsWorker implements Callable<List<Message>> {
	
	static private Log log = LogFactory.getLog(RdsWorker.class);
	
	List<Message> messages;
	AsynchronousDAO asynchronousManager;

	public RdsWorker(List<Message> messages, AsynchronousDAO asynchronousManager) {
		if(messages == null) throw new IllegalArgumentException("Messages cannot be null");
		if(asynchronousManager == null) throw new IllegalArgumentException("AsynchronousManager cannot be null");
		this.messages = messages;
		this.asynchronousManager = asynchronousManager;
	}

	@Override
	public List<Message> call() throws Exception {
		List<Message> processedMessages = new LinkedList<Message>();
		// process each message
		for(Message message: messages){
			// Extract the ChangeMessage
			ChangeMessage change = MessageUtils.extractMessageBody(message);
			// We only care about entity messages here
			if(ObjectType.ENTITY == change.getObjectType()){
				try{
					// Is this a create update or delete?
					if(ChangeType.CREATE == change.getChangeType()){
						// create
						asynchronousManager.createEntity(change.getObjectId());
					}else if(ChangeType.UPDATE == change.getChangeType()){
						// update
						asynchronousManager.updateEntity(change.getObjectId());
					}else if(ChangeType.DELETE == change.getChangeType()){
						// delete
						asynchronousManager.deleteEntity(change.getObjectId());
					}else{
						throw new IllegalArgumentException("Unknown change type: "+change.getChangeType());
					}
					// This message was processed.
					processedMessages.add(message);
				}catch(NotFoundException e){
					log.info("NotFound: "+e.getMessage()+". The message will be returend as processed and removed from the queue");
					// If an entity does not exist anymore then we want the message to be deleted from the queue
					processedMessages.add(message);
				}catch (Throwable e){
					// Something went wrong and we did not process the message.
					log.error("Failed to process message", e);
				}
			}else{
				// Non-entity messages must be returned so they can be removed from the queue.
				processedMessages.add(message);
			}
		}
		return processedMessages;
	}

}
