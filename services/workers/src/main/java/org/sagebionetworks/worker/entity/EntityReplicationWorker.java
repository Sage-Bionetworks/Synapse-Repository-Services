package org.sagebionetworks.worker.entity;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.database.semaphore.LockReleaseFailedException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.amazonaws.AmazonServiceException;

/**
 * This worker listens to entity change events and replicates the changes to the
 * index database. The replicated data supports both entity views and entity queries.
 * 
 * @author John
 *
 */
public class EntityReplicationWorker implements BatchChangeMessageDrivenRunner {

	public static final int MAX_ANNOTATION_CHARS = 500;
	public static final long THROTTLE_FREQUENCY_MS = 1000*30;
	
	static private Logger log = LogManager.getLogger(EntityReplicationWorker.class);

	@Autowired
	NodeDAO nodeDao;

	@Autowired
	ConnectionFactory connectionFactory;
	
	@Autowired
	WorkerLogger workerLogger;

	@Override
	public void run(ProgressCallback progressCallback,
			List<ChangeMessage> messages) throws RecoverableMessageException,
			Exception {
		try {
			replicate(progressCallback, messages);
		} catch (RecoverableMessageException
				| LockReleaseFailedException
				| CannotAcquireLockException
				| DeadlockLoserDataAccessException
				| AmazonServiceException e) {
			handleRecoverableException(e);
		} catch (Exception e) {
			boolean willRetry = false;
			workerLogger.logWorkerFailure(
					EntityReplicationWorker.class.getName(), e, willRetry);
			log.error("Failed while replicating:", e);
		}
	}
	
	/**
	 * Handle a Recoverable exception.
	 * @param exception
	 * @throws RecoverableMessageException
	 */
	private void handleRecoverableException(Exception exception) throws RecoverableMessageException{
		log.error("Failed while replicating. Will retry. Message: "+exception.getMessage());
		throw new RecoverableMessageException(exception);
	}

	/**
	 * Replicate the data for the provided entities.
	 * 
	 * @param progressCallback
	 * @param messages
	 */
	void replicate(final ProgressCallback progressCallback,
			List<ChangeMessage> messages) throws RecoverableMessageException {
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
			indexDao.createEntityReplicationTablesIfDoesNotExist();
			final TableIndexDAO indexDaoFinal = indexDao;
			indexDao.executeInWriteTransaction(new TransactionCallback<Void>() {

				@Override
				public Void doInTransaction(TransactionStatus status) {
					// clear everything.
					indexDaoFinal.deleteEntityData(progressCallback, allIds);
					indexDaoFinal.addEntityData(progressCallback, entityDTOs);
					return null;
				}
			});
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

}
