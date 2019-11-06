package org.sagebionetworks.worker.entity;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.entity.ReplicationMessageManager;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * <p>
 * Entity replication data is normally kept in-synch with the truth by the
 * {@link EntityReplicationWorker} by listening to entity change events.
 * However, message delivery is not guaranteed so a secondary process is needed
 * to ensure the entity replication data is kept up-to-date with the truth.
 * </p>
 * <p>
 * This worker reconciles discrepancies between the truth and the replicated data for
 * a given list of container IDs. This worker is driven by query events. Each time a query
 * is executed against the entity replication data, an event is generated that
 * includes the container IDs involved in the query. For example, when a query
 * is executed against a table view, an event is generated that includes that
 * IDs of the view's fully expanded scope. This worker 'listens' to these events
 * and performs delta checking for each container ID that has not been checked
 * in the past 1000 minutes. When deltas are detected, change events are generated
 * to trigger the {@link EntityReplicationWorker} to create, update, or deleted
 * entity replicated data as needed.
 * </p>
 */
public class EntityReplicationReconciliationWorker implements MessageDrivenRunner {

	static final int MAX_MESSAGE_TO_RUN_RECONCILIATION = 100;

	static private Logger log = LogManager
			.getLogger(EntityReplicationReconciliationWorker.class);

	/**
	 * Each container can only be re-synchronized at this frequency.
	 */
	public static final long SYNCHRONIZATION_FEQUENCY_MS = 1000 * 60 * 1000; // 1000 minutes.

	/**
	 * The frequency that progress events will propagate to out of this worker.
	 */
	public static final long PROGRESS_THROTTLE_FREQUENCY_MS = 1000 * 30;

	@Autowired
	NodeDAO nodeDao;

	@Autowired
	ConnectionFactory connectionFactory;

	@Autowired
	WorkerLogger workerLogger;
	
	@Autowired
	ReplicationMessageManager replicationMessageManager;

	@Autowired
	Clock clock;
	


	@Override
	public void run(ProgressCallback progressCallback, Message message) {
		try {
			/*
			 * Entity replication reconciliation is expensive. It serves as a fail-safe for
			 * lost messages and is not a replacement for the normal replication process.
			 * Therefore, reconciliation should only be run during quite periods. See:
			 * PLFM-5101 and PLFM-5051. The approximate number of message on the replication
			 * queue is used to determine if this is a quite period.
			 */
			long messagesOnQueue = this.replicationMessageManager.getApproximateNumberOfMessageOnReplicationQueue();
			if (messagesOnQueue > MAX_MESSAGE_TO_RUN_RECONCILIATION) {
				// do nothing during busy periods.
				log.info("Ignoring reconciliation request since the replication queue has: " + messagesOnQueue
						+ " messages");
				return;
			}
			
			// extract the containerIds to check from the message.
			List<Long> containerIds = getContainerIdsFromMessage(message);
			if (containerIds.isEmpty()) {
				// nothing to do.
				return;
			}
			// get a connection to an index database.
			TableIndexDAO indexDao = getRandomConnection();
			// Determine which of the given container IDs have expired.
			List<Long> expiredContainerIds = indexDao
					.getExpiredContainerIds(containerIds);
			if (expiredContainerIds.isEmpty()) {
				// nothing to do.
				return;
			}

			// Determine which parents are in the trash
			Set<Long> trashedParents = getTrashedContainers(expiredContainerIds);
			
			// Find all children deltas for the expired containers.
			findChildrenDeltas(progressCallback, indexDao, expiredContainerIds,
					trashedParents);
			
			// re-set the expiration for all containers that were synchronized.
			long newExpirationDateMs = clock.currentTimeMillis()
					+ SYNCHRONIZATION_FEQUENCY_MS;
			indexDao.setContainerSynchronizationExpiration(expiredContainerIds,
					newExpirationDateMs);

		} catch (Throwable cause) {
			log.error("Failed:", cause);
			boolean willRetry = false;
			workerLogger.logWorkerFailure(
					EntityReplicationReconciliationWorker.class.getName(), cause,
					willRetry);
		}
	}

	/**
	 * Get the sub-set of containerIds that are in the trash.
	 * 
	 * @param containerIds
	 * @return
	 */
	public Set<Long> getTrashedContainers(List<Long> containerIds) {
		// find the sub-set that is available.
		Set<Long> availableIds = nodeDao.getAvailableNodes(containerIds);
		Set<Long> inTrash = new HashSet<Long>();
		for (Long id : containerIds) {
			if (!availableIds.contains(id)) {
				inTrash.add(id);
			}
		}
		return inTrash;
	}

