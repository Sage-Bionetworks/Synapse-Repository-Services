package org.sagebionetworks.repo.manager.replication;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.metadata.ObjectDataProvider;
import org.sagebionetworks.repo.manager.table.metadata.ObjectDataProviderFactory;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;

@Service
public class ReplicationManagerImpl implements ReplicationManager {

	public static final int MAX_ANNOTATION_CHARS = 500;

	/**
	 * Each container can only be re-synchronized at this frequency.
	 */
	public static final long SYNCHRONIZATION_FEQUENCY_MS = 1000 * 60 * 1000; // 1000 minutes.

	private ConnectionFactory connectionFactory;

	private ObjectDataProviderFactory objectDataProviderFactory;

	private TableManagerSupport tableManagerSupport;

	private ReplicationMessageManager replicationMessageManager;

	private Clock clock;
	
	final private TableIndexConnectionFactory indexConnectionFactory;

	@Autowired
	public ReplicationManagerImpl(ConnectionFactory connectionFactory,
			ObjectDataProviderFactory objectDataProviderFactory, 
			TableManagerSupport tableManagerSupport,
			ReplicationMessageManager replicationMessageManager, 
			Clock clock,
			TableIndexConnectionFactory indexConnectionFactory) {
		this.connectionFactory = connectionFactory;
		this.objectDataProviderFactory = objectDataProviderFactory;
		this.tableManagerSupport = tableManagerSupport;
		this.replicationMessageManager = replicationMessageManager;
		this.clock = clock;
		this.indexConnectionFactory = indexConnectionFactory;
	}

	/**
	 * Replicate the data for the provided entities.
	 * 
	 * @param progressCallback
	 * @param messages
	 */
	@Override
	public void replicate(List<ChangeMessage> messages) throws RecoverableMessageException {

		Map<ReplicationType, ReplicationDataGroup> data = groupByObjectType(messages);
		
		for (ReplicationDataGroup group : data.values()) {
			
			updateReplicationTables(group);
		}

	}

	/**
	 * Update the replication tables within a single transaction that removes rows to be deleted
	 * and creates or updates rows from the provided group.
	 * 
	 * @param replicationType
	 * @param toDelete
	 * @param objectData
	 */
	void updateReplicationTables(ReplicationDataGroup group) {
		TableIndexManager indexManager = indexConnectionFactory.connectToFirstIndex();
		
		indexManager.deleteObjectData(group.getObjectType(), group.getToDeleteIds());
		
		ObjectDataProvider provider = objectDataProviderFactory.getObjectDataProvider(group.getObjectType());
		Iterator<ObjectDataDTO> objectData = provider.getObjectData(group.getCreateOrUpdateIds(),
				MAX_ANNOTATION_CHARS);
		
		indexManager.updateObjectReplication(group.getObjectType(), objectData);
	}

	/**
	 * Replicate a single entity.
	 * 
	 */
	@Override
	public void replicate(ReplicationType replicationType, String objectId) {
		ValidateArgument.required(replicationType, "objectType");
		ValidateArgument.required(objectId, "objectId");
		
		ReplicationDataGroup group = new ReplicationDataGroup(replicationType);
		group.addForCreateOrUpdate(KeyFactory.stringToKey(objectId));
		 
		updateReplicationTables(group);
	}

	@Override
	public void reconcile(IdAndVersion idAndVersion) {
		ValidateArgument.required(idAndVersion, "idAndVersion");

		// Gather the scope type
		ViewScopeType viewScopeType = tableManagerSupport.getViewScopeType(idAndVersion);
		ViewObjectType objectType = viewScopeType.getObjectType();

		Set<Long> containerIds = tableManagerSupport.getAllContainerIdsForReconciliation(idAndVersion);

		if (containerIds.isEmpty()) {
			// nothing to do.
			return;
		}

		// get a connection to an index database.
		TableIndexDAO indexDao = connectionFactory.getConnection(idAndVersion);

		// Determine which of the given container IDs have expired.
		List<Long> expiredContainerIds = indexDao.getExpiredContainerIds(objectType.getMainType(), new ArrayList<>(containerIds));

		if (expiredContainerIds.isEmpty()) {
			// nothing to do.
			return;
		}

		ObjectDataProvider provider = objectDataProviderFactory.getObjectDataProvider(objectType.getMainType());

		// Determine which parents are in the trash
		Set<Long> trashedParents = getTrashedContainers(expiredContainerIds, provider);

		// Find all children deltas for the expired containers.
		findChildrenDeltas(indexDao, provider, expiredContainerIds, trashedParents);

		// re-set the expiration for all containers that were synchronized.
		long newExpirationDateMs = clock.currentTimeMillis() + SYNCHRONIZATION_FEQUENCY_MS;
		indexDao.setContainerSynchronizationExpiration(objectType.getMainType(), expiredContainerIds, newExpirationDateMs);
	}
	
	Map<ReplicationType, ReplicationDataGroup> groupByObjectType(List<ChangeMessage> messages) {
		Map<ReplicationType, ReplicationDataGroup> data = new HashMap<>();

		for (ChangeMessage message : messages) {
			// Skip messages that do not map to a view object type
			ReplicationType.matchType(message.getObjectType()).ifPresent(r->{
				ReplicationDataGroup group = data.computeIfAbsent(r, ReplicationDataGroup::new);

				Long id = KeyFactory.stringToKey(message.getObjectId());

				if (ChangeType.DELETE.equals(message.getChangeType())) {
					group.addForDelete(id);
				} else {
					group.addForCreateOrUpdate(id);
				}
			});
		}

		return data;
	}

