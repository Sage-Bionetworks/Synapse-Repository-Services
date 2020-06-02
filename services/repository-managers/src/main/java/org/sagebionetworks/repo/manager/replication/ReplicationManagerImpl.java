package org.sagebionetworks.repo.manager.replication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
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

	private MetadataIndexProviderFactory metadataIndexProviderFactory;

	private TableManagerSupport tableManagerSupport;

	private ReplicationMessageManager replicationMessageManager;

	private Clock clock;

	@Autowired
	public ReplicationManagerImpl(ConnectionFactory connectionFactory,
			MetadataIndexProviderFactory metadataIndexProviderFactory, 
			TableManagerSupport tableManagerSupport,
			ReplicationMessageManager replicationMessageManager, 
			Clock clock) {
		this.connectionFactory = connectionFactory;
		this.metadataIndexProviderFactory = metadataIndexProviderFactory;
		this.tableManagerSupport = tableManagerSupport;
		this.replicationMessageManager = replicationMessageManager;
		this.clock = clock;
	}

	/**
	 * Replicate the data for the provided entities.
	 * 
	 * @param progressCallback
	 * @param messages
	 */
	@Override
	public void replicate(List<ChangeMessage> messages) throws RecoverableMessageException {

		Map<ViewObjectType, ReplicationDataGroup> data = groupByObjectType(messages);

		for (Entry<ViewObjectType, ReplicationDataGroup> groupEntry : data.entrySet()) {

			ViewObjectType objectType = groupEntry.getKey();
			ReplicationDataGroup groupData = groupEntry.getValue();

			MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(objectType);

			List<ObjectDataDTO> objectData = provider.getObjectData(groupData.getCreateOrUpdateIds(),
					MAX_ANNOTATION_CHARS);

			validateEntityDtos(objectData);

			// Get the connections
			List<TableIndexDAO> indexDaos = connectionFactory.getAllConnections();

			// make all changes in an index as a transaction
			for (TableIndexDAO indexDao : indexDaos) {
				// apply the change to this index.
				replicateInIndex(indexDao, objectType, objectData, groupData.getAllIds());
			}

		}

	}

	/**
	 * Replicate a single entity.
	 * 
	 */
	@Override
	public void replicate(ViewObjectType objectType, String objectId) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(objectId, "objectId");

		MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(objectType);

		Long id = KeyFactory.stringToKey(objectId);

		List<Long> ids = Collections.singletonList(id);

		List<ObjectDataDTO> objectDTOs = provider.getObjectData(ids, MAX_ANNOTATION_CHARS);

		// Connect only to the index for this table

		TableIndexDAO indexDao = connectionFactory.getConnection(IdAndVersion.parse(objectId));

		replicateInIndex(indexDao, objectType, objectDTOs, ids);
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
		List<Long> expiredContainerIds = indexDao.getExpiredContainerIds(objectType, new ArrayList<>(containerIds));

		if (expiredContainerIds.isEmpty()) {
			// nothing to do.
			return;
		}

		MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(objectType);

		// Determine which parents are in the trash
		Set<Long> trashedParents = getTrashedContainers(expiredContainerIds, provider);

		// Find all children deltas for the expired containers.
		findChildrenDeltas(indexDao, provider, expiredContainerIds, trashedParents);

		// re-set the expiration for all containers that were synchronized.
		long newExpirationDateMs = clock.currentTimeMillis() + SYNCHRONIZATION_FEQUENCY_MS;
		indexDao.setContainerSynchronizationExpiration(objectType, expiredContainerIds, newExpirationDateMs);
	}
	
	Map<ViewObjectType, ReplicationDataGroup> groupByObjectType(List<ChangeMessage> messages) {
		Map<ViewObjectType, ReplicationDataGroup> data = new HashMap<>();

		for (ChangeMessage message : messages) {
			// Skip messages that do not map to a view object type
			ViewObjectType.map(message.getObjectType()).ifPresent(viewObjectType -> {
				ReplicationDataGroup group = data.computeIfAbsent(viewObjectType, ReplicationDataGroup::new);

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
	Set<Long> getTrashedContainers(List<Long> containerIds, MetadataIndexProvider provider) {
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
	void findChildrenDeltas(TableIndexDAO indexDao, MetadataIndexProvider provider, List<Long> parentIds,
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
	List<ChangeMessage> findChangesForParentId(TableIndexDAO firstIndex, MetadataIndexProvider provider,
			Long outOfSynchParentId, boolean isParentInTrash) {

		ViewObjectType viewObjectType = provider.getObjectType();

		List<ChangeMessage> changes = new LinkedList<>();

		Set<IdAndEtag> replicaChildren = new LinkedHashSet<>(
				firstIndex.getObjectChildren(viewObjectType, outOfSynchParentId));

		if (!isParentInTrash) {
			// The parent is not in the trash so find entities that are
			// out-of-synch
			List<IdAndEtag> truthChildren = provider.getChildren(outOfSynchParentId);
			Set<Long> truthIds = new HashSet<Long>();
			// find the create/updates
			for (IdAndEtag test : truthChildren) {
				if (!replicaChildren.contains(test)) {
					changes.add(createChange(viewObjectType.getObjectType(), test.getId(), ChangeType.UPDATE));
				}
				truthIds.add(test.getId());
			}
			// find the deletes
			for (IdAndEtag test : replicaChildren) {
				if (!truthIds.contains(test.getId())) {
					changes.add(createChange(viewObjectType.getObjectType(), test.getId(), ChangeType.DELETE));
				}
			}
		} else {
			// the parent is the the trash so setup the delete of any children
			// that appear in the replica.
			for (IdAndEtag toDelete : replicaChildren) {
				changes.add(createChange(viewObjectType.getObjectType(), toDelete.getId(), ChangeType.DELETE));
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
	Set<Long> compareCheckSums(TableIndexDAO indexDao, MetadataIndexProvider provider, List<Long> parentIds,
			Set<Long> trashedParents) {
		Map<Long, Long> truthCRCs = provider.getSumOfChildCRCsForEachContainer(parentIds);
		Map<Long, Long> indexCRCs = indexDao.getSumOfChildCRCsForEachParent(provider.getObjectType(), parentIds);
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
	void replicateInIndex(TableIndexDAO indexDao, ViewObjectType objectType, List<ObjectDataDTO> entityDTOs,
			List<Long> ids) {
		indexDao.executeInWriteTransaction((TransactionStatus status) -> {
			indexDao.deleteObjectData(objectType, ids);
			indexDao.addObjectData(objectType, entityDTOs);
			return null;
		});
	}
	
	/**
	 * Validate the given list of DTOs
	 * 
	 * @param indexDaos
	 * @throws RecoverableMessageException
	 */
	static void validateEntityDtos(List<ObjectDataDTO> dtos) throws RecoverableMessageException {
		for (ObjectDataDTO dto : dtos) {
			// See PLFM-4497.
			if (dto.getBenefactorId() == null) {
				if (dtos.size() > 1) {
					throw new RecoverableMessageException(
							"Null benefactor found for batch.  Will retry each entry in the batch.");
				} else {
					throw new IllegalArgumentException(
							"Single null benefactor found for: " + dto.getId() + ".  Will not retry.");
				}
			}
		}
	}
}
