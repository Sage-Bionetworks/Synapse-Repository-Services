package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.grid.db.MessageChain;
import org.sagebionetworks.repo.manager.grid.ReplicaLockKey;
import org.sagebionetworks.repo.manager.grid.response.InternalReplicaToHubEventPublisher;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.semaphore.WriteLockRequest;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphore;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
public class GridReplicaManagerImpl implements GridReplicaManager {

	private final GridIndexManager gridIndexManager;
	private final WriteReadSemaphore writeReadSemaphore;
	private final InternalReplicaToHubEventPublisher publisher;
	private final SnsClient snsClient;
	private final String topicArn;

	public GridReplicaManagerImpl(GridIndexManager gridIndexManager, WriteReadSemaphore writeReadSemaphore,
			InternalReplicaToHubEventPublisher publisher, SnsClient snsClient, String gridReplicaChangeTopicArn) {
		this.gridIndexManager = gridIndexManager;
		this.writeReadSemaphore = writeReadSemaphore;
		this.publisher = publisher;
		this.snsClient = snsClient;
		this.topicArn = gridReplicaChangeTopicArn;

	}

	void synchronizeClock(ProgressCallback callback, GridConnectionInfo connection) {
		String context = "startSychronizeClock-" + connection.getConnectionId();
		writeReadSemaphore.tryRunWithWriteLock(new WriteLockRequest(callback, context, connection.getConnectionId()),
				(p) -> {
					MessageChain chain = gridIndexManager.startMessageChain(connection.getSessionId(),
							connection.getReplicaId(), SYNCHRONIZE_CLOCK);
					List<LogicalTimestamp> clock = gridIndexManager.getClock(connection.getSessionId(),
							connection.getReplicaId());
					sendClockMessage(chain.getId(), connection.getConnectionId(), clock);
					return null;
				});
	}

	void sendClockMessage(Integer methodId, String connectionId, List<LogicalTimestamp> clock) {
		publisher.publishEvent(new EventContext(EventType.MESSAGE, EventSource.INTERNAL, connectionId),
				new JsonRxMessage(JsonRxMessageType.RequestData).setMethod(SYNCHRONIZE_CLOCK).setId(methodId)
						.setBody(LogicalTimestampCompactSerializable.serializeClock(clock)));
	}

	@Override
	public void onResponseComplete(ProgressCallback callback, GridConnectionInfo connection, Integer methodId) {
		gridIndexManager.getMessageChain(connection.getSessionId(), connection.getReplicaId(), methodId)
				.ifPresent(chain -> {
					if ("patch".equals(chain.getMethod())) {
						// the patch builder saved a new patch
						synchronizeClock(callback, connection);
					}
				});
		gridIndexManager.completeMessageChain(connection.getSessionId(), connection.getReplicaId(), methodId);
	}

	@Override
	public void onApplyPatch(ProgressCallback callback, GridConnectionInfo connection, Integer messageId, Patch patch) {
		String context = "onApplyPatch-" + connection.getConnectionId();
		writeReadSemaphore.tryRunWithWriteLock(new WriteLockRequest(callback, context,
				new ReplicaLockKey(connection.getSessionId(), connection.getReplicaId()).getKey()), (p) -> {
					Map<IndexType, Set<LogicalTimestamp>> changes = gridIndexManager
							.applyPatch(connection.getSessionId(), connection.getReplicaId(), patch);
					List<LogicalTimestamp> clock = gridIndexManager.getClock(connection.getSessionId(),
							connection.getReplicaId());
					sendClockMessage(messageId, connection.getConnectionId(), clock);
					sendChangesToTopic(connection, patch.getPatchId(), changes);
					return null;
				});
	}

	void sendChangesToTopic(GridConnectionInfo connection, LogicalTimestamp patchId,
			Map<IndexType, Set<LogicalTimestamp>> changes) {
		snsClient.publish(PublishRequest.builder().targetArn(topicArn)
				.message(new ReplicaChangeSet(connection, patchId, changes).toJson()).build());
	}

	@Override
	public void onConnected(ProgressCallback callback, GridConnectionInfo connection) {
		synchronizeClock(callback, connection);
	}

	@Override
	public void onNewPatch(ProgressCallback callback, GridConnectionInfo connection) {
		synchronizeClock(callback, connection);
	}
}
