package org.sagebionetworks.grid.workers;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.grid.internal.replica.ReplicaChangeSet;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridReplicaValidationManager;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

@Service
public class GridReplicaValidationWorker implements MessageDrivenRunner {

	private static final Logger log = LogManager.getLogger(GridReplicaValidationWorker.class);

	private final GridReplicaValidationManager manager;

	public GridReplicaValidationWorker(GridReplicaValidationManager manager) {
		this.manager = manager;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		ReplicaChangeSet changeSet = new ReplicaChangeSet(MessageUtils.extractMessageBodyAsJSONObject(message));
		if (changeSet.getChanges() == null) {
			log.info("Null changeset");
			return;
		}
		Set<LogicalTimestamp> changedVectorIds = changeSet.getChanges().get(IndexType.vec);
		if (changedVectorIds == null || changedVectorIds.isEmpty()) {
			log.info("Null or empty changed vector IDs");
			return;
		}
		try {
			log.info("New changeSet: {}", StringUtils.truncate(changeSet.toJson(), 200));
			manager.validateChanges(changeSet.getSessionId(), changeSet.getReplicaId(),	changedVectorIds);
		} catch (RecoverableMessageException e) {
			log.info("Recoverable message: '{}' changeSet: {}", e.getMessage(), changeSet);
			throw e;
		} catch (LockUnavilableException e) {
			log.info("LockUnavilable message: '{}' changeSet: {}", e.getMessage(), changeSet);
			throw new RecoverableMessageException(e);
		} catch (Exception e) {
			log.error("New message: {}", changeSet, e);
		}
	}

}
