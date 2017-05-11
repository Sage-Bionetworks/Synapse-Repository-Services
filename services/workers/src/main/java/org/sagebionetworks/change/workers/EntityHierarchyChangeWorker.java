package org.sagebionetworks.change.workers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.message.ChangeMessageUtils;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeIdAndType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker listens to entity hierarchy change messages and then broadcasts
 * entity change events for all children of the container.
 *
 */
public class EntityHierarchyChangeWorker implements ChangeMessageDrivenRunner {
	
	static private Logger log = LogManager.getLogger(ChangeSentMessageSynchWorker.class);
	
	/**
	 * All messages older than 5 days will be ignored.
	 */
	public static final long MAX_MESSAGE_AGE_MS = 1000*60*60*24*5;
	
	@Autowired
	DBOChangeDAO changeDao;
	@Autowired
	NodeDAO nodeDao;
	@Autowired
	RepositoryMessagePublisher messagePublisher;
	@Autowired
	Clock clock;

	@Override
	public void run(ProgressCallback<Void> progressCallback,
			ChangeMessage message) throws RecoverableMessageException,
			Exception {
		// how old is this message?
		long messageAgeMS = clock.currentTimeMillis() - message.getTimestamp().getTime();
		// only process messages that are less than 5 days old.
		if(messageAgeMS < MAX_MESSAGE_AGE_MS){
			// only process messages 
			recursiveBroadcastMessages(progressCallback, message.getObjectId(), message.getChangeType());
		}else{
			log.info("Ignoring old message:  "+message.toString());
		}
	}
	
	/**
	 * Broadcast a change message for each child of the given container.
	 * This method will recurse for each child that is a project or folder.
	 * Messages are broadcast breadth-first.
	 * 
	 * @throws InterruptedException 
	 * 
	 */
	public void recursiveBroadcastMessages(ProgressCallback<Void> progressCallback, String parentId, ChangeType changeType) throws InterruptedException{
		long limit = ChangeMessageUtils.MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE;
		long offset = 0;
		List<NodeIdAndType> children = null;
		List<String> containerIds = new LinkedList<String>();
		do{
			// Get one page of children for this parentId
			children = nodeDao.getChildren(parentId, limit, offset);
			progressCallback.progressMade(null);
			if(!children.isEmpty()){
				Set<Long> childrenIds = new HashSet<Long>(children.size());
				for(NodeIdAndType idAndType: children){
					childrenIds.add(KeyFactory.stringToKey(idAndType.getNodeId()));
					if(NodeUtils.isProjectOrFolder(idAndType.getType())){
						// Keep track of children that are also containers for recursion.
						containerIds.add(idAndType.getNodeId());
					}
				}
				// Get the change messages for the direct children of this container.
				List<ChangeMessage> changeMessages = changeDao.getChangesForObjectIds(ObjectType.ENTITY, childrenIds);
				// Set all changes to match the type
				for(ChangeMessage message:  changeMessages){
					message.setChangeType(changeType);
				}
				// Send this batch of messages
				messagePublisher.publishBatchToTopic(ObjectType.ENTITY, changeMessages);
				progressCallback.progressMade(null);
				// Get the next page
				offset += limit;
				// sleep between pages to reduce the load.
				clock.sleep(10L);
			}
		}while(!children.isEmpty());
		
		// recursive call for each child that is also a container.
		for(String containerId: containerIds){
			recursiveBroadcastMessages(progressCallback, containerId, changeType);
		}
	}
	
}
