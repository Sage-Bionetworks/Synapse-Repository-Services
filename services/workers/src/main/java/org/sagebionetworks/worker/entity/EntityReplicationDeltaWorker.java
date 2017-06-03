package org.sagebionetworks.worker.entity;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.common.util.progress.ThrottlingProgressCallback;
import org.sagebionetworks.repo.manager.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.common.collect.Lists;

/**
 * Entity replication data is normally, kept in-synch with the truth by the
 * {@link EntityReplicationWorker} which replicates entity data by listening to
 * entity change events. However, message delivery is not guaranteed so a
 * secondary process is needed to ensure the entity replication data is kept
 * up-to-date with the truth. This worker acts as the backup replication process
 * by scanning for deltas between the truth and the replicated data.
 * Specifically, this worker scans for deltas by comparing check-sums (cyclic
 * redundancy check CRC32) between the truth and the replicated data.
 * 
 */
public class EntityReplicationDeltaWorker implements ProgressingRunner<Void> {
	
	public static final long THROTTLE_FREQUENCY_MS = 1000*30;
	static Long MAX_PAGE_SIZE = 10000L;
	static private Logger log = LogManager.getLogger(EntityReplicationDeltaWorker.class);
	
	@Autowired
	NodeDAO nodeDao;
	
	@Autowired
	ConnectionFactory connectionFactory;
	
	@Autowired
	AmazonSQSClient sqsClient;
	
	@Autowired
	WorkerLogger workerLogger;
	
	String queueName;
	String queueUrl;
	
	/**
	 * Injected.
	 * @param queueName
	 */
	public void setQueueName(String queueName){
		this.queueName = queueName;
	}


	@Override
	public void run(ProgressCallback<Void> progressCallback) throws Exception {
		try{
			// wrap the callback to throttle
			progressCallback = new ThrottlingProgressCallback<Void>(progressCallback, THROTTLE_FREQUENCY_MS);
			// All parentIds that are in the trash need to be ignored.
			Set<Long> trashedParents = new HashSet<Long>(
					nodeDao.getAllContainerIds(StackConfiguration.getTrashFolderEntityIdStatic()));
					
			long limit = MAX_PAGE_SIZE;
			long offset = 0L;
			// process one page of parentIds at a time.
			List<Long> parentIds = nodeDao.getContainerIds(limit, offset);
			while(!parentIds.isEmpty()){
				// make progress between each page
				progressCallback.progressMade(null);
				findDeltas(progressCallback, parentIds, trashedParents);
				// get the next page
				offset = offset+limit;
				parentIds = nodeDao.getContainerIds(limit, offset);
			}
		}catch (Throwable cause){
			boolean willRetry = true;
			workerLogger.logWorkerFailure(EntityReplicationDeltaWorker.class.getName(), cause, willRetry);
		}
	}
	
	/**
	 * Find the deltas for one page of parentIds.
	 * @param progressCallback
	 * @param parentIds
	 * @throws JSONObjectAdapterException 
	 */
	public void findDeltas(ProgressCallback<Void> progressCallback, List<Long> parentIds, Set<Long> trashedParents) throws JSONObjectAdapterException{
		// Synch with each index.
		List<TableIndexDAO> indexDaos = connectionFactory.getAllConnections();
		Collections.shuffle(indexDaos);
		TableIndexDAO firstIndex = indexDaos.get(0);
		// Find the parents out-of-synch.
		Set<Long> outOfSynchParentIds = compareCheckSums(progressCallback, parentIds, firstIndex, trashedParents);
		log.info("Out-of-synch parents: "+outOfSynchParentIds.size());
		
		for(Long outOfSynchParentId: outOfSynchParentIds){
			boolean isParentInTrash = trashedParents.contains(outOfSynchParentId);
			List<ChangeMessage> childChanges = findChangesForParentId(progressCallback, firstIndex, outOfSynchParentId, isParentInTrash);
			pushMessagesToQueue(childChanges);
			log.info("Published: "+childChanges.size()+" messages to "+queueUrl);
		}
	}