	/**
	 * Get the sub-set of containerIds that are in the trash.
	 * 
	 * @param containerIds
	 * @return
	 */
	Set<Long> getTrashedContainers(List<Long> containerIds, ObjectDataProvider provider) {
		// find the sub-set that is available.
		Set<Long> availableIds = provider.getAvailableContainers(containerIds);
		Set<Long> inTrash = new HashSet<Long>();
		for (Long id : containerIds) {
			if (!availableIds.contains(id)) {
				inTrash.add(id);
			}
		}
		return inTrash;
	}

	/**
	 * Find the children deltas for the parents that are out-of-synch.
	 * 
	 * @param progressCallback
	 * @param parentIds
	 * @throws JSONObjectAdapterException
	 */
	void findChildrenDeltas(TableIndexDAO indexDao, ObjectDataProvider provider, List<Long> parentIds,
			Set<Long> trashedParents) {
		// Find the parents out-of-synch.
		Set<Long> outOfSynchParentIds = compareCheckSums(indexDao, provider, parentIds, trashedParents);

		for (Long outOfSynchParentId : outOfSynchParentIds) {
			boolean isParentInTrash = trashedParents.contains(outOfSynchParentId);
			List<ChangeMessage> childChanges = findChangesForParentId(indexDao, provider, outOfSynchParentId,
					isParentInTrash);
			replicationMessageManager.pushChangeMessagesToReplicationQueue(childChanges);
		}
	}

	/**
	 * Create the changes for a parentId that is out of Synch.
	 * 
	 * @param firstIndex
	 * @param outOfSynchParentId
	 * @param isParentInTrash
	 * @return
	 */
	List<ChangeMessage> findChangesForParentId(TableIndexDAO firstIndex, ObjectDataProvider provider,
			Long outOfSynchParentId, boolean isParentInTrash) {

		ReplicationType replicationType = provider.getReplicationType();

		List<ChangeMessage> changes = new LinkedList<>();

		Set<IdAndEtag> replicaChildren = new LinkedHashSet<>(
				firstIndex.getObjectChildren(replicationType, outOfSynchParentId));

		if (!isParentInTrash) {
			// The parent is not in the trash so find entities that are
			// out-of-synch
			List<IdAndEtag> truthChildren = provider.getChildren(outOfSynchParentId);
			Set<Long> truthIds = new HashSet<Long>();
			// find the create/updates
			for (IdAndEtag test : truthChildren) {
				if (!replicaChildren.contains(test)) {
					changes.add(createChange(replicationType.getObjectType(), test.getId(), ChangeType.UPDATE));
				}
				truthIds.add(test.getId());
			}
			// find the deletes
			for (IdAndEtag test : replicaChildren) {
				if (!truthIds.contains(test.getId())) {
					changes.add(createChange(replicationType.getObjectType(), test.getId(), ChangeType.DELETE));
				}
			}
		} else {
			// the parent is the the trash so setup the delete of any children
			// that appear in the replica.
			for (IdAndEtag toDelete : replicaChildren) {
				changes.add(createChange(replicationType.getObjectType(), toDelete.getId(), ChangeType.DELETE));
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
	ChangeMessage createChange(ObjectType objectType, Long id, ChangeType type) {
		ChangeMessage message = new ChangeMessage();
		message.setChangeNumber(1L);
		message.setChangeType(type);
		message.setObjectId(id.toString());
		message.setObjectType(objectType);
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
	Set<Long> compareCheckSums(TableIndexDAO indexDao, ObjectDataProvider provider, List<Long> parentIds,
			Set<Long> trashedParents) {
		Map<Long, Long> truthCRCs = provider.getSumOfChildCRCsForEachContainer(parentIds);
		Map<Long, Long> indexCRCs = indexDao.getSumOfChildCRCsForEachParent(provider.getReplicationType(), parentIds);
		HashSet<Long> parentsOutOfSynch = new HashSet<Long>();
		// Find the mismatches
		for (Long parentId : parentIds) {
			Long truthCRC = truthCRCs.get(parentId);
			Long indexCRC = indexCRCs.get(parentId);
			/*
			 * If the parent is in the trash then it should not exist in the replica.
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
			 * The parent is not in the trash and the truth CRC is not null. The index CRC
			 * must match the truth.
			 */
			if (!truthCRC.equals(indexCRC)) {
				parentsOutOfSynch.add(parentId);
			}
		}
		return parentsOutOfSynch;
	}

	/**
	 * Replicate the given
	 * 
	 * @param indexDao
	 * @param entityDTOs DTO to be created/updated
	 * @param ids        All of the ids to be created/updated/deleted.
	 */
	void replicateInIndex(TableIndexDAO indexDao, ReplicationType objectType, List<ObjectDataDTO> entityDTOs,
			List<Long> ids) {
		indexDao.executeInWriteTransaction((TransactionStatus status) -> {
			indexDao.deleteObjectData(objectType, ids);
			indexDao.addObjectData(objectType, entityDTOs);
			return null;
		});
	}

}
