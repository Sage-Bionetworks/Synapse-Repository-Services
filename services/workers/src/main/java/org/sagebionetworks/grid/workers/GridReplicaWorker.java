package org.sagebionetworks.grid.workers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.InternalMessageDispatcher;
import org.sagebionetworks.repo.manager.grid.internal.replica.JsonRxMessageBundle;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.Message;

@Service
public class GridReplicaWorker implements MessageDrivenRunner {

	private static final Logger log = LogManager.getLogger(GridReplicaWorker.class);

	private final GridManager gridManager;
	private final InternalMessageDispatcher dispatcher;

	public GridReplicaWorker(GridManager gridManager, InternalMessageDispatcher dispatcher) {
		super();
		this.gridManager = gridManager;
		this.dispatcher = dispatcher;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		ValidateArgument.required(message, "message");
		ValidateArgument.required(message.getMessageAttributes(), "message.messageAttributes");
		MessageAttributeValue conIdValue = message.getMessageAttributes().get("ConnectionId");
		ValidateArgument.required(conIdValue, "message.messageAttributes.get(ConnectionId)");
		String connectionId = conIdValue.getStringValue();
		String body = message.getBody();
		try {
			Optional<GridConnectionInfo> conOption = gridManager.getConnectionInfoOptional(connectionId);
			if (conOption.isEmpty()) {
				log.info("No active connection found for connectionId: {}, this message will be ignored.",
						connectionId);
				return;
			}
			GridConnectionInfo connection = conOption.get();
			log.info("New message for connectionId: {}, body: {}", connectionId, body);
			dispatcher.dispatchMessage(new JsonRxMessageBundle(new JsonRxMessage(body), connection, progressCallback));
		} catch (RecoverableMessageException e) {
			log.info("Will retry message for connectionId: {}, body: {}", connectionId, body);
			throw e;
		} catch (LockUnavilableException e) {
			log.info("Will retry message for connectionId: {}, body: {}", connectionId, body);
			throw new RecoverableMessageException(e);
		} catch (Exception e) {
			log.error(String.format("Failed to process message for connectionId: %s, body: %s", connectionId, body), e);
		}
	}

	@Override
	public List<String> getMessageAttributeNames() {
		return Collections.singletonList(".*");
	}

}
