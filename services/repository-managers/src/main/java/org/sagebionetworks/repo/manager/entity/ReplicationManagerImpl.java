package org.sagebionetworks.repo.manager.entity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.EntityDTO;
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
	
	@Autowired
	NodeDAO nodeDao;

	@Autowired
	ConnectionFactory connectionFactory;
	
	/**
	 * Replicate the data for the provided entities.
	 * 
	 * @param progressCallback
	 * @param messages
	 */
	@Override
	public void  replicate(List<ChangeMessage> messages) throws RecoverableMessageException {
		// batch the create/update events and delete events
		List<String> createOrUpdateIds = new LinkedList<>();
		List<String> deleteIds = new LinkedList<String>();
		groupByChangeType(messages, createOrUpdateIds, deleteIds);
		final List<Long> allIds = new LinkedList<Long>();
		allIds.addAll(KeyFactory.stringToKey(createOrUpdateIds));
		allIds.addAll(KeyFactory.stringToKey(deleteIds));
		
		// Get a copy of the batch of data.
		final List<EntityDTO> entityDTOs = nodeDao.getEntityDTOs(createOrUpdateIds,
				MAX_ANNOTATION_CHARS);
		validateEntityDtos(entityDTOs);
		// Get the connections
		List<TableIndexDAO> indexDaos = connectionFactory.getAllConnections();
		// make all changes in an index as a transaction
		for(TableIndexDAO indexDao: indexDaos){
			// apply the change to this index.
			replicateInIndex(indexDao, entityDTOs, allIds);
		}
	}
	
	/**
	 * Group the given batch of change massages into deleteIds or
	 * createOrUpdateIds.
	 * 
	 * @param messages
	 * @param createOrUpdateIds
	 * @param deleteIds
	 */
	public static void groupByChangeType(List<ChangeMessage> messages,
			List<String> createOrUpdateIds, List<String> deleteIds) {
		for (ChangeMessage change : messages) {
			if (ObjectType.ENTITY.equals(change.getObjectType())) {
				if (ChangeType.DELETE.equals(change.getChangeType())) {
					// entity delete
					deleteIds.add(change.getObjectId());
				} else {
					// entity create or update.
					createOrUpdateIds.add(change.getObjectId());
				}
			}
		}
	}
	
	/**
	 * Validate the given list of DTOs
	 * @param indexDaos
	 * @throws RecoverableMessageException 
	 */
	public static void validateEntityDtos(List<EntityDTO> dtos) throws RecoverableMessageException{
		for(EntityDTO dto: dtos){
			// See PLFM-4497.
			if(dto.getBenefactorId() == null){
				if(dtos.size() > 1){
					throw new RecoverableMessageException("Null benefactor found for batch.  Will retry each entry in the batch.");
				}else{
					throw new IllegalArgumentException("Single null benefactor found for: "+dto.getId()+".  Will not retry.");
				}
			}
		}
	}

	/**
	 * Replicate a single entity.
	 * 
	 */
	@Override
	public void replicate(String entityId) {
		ValidateArgument.required(entityId, "EntityId");
		final List<EntityDTO> entityDTOs = nodeDao.getEntityDTOs(Collections.singletonList(entityId),
				MAX_ANNOTATION_CHARS);
		// Connect only to the index for this table
		
		final TableIndexDAO indexDao = connectionFactory.getConnection(IdAndVersion.parse(entityId));
		List<Long> ids = Collections.singletonList(KeyFactory.stringToKey(entityId));
		replicateInIndex(indexDao, entityDTOs, ids);
	}

	/**
	 * Replicate the given 
	 * @param indexDao
	 * @param entityDTOs DTO to be created/updated
	 * @param ids All of the ids to be created/updated/deleted.
	 */
	void replicateInIndex(final TableIndexDAO indexDao, final List<EntityDTO> entityDTOs, List<Long> ids) {
		indexDao.executeInWriteTransaction((TransactionStatus status) -> {
			// TODO should get the object type in input
			indexDao.deleteObjectData(ObjectType.ENTITY, ids);
			indexDao.addObjectData(ObjectType.ENTITY, entityDTOs);
			return null;
		});
	}
}
