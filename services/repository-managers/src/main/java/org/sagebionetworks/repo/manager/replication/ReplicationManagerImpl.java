package org.sagebionetworks.repo.manager.replication;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeUtils;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;

@Service
public class ReplicationManagerImpl implements ReplicationManager {

	public static final int MAX_ANNOTATION_CHARS = 500;

	private ConnectionFactory connectionFactory;

	private MetadataIndexProviderFactory metadataIndexProviderFactory;

	@Autowired
	public ReplicationManagerImpl(ConnectionFactory connectionFactory,
			MetadataIndexProviderFactory metadataIndexProviderFactory) {
		this.connectionFactory = connectionFactory;
		this.metadataIndexProviderFactory = metadataIndexProviderFactory;
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
			
			List<ObjectDataDTO> objectData = provider.getObjectData(groupData.getCreateOrUpdateIds(), MAX_ANNOTATION_CHARS);
			
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

	Map<ViewObjectType, ReplicationDataGroup> groupByObjectType(List<ChangeMessage> messages) {
		Map<ViewObjectType, ReplicationDataGroup> data = new HashMap<>();

		for (ChangeMessage message : messages) {
			// Skip messages that do not map to a view object type
			ViewScopeUtils.map(message.getObjectType()).ifPresent(viewObjectType -> {
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
	 * Validate the given list of DTOs
	 * 
	 * @param indexDaos
	 * @throws RecoverableMessageException
	 */
	public static void validateEntityDtos(List<ObjectDataDTO> dtos) throws RecoverableMessageException {
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

	/**
	 * Replicate the given
	 * 
	 * @param indexDao
	 * @param entityDTOs DTO to be created/updated
	 * @param ids        All of the ids to be created/updated/deleted.
	 */
	void replicateInIndex(TableIndexDAO indexDao, ViewObjectType objectType, List<ObjectDataDTO> entityDTOs, List<Long> ids) {
		indexDao.executeInWriteTransaction((TransactionStatus status) -> {
			indexDao.deleteObjectData(objectType, ids);
			indexDao.addObjectData(objectType, entityDTOs);
			return null;
		});
	}
}
