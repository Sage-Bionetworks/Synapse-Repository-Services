package org.sagebionetworks.table.worker;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.model.EntityDTO;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker replicates entity data from the truth database to the
 * table's index database.  The worker listens to entity change
 * events and replicates they changes to the index database.
 * 
 * EntityView tables are built from the replicated data in the index database.
 * 
 * @author John
 *
 */
public class EntityReplicationWorker implements BatchChangeMessageDrivenRunner {
	
	public static final int MAX_ANNOTATION_CHARS = 500;
	
	@Autowired
	NodeDAO nodeDao;
	
	@Autowired
	TableIndexConnectionFactory connectionFactory;

	@Override
	public void run(ProgressCallback<Void> progressCallback,
			List<ChangeMessage> messages) throws RecoverableMessageException,
			Exception {
		
		// batch the create/update events and delete events
		List<String> createOrUpdateIds = new LinkedList<>();
		List<String> deleteIds = new LinkedList<String>();
		groupByChangeType(messages, createOrUpdateIds, deleteIds);
		progressCallback.progressMade(null);
		// Get a copy of the batch of data.
		List<EntityDTO> entityDTOs = nodeDao.getEntityDTOs(createOrUpdateIds, MAX_ANNOTATION_CHARS);
		
	}

	/**
	 * Group the given batch of change massages into deleteIds or createOrUpdateIds.
	 * 
	 * @param messages
	 * @param createOrUpdateIds
	 * @param deleteIds
	 */
	void groupByChangeType(List<ChangeMessage> messages,
			List<String> createOrUpdateIds, List<String> deleteIds) {
		for(ChangeMessage change: messages){
			if(ObjectType.ENTITY.equals(change.getObjectType())){
				if(ChangeType.DELETE.equals(change.getChangeType())){
					// entity delete
					deleteIds.add(change.getObjectId());
				}else{
					// entity create or update.
					createOrUpdateIds.add(change.getObjectId());
				}
			}
		}
	}

}