	/**
	 * Extract the containerIs from the message.
	 * 
	 * @param message
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	public List<Long> getContainerIdsFromMessage(Message message)
			throws JSONObjectAdapterException {
		ValidateArgument.required(message, "message");
		ValidateArgument.required(message.getBody(), "message.body");
		// Extract the container IDs to check from the message.
		IdList containers = EntityFactory.createEntityFromJSONString(
				message.getBody(), IdList.class);
		ValidateArgument.required(containers, "containers");
		ValidateArgument.required(containers.getList(), "containers.list");
		return containers.getList();
	}

	/**
	 * Find the children deltas for the parents that are out-of-synch.
	 * 
	 * @param progressCallback
	 * @param parentIds
	 * @throws JSONObjectAdapterException
	 */
	public void findChildrenDeltas(ProgressCallback progressCallback,
			TableIndexDAO indexDao, List<Long> parentIds,
			Set<Long> trashedParents) throws JSONObjectAdapterException {
		// Find the parents out-of-synch.
		Set<Long> outOfSynchParentIds = compareCheckSums(progressCallback,
				indexDao, parentIds, trashedParents);
		log.info("Out-of-synch parents: " + outOfSynchParentIds.size());
		for (Long outOfSynchParentId : outOfSynchParentIds) {
			boolean isParentInTrash = trashedParents
					.contains(outOfSynchParentId);
			List<ChangeMessage> childChanges = findChangesForParentId(
					progressCallback, indexDao, outOfSynchParentId,
					isParentInTrash);
			replicationMessageManager.pushChangeMessagesToReplicationQueue(childChanges);
			log.info("Published: " + childChanges.size() + " messages to replication queue");
		}
	}

	/**
	 * Get a random connection from the connection factory.
	 * 
	 * @return
	 */
	private TableIndexDAO getRandomConnection() {
		List<TableIndexDAO> indexDaos = connectionFactory.getAllConnections();
		Collections.shuffle(indexDaos);
		TableIndexDAO firstIndex = indexDaos.get(0);
		return firstIndex;
	}

	/**
	 * Create the changes for a parentId that is out of Synch.
	 * 
	 * @param firstIndex
	 * @param outOfSynchParentId
	 * @param isParentInTrash
	 * @return
	 */
	public List<ChangeMessage> findChangesForParentId(
			ProgressCallback progressCallback, TableIndexDAO firstIndex,
			Long outOfSynchParentId, boolean isParentInTrash) {
		List<ChangeMessage> changes = new LinkedList<>();
		Set<IdAndEtag> replicaChildren = new LinkedHashSet<>(
				firstIndex.getEntityChildren(outOfSynchParentId));
		if (!isParentInTrash) {
			// The parent is not in the trash so find entities that are
			// out-of-synch
			List<IdAndEtag> truthChildren = nodeDao
					.getChildren(outOfSynchParentId);
			Set<Long> truthIds = new HashSet<Long>();
			// find the create/updates
			for (IdAndEtag test : truthChildren) {
				if (!replicaChildren.contains(test)) {
					changes.add(createChange(test, ChangeType.UPDATE));
				}
				truthIds.add(test.getId());
			}
			// find the deletes
			for (IdAndEtag test : replicaChildren) {
				if (!truthIds.contains(test.getId())) {
					changes.add(createChange(test, ChangeType.DELETE));
				}
			}
		} else {
			// the parent is the the trash so setup the delete of any children
			// that appear in the replica.
			for (IdAndEtag toDelete : replicaChildren) {
				changes.add(createChange(toDelete, ChangeType.DELETE));
			}
		}
		return changes;
	}

	/**
	 * Create a ChangeMessage from the given info and type.
	 * 
	 * @param info
	 * @param type
	 * @return
	 */
	public ChangeMessage createChange(IdAndEtag info, ChangeType type) {
		ChangeMessage message = new ChangeMessage();
		message.setChangeNumber(1L);
		message.setChangeType(type);
		message.setObjectId("" + info.getId());
		message.setObjectType(ObjectType.ENTITY);
		message.setTimestamp(new Date());
		return message;
	}

	/**
	 * Compare the check-sums between the truth and the index for the given
	 * parentIds.
	 * 
	 * @param progressCallback
	 * @param parentIds
	 * @param truthCRCs
	 * @param indexDao
	 * @param trashedParents
	 * @return
	 */
	public Set<Long> compareCheckSums(ProgressCallback progressCallback,
			TableIndexDAO indexDao, List<Long> parentIds,
			Set<Long> trashedParents) {
		Map<Long, Long> truthCRCs = nodeDao
				.getSumOfChildCRCsForEachParent(parentIds);
		Map<Long, Long> indexCRCs = indexDao
				.getSumOfChildCRCsForEachParent(parentIds);
		HashSet<Long> parentsOutOfSynch = new HashSet<Long>();
		// Find the mismatches
		for (Long parentId : parentIds) {
			Long truthCRC = truthCRCs.get(parentId);
			Long indexCRC = indexCRCs.get(parentId);
			/*
			 * If the parent is in the trash then it should not exist in the
			 * replica.
			 */
			if (trashedParents.contains(parentId)) {
				if (indexCRC != null) {
					parentsOutOfSynch.add(parentId);
				}
				continue;
			}
			/*
			 * If the truth CRC is null then the indexCRC must also be null.
			 */
			if (truthCRC == null) {
				if (indexCRC != null) {
					parentsOutOfSynch.add(parentId);
				}
				continue;
			}
			/*
			 * The parent is not in the trash and the truth CRC is not null. The
			 * index CRC must match the truth.
			 */
			if (!truthCRC.equals(indexCRC)) {
				parentsOutOfSynch.add(parentId);
			}
		}
		return parentsOutOfSynch;
	}
}
