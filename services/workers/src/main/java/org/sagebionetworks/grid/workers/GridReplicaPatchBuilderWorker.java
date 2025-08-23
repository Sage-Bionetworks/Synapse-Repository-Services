package org.sagebionetworks.grid.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.GridReplicaPatchBuilderManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangeSerializable;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangeSet;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

/**
 * Patches for a replica must be created in series to prevent data loss from
 * conflicting patches. Messages are grouped by 'sessionId-replicaId' to ensure
 * the FIFO queue triggers the creation of all patches for the same replica
 * sequentially.
 */
@Service
public class GridReplicaPatchBuilderWorker implements MessageDrivenRunner {

	private static final Logger log = LogManager.getLogger(GridReplicaPatchBuilderWorker.class);

	private final GridReplicaPatchBuilderManager manager;

	public GridReplicaPatchBuilderWorker(GridReplicaPatchBuilderManager manager) {
		super();
		this.manager = manager;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		try {
			log.info("Changeset: {}", message.getBody());
			IntendedChangeSet changeSet = IntendedChangeSerializable.deserialize(new JSONObject(message.getBody()));
			manager.buildPatch(changeSet);
		} catch (RecoverableMessageException e) {
			log.error("Will retry.  Message: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Failed to process changeset, message: {}", e.getMessage(), e);
		}
	}

}