	/**
	 * Create the changes for a parentId that is out of Synch.
	 * @param firstIndex
	 * @param outOfSynchParentId
	 * @param isParentInTrash
	 * @return
	 */
	private List<ChangeMessage> findChangesForParentId(
			ProgressCallback<Void> progressCallback,
			TableIndexDAO firstIndex, Long outOfSynchParentId,
			boolean isParentInTrash) {
		List<ChangeMessage> changes = new LinkedList<>();
		Set<IdAndEtag> replicaChildren = new HashSet<>(
				firstIndex.getEntityChildren(outOfSynchParentId));
		progressCallback.progressMade(null);
		if(!isParentInTrash){
			// The parent is not in the trash so find entities that are out-of-synch
			List<IdAndEtag> truthChildren = nodeDao.getChildren(outOfSynchParentId);
			progressCallback.progressMade(null);
			Set<Long> truthIds = new HashSet<Long>();
			// find the create/updates
			for(IdAndEtag test: truthChildren){
				if(!replicaChildren.contains(test)){
					changes.add(createChange(test, ChangeType.UPDATE));
				}
				truthIds.add(test.getId());
			}
			// find the deletes
			for(IdAndEtag test: replicaChildren){
				if(!truthIds.contains(test.getId())){
					changes.add(createChange(test, ChangeType.DELETE));
				}
			}
		}else{
			// the parent is the the trash so setup the delete of any children that appear in the replica.
			for(IdAndEtag toDelete: replicaChildren){
				changes.add(createChange(toDelete, ChangeType.DELETE));
			}
		}
		return changes;
	}

	/**
	 * Create a ChangeMessage from the given info and type.
	 * @param info
	 * @param type
	 * @return
	 */
	public ChangeMessage createChange(IdAndEtag info, ChangeType type){
		ChangeMessage message = new ChangeMessage();
		message.setChangeNumber(1L);
		message.setChangeType(type);
		message.setObjectId(""+info.getId());
		message.setObjectEtag(info.getEtag());
		message.setObjectType(ObjectType.ENTITY);
		message.setTimestamp(new Date());
		return message;
	}
	/**
	 * Compare the check-sums between the truth and the index for the given parentIds.
	 * @param progressCallback
	 * @param parentIds
	 * @param truthCRCs
	 * @param indexDao
	 * @param trashedParents
	 * @return
	 */
	public Set<Long> compareCheckSums(ProgressCallback<Void> progressCallback,
			List<Long> parentIds,
			TableIndexDAO indexDao, Set<Long> trashedParents) {
		Map<Long, Long> truthCRCs = nodeDao.getSumOfChildCRCsForEachParent(parentIds);
		Map<Long, Long> indexCRCs = indexDao.getSumOfChildCRCsForEachParent(parentIds);
		HashSet<Long> parentsOutOfSynch = new HashSet<Long>();
		// Find the mismatches
		for(Long parentId: parentIds){
			progressCallback.progressMade(null);
			Long truthCRC = truthCRCs.get(parentId);
			Long indexCRC = indexCRCs.get(parentId);
			/* 
			 * If the parent is in the trash then it should not
			 * exist in the replica.
			 */
			if(trashedParents.contains(parentId)){
				if(indexCRC != null){
					parentsOutOfSynch.add(parentId);
				}
				continue;
			}
			/*
			 *If the truth CRC is null then the index
			 *CRC must also be null. 
			 */
			if(truthCRC == null){
				if(indexCRC != null){
					parentsOutOfSynch.add(parentId);
				}
				continue;
			}
			/*
			 * The parent is not in the trash
			 * and the truth CRC is not null.
			 * The index CRC must match the truth.
			 */
			if(!truthCRC.equals(indexCRC)){
				parentsOutOfSynch.add(parentId);
			}
		}
		return parentsOutOfSynch;
	}
	
	/**
	 * Publish the given messages to the Entity replication queue.
	 * @param toPush
	 * @throws JSONObjectAdapterException
	 */
	public void pushMessagesToQueue(List<ChangeMessage> toPush) throws JSONObjectAdapterException{
		List<List<ChangeMessage>> batches = Lists.partition(toPush, ChangeMessageUtils.MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE);
		for(List<ChangeMessage> batch: batches){
			ChangeMessages messages = new ChangeMessages();
			messages.setList(batch);
			String messageBody = EntityFactory.createJSONStringForEntity(messages);
			String queueUrl = lazyLoadQueueUrl();
			sqsClient.sendMessage(new SendMessageRequest(queueUrl, messageBody));
		}
	}
	
	/**
	 * Lazy load the queue URL.
	 * @return
	 */
	public String lazyLoadQueueUrl(){
		if(queueName == null){
			throw new IllegalStateException("Queue name cannot be null");
		}
		if(this.sqsClient == null){
			throw new IllegalStateException("SQS client cannot be null");
		}
		if(queueUrl == null){
			// lookup the queue name the first time only.
			CreateQueueResult result = this.sqsClient.createQueue(queueName);
			this.queueUrl = result.getQueueUrl();
		}
		return this.queueUrl;
	}

}
